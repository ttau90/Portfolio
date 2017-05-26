<%-- 
    Document   : dashboard
    Created on : Nov 17, 2016, 6:25:11 PM
    Author     : Tim
--%>

<%@page import="DS.Request"%>
<%@page import="java.util.Enumeration"%>
<%@page import="java.util.Iterator"%>
<%@page import="java.util.ArrayList"%>
<%@page import="java.util.List"%>
<%@page import="DS.Restaurant"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Dashboard</title>
    </head>
    <body>
        <h1>Dashboard</h1>
        <%
          List<Restaurant> resList = (ArrayList<Restaurant>) request.getAttribute("resList");
          List<Request> reqList = (ArrayList<Request>) request.getAttribute("reqList");
          String popTerm = (String) request.getAttribute("popTerm");
          String popLoc = (String) request.getAttribute("popLoc");
          double avgDelay = (Double) request.getAttribute("avgDelay");
          double avgRating = (Double) request.getAttribute("avgRating");
        %>
        <h1>Statistics</h1>
        <table>
            <tr>
                <td><input type="text" size="50" value="Most Popular Term"/></td>
                <td><input type="text" size="50" value="<%=popTerm%>"/></td>
            </tr>
            <tr>
                <td><input type="text" size="50" value="Most Popular Location"/></td>
                <td><input type="text" size="50" value="<%=popLoc%>"/></td>
            </tr>
            <tr>
                <td><input type="text" size="50" value="Avg Restaurant Rating"/></td>
                <td><input type="text" size="50" value="<%=avgRating%>"/></td>
            </tr>
            <tr>
                <td><input type="text" size="50" value="Avg Delay from Yelp"/></td>
                <td><input type="text" size="50" value="<%=avgDelay%>"/></td>
            </tr>
        </table>
            <br><br>
            <h1>Requests</h1>
        <table>
        <tr>
        <th>Entered Term</th>
        <th>Entered Location</th>
        <th>Timestamp</th>
        <th>Device Type</th>
        <th>Delay from Yelp</th>
        </tr>
        <%
        Iterator i = reqList.iterator();
        while(i.hasNext()){
            Request r = (Request)i.next();
        %>
        <tr>
           <td><input type="text" size="50" value="<%=r.getTerm()%>" /></td>
           <td><input type="text" size="50" value="<%=r.getLocation()%>" /></td>
           <td><input type="text" size="30" value="<%=r.getTimestamp()%>" /></td>
           <td><input type="text" size="20" value="<%=r.getPhone()%>" /></td>
           <td><input type="text" size="5" value="<%=r.getDelay()%>" /></td>
        </tr>
        <% } %>
        </table>
        <br><br>
        <h1>Restaurants</h1>
        <table>
        <tr>
        <th>Name</th>
        <th>Address</th>
        <th>Rating</th>
        </tr>
        <%
        i = resList.iterator();
        while(i.hasNext()){
            Restaurant r = (Restaurant)i.next();
        %>
        <tr>
           <td><input type="text" size="50" value="<%=r.getName()%>" /></td>
           <td><input type="text" size="50" value="<%=r.getLocation()%>" /></td>
           <td><input type="text" size="5" value="<%=r.getRating()%>" /></td>
        </tr>
        <% } %>
        </table>
    </body>
</html>
