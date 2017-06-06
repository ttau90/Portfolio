# Distributed Datastores

The objective for this project is to implement a crossregion system of Coordinators and KeyValueStores to handle requests from each region. There are three total regions, as specified previously. Each region will have its own Coordinator, and KeyValueStore. Each Coordinator and KeyValueStore exists in its own AMI instance. Thus, there are 6 instances in total 1 Coordinator and 1 KeyValueStore for each of the 3 regions. Additionally, there is a Client instance which was used to test the implementation.

The coordinators need to be able to handle strong and eventual consistency, depending on the value passed to it by the client.  Each key passed to the coordinators will be hashed to designate a primary coordinator for that key.  Only the primary coordinator may handle PUT requests for a given key.  GET requests can be handled by any coordinator.


To assist with the coordination between the datacenters and coordinators, the KeyValueLib API was provided to us with the following methods:

KeyValueLib.PUT(String datastoreDNS, String key, String value, String timestamp, String consistency) throws IOException
	
	This API method will pass along all the information of the PUT request to the specified datastore, in addition to the region associated with the coordinator making the call. There is a delay with this method, check the table below for the actual delay. Because of the delay, there may be some time between when the Coordinator issues the API call, and when the datacenter receives it. The parameters available to your KeyValueStore as a result of this call are:

	key : String
	value : String
	timestamp : Long
	region : int
	consistency : String


KeyValueLib.GET(String datastoreDNS, String key, String timestamp, String consistency) throws IOException

	This API method will pass along the information for the GET request to the specified datastore, and return the value that is provided by the datastore back to the Coordinator. There is no delay with this method, because the GET method is expected to be serviced by the local datacenter. You will still need the timestamp to appropriately handle the request for strong consistency. The parameters available to your KeyValueStore as a result of this call are:

	key : String
	timestamp : Long
	consistency : String


KeyValueLib.FORWARD(String coordinatorDNS, String key, String value, String timestamp) throws IOException

	This API method will forward the request to the specified coordinator. It will be as if the target coordinator received the request from a client in its region, however, an additional parameter of {"forward" : "true"} will be provided in the URL, as well as the region from which the request originated. You may check for this parameter to determine whether a request is sent by a client, (since it will be null), or if it was forwarded by another coordinator, (since it will be "true"). You will need to use this method for passing PUT requests between coordinators. There is a delay with this method, check the table below for the actual delay. The parameters available to the target Coordinator as a result of this call are:

	key : String
	value : String
	timestamp : Long
	region : int
	forward = "true" : String


KeyValueLib.AHEAD(String key, String timestamp) throws IOException

	This API method will contact the datastores in every region, and notify them that a PUT request is being serviced for the specified key, starting at the specified timestamp. It will be up to you whether or not to use this method, and how you choose to use it. There is no delay in this communication to any of the datastores. The timestamp that is provided is the timestamp that the datastores will see upon receiving this message. The parameters available to your KeyValueStore as a result of this call are:

	key : String
	timestamp : Long


KeyValueLib.COMPLETE(String key, String timestamp) throws IOException

	This API method will contact the datastores in every region, and notify them that a PUT request for the specified key that started at the given timestamp has been completed. Again, it is up to you to decide if you want to use this method. There is no delay in this communication to any of the datastores. The timestamp that is provided is the timestamp that the datastores will see upon receiving this message. The parameters available to your KeyValueStore as a result of this call are:

	key : String
	timestamp : Long