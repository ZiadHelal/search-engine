package com.example.search;

import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.mongodb.Block;
import com.mongodb.MongoWriteException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.stitch.android.core.Stitch;
import com.mongodb.stitch.android.core.StitchAppClient;
import com.mongodb.stitch.android.services.mongodb.local.LocalMongoDbService;

import org.bson.Document;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    final String TAG = "Search";
    static String url = "http://192.168.1.7:8080/SearchEngineRequest?";
    final int NUMBER_OF_PERSONALIZED = 5;

    private AutoCompleteTextView autoCompleteTextViewET;
    private EditText portNumberET, ipAddressET;
    private ImageButton voiceSearchIB;
    private Button searchButton;
    private Button clearPersonalizedButton;
    private Button clearAutoCompleteButton;
    private CheckBox searchImagesCB;
    private Spinner localeSpinner;
    private TextView test;

    private String Query;
    private String Locale = ".eg";
    private String ImageSearch;
    private String PhraseSearch;
    private String PersonalizedOneString;
    private String[] Personalized;
    private String[] AutoComplete;

    private RequestQueue requestQueue;
    private MongoDatabase Database;
    private MongoCollection<Document> PersonalizedColl;
    private MongoCollection<Document> AutoCompleteColl;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        test = findViewById(R.id.testtext);
        test.setSelected(true);

        requestQueue = Volley.newRequestQueue(this);

        initializeDB();
        findViews();
        autoCompleteTextViewET.setText("");
        portNumberET.setText("8080");
        fillLocaleSpinner();
        setListeners();
    }

    private void initializeDB() {
        final StitchAppClient client;
        if (Stitch.hasAppClient(getResources().getString(R.string.STITCH_APP_ID))) {
            client = Stitch.getDefaultAppClient();
        } else {
            client = Stitch.initializeDefaultAppClient(getResources().getString(R.string.STITCH_APP_ID));
        }
        final MongoClient mobileClient =
                (MongoClient) client.getServiceClient(LocalMongoDbService.clientFactory);
        Database = mobileClient.getDatabase("Database");
        PersonalizedColl = Database.getCollection("Personalized");
        AutoCompleteColl = Database.getCollection("AutoComplete");
    }

    /**
     * updates the Personalized array from the database
     */
    private void updatePersonalized() {
        Personalized = new String[NUMBER_OF_PERSONALIZED];
        if (PersonalizedColl.countDocuments() != 0) {
            //get personalized
            for (int i = 0; i < NUMBER_OF_PERSONALIZED; i++) {
                Document document = PersonalizedColl.find().sort(Sorts.descending("Times Visited")).skip(i).first();
                if (document != null) {
                    Personalized[i] = document.getString("Website");
                } else {
                    Personalized[i] = "null";
                }
            }
        } else {
            for (int i = 0; i < NUMBER_OF_PERSONALIZED; i++) {
                Personalized[i] = "null";
            }
        }
        Log.d(TAG, "updatePersonalized: " + Arrays.toString(Personalized));
    }

    /**
     * updates the AutoComplete array from the database
     */
    private void updateAutoComplete() {
        ArrayList<String> AutoCompleteList = new ArrayList<>();
        for (Document document : AutoCompleteColl.find()) {
            AutoCompleteList.add(document.getString("_id"));
        }
        autoCompleteTextViewET.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, AutoCompleteList));
        Log.d(TAG, "updateAutoComplete: " + Arrays.toString(AutoCompleteList.toArray()));
    }

    private void setListeners() {
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (loading) {
                    Toast.makeText(getApplicationContext(), "Loading query please wait...", Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean queryEmpty = autoCompleteTextViewET.getText().toString().equals("");
                boolean ipAddressEmpty = ipAddressET.getText().toString().equals("");
                boolean portNumberEmpty = portNumberET.getText().toString().equals("");
                if (queryEmpty || ipAddressEmpty || portNumberEmpty) {
                    Toast.makeText(getApplicationContext(), "One of the text boxes is empty.", Toast.LENGTH_SHORT).show();
                    return;
                }
                startSearchRequest();
            }
        });
        voiceSearchIB.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                inputVoice();
            }
        });
        clearPersonalizedButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PersonalizedColl != null)
                    PersonalizedColl.drop();
                updatePersonalized();
            }
        });
        clearAutoCompleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (AutoCompleteColl != null)
                    AutoCompleteColl.drop();
                updateAutoComplete();
            }
        });
    }

    private void inputVoice() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, java.util.Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "What do you want to search for");
        if (intent.resolveActivity(getPackageManager()) != null)
            startActivityForResult(intent, 10);
        else {
            Toast.makeText(getApplicationContext(), "This action is not supported on your device.", Toast.LENGTH_SHORT).show();
            return;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10) {
            if (resultCode == RESULT_OK && data != null) {
                String result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS).get(0);
                Log.d(TAG, "onActivityResult: " + result);
                autoCompleteTextViewET.setText(result);
            }
        }
    }

    private void startSearchRequest() {
        setVariables();
        String URL = url;
        URL += "NewQuery=1";
        URL += "&Query=" + Query;
        URL += "&Locale=" + Locale;
        URL += "&Personalized=" + PersonalizedOneString;
        URL += "&ImageSearch=" + ImageSearch;
        URL += "&PhraseSearch=" + PhraseSearch;
        Log.d(TAG, "startSearchRequest: " + URL);

        long startTime = System.currentTimeMillis();    //record time of sending request
        JsonObjectRequest request = new JsonObjectRequest(Request.Method.GET, URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        loading = false;
                        try {
                            if (response.getInt("Status") == 0) {
                                Toast.makeText(getApplicationContext(), "The Query you entered didn't return any results.", Toast.LENGTH_SHORT).show();
                            } else {
                                float duration = (float) (System.currentTimeMillis() - startTime) / 1000;
                                String message = "Retrieved " + response.getInt("Number of Results")
                                        + " results in " + String.format("%.2f", duration) + " seconds.";
                                Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
                                addToAutoComplete();
                                displayResults(response);
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                loading = false;
                Toast.makeText(getApplicationContext(), "Server timeout please retry again.", Toast.LENGTH_SHORT).show();
            }
        });
        request.setRetryPolicy(new DefaultRetryPolicy(
                10000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));
        requestQueue.add(request);
        loading = true;
    }

    private void addToAutoComplete() {
        try {
            AutoCompleteColl.insertOne(new Document("_id", autoCompleteTextViewET.getText().toString()));
        } catch (MongoWriteException e) {
            Log.d(TAG, "addToAutoComplete: exception writing");
        }
    }

    private void displayResults(JSONObject response) {
        Intent intent;
        if (ImageSearch.equals("0")) {
            intent = new Intent(this, TextResults.class);
        } else {
            intent = new Intent(this, ImageResults.class);
        }
        try {
            intent.putExtra("resultsJsonArray", response.getJSONArray("Results").toString());
            intent.putExtra("ResultsCount", response.getInt("Number of Results"));
        } catch (JSONException e) {
            Log.d(TAG, "displayResults: Failed to get number of results so set to 0 automatically");
            intent.putExtra("ResultsCount", 0);
        }
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        updatePersonalized();
        updateAutoComplete();
    }

    private void setVariables() {
        String userInput = autoCompleteTextViewET.getText().toString();
        //get phrase search
        PhraseSearch = (userInput.startsWith("\"") && userInput.endsWith("\"")) ? "1" : "0";

        //get image search status
        ImageSearch = (searchImagesCB.isChecked()) ? "1" : "0";

        //get query and replace all spaces with %20 to allow it to be url
        Query = userInput.replaceAll("\\s+", "%20");
        if (PhraseSearch.equals("1"))
            Query = Query.substring(1, Query.length() - 1);

        //get Personalized
        PersonalizedOneString = Arrays.toString(Personalized).replaceAll("\\[|\\]", "").replaceAll("\\s+", "");

        //get ipAddress and port number
        url = "http://192.168.1." + ipAddressET.getText().toString() + ":"
                + portNumberET.getText().toString() + "/SearchEngineRequest?";
    }

    private void fillLocaleSpinner() {
        List<String> locales = new ArrayList<>();
        locales.add(".eg");
        locales.add(".cn");
        locales.add(".br");
        locales.add(".fr");
        locales.add(".sa");
        locales.add(".de");
        ArrayAdapter<String> localesAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, locales);
        localesAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        localeSpinner.setAdapter(localesAdapter);

        localeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Locale = localeSpinner.getSelectedItem().toString();
                Log.d(TAG, "onItemSelected: " + Locale);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void findViews() {
        autoCompleteTextViewET = findViewById(R.id.searchET);
        voiceSearchIB = findViewById(R.id.voice_seachIB);
        searchButton = findViewById(R.id.searchBT);
        clearPersonalizedButton = findViewById(R.id.clear_personalBT);
        clearAutoCompleteButton = findViewById(R.id.clear_autocompleteBT);
        searchImagesCB = findViewById(R.id.search_imagesCB);
        localeSpinner = findViewById(R.id.localeSP);
        ipAddressET = findViewById(R.id.ip_address_et);
        portNumberET = findViewById(R.id.port_number_et);
    }

    Block<Document> printBlock = new Block<Document>() {
        @Override
        public void apply(final Document document) {
            Log.d(TAG, "apply: " + document.toJson());
        }
    };
}
