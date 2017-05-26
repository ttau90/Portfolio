/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DS;

import org.scribe.builder.ServiceBuilder;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.google.gson.*;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bson.Document;
/**
 *
 * @author Tim
 */
public class Model{
    //Keys for yelp
    //Removed for privacy
    private String consumerKey = "";
    private String consumerSecret = "";
    private String token = "";
    private String tokenSecret = "";
    
    //Mongodb URI
    //Removed for privacy
    private String mongoURI = "";
    
    //Objects for Yelp OAuth
    private OAuthService service;
    private Token accessToken;
    private OAuthRequest request;
    
    //Objects for MongoDB
    private MongoClient mClient;
    private MongoDatabase db;
    
    //Lists and maps to record restaurants and requests retrieved from Mongodb server
    private List<Restaurant> resList;
    private List<Request> reqList;
    private Map<String, Integer> termCount; //Used to find the most popular term
    private Map<String, Integer> locCount;  //Used to find the most popular location
    
    public Model(){
        //Create the tokens to access Yelp API
        service = new ServiceBuilder().provider(TwoStepOAuth.class).apiKey(consumerKey).apiSecret(consumerSecret).build();
        accessToken = new Token(token, tokenSecret);
        
        //Instantiate the lsits and maps
        resList = new ArrayList<Restaurant>();
        reqList = new ArrayList<Request>();
        termCount = new HashMap<String, Integer>();
        locCount = new HashMap<String, Integer>();
    }
    
    //Search the yelp API using the given term and location
    public String search(String term, String location, String phone, Timestamp dt){
        request = new OAuthRequest(Verb.GET, "http://api.yelp.com/v2/search");
        //Only add the term if one was provided
        if(!term.equals("")){
            request.addQuerystringParameter("term", term);
        }
        request.addQuerystringParameter("location", location);
        request.addQuerystringParameter("sort", "1");   //Sorts by closest distance
        request.addQuerystringParameter("limit", "3");  //Returns only 3 restaurants
        service.signRequest(this.accessToken, request);
        Response response = request.send();
        
        //Get the current datetime, to be used to calculate delay
        String S = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss").format(dt);
        java.util.Date date = new java.util.Date();
        Timestamp dt2 = new Timestamp(date.getTime());
        long delay = dt2.getTime() - dt.getTime();
        
        //Try and record the request to the mongodb server
        try{
            MongoConnect();
            logRequest(term, location, S, phone, delay);
        }finally{
            CloseMongo();
        }
        
        return parseResponse(response.getBody());
    }
    
    //Parse the json that's received from yelp
    private String parseResponse(String json){
        JsonParser parser = new JsonParser();
        JsonElement jsonTree = parser.parse(json);  //Parse json from yelp
        JsonObject jsonReply = new JsonObject();
        JsonArray jsonReplyArray = new JsonArray(); //The json that will be sent to Android
        
        //Check to see if the json element is a json object
        if(jsonTree.isJsonObject()) {
            JsonObject jsonObject = jsonTree.getAsJsonObject();
            JsonArray jarray = jsonObject.getAsJsonArray("businesses");
            
            //Loop through the businesses that were sent in the json array
            Iterator i = jarray.iterator();
            while(i.hasNext()){
                //get the name, rating, and address from the business
                //Add the information to the replyBusiness json which will be sent to Android
                JsonObject replyBusiness = new JsonObject();
                JsonObject business = (JsonObject) i.next();
                
                String name = business.get("name").getAsString();
                replyBusiness.addProperty("name", name);
                
                String rating = business.get("rating").getAsString();
                replyBusiness.addProperty("rating", rating);
                
                JsonElement location = business.get("location");
                JsonObject locObject = location.getAsJsonObject();
                JsonArray addressArray = locObject.getAsJsonArray("display_address");
                Iterator i2 = addressArray.iterator();
                String displayAddress = "";
                
                //The address is split across multiple lines, so retrieve all lines and combine into one line
                while(i2.hasNext()){
                    JsonElement line = (JsonElement) i2.next();
                    displayAddress += line.toString();
                }
                displayAddress = displayAddress.replaceAll("\\\"+", " ").trim();
                replyBusiness.addProperty("address", displayAddress);
                jsonReplyArray.add(replyBusiness);
                
                //Try and log the restaurant to the mongodb database
                try{
                    MongoConnect();
                    logRestaurant(name, Double.parseDouble(rating), displayAddress);
                }finally{
                    CloseMongo();
                }
            }
            jsonReply.add("businesses", jsonReplyArray);
        }
        
        //Returns the json string containing 3 restaurants to android
        return jsonReply.toString();
    }
    
    //Establish the connection to the mongo db
    public void MongoConnect(){
        MongoClientURI connString = new MongoClientURI(mongoURI);
        mClient = new MongoClient(connString);
        db = mClient.getDatabase(connString.getDatabase());
    }
    
    //Close the mongodb client
    public void CloseMongo(){
        mClient.close();
    }
    
