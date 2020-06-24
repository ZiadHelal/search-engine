import com.mongodb.BasicDBObject;
import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.util.ArrayList;
import java.util.function.Consumer;

import static com.mongodb.client.model.Updates.set;

public class TextIndexer {
    private static MongoClient mongo;
    private static MongoDatabase mongoDatabase;
    private static MongoCollection<Document> IndexerCollection;
    private static MongoCollection<Document> IndexerIndex;

    public static void tfidf() {

        mongo = new MongoClient("localhost", 27017);
        mongoDatabase = mongo.getDatabase("SearchEngine");
        IndexerCollection = mongoDatabase.getCollection("Indexer");
        IndexerIndex = mongoDatabase.getCollection("TextIndexerCount");
        long index = IndexerIndex.find().first().getLong("Index");
        long count = IndexerCollection.count();

        long startTime = System.currentTimeMillis();
        IndexerCollection.find().skip((int) index).forEach(new Block<Document>() {
            long i = index;

            @Override
            public void apply(Document document) {
                double idf = document.getDouble("IDF");
                ArrayList<Document> info = (ArrayList<Document>) document.get("Information");
                info.forEach(new Consumer<Document>() {
                    @Override
                    public void accept(Document document1) {
                        double TfIdf;
                        try {
                            TfIdf = (double) document1.get("TF-IDF");
                        } catch (ClassCastException e) {
                            TfIdf = 1.0d;
                            System.out.println("Converted to double: " + TfIdf);
                        }
                        TfIdf *= idf;
                        document1.put("TF-IDF", TfIdf);
                    }
                });
                BasicDBObject x = new BasicDBObject("Word", document.getString("Word"));
                BasicDBObject y = new BasicDBObject("Information", info);
                BasicDBObject z = new BasicDBObject("$set", y);
                IndexerCollection.findOneAndUpdate(x, z);
                i++;
                updateIndex(i);
                System.out.println("Finished " + i + " out of " + count);
            }
        });

//        IndexerCollection.find().skip((int) index).limit(10).forEach(new Block<Document>() {
//            long i = index;
//
//            @Override
//            public void apply(Document document) {
//                double idf = document.getDouble("IDF");
//                ArrayList<Document> info = (ArrayList<Document>) document.get("Information");
//                info.forEach(new Consumer<Document>() {
//                    @Override
//                    public void accept(Document document1) {
//                        double TfIdf = document1.getDouble("TF-IDF");
//                        TfIdf *= idf;
//                        document1.put("TF-IDF", TfIdf);
//                        BasicDBObject updatedObj = new BasicDBObject("Information.$.TF-IDF", TfIdf);
//                        BasicDBObject setObj = new BasicDBObject("$set", updatedObj);
//                        String website = document1.getString("Website");
//                        BasicDBObject websiteObj = new BasicDBObject("Information.Website", website);
//                        BasicDBObject wordObj = new BasicDBObject("Word", document.getString("Word"));
//                        IndexerCollection.updateOne(and(wordObj, websiteObj), setObj);
//                    }
//                });
//                i++;
//                updateIndex(i);
//                System.out.println("Finished " + i + " out of " + count);
//            }
//        });

//        IndexerCollection.find().skip((int) index).limit(10).forEach(new Block<Document>() {
//            long i = index;
//
//            @Override
//            public void apply(Document document) {
//                double idf = document.getDouble("IDF");
//                ArrayList<Document> info = (ArrayList<Document>) document.get("Information");
//                info.forEach(new Consumer<Document>() {
//                    @Override
//                    public void accept(Document document1) {
//                        double TfIdf = document1.getDouble("TF-IDF");
//                        TfIdf *= idf;
//                        document1.put("TF-IDF", TfIdf);
//                        BasicDBObject updatedObj = new BasicDBObject("Information.$.TF-IDF", TfIdf);
//                        BasicDBObject setObj = new BasicDBObject("$set", updatedObj);
//                        IndexerCollection.updateOne(document, setObj);
//                    }
//                });
//                i++;
//                updateIndex(i);
//                System.out.println("Finished " + i + " out of " + count);
//            }
//        });

        long duration = (System.currentTimeMillis() - startTime) * 1000;
        System.out.println("Duration = " + duration);
        System.out.println("\n");
    }

    private static void updateIndex(long i) {
        Document Index = IndexerIndex.find().first();
        IndexerIndex.updateOne(Index, set("Index", i));
    }
}
