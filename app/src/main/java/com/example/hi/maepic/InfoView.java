package com.example.hi.maepic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class InfoView extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;         //an instance for Firebase Database
    private DatabaseReference mDatabaseReference;       //an instance for the database listener
    private ChildEventListener mChildEventListener;     //an instance for the child listener in the database

    private SharedPreferences sharedPref;               //an instance for the shared preference

    private CommentAdapter mCommentAdapter;             //the comment adapter is used as the same as a list
    private Comment comment;                            //a comment instance
    //this is the array list used as the reference for the comment adapter
    private ArrayList<Comment> commentList = new ArrayList<Comment>();

    private String ownerName;                           //the owner of the article
    private String content;                             //the content of the article
    private String username;                            //the current user
    private String articleKey;                          //the article key ID
    private String photoURL;

    private ListView mCommentListView;                  //an instance of the list view
    private TextView editText;                          //the edit text to put in new comment
    private Button commentButton;                       //the button to send the comment

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_view);

        Log.i("InfoView", "setup Firebase Database");
        //get instance for both the database
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //set the reference to specific on the "streets" child in the database
        mDatabaseReference = mFirebaseDatabase.getReference().child("comments");

        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        //get the values from the previous activity (MapsActivity)
        ownerName = sharedPref.getString("Article Owner", "anonymous");
        content = sharedPref.getString("Article Content", "This is a content");
        username = sharedPref.getString("Current User", "anonymous");
        articleKey = sharedPref.getString("Article Key", "article");
        photoURL = sharedPref.getString("Photo URL", null);

        //initialize the article
        TextView ownerText = (TextView) findViewById(R.id.ownerText);
        ownerText.setText(ownerName);
        TextView contentText = (TextView) findViewById(R.id.contentText);
        contentText.setText(content);

        //set up the list view, the text view and the button
        mCommentListView = (ListView) findViewById(R.id.commentListView);
        editText = (TextView) findViewById(R.id.editTextComment);
        commentButton = (Button) findViewById(R.id.buttonComment);
        ImageView photoImageView = (ImageView) findViewById(R.id.imageViewPhoto);

        boolean isPhoto = photoURL != null;
        if(isPhoto) {
            photoImageView.setVisibility(View.VISIBLE);
            Glide.with(photoImageView.getContext())
                    .load(photoURL)
                    .into(photoImageView);
        }
        else {
            photoImageView.setVisibility(View.GONE);
        }

        //set up the adapter and add it to the list view
        mCommentAdapter = new CommentAdapter(this, R.layout.item_comment, commentList);
        mCommentListView.setAdapter(mCommentAdapter);

        //attach database listener
        attachDatabaseReadListener();

        //when user send the comment
        commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //set new comment with: comment content, current user, the article key, and the date
            Comment newComment = new Comment(editText.getText().toString(), username, articleKey, new Date());
            editText.setText("");       //empty the text view
            //push the new data to the database
            mDatabaseReference.push().setValue(newComment);
            }
        });
    }

    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    comment = dataSnapshot.getValue(Comment.class);
                    //if the comment is from the current article
                    if(comment.getArticleKey().equals(articleKey)) {
                        //add the comment to the adapter to display
                        mCommentAdapter.add(comment);
                    }
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }
}
