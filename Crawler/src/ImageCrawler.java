import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.awt.*;
import java.io.IOException;

import static com.mongodb.client.model.Updates.set;


public class ImageCrawler {

    MongoDatabase mongoDatabase;
    MongoCollection<Document> mongoUrlCollection;
    MongoCollection<Document> mongoImageCollection;
    MongoCollection<Document> imageCollectionIndex;

    void run() {
        MongoClient mongo = new MongoClient("localhost", 27017);
        mongoDatabase = mongo.getDatabase("SearchEngine");
        mongoUrlCollection = mongoDatabase.getCollection("Crawler");
        mongoImageCollection = mongoDatabase.getCollection("Images");
        imageCollectionIndex = mongoDatabase.getCollection("ImagesIndexCount");
        long index = imageCollectionIndex.find().first().getLong("Index");
        long count = mongoUrlCollection.count();
        MongoCursor<Document> allDocuments = mongoUrlCollection.find().iterator();
        for (long i = index; i <= count; i++) {
            String url = (String) allDocuments.next().get("URL");
            org.jsoup.nodes.Document jsoupDocument = null;
            try {
                jsoupDocument = Jsoup.connect(url).get();
            } catch (IOException e) {
                Toolkit.getDefaultToolkit().beep();
                e.printStackTrace();
            }
            Elements images = jsoupDocument.select("img");
            for (Element image : images) {
                Document imageDocument = new Document();
                boolean valid = image.hasAttr("src")
                        && !image.attr("abs:src").equals("")
                        && image.hasAttr("alt")
                        && !image.attr("alt").equals("");
                if (valid) {
                    imageDocument.append("src", image.attr("abs:src"))
                            .append("Content", image.attr("alt"));
                    mongoImageCollection.insertOne(imageDocument);
                }
            }
            updateIndex(i);
            System.out.println("Finished " + i + " out of " + mongoUrlCollection.count());
        }
        mongo.close();
    }

    private void updateIndex(long i) {
        Document Index = imageCollectionIndex.find().first();
        imageCollectionIndex.updateOne(Index, set("Index", i));
    }
}
