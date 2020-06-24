package com.example.search;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class ImageResults extends AppCompatActivity {
    final String TAG = "Search";
    final int RESULTS_PER_PAGE = 10;

    private RequestQueue requestQueue;
    private JSONArray resultsJsonArray;
    private RecyclerView resultsRecyclerView;
    private ImageResultAdapter adapter;
    private RecyclerView.LayoutManager layoutManager;
    private int page;
    private int ResultsCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_results);

        findViews();
        requestQueue = Volley.newRequestQueue(this);
        page = 1;

        try {
            resultsJsonArray = new JSONArray(getIntent().getStringExtra("resultsJsonArray"));
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ResultsCount = getIntent().getIntExtra("ResultsCount", 1);
        setActionBarTitle();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        fillRecyclerView();
    }

    private void findViews() {
        resultsRecyclerView = findViewById(R.id.image_results_rv);
    }


    /**
     * fills the RecyclerView with the data in resultsJsonArray
     */
    private void fillRecyclerView() {
        ArrayList<ImageResult> resultArrayList = new ArrayList<>();
        for (int i = 0; i < resultsJsonArray.length(); i++) {
            try {
                JSONObject jsonObject = resultsJsonArray.getJSONObject(i);
                String imageUrl = jsonObject.getString("ImageWebsite");
                resultArrayList.add(new ImageResult(imageUrl));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        resultsRecyclerView.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        adapter = new ImageResultAdapter(resultArrayList);

        resultsRecyclerView.setLayoutManager(layoutManager);
        resultsRecyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.results_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.next_button_it:
                nextButtonPressed();
                return true;
            case R.id.previous_button_it:
                previousButtonPressed();
                return true;
            case android.R.id.home:
                this.finish();
                return true;
            default:
                return true;
        }
    }

    private void nextButtonPressed() {
        if (page * RESULTS_PER_PAGE < ResultsCount) {
            sendRequest(page + 1);
            page++;
        } else {
            Toast.makeText(this, "You are already in the last page", Toast.LENGTH_SHORT).show();
        }
    }

    private void previousButtonPressed() {
        if (page > 1) {
            sendRequest(page - 1);
            page--;
        } else {
            Toast.makeText(this, "You are already in the first page", Toast.LENGTH_SHORT).show();
        }
    }

    private void sendRequest(int page) {
        String URL = MainActivity.url;
        URL += "NewQuery=0";
        URL += "&Page=" + page;
        Log.d(TAG, "startSearchRequest: " + URL);
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, URL, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            resultsJsonArray = response.getJSONArray("Results");
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        fillRecyclerView();
                        setActionBarTitle();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                error.printStackTrace();
            }
        });
        requestQueue.add(jsonObjectRequest);
    }

    void setActionBarTitle() {
        int from;
        int to;
        if (page == 1) {
            from = 1;
            to = resultsJsonArray.length();
        } else {
            from = (page - 1) * RESULTS_PER_PAGE;
            if (page * RESULTS_PER_PAGE >= ResultsCount) {   //check if in last page
                to = ResultsCount;
            } else {
                to = from + RESULTS_PER_PAGE;
            }
        }
        getSupportActionBar().setTitle("Displaying " + from + "-" + to + " of " + ResultsCount);
    }
}
