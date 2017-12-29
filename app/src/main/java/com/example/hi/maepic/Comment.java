package com.example.hi.maepic;

import java.text.SimpleDateFormat;
import java.util.Date;

// class to store the data of each comment when fetched from Firebase
public class Comment {
    private String text; // content of the comment
    private String author; // name of the user posting the comment
    private String articleKey; // the key of the article this comment was posted on on Firebase
    private Date date;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd MM yyyy");

    //no argument constructor
    Comment() {}
    //constructor
    Comment(String text, String author, String articleKey, Date date) {
        this.text = text;
        this.author = author;
        this.articleKey = articleKey;
        this.date = date;
    }
    //get methods to retrieve data from the database
    public String getText() {return text;}
    public String getAuthor() {return author;}
    public String getArticleKey() {return  articleKey;}
    public Date getDate() {return date;}
    //a method to print out the date in specific format
    public String printDate() {return  dateFormat.format(date);}

}