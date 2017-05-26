package com.example.tempadmin.quickeats;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ListView businessListView;
    private ProgressBar loadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Intiate the listview and the loading bar
        businessListView = (ListView) findViewById(R.id.businessListView);
        loadingBar = (ProgressBar) findViewById(R.id.progressBar);
    }

    //Action that's performed when the user presses the search button
    public void searchButtonClick(View v){
        TextView locationTV = (TextView) findViewById(R.id.locationTextBox);
        TextView termTV = (TextView) findViewById(R.id.termTextBox);

        //Get the term and location that was inputted into the textviews
        String searchAddress = locationTV.getText().toString();
        String searchTerm = termTV.getText().toString();

        //Show the loading bar so the user knows that the query is being performed
        loadingBar.setVisibility(View.VISIBLE);

        //Perform the serach
        SearchYelp sy = new SearchYelp();
        sy.search(searchTerm, searchAddress, this);
    }

    //When the search is done, this is called to update the UI
    public void resultsReady(List<Business> businessList){
        loadingBar.setVisibility(View.GONE);

        //Add the restaurants to the lsit adapter and display it
        Adapter ad = new Adapter(this, businessList);
        businessListView.setAdapter(ad);
    }
}
