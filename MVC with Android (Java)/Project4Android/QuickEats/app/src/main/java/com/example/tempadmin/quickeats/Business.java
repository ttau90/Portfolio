package com.example.tempadmin.quickeats;

/**
 * Created by Tim on 11/16/2016.
 */
public class Business {
    String name = "";
    String rating = "";
    String address = "";

    public Business(String name, String rating, String address){
        this.name = name;
        this.rating = rating;
        this.address = address;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }
}
