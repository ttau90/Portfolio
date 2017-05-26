# MVC with Android (Java)

These programs create a server and a very basic android app where the user can utilize the android app to query
yelp for a search term and location.  The 3 closest restaurants matching are returned.
The search term, as well as restaurant ratings are saved on to a MongoDB database and can be displayed in a dashboard on the server.
For additional details and screenshots, see Documentation.pdf.

Server - This program creates a server that utilizes the MVC pattern to handle the main processing of querying yelp, storing data, and displaying data.
Project4Android - This contains the very basic android app that will prompt the user to enter a search term and location and queries the server.