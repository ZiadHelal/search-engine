import com.mongodb.Block;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.*;
import org.bson.Document;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

import static com.mongodb.client.model.Updates.set;


public class SearchEngine extends HttpServlet {

    private final int NUMBER_SNIPPET_CHARACTERS = 100;
    private final double GEO_MULTIPLIER = 3;
    private final double RECENT_MULTIPLIER = 2;
    private final double PERSONALIZED_MULTIPLIER = 4;
    private final double PHRASE_MULTIPLIER = 5;
    private final double EXACT_PHRASE_MULTIPLIER = 5;
    private final int MAX_NUM_OF_RESULTS = 200;
    private final int NUMBER_PERSONALIZED_ENTRIES = 5;
    private final int RESULTS_PER_PAGE = 10;

    ArrayList<String> StopWords;
    private MongoDatabase Database;
    private MongoCollection<Document> WordsCollection;
    private MongoCollection<Document> ImagesCollection;
    private MongoCollection<Document> Websites;
    private MongoCollection<Document> Results;
    private MongoCollection<Document>[] TempResults;

    private String[] Personalized;
    private String UnEditedSearchQuery;
    boolean online = false;
    private boolean testing = false;
    private boolean showResults = true;
    boolean ImageSearch;
    boolean PhraseSearch;
    private String Locale = "";

    String url = "mongodb://ziadabdo98:hardpassword@cluster0-shard-00-00-uz5c4.mongodb.net:27017,cluster0-shard-00-01-uz5c4.mongodb.net:27017,cluster0-shard-00-02-uz5c4.mongodb.net:27017/test?ssl=true&replicaSet=Cluster0-shard-0&authSource=admin&w=majority";

    Block<Document> printBlock = new Block<Document>() {
        @Override
        public void apply(final Document document) {
            System.out.println(document.toJson());
        }
    };

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        resp.setContentType("application/json;charset=UTF-8");

