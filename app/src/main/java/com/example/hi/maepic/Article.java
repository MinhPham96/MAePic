package com.example.hi.maepic;

/**
 * Created by Hi on 16-Dec-17.
 */

public class Article {
    private String text;
    private String owner;
    private String uid;
    private double latitude;
    private double longitude;
    private String photoURL;
    private String iconURL;

    Article() {}

    Article(String text, String owner, String uid, double latitude, double longitude, String photoURL, String iconURL) {
        this.text = text;
        this.owner = owner;
        this.uid = uid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.photoURL = photoURL;
        this.iconURL = iconURL;
    }

    public String getText() {return text;}
    public String getOwner() {return owner;}
    public String getUid() {return uid;}
    public double getLatitude() {return  latitude;}
    public double getLongitude() {return longitude;}
    public String getPhotoURL() {return photoURL;}
    public String getIconURL() {return iconURL;}

}
