# Distributed Datastores

### Project Objective
The objective of this project is to handle strong and eventual consistency of data across distributed datastores.  In this project, data is distributed across three backend storage systems, each in its own region.  Each region is then managed by a coordinator that is in charge of handling requests from clients in its respective region.

### Project Scenario and Constraints
Coordinators will receive GET and PUT requests to retrieve and send key value pairs.  The coordinator will then use the key value and a timestamp to appropriately access data stores.  

In this assignment, each key is assigned a Primary Coordinator.  Only the Primary Coordinator may handle PUT operations for the given key.  If a non-Primary Coordinator recieves a PUT operation for a key, that non-Primary Coordinator must forward the PUT request to the key's Primary Coordinator.  The Primary Coordinator will then take the key value pair and store the information across all datastores in the different regions.  Storing the information in datastores of different regions have a delay due to the distance, and therefore data may not be consistent across all the datastores immediately after a PUT operation is processed.  

When a coordinator receives a GET request, the coordinator must get the key value information from its local datastore.  When receiving the GET request, the coordinator may need to know whether a PUT operation on that key has already been performed, and if its datacenter has the most up-to-date value for that key.  This knowledge is necessary for enforcing strict consistency.

To coordinate the communication across Coordinators and data stores, API functions in KeyValueLib were provided by the instructor.  The API functions would direct GET and PUT requests to different data stores as appropriate, and would simulate a delay with each call as appropriate.  The details of the KeyValueLib API functions are detailed in a below section.

### Grading Details
In grading this assignment, 3 Coordinators and 3 data stores are deployed to AWS.  An autograder then would rapidly send many GET and PUT requests to the 3 Coordinators and compare the returned value against the correct value.  The autograder has many specific test cases to ensure that eventual and strict consistency are enforced as appropriate.  The autograder is a blackbox that the students have now knowledge about.

### Coordinator Endpoints
Below are the endpoints that the coordinator must implement:

#### CoordinatorDNS:8080/consistency?consistency=CONSISTENCY_LEVEL
> This endpoint is used by the autograder to specify the level of consistency the coordinator has to support. The expected values are strong and eventual. You need to use this value to program your coordinator to handle the expected consistency level.


#### CoordinatorDNS:8080/put?key=KEY&value=VALUE&timestamp=TIME
> This endpoint will receive the key, value pair that needs to be stored in the datastore instances, as well as the timestamp representing when the client sent the request. You will need to use this timestamp to order your operations appropriately. The timestamp will be a Long value.

#### CoordinatorDNS:8080/get?key=KEY&timestamp=TIME
> This endpoint will receive the key for which the value have to be returned by the coordinator. The coordinator has to retrieve the value associated with the requested key from the local datastore, and send it back to the client. The timestamp will be necessary for determining when the request should be serviced in strong consistency.


### KeyValueLib API Functions
Below are the API functions that the KeyValueLib provides:

#### KeyValueLib.PUT(String datastoreDNS, String key, String value, String timestamp, String consistency) throws IOException
> This API method will pass along all the information of the PUT request to the specified datastore, in addition to the region associated with the coordinator making the call. There is a delay with this method, check the table below for the actual delay. Because of the delay, there may be some time between when the Coordinator issues the API call, and when the datacenter receives it. The parameters available to your KeyValueStore as a result of this call are:
>
> a. key : String
>
> b. value : String
>
> c. timestamp : Long
>
> d. region : int
>
> e. consistency : String

#### KeyValueLib.GET(String datastoreDNS, String key, String timestamp, String consistency) throws IOException
> This API method will pass along the information for the GET request to the specified datastore, and return the value that is provided by the datastore back to the Coordinator. There is no delay with this method, because the GET method is expected to be serviced by the local datacenter. You will still need the timestamp to appropriately handle the request for strong consistency. The parameters available to your KeyValueStore as a result of this call are:
>
> a. key : String
> 
> b. timestamp : Long
> 
> c. consistency : String

#### KeyValueLib.FORWARD(String coordinatorDNS, String key, String value, String timestamp) throws IOException

> This API method will forward the request to the specified coordinator. It will be as if the target coordinator received the request from a client in its region, however, an additional parameter of {"forward" : "true"} will be provided in the URL, as well as the region from which the request originated. You may check for this parameter to determine whether a request is sent by a client, (since it will be null), or if it was forwarded by another coordinator, (since it will be "true"). You will need to use this method for passing PUT requests between coordinators. There is a delay with this method, check the table below for the actual delay. The parameters available to the target Coordinator as a result of this call are:
> 
> a. key : String
> 
> b. value : String
> c. timestamp : Long
> 
> d. region : int
> 
> e. forward = "true" : String

#### KeyValueLib.AHEAD(String key, String timestamp) throws IOException

> This API method will contact the datastores in every region, and notify them that a PUT request is being serviced for the specified key, starting at the specified timestamp. It will be up to you whether or not to use this method, and how you choose to use it. There is no delay in this communication to any of the datastores. The timestamp that is provided is the timestamp that the datastores will see upon receiving this message. The parameters available to your KeyValueStore as a result of this call are:
> 
> a. key : String
> 
> b. timestamp : Long


#### KeyValueLib.COMPLETE(String key, String timestamp) throws IOException

> This API method will contact the datastores in every region, and notify them that a PUT request for the specified key that started at the given timestamp has been completed. Again, it is up to you to decide if you want to use this method. There is no delay in this communication to any of the datastores. The timestamp that is provided is the timestamp that the datastores will see upon receiving this message. The parameters available to your KeyValueStore as a result of this call are:
> 
> a. key : String
> 
> b. timestamp : Long