package com.example.tempadmin.quickeats;

import android.os.AsyncTask;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gson.*;

/**
 * Created by Tim on 11/16/2016.
 */
public class SearchYelp {
    MainActivity ma = null;

    //Calls the async task to make a network request to the servlet
    public void search(String term, String location, MainActivity ma){
        this.ma = ma;
        new AsyncYelpSearch().execute(term, location);
    }

    private class AsyncYelpSearch extends AsyncTask<String, Void, List<Business>>{
        protected List<Business> doInBackground(String... searchStrings){
            return search(searchStrings[0], searchStrings[1]);
        }

        //When the query is done, call the resultsReady function of the main activity
        protected void onPostExecute(List<Business> businessList) {
            ma.resultsReady(businessList);
        }

        //Main search function
        private List<Business> search(String term, String location){
            String output = "";

            //Try and connect to the servlet, passing the term and location as attributes
            try {
                //First URL is for task 1
                //URL url = new URL("URL Removed for privacy" + term + "&searchLocation=" + location);

                //URL for task 2
                URL url = new URL("URL removed for privacy" + term + "&searchLocation=" + location);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();

                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "text/plain");

                int status = conn.getResponseCode();

                if(status != 200){
                    //Handle failure here
                }

                String temp = "";
                BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

                //Read the response from the servlet, it is in a json format
                while((temp = br.readLine()) != null){
                    output += temp;
                }

                conn.disconnect();
            }catch(Exception e){
                e.printStackTrace();
            }
            return parseJson(output);
        }

        //Parses the json reply from the servlet
        private List<Business> parseJson(String json){
            List<Business> businessList = new ArrayList<Business>();
            JsonParser parser = new JsonParser();
            JsonElement jsonTree = parser.parse(json);

            if(jsonTree.isJsonObject()){
                JsonObject jsonObject = jsonTree.getAsJsonObject();
                JsonArray jArray = jsonObject.getAsJsonArray("businesses");

                Iterator i = jArray.iterator();

                //terate through the businesses to add each as a business
                while(i.hasNext()){
                    JsonObject business = (JsonObject) i.next();
                    String name = business.get("name").getAsString();
                    String rating = business.get("rating").getAsString();
                    String address = business.get("address").getAsString();

                    businessList.add(new Business(name, rating, address));
                }
            }

            return businessList;
        }
    }
}
