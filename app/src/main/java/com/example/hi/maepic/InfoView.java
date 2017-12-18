package com.example.hi.maepic;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

public class InfoView extends AppCompatActivity {

    private SharedPreferences sharedPref;               //an instance for the shared preference

    String ownerName;
    String content;
    String username;
    String articleKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_view);

        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        //get the index and username from the previous activity (MapView)
        ownerName = sharedPref.getString("Article Owner", "anonymous");
        content = sharedPref.getString("Article Content", "This is a content");
        username = sharedPref.getString("Current User", "anonymous");
        articleKey = sharedPref.getString("Article Key", "article");

        TextView ownerText = (TextView) findViewById(R.id.ownerText);
        ownerText.setText(ownerName);
        TextView contentText = (TextView) findViewById(R.id.contentText);
        contentText.setText(content);
    }
}
