# Distributed Datastores

The objective for this project is to implement a crossregion system of Coordinators and KeyValueStores to handle requests from each region. There are three total regions, as specified previously. Each region will have its own Coordinator, and KeyValueStore. Each Coordinator and KeyValueStore exists in its own AMI instance. Thus, there are 6 instances in total 1 Coordinator and 1 KeyValueStore for each of the 3 regions. Additionally, there is a Client instance which was used to test the implementation.


The coordinators need to be able to handle strong and eventual consistency, depending on the value passed to it by the client.  Each key passed to the coordinators will be hashed to designate a primary coordinator for that key.  Only the primary coordinator may handle PUT requests for a given key.  GET requests can be handled by any coordinator.