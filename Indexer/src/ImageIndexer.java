
import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.function.Consumer;

public class ImageIndexer {

    private static MongoClient mongo;
    private static MongoDatabase mongoDatabase;
    private static MongoCollection<Document> mongoUrlCollection;
    private static MongoCollection<Document> mongoIndexerCollection;

    public static void run() {
        mongo = new MongoClient("localhost", 27017);
        mongoDatabase = mongo.getDatabase("SearchEngine");
        mongoUrlCollection = mongoDatabase.getCollection("Images");
        mongoIndexerCollection = mongoDatabase.getCollection("ImagesIndexer");
        SnowballStemmer snowballStemmer = new englishStemmer();
        ArrayList<String> stopwordsList = loadStopwords();
        ArrayList<Document> urlDocuments = new ArrayList<Document>();
        ArrayList<String> url = new ArrayList<String>();
        ArrayList<String> content = new ArrayList<String>();

        mongoUrlCollection.find().forEach(new Block<Document>() {
            int i = 0;

            @Override
            public void apply(Document document) {
                urlDocuments.add(document);
                System.out.println(i);
                i++;
            }
        });

        for (Document document : urlDocuments) {
            url.add((String) document.get("src"));
            content.add((String) document.get("Content"));
        }

        long size = url.size();
        for (int i = 0; i < size; i++) {
            String urlContent = content.get(i);
            urlContent = urlContent.replaceAll("[^a-zA-Z0-9]", " ");
            urlContent = urlContent.replaceAll("\\s+", " ");
            String[] urlContents = urlContent.split(" ");
            ArrayList<String> contentsList = new ArrayList<String>();
            for (int k = 0; k < urlContents.length; k++) {
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
            for (String word : uniqueWords) {
                BasicDBObject wordQuery = new BasicDBObject("Word", word);
                FindIterable<Document> findWord = mongoIndexerCollection.find(wordQuery);
                if (findWord.iterator().hasNext()) {
                    BasicDBObject newObject = new BasicDBObject();
                    newObject.append("Website", url.get(i))
                            .append("Count", Collections.frequency(contentsList, word))
                            .append("TotalCount", contentsList.size())
                            .append("TF-IDF", ((double) Collections.frequency(contentsList, word) / (double) contentsList.size()));
                    BasicDBObject newEntry = new BasicDBObject("Information", newObject);
                    BasicDBObject updateWord = new BasicDBObject("$push", newEntry);
                    mongoIndexerCollection.updateOne(wordQuery, updateWord);
                } else {
                    Document newDocument = new Document("Word", word);
                    mongoIndexerCollection.insertOne(newDocument);
                    BasicDBObject newWordQuery = new BasicDBObject("Word", word);
                    BasicDBObject newObject = new BasicDBObject();
                    newObject.append("Website", url.get(i))
                            .append("Count", Collections.frequency(contentsList, word))
                            .append("TotalCount", contentsList.size())
                            .append("TF-IDF", ((double) Collections.frequency(contentsList, word) / (double) contentsList.size()));
                    BasicDBObject newEntry = new BasicDBObject("Information", newObject);
                    BasicDBObject updateWord = new BasicDBObject("$push", newEntry);
                    mongoIndexerCollection.updateOne(newWordQuery, updateWord);
                }
            }
            System.out.println("Finished " + i + " out of " + size);
        }

        //// IDF
        long totalNumberDocuments = mongoUrlCollection.count();
        for (int j = 0; j < mongoIndexerCollection.count(); j++) {
            MongoCursor<Document> allDocuments = mongoIndexerCollection.find().skip(j).iterator();
            MongoCursor<Document> allDocuments_1 = mongoIndexerCollection.find().skip(j).iterator();
            String word = (String) allDocuments.next().get("Word");
            @SuppressWarnings("unchecked")
            int documentsNumberContainingWord = ((ArrayList<BasicDBObject>) allDocuments_1.next().get("Information")).size();
            double IDF = Math.log10((double) totalNumberDocuments / (double) documentsNumberContainingWord);
            BasicDBObject wordObject = new BasicDBObject("Word", word);
            BasicDBObject idfObject = new BasicDBObject("IDF", IDF);
            BasicDBObject updateWord = new BasicDBObject("$set", idfObject);
            mongoIndexerCollection.updateOne(wordObject, updateWord);
        }

        mongo.close();
    }

    public static ArrayList<String> loadStopwords() {
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("src/StopWords.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        ArrayList<String> list = new ArrayList<String>();
        while (scanner.hasNext()) {
            list.add(scanner.next().toLowerCase());
        }
        scanner.close();
        return list;
    }

    public static void tfidf() {

        mongo = new MongoClient("localhost", 27017);
        mongoDatabase = mongo.getDatabase("SearchEngine");
        mongoIndexerCollection = mongoDatabase.getCollection("ImagesIndexer");

        mongoIndexerCollection.find().forEach(new Block<Document>() {
            int i = 0;
            @Override
            public void apply(Document document) {
                double idf = document.getDouble("IDF");
                ArrayList<Document> info = (ArrayList<Document>) document.get("Information");
                info.forEach(new Consumer<Document>() {
                    @Override
                    public void accept(Document document1) {
                        double TfIdf = document1.getDouble("TF-IDF");
                        TfIdf *= idf;
                        document1.put("TF-IDF", TfIdf);
                    }
                });
                BasicDBObject x = new BasicDBObject("Word", document.getString("Word"));
                BasicDBObject y = new BasicDBObject("Information", info);
                BasicDBObject z = new BasicDBObject("$set", y);
                mongoIndexerCollection.findOneAndUpdate(x, z);
                System.out.println(i);
                i++;
            }
        });
    }

}
