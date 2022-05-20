package indexer;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.bson.Document;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Consumer;
import java.lang.Math; 

import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

public class Indexer {

	private static MongoClient mongo;
    private static MongoDatabase mongoDatabase;
    private static MongoCollection<Document> mongoUrlCollection;
    private static MongoCollection<Document> mongoIndexerCollection;
    
	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws FileNotFoundException {
		mongo = new MongoClient("localhost", 27017);
		mongoDatabase = mongo.getDatabase("SearchEngine");
		mongoUrlCollection = mongoDatabase.getCollection("Crawler");
		mongoIndexerCollection = mongoDatabase.getCollection("Indexer");
		SnowballStemmer snowballStemmer = new englishStemmer();
		ArrayList<String> stopwordsList = loadStopwords();
		ArrayList<Document> urlDocuments = new ArrayList<Document>();
		ArrayList<String> url = new ArrayList<String>();
		//ArrayList<String> imageUrl = new ArrayList<String>();
		ArrayList<String> titles = new ArrayList<String>();
		ArrayList<Date> datePublished = new ArrayList<Date>();
		ArrayList<String> content = new ArrayList<String>();
		MongoCursor<Document> cursor = mongoUrlCollection.find().iterator();
		try {
		    while (cursor.hasNext()) {
		        urlDocuments.add(cursor.next());
		    }
		} finally {
		    cursor.close();
		}
		for(Document document : urlDocuments) {
			url.add((String) document.get("URL"));
			//imageUrl.add((String) document.get("ImageURL"));
			content.add((String) document.get("Content"));
			titles.add((String) document.get("Title"));
			datePublished.add((Date) document.get("Date"));
		}
		for(int i = 0; i < url.size(); i++) {
			String urlContent = content.get(i);
			urlContent = urlContent.replaceAll("[^a-zA-Z0-9]", " ");
			urlContent = urlContent.replaceAll("   ", " ");
			urlContent = urlContent.replaceAll("  ", " ");
			String[] urlContents = urlContent.split(" ");
			ArrayList<String> contentsList = new ArrayList<String>();
			for(int k = 0; k < urlContents.length; k++) {
				contentsList.add(urlContents[k].toLowerCase());
			}
			contentsList.removeAll(stopwordsList);
			for (int j = 0; j < contentsList.size(); j++) {
				snowballStemmer.setCurrent(contentsList.get(j));
				snowballStemmer.stem();
				contentsList.set(j, snowballStemmer.getCurrent());
			}
			Set<String> uniqueWords = new HashSet<String>(contentsList);
			uniqueWords.remove("");
			uniqueWords.remove(" ");
			for(String word:uniqueWords) {
				BasicDBObject wordQuery = new BasicDBObject("Word", word);
			    FindIterable<Document> findWord = mongoIndexerCollection.find(wordQuery);
			    if (findWord.cursor().hasNext()) {
			    	BasicDBObject newObject = new BasicDBObject();
			    	newObject.append("Website", url.get(i))
			    	.append("Title", titles.get(i))
		        	.append("Date", datePublished.get(i))
		        	.append("Count", Collections.frequency(contentsList, word))
		        	.append("TotalCount", contentsList.size())
		        	.append("TF-IDF", ((double) Collections.frequency(contentsList, word) / (double) contentsList.size()));
			    	BasicDBObject newEntry = new BasicDBObject("Information", newObject);
			    	BasicDBObject updateWord = new BasicDBObject("$push", newEntry);
			    	mongoIndexerCollection.updateOne(wordQuery, updateWord);
			    }
			    else {
			    	Document newDocument = new Document("Word", word);
			    	mongoIndexerCollection.insertOne(newDocument);
			    	BasicDBObject newWordQuery = new BasicDBObject("Word", word);
			    	BasicDBObject newObject = new BasicDBObject();
			    	newObject.append("Website", url.get(i))
			    	.append("Title", titles.get(i))
		        	.append("Date", datePublished.get(i))
		        	.append("Count", Collections.frequency(contentsList, word))
		        	.append("TotalCount", contentsList.size())
		        	.append("TF-IDF", ((double) Collections.frequency(contentsList, word) / (double) contentsList.size()));
			    	BasicDBObject newEntry = new BasicDBObject("Information", newObject);
			    	BasicDBObject updateWord = new BasicDBObject("$push", newEntry);
			    	mongoIndexerCollection.updateOne(newWordQuery, updateWord);
			    }
			}
		}
		long totalNumberDocuments = mongoUrlCollection.countDocuments();
		BasicDBObject x = new BasicDBObject("$exists", false);
		BasicDBObject y = new BasicDBObject("IDF", x);
		MongoCursor<Document> allDocuments = mongoIndexerCollection.find(y).cursor();
		MongoCursor<Document> allDocuments_1 = mongoIndexerCollection.find(y).cursor();
		try {
			while(allDocuments.hasNext()) {
				String word = (String) allDocuments.next().get("Word");
				@SuppressWarnings("unchecked")
				int documentsNumberContainingWord = ((ArrayList<BasicDBObject>) allDocuments_1.next().get("Information")).size();
				double IDF = Math.log10((double) totalNumberDocuments / (double) documentsNumberContainingWord);
				BasicDBObject wordObject = new BasicDBObject("Word", word);
				BasicDBObject idfObject = new BasicDBObject("IDF", IDF);
				BasicDBObject updateWord = new BasicDBObject("$set", idfObject);
		    	mongoIndexerCollection.updateOne(wordObject, updateWord);
			}
		}
		finally {
			allDocuments_1.close();
			allDocuments.close();
		}
		ArrayList<Document> wordDocuments = new ArrayList<Document>();
		cursor = mongoIndexerCollection.find().iterator();
		try {
		    while (cursor.hasNext()) {
		    	wordDocuments.add(cursor.next());
		    }
		} finally {
		    cursor.close();
		}
		mongoIndexerCollection.find().forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                double IDF = document.getDouble("IDF");
                @SuppressWarnings("unchecked")
				ArrayList<Document> info = (ArrayList<Document>) document.get("Information");
                info.forEach(new Consumer<Document>() {
                    @Override
                    public void accept(Document document1) {
                        double TFIDF;
                        try {
                            TFIDF = (double) document1.get("TF-IDF");
                        } catch (ClassCastException e) {
                            TFIDF = 1.0d;
                        }
                        TFIDF *= IDF;
                        document1.put("TF-IDF", TFIDF);
                    }
                });
                BasicDBObject wordObject = new BasicDBObject("Word", document.getString("Word"));
                BasicDBObject infoObject = new BasicDBObject("Information", info);
                BasicDBObject setObject = new BasicDBObject("$set", infoObject);
                mongoIndexerCollection.findOneAndUpdate(wordObject, setObject);
            }
        });
		mongo.close();
	}
	
	public static ArrayList<String> loadStopwords() throws FileNotFoundException {
		Scanner scanner = new Scanner(new File("src/StopWords.txt"));
		ArrayList<String> list = new ArrayList<String>();
		while (scanner.hasNext()){
		    list.add(scanner.next().toLowerCase());
		}
		scanner.close();
		return list;
	}
}
