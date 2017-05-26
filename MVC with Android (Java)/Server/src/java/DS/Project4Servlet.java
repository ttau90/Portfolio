/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package DS;

import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Timestamp;
import java.util.HashSet;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
/**
 *
 * @author Tim
 */
@WebServlet(name = "Project4Servlet", 
        urlPatterns = {"/Project4Servlet", "/dashboard"})

public class Project4Servlet extends HttpServlet {
    private Model m;
    @Override
    
    public void init() throws ServletException {
        m = new Model();
    }
    
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        try (PrintWriter out = response.getWriter()) {
            /* TODO output your page here. You may use following sample code. */
            out.println("<!DOCTYPE html>");
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        //Check to see if the user is request is asking for the dashboard
        if(request.getServletPath().equals("/dashboard")){
            try{
                //Establish the connection to mongodb
                m.MongoConnect();
                
                //Get the list of requests and restaurants, and the statistics
                List<Restaurant> resList = m.getRestaurants();
                List<Request> reqList = m.getRequests();
                String popTerm = m.getPopTerm();
                String popLoc = m.getPopLocation();
                double avgRating = m.getAvgRating();
                double avgDelay = m.getAvgDelay();

                //Add the information to the request
                request.setAttribute("resList", resList);
                request.setAttribute("reqList", reqList);
                request.setAttribute("popTerm", popTerm);
                request.setAttribute("popLoc", popLoc);
                request.setAttribute("avgDelay", avgDelay);
                request.setAttribute("avgRating", avgRating);
                request.getRequestDispatcher("/WEB-INF/dashboard.jsp").forward(request, response);
            }finally{
                m.CloseMongo();
            }
        }else{
            //User was not looking for dashboard, so they are searching for term/location
            String ua = request.getHeader("User-Agent");
            String term = request.getParameter("searchTerm");
            String location = request.getParameter("searchLocation");
            String phone = "";
            
            //Check to see if the device is an anroid or an iphone
            if (ua != null && ua.indexOf("Android") != -1){
                phone = "Android";
            }else if( ua != null && (ua.indexOf("iPhone") != -1)){
                phone = "iPhone";
            }
            
            //Get current datetime to be used as the timestamp
            java.util.Date date = new java.util.Date();
            Timestamp dt =  new Timestamp(date.getTime());
    
            //Search yelp api for the term and location, gets back json of 3 restaurants
            String jsonReply = m.search(term, location, phone, dt);

            response.setStatus(200);
            response.setContentType("text/plain;charset=UTF-8");
            PrintWriter out = response.getWriter();
            out.println(jsonReply); //Returns the json string to the device
        }
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

}
