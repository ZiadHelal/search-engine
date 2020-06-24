import java.lang.module.ResolutionException;

public class Test {
    static SearchEngine searchEngine = new SearchEngine();
    static String locale = "";
    static String[] Personalized;
    private static boolean phraseSearch;
    private static boolean imageSearch;

    public static void main(String[] args) {
        //Enter the search query here
        String SearchQuery = "Artificial Intelligence using python";

        //Set locale to any String starting with a . like ".eg" or ".uk" or ".cn" etc.
        locale = ".eg";

        //Set Personalized to array of String containing url host. only the first five
        Personalized = new String[]{"wikipedia.com", "google.com", "facebook"};

        //Set if you want phrase search
        phraseSearch = false;

        //Set if you want image search
        imageSearch = false;

        //Then search the database
        search(SearchQuery);

    }

    static void search(String query) {
        String[] Query = searchEngine.queryProcessor(query);
        if (Query[0].equals("")) {
            System.out.println("Query entered is empty or made of all stop words.");
            return;
        }

        //you don't need to initialize the database until this point
        searchEngine.initializeDb();

        //set Locale
        searchEngine.setLocale(locale);

        //set Personalized
        searchEngine.setPersonalized(Personalized);

        //set Phrase search
        searchEngine.PhraseSearch = phraseSearch;

        //set Image Search
        searchEngine.ImageSearch = imageSearch;

        //search for the query
        searchEngine.search(Query);
    }
}