    //Log the restaurant to the mongodb restaurant collection
    private void logRestaurant(String name, double rating, String address){
        //Get the restaurant collection
        MongoCollection<Document> collection = db.getCollection("restaurants");
        
        //Check to see if the restaurant already exists
        long count = collection.count(eq("name", name));
        
        //if the restuarant doesn't exist, add it as a new restaurant
        if(count == 0){
            Document doc1 = new Document("name", name)
                    .append("rating", rating)
                    .append("address", address);
            collection.insertOne(doc1);
        }
    }
    
    //Log the request to the request collection
    private void logRequest(String term, String location, String time, String phone, long delay){
        //get the requests collection
        MongoCollection<Document> collection = db.getCollection("requests");
        
        //Add the request to the request collection
        Document doc = new Document("term", term)
                .append("location", location)
                .append("time", time)
                .append("phone", phone)
                .append("delay", Long.toString(delay));
        
        collection.insertOne(doc);
    }
    
    //Retrieve all restaurants from the mongodb
    public List<Restaurant> getRestaurants(){
        MongoCollection<Document> collection = db.getCollection("restaurants");
        MongoCursor<Document> cursor = collection.find().iterator();
        
        resList = new ArrayList<Restaurant>();
        try{
            //Iterate through all the returned restaurants and store the information into the resList list
            while(cursor.hasNext()){
                String json = cursor.next().toJson();
                JsonParser parser = new JsonParser();
                JsonElement jsonTree = parser.parse(json);
                if(jsonTree.isJsonObject()){
                    Restaurant r = new Restaurant();
                    JsonObject j = jsonTree.getAsJsonObject();
                    r.setName(j.get("name").getAsString());
                    r.setLocation(j.get("address").getAsString());
                    r.setRating(Double.parseDouble(j.get("rating").getAsString()));
                    resList.add(r);
                }
            }
        }finally{
            cursor.close();
        }
        
        return resList;
    }
    
    //Retrieve all requests from the mongodb request collection
    public List<Request> getRequests(){
        MongoCollection<Document> collection = db.getCollection("requests");
        MongoCursor<Document> cursor = collection.find().iterator();
        
        reqList = new ArrayList<Request>();
        try{
            //Iterate through all the requests to retrieve the information and add to the reqList list
            while(cursor.hasNext()){
                String json = cursor.next().toJson();
                JsonParser parser = new JsonParser();
                JsonElement jsonTree = parser.parse(json);
                if(jsonTree.isJsonObject()){
                    Request r = new Request();
                    JsonObject j = jsonTree.getAsJsonObject();
                    String term = j.get("term").getAsString();
                    String location = j.get("location").getAsString();
                    r.setLocation(location);
                    r.setTerm(term);
                    r.setTimestamp(j.get("time").getAsString());
                    r.setPhone(j.get("phone").getAsString());
                    r.setDelay(Double.parseDouble(j.get("delay").getAsString()));
                    reqList.add(r);
                    
                    //Add the term to the termCount map, used to find which term is the most popular
                    //Check if the term already exists.  IF yes, increment by one, else add as new term
                    if(termCount.containsKey(term)){
                        int count = termCount.get(term);
                        termCount.put(term, count+1);
                    }else{
                        termCount.put(term, 0);
                    }
                    
                    //Add the location to the locCount map, used to find which term is the most popular
                    //Check if the location already exists.  IF yes, increment by one, else add as new loc
                    if(locCount.containsKey(location)){
                        int count = locCount.get(location);
                        locCount.put(location, count+1);
                    }else{
                        locCount.put(location, 0);
                    }
                }
            }
        }finally{
            cursor.close();
        }
        
        return reqList;
    }
    
    //Calculates the average ratings of the stored restaurants
    public double getAvgRating(){
        Iterator i = resList.iterator();
        double ratingSum = 0.0;
        int ratingCount = 0;
        //Iterate through the resList to get restaurant rating
        while(i.hasNext()){
            Restaurant r = (Restaurant) i.next();
            ratingSum += r.getRating();
            ratingCount++;
        }
        return ratingSum/ratingCount;
    }
    
    //Calculates the average delay from all the requests
    public double getAvgDelay(){
        Iterator i = reqList.iterator();
        double delaySum = 0.0;
        int delayCount = 0;
        
        //Iterate through the requests to calculate avg delay
        while(i.hasNext()){
            Request r = (Request)i.next();
            delaySum += r.getDelay();
            delayCount++;
        }
        
        return delaySum/delayCount;
    }
    
    //Finds which term was the most searched for
    public String getPopTerm(){
        Iterator i = termCount.entrySet().iterator();
        String term = "";
        int count = 0;
        
        //Iterates through the hashmap to find the term that has been searched for the most
        while(i.hasNext()){
            Map.Entry pair = (Map.Entry)i.next();
            if ((int)pair.getValue() > count){
                term = (String) pair.getKey();
                count = (int) pair.getValue();
            }
        }
        
        return term;
    }
    
    //Finds which location was the most searched for
    public String getPopLocation(){
        Iterator i = locCount.entrySet().iterator();
        String loc = "";
        int count = 0;
        
        //iterates through the hashmap to find the location that has been searched for the most
        while(i.hasNext()){
            Map.Entry pair = (Map.Entry)i.next();
            if ((int)pair.getValue() > count){
                loc = (String) pair.getKey();
                count = (int) pair.getValue();
            }
        }
        
        return loc;
    }
}
