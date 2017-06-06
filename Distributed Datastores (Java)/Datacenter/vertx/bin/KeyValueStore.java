import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Queue;
import java.util.LinkedList;
import java.util.Map;
import java.util.PriorityQueue;

import org.vertx.java.core.Handler;
import org.vertx.java.core.MultiMap;
import org.vertx.java.core.http.HttpServer;
import org.vertx.java.core.http.HttpServerRequest;
import org.vertx.java.core.http.RouteMatcher;
import org.vertx.java.platform.Verticle;

public class KeyValueStore extends Verticle {

	//Hashmap to store the key/value pair
	//Hashmap to create a lock for each key
	private static ConcurrentHashMap<String, PriorityQueue> keyHashMap = new ConcurrentHashMap<String, PriorityQueue>();
	private static HashMap<String, String> dataHashMap = new HashMap<String, String>();
	private static ConcurrentHashMap<String, Long> latestTimeHashMap = new ConcurrentHashMap<String, Long>();

	@Override
	public void start() {
		final KeyValueStore keyValueStore = new KeyValueStore();
		final RouteMatcher routeMatcher = new RouteMatcher();
		final HttpServer server = vertx.createHttpServer();
		server.setAcceptBacklog(32767);
		server.setUsePooledBuffers(true);
		server.setReceiveBufferSize(4 * 1024);
		routeMatcher.get("/put", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				MultiMap map = req.params();
				String key = map.get("key");
				String value = map.get("value");
				Long timestamp = Long.parseLong(map.get("timestamp"));
				String consistency = map.get("consistency");
				Integer region = Integer.parseInt(map.get("region"));

				Thread t = new Thread(new Runnable(){
					public void run(){
						//Only check position to determine whether to lock if it's strong consistency
						if(consistency.equals("strong")){
							checkPosition(key, timestamp);
							//If it's not strong consistency, check if the new timestamp is greater than the last written
							//timestamp.  If it's not, then don't do anything.
						}else{
							if(!latestTimeHashMap.containsKey(key)){
								latestTimeHashMap.put(key, timestamp);
							}else if(timestamp > latestTimeHashMap.get(key)){
								latestTimeHashMap.put(key, timestamp);
							}else{
								return;
							}
						}
						
						//Check the ConcurrentHashMap to see if the key is already in there.  Replace if true, otherwise
						//Add key and value
						if(dataHashMap.containsKey(key)){
							dataHashMap.replace(key, value);
						}else{
							dataHashMap.putIfAbsent(key, value);
						}
						
						//if(consistency.equals("strong")){
							String response = "stored";
							req.response().putHeader("Content-Type", "text/plain");
							req.response().putHeader("Content-Length",
									String.valueOf(response.length()));
							req.response().end(response);
							req.response().close();
						//}
					}
				});
				t.start();
				
			}
		});
		routeMatcher.get("/get", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				MultiMap map = req.params();
				final String key = map.get("key");
				String consistency = map.get("consistency");
				final Long timestamp = Long.parseLong(map.get("timestamp"));
				
				Thread t = new Thread(new Runnable(){
					public void run(){
						String response = "";
						
						//If strong consistency, call the getStrong which will check the thread's timestamp position
						//and waite if necessary.
						if(consistency.equals("strong")){
							response = getStrong(key, timestamp);
						}else{
							//If it's not strong, just go ahead and read the hashmap
							response = dataHashMap.getOrDefault(key, "0");
						}
						
						System.out.println("Got Key: " + key + " Response: " + response);

						req.response().putHeader("Content-Type", "text/plain");
						if (response != null)
							req.response().putHeader("Content-Length",
									String.valueOf(response.length()));
						req.response().end(response);
						req.response().close();
					}
				});
				t.start();
			}
		});
		// Clears this stored keys. Do not change this
		routeMatcher.get("/reset", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				keyHashMap.clear();
				latestTimeHashMap.clear();
				dataHashMap.clear();
				System.out.println("Reset");
				
				req.response().putHeader("Content-Type", "text/plain");
				req.response().end();
				req.response().close();
			}
		});
		// Handler for when the AHEAD is called
		routeMatcher.get("/ahead", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				MultiMap map = req.params();
				String key = map.get("key");
				final Long timestamp = Long.parseLong(map.get("timestamp"));

				Thread t = new Thread(new Runnable(){
					public void run(){
						
						//Call the addToHashmap method which will add the thread's timestamp to the HM
						//using a synchronized block
						addToHashmap(key, timestamp);		

						req.response().putHeader("Content-Type", "text/plain");
						req.response().end();
						req.response().close();
					}
				});
				t.start();
			}
		});
		// Handler for when the COMPLETE is called
		routeMatcher.get("/complete", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				MultiMap map = req.params();
				String key = map.get("key");
				final Long timestamp = Long.parseLong(map.get("timestamp"));
				
				Thread t = new Thread(new Runnable(){
					public void run(){
						//Remove the completed thread from the HM and notify the other threads to check if they may proceed.
						synchronized(keyHashMap.get(key)){
							System.out.println("Removed Key: " + key + " Time: " + timestamp);
							keyHashMap.get(key).remove(timestamp);
							keyHashMap.get(key).notifyAll();
						}
						
						req.response().putHeader("Content-Type", "text/plain");
						req.response().end();
						req.response().close();
					}
				});
				t.start();
				

			}
		});
		routeMatcher.noMatch(new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				req.response().putHeader("Content-Type", "text/html");
				String response = "Not found.";
				req.response().putHeader("Content-Length",
						String.valueOf(response.length()));
				req.response().end(response);
				req.response().close();
			}
		});
		server.requestHandler(routeMatcher);
		server.listen(8080);
	}
	
	//Method that will add threads' timestamps to the ConcurrentHM
	//Used by both the ahead and the get methods
	private void addToHashmap(String key, Long timestamp){
		keyHashMap.putIfAbsent(key, new PriorityQueue<Long>());
		synchronized(keyHashMap.get(key)){
			System.out.println("Added Key: " + key + " Time: " + timestamp);
			keyHashMap.get(key).add(timestamp);
		}
	}
	
	//Lock the PQ for the key and check if the thread's timestamp is first
	//If yes, then proceed.  If not, then wait.
	private void checkPosition(String key, Long timestamp){
		synchronized(keyHashMap.get(key)){
			while(!keyHashMap.get(key).peek().equals(timestamp)){
				try{
					keyHashMap.get(key).wait();
				}catch(InterruptedException e){
					e.printStackTrace();
				}
			}
		}
	}
	
	//Used by the get method for strong consistency.  It gets
	//the data from the data hashmap and then removes the timestamp
	//using a synchronized block
	private String getStrong(String key, Long timestamp){
		String response = "";
		
		addToHashmap(key, timestamp);
		checkPosition(key, timestamp);
		
		response = dataHashMap.getOrDefault(key, "0");
		
		synchronized(keyHashMap.get(key)){
			System.out.println("Removed Key: " + key + " Time: " + timestamp);
			keyHashMap.get(key).remove(timestamp);
			keyHashMap.get(key).notifyAll();
		}
		
		return response;
	}
}