package crawler;

//Java imports
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
//j soup imports
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.bson.Document;
// MongoDB imports
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.*;

public class Crawler implements Runnable {
    private static MongoClient mongo;
    private static MongoDatabase mongoDatabase;
    private static MongoCollection<Document> mongoUrlCollection;
    private static MongoCollection<Document> mongoImageCollection;
    
	Crawler() { }
    
	@Override
	public void run() {
		try {
			Crawl(Thread.currentThread().getName());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws InterruptedException, IOException {
		mongo = new MongoClient("localhost", 27017);
		mongoDatabase = mongo.getDatabase("SearchEngine");
		mongoUrlCollection = mongoDatabase.getCollection("Crawler");
		mongoImageCollection = mongoDatabase.getCollection("Images");
		Thread t1 = new Thread(new Crawler());
		Thread t2 = new Thread(new Crawler());
		Thread t3 = new Thread(new Crawler());
		Thread t4 = new Thread(new Crawler());
		Thread t5 = new Thread(new Crawler());
		Thread t6 = new Thread(new Crawler());
		Thread t7 = new Thread(new Crawler());
		Thread t8 = new Thread(new Crawler());
		Thread t9 = new Thread(new Crawler());
		Thread t10 = new Thread(new Crawler());
		Thread t11 = new Thread(new Crawler());
		Thread t12 = new Thread(new Crawler());
		Thread t13 = new Thread(new Crawler());
		Thread t14 = new Thread(new Crawler());
		Thread t15 = new Thread(new Crawler());
		Thread t16 = new Thread(new Crawler());
		Thread t17 = new Thread(new Crawler());
		Thread t18 = new Thread(new Crawler());
		Thread t19 = new Thread(new Crawler());
		t1.setName("https://sports.yahoo.com/");
		t2.setName("https://www.espn.com/");
		t3.setName("https://www.bleacherreport.com/");
		t4.setName("https://www.cbssports.com/");
		t5.setName("https://www.si.com/");
		t6.setName("https://www.nbcsports.com/");
		t7.setName("https://www.hulu.com/welcome");
		t8.setName("https://www.hbo.com/");
		t9.setName("https://www.primevideo.com/");
		t10.setName("https://mubi.com");
		t11.setName("https://www.disneyplus.com");
		t12.setName("https://www.udemy.com/");
		t13.setName("https://www.coursera.org/");
		t14.setName("https://www.edx.org/");
		t15.setName("https://www.khanacademy.org/");
		t16.setName("https://www.udacity.com/");
		t17.setName("https://www.canvas.net/");
		t18.setName("https://www.futurelearn.com/");
		t19.setName("https://www.kadenze.com/");
		long startTime = System.currentTimeMillis();
		t1.start(); t2.start(); t3.start(); t4.start(); t5.start(); t6.start(); t7.start(); t8.start(); t9.start();
		t10.start(); t11.start(); t12.start(); t13.start(); t14.start(); t15.start(); t16.start(); t17.start(); t18.start(); t19.start();
		t1.join(); t2.join(); t3.join(); t4.join(); t5.join(); t6.join(); t7.join(); t8.join(); t9.join(); t10.join(); t11.join();
		t12.join(); t13.join(); t14.join(); t15.join(); t16.join(); t17.join(); t18.join(); t19.join();
		long endTime = System.currentTimeMillis();
		System.out.println("Crawling Process took " + ((endTime - startTime) * 1.6666667 / 100000) + " minutes");
		startTime = System.currentTimeMillis();
		fillEmptyFields();
		endTime = System.currentTimeMillis();
		System.out.println("Filling Empty Fields (OtherFormsofURL & OutLinks) Process took " + ((endTime - startTime) * (1.6666667 / 100000)) + " minutes");
		startTime = System.currentTimeMillis();
		handleInLinks();
		endTime = System.currentTimeMillis();
		System.out.println("Handling InLinks Process took " + ((endTime - startTime) * (1.6666667 / 100000)) + " minutes");
		startTime = System.currentTimeMillis();
		fillEmptyInLinks();
		endTime = System.currentTimeMillis();
		System.out.println("Handling Empty InLinks Process took " + ((endTime - startTime) * (1.6666667 / 100000)) + " minutes");
		startTime = System.currentTimeMillis();
		fillEmptyFields();
		endTime = System.currentTimeMillis();
		System.out.println("Filling Empty Fields (OtherFormsofURL & OutLinks) Process took " + ((endTime - startTime) * (1.6666667 / 100000)) + " minutes");
		mongo.close();
	}
	
	//robots.txt check
	public static boolean isAllowed(String url) throws IOException {
		if(!urlValid(url)) {
			return false;
		}
		URL obj = new URL(url);
		String Robot = obj.getProtocol() + "://" + obj.getHost() + "/robots.txt";
		org.jsoup.nodes.Document document = Jsoup.connect(Robot).get();
		String urlBody = document.body().text();
		int startIndex = urlBody.indexOf("User-agent: *");
		int stopIndex = urlBody.indexOf("User-agent:", startIndex + 13);
		//Check if no (User-agent: *) exists in which case is us.
		if(startIndex == -1) {
			return true;
		}
		//Check if website denies access to all its contents
		int denyAll = urlBody.indexOf("Disallow: / ", startIndex + 13);
		if(denyAll != -1) {
			if(stopIndex != -1) {
				if(denyAll < stopIndex) {
					return false;
				}
			}
			else {
				return false;
			}
		}
		if(obj.getPath().equals("") || obj.getPath().equals("/")) {
			return true;
		}
		//Check if the given URL is valid to be crawled
		int isValid = urlBody.indexOf("Disallow: " + obj.getPath() + " ", startIndex + 13);
		if(isValid != -1) {
			if(stopIndex != -1) {
				if(isValid < stopIndex) {
					return false;
				}
			}
			else {
				return false;
			}
		}
		return true;
	}
	
	//checks if current page (before crawling) if it exists in DB
	public static boolean isStored(String url, String content) {
		  BasicDBObject urlQuery = new BasicDBObject("URL", url);
	      BasicDBObject contentQuery = new BasicDBObject("Content", content);
	      FindIterable<Document> urlCursor = mongoUrlCollection.find(urlQuery);
	      FindIterable<Document> contentCursor = mongoUrlCollection.find(contentQuery);
	      if(urlCursor.cursor().hasNext() || contentCursor.cursor().hasNext()) {
	    	  BasicDBObject newDocument = new BasicDBObject("OtherFormsOfURL", url);
              BasicDBObject updateObject = new BasicDBObject("$push", newDocument);
              mongoUrlCollection.updateOne(contentQuery, updateObject);
	    	  return true;
	      }
	      return false;
	}
	
	//just additional function to check if url is valid
	public static boolean urlValid(String url) 
    { 
        try { 
            new URL(url).toURI(); 
            return true; 
        } 
        catch (Exception e) { 
            return false; 
        } 
    }
	
	public static boolean isHtml(String url) 
    {
        try { 
        	Jsoup.connect(url).get();
            return true; 
        } 
        catch (Exception e) { 
            return false; 
        } 
    }
	
	public static Date getDate(org.jsoup.nodes.Document urlDocument) {
		try {
		    if(urlDocument.select("time").first() == null) {
		    	return null;
		    }
		    else {
		    	String datetimeAttribute = urlDocument.select("time").first().attributes().get("datetime");
		    	String urlDate = datetimeAttribute.substring(0, 10);
		    	Date date= new SimpleDateFormat("yyyy-MM-dd").parse(urlDate);
		    	return date;
		    }
		}
		catch (Exception e) { 
            return null; 
        }
	}
	
	public static void handleInLinks() {
		for(long k = 0; k < mongoUrlCollection.countDocuments(); k++) {
			System.out.println("skipped " + k);
			MongoCursor<Document> allDocuments = mongoUrlCollection.find().skip((int) k).cursor();
			MongoCursor<Document> allDocuments_1 = mongoUrlCollection.find().skip((int) k).cursor();
			MongoCursor<Document> allDocuments_2 = mongoUrlCollection.find().skip((int) k).cursor();
			@SuppressWarnings("unchecked")
			ArrayList<String> outLinks = (ArrayList<String>) allDocuments.next().get("OutLinks");
			String content = (String) allDocuments_1.next().get("Content");
			String url = (String) allDocuments_2.next().get("URL");
			if(outLinks == null || outLinks.size() == 0) {
				continue;
			}
			else {
				for(int i = 0; i < outLinks.size(); i++) {
					boolean foundURL = mongoUrlCollection.find(and(in("OtherFormsOfURL", outLinks.get(i)), eq("Content", content))).cursor().hasNext();
					boolean foundOtherURL = mongoUrlCollection.find(and(eq("URL", outLinks.get(i)), eq("Content", content))).cursor().hasNext();
					boolean checkInserted = mongoUrlCollection.find(and(or(eq("URL", outLinks.get(i)), in("OtherFormsofURL", outLinks.get(i))), in("InLinks", url))).cursor().hasNext();
					if(foundURL || foundOtherURL || checkInserted) {
						BasicDBObject contentObject = new BasicDBObject("Content", content);
					    BasicDBObject outLinkObject = new BasicDBObject("OutLinks", outLinks.get(i));
					    BasicDBObject removeOutLink = new BasicDBObject("$pull", outLinkObject);
					    mongoUrlCollection.findOneAndUpdate(contentObject, removeOutLink);
					}
					else {
						boolean findURLfromURL = mongoUrlCollection.find(eq("URL", outLinks.get(i))).cursor().hasNext();
						boolean findURLFromOthers = mongoUrlCollection.find(in("OtherFormsOfURL", outLinks.get(i))).cursor().hasNext();
						if(findURLfromURL) {
							BasicDBObject urlObject = new BasicDBObject("URL", outLinks.get(i));
						    BasicDBObject inLinkObject = new BasicDBObject("InLinks", url);
						    BasicDBObject pushInLink = new BasicDBObject("$push", inLinkObject);
						    mongoUrlCollection.findOneAndUpdate(urlObject, pushInLink);
						}
						else if(findURLFromOthers) {
							BasicDBObject inLinkObject = new BasicDBObject("InLinks", url);
						    BasicDBObject pushInLink = new BasicDBObject("$push", inLinkObject);
						    mongoUrlCollection.findOneAndUpdate(in("OtherFormsOfURL", outLinks.get(i)), pushInLink);
						}
					}
				}
			}
		}
	}
	
	public static void fillEmptyFields() {
		//OtherFormsOfURL Handler
		BasicDBObject othersQuery = new BasicDBObject("OtherFormsOfURL", new BasicDBObject("$exists", false));
		MongoCursor<Document> otherForms = mongoUrlCollection.find(othersQuery).cursor();
		try {
			while (otherForms.hasNext()) {
				BasicDBObject contentQuery = new BasicDBObject();
			    contentQuery.put("Content", otherForms.next().get("Content"));
				BasicDBObject newDocument = new BasicDBObject("OtherFormsOfURL", null);
	            BasicDBObject updateObject = new BasicDBObject("$set", newDocument);
	            mongoUrlCollection.updateOne(contentQuery, updateObject);
			}
		} finally {
			otherForms.close();
		}
		//OutLinks Handler
		BasicDBObject outLinkQuery = new BasicDBObject("OutLinks", new BasicDBObject("$exists", false));
		MongoCursor<Document> outLinks = mongoUrlCollection.find(outLinkQuery).cursor();
		try {
			while (outLinks.hasNext()) {
				BasicDBObject contentQuery = new BasicDBObject();
			    contentQuery.put("Content", outLinks.next().get("Content"));
				BasicDBObject newDocument = new BasicDBObject();
	            newDocument.put("OutLinks", null);
	            BasicDBObject updateObject = new BasicDBObject();
	            updateObject.put("$set", newDocument);
	            mongoUrlCollection.updateOne(contentQuery, updateObject);
			}
		} finally {
			outLinks.close();
		}
	}
	//InLinks Handler
	public static void fillEmptyInLinks() {
		//OtherFormsOfURL Handler
		BasicDBObject inLinksQuery = new BasicDBObject("InLinks", new BasicDBObject("$exists", false));
		MongoCursor<Document> inLinks = mongoUrlCollection.find(inLinksQuery).cursor();
		try {
			while (inLinks.hasNext()) {
				BasicDBObject contentQuery = new BasicDBObject("Content", inLinks.next().get("Content"));
				BasicDBObject newDocument = new BasicDBObject("InLinks", null);
	            BasicDBObject updateObject = new BasicDBObject("$set", newDocument);
	            mongoUrlCollection.updateOne(contentQuery, updateObject);
			}
		} finally {
			inLinks.close();
		}
	}
	
	//main crawler function
	public static void Crawl(String url) throws IOException {
    	if (mongoUrlCollection.countDocuments() > 5500 || !isAllowed(url) || !urlValid(url)) {
    		return;
    	}
		try {
            org.jsoup.nodes.Document jsoupDocument = Jsoup.connect(url).get();
    		if (!isStored(url, jsoupDocument.body().text())) {
    			Elements linksOnPage = jsoupDocument.select("a[href]");
            	Document mongoDocument = new Document("URL", url)
				.append("Content", jsoupDocument.body().text())
				.append("Title", jsoupDocument.title())
				.append("Date", getDate(jsoupDocument));
                mongoUrlCollection.insertOne(mongoDocument);
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
                BasicDBObject urlQuery = new BasicDBObject();
                urlQuery.put("URL" , url);
                BasicDBObject newDocument = new BasicDBObject();
                if(linksOnPage.eachAttr("abs:href").isEmpty()) {
                    newDocument.put("OutLinks", null);
                }
                else {
                	Set<String> links = new HashSet<String>(linksOnPage.eachAttr("abs:href"));
                	newDocument.put("OutLinks", links);
                }
                BasicDBObject updateObject = new BasicDBObject();
      			updateObject.put("$set", newDocument);
      			mongoUrlCollection.updateOne(urlQuery, updateObject);
                for (Element page : linksOnPage) {
                    Crawl(page.attr("abs:href"));
                }
            }
    	}
		catch(NullPointerException e) {
    		System.err.println("For '" + url + "': " + e.getMessage());
    		return;
    	}
		catch (IOException e) {
            System.err.println("For '" + url + "': " + e.getMessage());
            return;
        }
    }
}
