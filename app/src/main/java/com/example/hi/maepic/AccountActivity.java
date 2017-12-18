package com.example.hi.maepic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class AccountActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;         //an instance for Firebase Database
    private DatabaseReference mDatabaseReference;       //an instance for the database listener
    private ChildEventListener mChildEventListener;     //an instance for the child listener in the database

    private SharedPreferences sharedPref;       //an instance for the shared preference

    double latitude = 0.0;
    double longitude = 0.0;
    String username;
    String userKey;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        Log.i("AccountActivity", "setup Firebase Database");
        //get instance for both the database and authentiaction
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //set the reference to specific on the "streets" child in the database
        mDatabaseReference = mFirebaseDatabase.getReference().child("articles");

        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);

        username = sharedPref.getString("Current User", "anonymous");
        Log.i("AccountActivity", username);
        userKey = sharedPref.getString("User Key", "nothing");
        Log.i("AccountActivity", userKey);
        latitude = (double) sharedPref.getFloat("Current Latitude", 0);
        Log.i("AccountActivity", String.valueOf(latitude));
        longitude = (double) sharedPref.getFloat("Current Longitude", 0);
        Log.i("AccountActivity", String.valueOf(longitude));

        final EditText editText = (EditText) findViewById(R.id.editText);

        final Button buttonPost = findViewById(R.id.buttonPost);
        buttonPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String content = editText.getText().toString();
                Article newArticle = new Article(content, username, userKey, latitude, longitude, null, null);
                mDatabaseReference.push().setValue(newArticle);
                editText.setText("");
            }
        });
    }
}
