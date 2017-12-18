package com.example.hi.maepic;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Hi on 18-Dec-17.
 */

public class Comment {
    private String text;
    private String author;
    private String articleKey;
    private Date date;
    private static final SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yy");

    Comment() {}

    Comment(String text, String author, String articleKey, Date date) {
        this.text = text;
        this.author = author;
        this.articleKey = articleKey;
        this.date = date;
    }

    public String getText() {return text;}
    public String getAuthor() {return author;}
    public String getArticleKey() {return  articleKey;}
    public Date getDate() {return date;}
    public String printDate() {return  dateFormat.format(date);}

}