        //check if this is a new query or just updating pages
        if (req.getParameter("NewQuery").equals("0")) {
            initializeDb();
            int page = Integer.parseInt(req.getParameter("Page")) - 1;
            Document responseDoc = new Document("Results", Results.find().sort(Sorts.descending("Relevance"))
                    .skip(page * RESULTS_PER_PAGE).limit(RESULTS_PER_PAGE));
            String response = responseDoc.toJson();
            try {
                resp.getWriter().println(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return;
        }

        //apply query processor
        String[] arr = queryProcessor(req.getParameter("Query"));
        if (arr.length == 0) {
            // if array length is zero means all query words were removed during stop word removing
            Document responseDoc = new Document("Status", 0);
            String response = responseDoc.toJson();
            try {
                resp.getWriter().println(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            //you don't need to initialize the database until this point
            initializeDb();

            //get the locale from request and set it
            Locale = req.getParameter("Locale");

            //get personalized sites from the req and set it
            String personalized = req.getParameter("Personalized");
            setPersonalized(personalized.split(","));

            //get image search
            String imageSearch = req.getParameter("ImageSearch");
            ImageSearch = imageSearch.equals("1");

            //get phrase search
            String phraseSearch = req.getParameter("PhraseSearch");
            PhraseSearch = phraseSearch.equals("1");

            //then search the database for the query
            search(arr);

            String response;

            //return status 0 if no results returned
            if (Results.count() == 0) {
                response = new Document("Status", 0).toJson();
            } else {
                Document responseDoc = new Document("Status", 1);
                responseDoc.append("Number of Results", (int) Results.count());
                responseDoc.append("Results", Results.find()
                        .sort(Sorts.descending("Relevance"))
                        .limit(RESULTS_PER_PAGE)
                        .projection(Projections.exclude("Date", "Relevance")));
                response = responseDoc.toJson();
            }

            try {
                resp.getWriter().println(response);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    Document testResponse(String[] arr) {
        String print = Arrays.toString(arr);
        Document responseDoc = new Document("Status", 1);
        responseDoc.append("Query", print);
        responseDoc.append("Locale", Locale);
        responseDoc.append("Personalized", Arrays.toString(Personalized));
        responseDoc.append("ImageSearch", ImageSearch);
        responseDoc.append("PhraseSearch", PhraseSearch);
        return responseDoc;
    }

    /**
     * Removes stop words then applies stemming and returns array of stemmed words
     *
     * @param searchQuery original search query from the android interface
     * @return array of stemmed words
     */
    public String[] queryProcessor(String searchQuery) {
        long startTime = System.currentTimeMillis();
        UnEditedSearchQuery = searchQuery.toLowerCase();
        String searchQueryProcessing = searchQuery;
        searchQueryProcessing = searchQueryProcessing.replaceAll("[^a-zA-Z0-9]", " ");
        searchQueryProcessing = searchQueryProcessing.replaceAll("\\s+", " ");
        String[] searchQueryArray = searchQueryProcessing.split(" ");
        for (int i = 0; i < searchQueryArray.length; i++) {
            searchQueryArray[i] = searchQueryArray[i].toLowerCase();
        }
        ArrayList<String> searchQueryWords = new ArrayList<String>(Arrays.asList(searchQueryArray));
        loadStopWords();
        searchQueryWords.removeAll(StopWords);
        SnowballStemmer snowballStemmer = new englishStemmer();
        for (int i = 0; i < searchQueryWords.size(); i++) {
            snowballStemmer.setCurrent(searchQueryWords.get(i));
            snowballStemmer.stem();
            searchQueryWords.set(i, snowballStemmer.getCurrent());
        }

        String[] str = new String[searchQueryWords.size()];

        // Convert ArrayList to object array
        Object[] objArr = searchQueryWords.toArray();

        // Iterating and converting to String
        int i = 0;
        for (Object obj : objArr) {
            str[i++] = (String) obj;
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Process Query Duration = " + duration);
        System.out.println("\n");
        return str;
    }

    /**
     * loads stop words from text file to Arraylist called StopWords
     */
    public void loadStopWords() {
        long startTime = System.currentTimeMillis();
        Scanner scanner = null;
        try {
            scanner = new Scanner(new File("D:\\uni stuff\\APT\\project\\Search Engine\\StopWords.txt"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StopWords = new ArrayList<String>();
        while (scanner.hasNext()) {
            StopWords.add(scanner.next());
        }
        scanner.close();
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Load Stop Words Duration = " + duration);
        System.out.println("\n");
    }

    /**
     * initialize database client and set the database global variables
     */
    void initializeDb() {
        long startTime = System.currentTimeMillis();
        MongoClient mongoClient;
        if (online) {
            MongoClientURI clientURI = new MongoClientURI(url);
            mongoClient = new MongoClient(clientURI);
        } else {
            mongoClient = new MongoClient();
        }
        Database = mongoClient.getDatabase("SearchEngine");
        WordsCollection = Database.getCollection("Indexer");
        ImagesCollection = Database.getCollection("ImagesIndexer");
        Websites = Database.getCollection("Crawler");
        Personalized = new String[NUMBER_PERSONALIZED_ENTRIES];
        System.out.println(Personalized.length);
        for (int i = 0; i < NUMBER_PERSONALIZED_ENTRIES; i++) {
            System.out.println(Personalized[i]);
        }
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Initialize Database Duration = " + duration);
        System.out.println("\n");
    }

    /**
     * Searches through the database for the query
     *
     * @param Query Array of Stemmed words to search for
     */
    public void search(String[] Query) {
        long startTime = System.currentTimeMillis();
        TempResults = new MongoCollection[Query.length];

        for (int i = 0; i < Query.length; i++) {        //make a new results collection for each word in the query
            makeResultsCollection(Query[i], i);
        }

        //merge all the results collections into the Results collection, and add Tf-Idf of duplicates
        mergeResults();

        //then refine and change the relevance of results based on the following (these are all website related)
//        applyPageRank();
        geoRank();
        personalizedRank();
        recentRank();

        //if not image search add snippets
        if (!ImageSearch) {
            putSnippet(Query);
        }

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Total Duration = " + duration);
        System.out.println("\n");

        //print the results
        Results.find().sort(Sorts.descending("Relevance")).projection(Projections.excludeId()).forEach(printBlock);
        System.out.println("Number of results: " + Results.count());
    }

    private void filterResults() {
        //loop on results and if the url doesn't contain
        Results.find(Filters.text("https. -.jpg -.png -.jpeg"))
                .projection(Projections.excludeId()).forEach(printBlock);
    }

    /**
     * Puts Snippets in the Results collection
     *
     * @param Query The Search query
     */
    private void putSnippet(String[] Query) {
        long startTime = System.currentTimeMillis();

        Results.updateMany(new Document(), set("Snippet", ""));
        Results.find().forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                String content = Websites.find(Filters.eq("URL", document.getString("_id")))
                        .first().getString("Content");
                int index = 0;
                //start by the first word in the query and if found make snippet from first word.
                for (String s : Query) {
                    index = content.toLowerCase().indexOf(s);
                    if (index != -1) break;
                }
                String Content = null;
                if (index != -1) {
                    try {
                        Content = content.substring(index, index + NUMBER_SNIPPET_CHARACTERS);
                    } catch (StringIndexOutOfBoundsException e) {
                        Content = content.substring(index);
                    }
                } else Content = "";

                Document update = new Document(document);
                update.put("Snippet", Content);
                Results.replaceOne(document, update);
            }
        });

        System.out.println("putSnippet Results");
        if (showResults)
            Results.find().projection(Projections.excludeId()).forEach(printBlock);
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("putSnippet Duration = " + duration);
        System.out.println("\n");
    }

    /**
     * Applies personalized rank to Results collection
     */
    private void personalizedRank() {
        long startTime = System.currentTimeMillis();
        Results.createIndex(Indexes.text("_id"));
        for (int i = 0; i < NUMBER_PERSONALIZED_ENTRIES; i++) {
            if (Personalized[i] == null) continue;
            Results.find(Filters.text("\"" + Personalized[i] + "\"")).forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {
                    double relevance = document.getDouble("Relevance");
                    Results.updateOne(document, set("Relevance", relevance * PERSONALIZED_MULTIPLIER));
                }
            });
        }
        System.out.println("personalizedRank Results");
        if (showResults)
            Results.find().projection(Projections.excludeId()).forEach(printBlock);
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("personalizedRank Duration = " + duration);
        System.out.println("\n");
    }

    /**
     * Sets the personalized array with the parameter array
     *
     * @param array array to set personalized with
     */
    void setPersonalized(String[] array) {
        Personalized = new String[NUMBER_PERSONALIZED_ENTRIES];
        for (int i = 0; (i < array.length) && (i < NUMBER_PERSONALIZED_ENTRIES); i++) {
            if (array[i].equals("null")) Personalized[i] = null;
            else Personalized[i] = array[i];
        }
        System.out.println(Personalized.length);
        for (int i = 0; i < NUMBER_PERSONALIZED_ENTRIES; i++) {
            System.out.println(Personalized[i]);
        }
    }

    /**
     * Applies pageRank to Results collection
     */
    private void applyPageRank() {
        long startTime = System.currentTimeMillis();
        Results.find().forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                String Website = document.getString("_id");
                double PageRank = Websites.find(new Document("Website", Website)).first().getDouble("PageRank");
                Results.updateOne(document, set("Relevance", document.getDouble("Relevance") * PageRank));
            }
        });

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("PageRank Results");
        if (showResults)
            Results.find().projection(Projections.excludeId()).forEach(printBlock);
        System.out.println("PageRank Duration = " + duration);
        System.out.println("\n");
    }

    /**
     * Makes temporary Results collection for the passed word
     *
     * @param Query word to make temporary results collection for
     * @param i     index of word from the Query array
     */
    private void makeResultsCollection(String Query, int i) {
        long startTime = System.currentTimeMillis();

        //make new results collection
        if (ImageSearch) {
            ImagesCollection.aggregate(
                    Arrays.asList(
                            Aggregates.match(Filters.eq("Word", Query)),
                            Aggregates.unwind("$Information"),
                            Aggregates.group("$Information.Website", Accumulators.first("Relevance", "$Information.TF-IDF"),
                                    Accumulators.first("ImageWebsite", "$Information.Website")),
                            Aggregates.sort(Sorts.descending("Relevance")),
                            Aggregates.limit(MAX_NUM_OF_RESULTS),
                            Aggregates.out("Results" + Integer.toString(i))
                    )).toCollection();
        } else {
            WordsCollection.aggregate(
                    Arrays.asList(
                            Aggregates.match(Filters.eq("Word", Query)),
                            Aggregates.unwind("$Information"),
                            Aggregates.group("$Information.Website", Accumulators.first("Relevance", "$Information.TF-IDF"),
                                    Accumulators.first("Title", "$Information.Title"),
                                    Accumulators.first("Date", "$Information.Date")),
                            Aggregates.sort(Sorts.descending("Relevance")),
                            Aggregates.limit(MAX_NUM_OF_RESULTS),
                            Aggregates.out("Results" + Integer.toString(i))
                    )).toCollection();
        }

        //make Results point to it
        TempResults[i] = Database.getCollection("Results" + Integer.toString(i));

        long duration = System.currentTimeMillis() - startTime;
        System.out.println("tempResults" + Integer.toString(i) + ": " + Query);
        if (showResults)
            TempResults[i].find().projection(Projections.excludeId()).forEach(printBlock);
        System.out.println("tempResults Duration = " + duration);
        System.out.println("\n");
    }

    /**
     * Merges temporary results collections into Results collection and handles phrase search for
     * websites that contain all words in the Query
     */
    private void mergeResults() {
        long startTime = System.currentTimeMillis();
        Results = Database.getCollection("Results");
        Results.drop();
        Results.createIndex(Indexes.text("_id"));
        Map<String, Integer> Recurring = new HashMap<>();
        for (int i = 0; i < TempResults.length; i++) {
            TempResults[i].find().forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {
                    try {
                        Results.insertOne(document);
                    } catch (com.mongodb.MongoWriteException E) {//exception if the same website contains multiple words from the query
                        Document ResultWithSameURL = Results.find(Filters.eq("_id", document.getString("_id"))).first();
                        ResultWithSameURL.put("Relevance", ResultWithSameURL.getDouble("Relevance") + document.getDouble("Relevance"));
                        Results.replaceOne(Results.find(new Document("_id", ResultWithSameURL.getString("_id"))).first(), ResultWithSameURL);
                        if (PhraseSearch) {
                            //increment the counter of the website if it has multiple words from the query
                            String url = document.getString("_id");
                            Recurring.merge(url, 1, Integer::sum);
                            if (Recurring.get(url) == TempResults.length - 1) {//if website contains all words of search query
                                Document website = Results.find(Filters.text(url)).first();
                                Results.updateOne(website, set("Relevance", website.getDouble("Relevance") * PHRASE_MULTIPLIER));
                                boolean hasExactPhrase = Websites.find(Filters.eq("URL", url)).first()
                                        .getString("Content").toLowerCase().contains(UnEditedSearchQuery);
                                if (hasExactPhrase) {
                                    Results.updateOne(website, set("Relevance", website.getDouble("Relevance") * EXACT_PHRASE_MULTIPLIER));
                                }

                            }
                        }
                    }
                }
            });
            TempResults[i].drop();
        }
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Merged Results");
        if (showResults)
            Results.find().projection(Projections.excludeId()).forEach(printBlock);
        System.out.println("Merged Duration = " + duration);
        System.out.println("\n");
    }

