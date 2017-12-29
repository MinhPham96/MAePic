package com.example.hi.maepic;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

// activity used to show info about the app, and introduction on how to use it
public class AboutApp extends AppCompatActivity {
    String TAG = "AboutApp";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_app);
        // a text field, and the associated text
        TextView appIntro = (TextView) this.findViewById(R.id.introduction);
        appIntro.setText("MAePic - Instructions to usage \n" +
                "\n" +
                "This application requires the user to log in and the location services permission. \n \n" +
                "When starting the application, the user will be taken to the map. \n" +
                "Here, the user can tap on markers' info windows to view the respective articles" +
                " posted by themselves or other users. The user can move to 'Home' (via the options menu in the top right corner) where they can view their own articles" +
                " or log out of the application. \n \n" +
                "In the 'Home' activity, the user can view their posted articles and/or post new ones." +
                " When posting an article, the user can add an image via gallery or camera." +
                " An article posted will be associated to a marker on the map activity. It can also be deleted \n \n" +
                " When viewing another user's article. A user can post a comment on it. \n" +
                "By pressing the direction button, a route will be drawn between the user's current location" +
                " and the article's location.");
        Log.i(TAG, "Text displayed.");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i(TAG, "Running onPause.");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i(TAG, "Running onStart.");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i(TAG, "Running onRestart.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i(TAG, "Running onResume.");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i(TAG, "Running onDestroy.");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i(TAG, "Running onStop.");
    }
}