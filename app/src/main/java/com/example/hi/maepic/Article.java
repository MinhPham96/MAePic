package com.example.hi.maepic;

import java.text.SimpleDateFormat;
import java.util.Date;

// class to store information of each article when data is taken from Firebase

public class Article {
    private String text; // text content of the article
    private String owner; // name of the posting user
    private String uid; // ID of the user posting the article
    // location of the posting user
    private double latitude;
    private double longitude;
    private String photoURL; // URL for the photo posted along with the text
    private Integer iconURL; // URL for the icon associated to the activity in the post
    private Date date;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MM yyyy");

    //no argument constructors
    Article() {}
    //constructor
    Article(String text, String owner, String uid, double latitude, double longitude, String photoURL, Integer iconURL, Date date) {
        this.text = text;
        this.owner = owner;
        this.uid = uid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoURL = photoURL;
        this.iconURL = iconURL;
        this.date = date;
    }

    //get methods to retrieve data from the database
    public String getText() {return text;}
    public String getOwner() {return owner;}
    public String getUid() {return uid;}
    public double getLatitude() {return  latitude;}
    public double getLongitude() {return longitude;}
    public String getPhotoURL() {return photoURL;}
    public Integer getIconURL() {return iconURL;}
    public Date getDate() {return date;}
    //a method to print out the date with a specific format
    public String printDate() {return  dateFormat.format(date);}

}