    public void setLocale(String locale) {
        this.Locale = locale;
    }

    /**
     * Applies geoRank to Results collection
     */
    private void geoRank() {
        if (Locale.equals("")) return;
        long startTime = System.currentTimeMillis();
        Results.createIndex(Indexes.text("_id"));
        Results.find(Filters.text(Locale)).forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                double relevance = document.getDouble("Relevance");
                Results.updateOne(document, set("Relevance", relevance * GEO_MULTIPLIER));
            }
        });
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("geoRank Results");
        if (showResults)
            Results.find().projection(Projections.excludeId()).forEach(printBlock);
        System.out.println("geoRank Duration = " + duration);
        System.out.println("\n");
    }

    /**
     * Applies recentRank to Results collection
     */
    private void recentRank() {
        long startTime = System.currentTimeMillis();
        Results.find().sort(Sorts.descending("Date")).limit(20).forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                if (document.getDate("Date") != null) {
                    double relevance = document.getDouble("Relevance");
                    Document update = new Document(document);
                    Results.updateOne(document, set("Relevance", relevance * RECENT_MULTIPLIER));
                }
            }
        });
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("recentRank Results");
        if (showResults)
            Results.find().projection(Projections.excludeId()).forEach(printBlock);
        System.out.println("recentRank Duration = " + duration);
        System.out.println("\n");
    }

    void pageRank() {
        long startTime = System.currentTimeMillis();

        //add the PageRank field to all websites, initialize it to 1/NumOfWebsites
        MongoCollection<Document> Websites = Database.getCollection("pgtest");
        long NumOfWebsites = Database.getCollection("pgtest").count();
        Websites.updateMany(new Document(), set("PageRank", (double) 1 / NumOfWebsites));
        Websites.updateMany(new Document(), set("TempPageRank", (double) 0));

        //perform the algorithm for k times
        for (int k = 0; k < 1; k++) {
            //make collection of website pageranks, initialize them with 1/n
            Websites.find().forEach(new Block<Document>() {
                @Override
                public void apply(Document document) {
                    //get the in Inbois
                    ArrayList<String> InLinks = (ArrayList<String>) document.get("InLinks");
                    if (InLinks == null || InLinks.size() == 0) {
                        Document update = new Document(document);
                        update.put("TempPageRank", document.getDouble("PageRank"));
                        Websites.replaceOne(document, update);
                    } else {
                        double PageRank = 0d;
                        for (int i = 0; i < InLinks.size(); i++) {
                            String InLinkName = InLinks.get(i);
                            Document InLink = Websites.find(new Document("Website", InLinkName)).first();

                            int NumOfOutLinks;
                            if (InLink == null) NumOfOutLinks = 1;
                            else NumOfOutLinks = ((ArrayList<String>) InLink.get("OutLinks")).size();

                            double InLinkRank;
                            if (InLink == null) InLinkRank = 1;
                            else InLinkRank = InLink.getDouble("PageRank");

                            PageRank += InLinkRank / (double) NumOfOutLinks;
                        }
                        Document update = new Document(document);
                        update.put("TempPageRank", PageRank);
                        Websites.replaceOne(document, update);
                    }
                }
            });
            updatePageRank();
        }
        long duration = System.currentTimeMillis() - startTime;
        System.out.println("Duration = " + duration);

        String total = Websites.aggregate(Arrays.asList(
                Aggregates.group("", Accumulators.sum("total", "$PageRank"))
        )).first().toJson();
        System.out.println(total);
    }

    private void updatePageRank() {
        MongoCollection<Document> Websites = Database.getCollection("pgtest");

        //transfer TempPageRank to PageRank
        Websites.find().forEach(new Block<Document>() {
            @Override
            public void apply(Document document) {
                Document update = new Document(document);
                update.put("PageRank", document.getDouble("TempPageRank"));
                Websites.replaceOne(document, update);
            }
        });
    }

}
