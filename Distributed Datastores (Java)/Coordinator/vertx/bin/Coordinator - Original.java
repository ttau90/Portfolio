import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
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

import java.util.Date;

public class Coordinator extends Verticle {

	// This integer variable tells you what region you are in
	// 1 for US-E, 2 for US-W, 3 for Singapore
	private static int region = KeyValueLib.region;

	// Default mode: Strongly consistent
	// Options: strong, eventual
	private static String consistencyType = "strong";

	/**
	 * TODO: Set the values of the following variables to the DNS names of your
	 * three dataCenter instances. Be sure to match the regions with their DNS!
	 * Do the same for the 3 Coordinators as well.
	 */
	private static final String dataCenterUSE = "ec2-52-90-101-118.compute-1.amazonaws.com";
	private static final String dataCenterUSW = "ec2-54-175-129-209.compute-1.amazonaws.com";
	private static final String dataCenterSING = "ec2-52-201-255-218.compute-1.amazonaws.com";

	private static final String coordinatorUSE = "ec2-54-174-76-39.compute-1.amazonaws.com";
	private static final String coordinatorUSW = "ec2-52-23-238-166.compute-1.amazonaws.com";
	private static final String coordinatorSING = "ec2-54-175-121-139.compute-1.amazonaws.com";
	
	private static final String[] coordArray = {coordinatorUSE, coordinatorUSW, coordinatorSING};
	private static final String[] dcArray = {dataCenterUSE, dataCenterUSW, dataCenterSING};
	
	SimpleDateFormat sdf = new SimpleDateFormat("MMM dd,yyyy HH:mm:ss:SS");
	
	@Override
	public void start() {
		KeyValueLib.dataCenters.put(dataCenterUSE, 1);
		KeyValueLib.dataCenters.put(dataCenterUSW, 2);
		KeyValueLib.dataCenters.put(dataCenterSING, 3);
		KeyValueLib.coordinators.put(coordinatorUSE, 1);
		KeyValueLib.coordinators.put(coordinatorUSW, 2);
		KeyValueLib.coordinators.put(coordinatorSING, 3);
		final RouteMatcher routeMatcher = new RouteMatcher();
		final HttpServer server = vertx.createHttpServer();
		server.setAcceptBacklog(32767);
		server.setUsePooledBuffers(true);
		server.setReceiveBufferSize(4 * 1024);

		routeMatcher.get("/put", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				MultiMap map = req.params();
				final String key = map.get("key");
				final String value = map.get("value");
				final String timestamp = map.get("timestamp");
				final String forwarded = map.get("forward");
				final String forwardedRegion = map.get("region");
				
				Thread t = new Thread(new Runnable() {
					public void run() {
						
						int primaryCoordinator = 0;

						//Alert DC that a new request has arrived and call the hashing function
						//to determine which coordinator should handle the request
						//Only call the ahead method if it's strong consistency
						if(!Boolean.parseBoolean(forwarded)){
							if(consistencyType.equals("strong")){
								try{
									KeyValueLib.AHEAD(key, timestamp);
								}catch(IOException e){
									e.printStackTrace();
								}
							}
							primaryCoordinator = hashIt(key);
							System.out.println("P-Key:\t" + key + "\tValue:\t" + value + "\tTime:\t" + timestamp + "\tPC:\t" + primaryCoordinator + "\tCR:\t" + region);
						}
						
						//If this isn't the designated primary coordinator, then forward the
						//request to the primary coordinator determined by the hashing function
						if(primaryCoordinator != region && !Boolean.parseBoolean(forwarded)){
							try{
								KeyValueLib.FORWARD(coordArray[primaryCoordinator -1], key, value, timestamp);
							}catch(IOException e){
								e.printStackTrace();
							}
						}else{
							//If this is the primary coordinator then make a thread to send a put
							//request to each data center
							
							Thread dc1 = createThread(0, key, value, timestamp);							
							Thread dc2 = createThread(1, key, value, timestamp);						
							Thread dc3 = createThread(2, key, value, timestamp);	
							
							Thread[] threadArray = {dc1, dc2, dc3};
							
							//Start all 3 threads to put to DCs
							dc1.start();
							dc2.start();
							dc3.start();
							
							if(consistencyType.equals("strong")){
								//Wait for all 3 DCs to finish only for strong consistency
								for(Thread thread: threadArray){
									try{
										thread.join();
									}catch(InterruptedException e){
										e.printStackTrace();
									}
								}
							
								//Once all 3 threads have joined, notify DCs that operation is complete if Strong consistency							
								try{
									KeyValueLib.COMPLETE(key, timestamp);
								}catch(IOException e){
									e.printStackTrace();
								}
							}
						}
					}
				});
				t.start();
				req.response().end(); // Do not remove this
			}
		});

		routeMatcher.get("/get", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				MultiMap map = req.params();
				final String key = map.get("key");
				final String timestamp = map.get("timestamp");
				Thread t = new Thread(new Runnable() {
					public void run() {
						String response = "0";
						
						//Send the get request to the data center that corresponds to this coordinator's region
						try{
							response = KeyValueLib.GET(dcArray[region-1], key, timestamp, consistencyType);
							System.out.println("G-Key\t" + key + "\tResponse:\t" + response + "\tTime:\t" + timestamp);
						}catch(IOException e){
							e.printStackTrace();
						}
						req.response().end(response);
					}
				});
				t.start();
			}
		});
		/* This endpoint is used by the grader to change the consistency level */
		routeMatcher.get("/consistency", new Handler<HttpServerRequest>() {
			@Override
			public void handle(final HttpServerRequest req) {
				MultiMap map = req.params();
				consistencyType = map.get("consistency");
				req.response().end();
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
	
	//Hashing function that works by converting the key to char array
	//Then converts to ascii and sums the amount.  Then we get the 2nd
	//half of the sum and mod 3.  The +2 is to ensure that key a goes to
	//dc 1, key b goes to dc 2, and key c goes to dc 3.
	private int hashIt(String key){
		int sumOfAscii = 0;
		String temp = "";
		char[] charArray = key.toCharArray();
		
		for(int i = 0; i < charArray.length; i++){
			sumOfAscii += (int)charArray[i];
		}
		
		temp = Integer.toString(sumOfAscii);
		temp = temp.substring(temp.length()/2);
		
		return ((Integer.parseInt(temp)+2)%3)+1;
	}
	
	//Creates a thread for the specified DC
	private Thread createThread(int dc, String key, String value, String timestamp){
		Thread t = new Thread(new Runnable(){
			public void run(){
				try{
					KeyValueLib.PUT(dcArray[dc], key, value, timestamp, consistencyType);
				}catch(IOException e){
					e.printStackTrace();
				}
			}
		});
		
		return t;
	}
}
