package com.example.hi.maepic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

public class InfoView extends AppCompatActivity implements AbsListView.OnScrollListener{

    private FirebaseDatabase mFirebaseDatabase;                 //an instance for Firebase Database
    private DatabaseReference mCommentsDatabaseReference;       //an instance for the "comments" child
    private DatabaseReference mArticleDatabaseReference;        //an instance for the "articles" child
    private ChildEventListener mCommentChildEventListener;      //an instance for the child listener in the database
    private FirebaseStorage mFirebaseStrorage;                  //an instance for the Firebase storage

    private FirebaseAuth mFirebaseAuth;                         //an instance for the authentication
    //an instance for the authentiation state listener
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    public static final int RC_SIGN_IN = 1;                     //a constance for sign in

    private SharedPreferences sharedPref;               //an instance for the shared preference

    private CommentAdapter mCommentAdapter;             //the comment adapter is used as the same as a list
    private Comment comment;                            //a comment instance
    //this is the array list used as the reference for the comment adapter
    private ArrayList<Comment> commentList = new ArrayList<Comment>();

    private String ownerName;                           //the owner of the article
    private String content;                             //the content of the article
    private String username;                            //the current user name
    private String userKey;                             //the current user key
    private String articleKey;                          //the article key ID
    private String articleOwnerKey;                     //the article owner ID
    private String photoURL;                            //the photo URL in the article
    private Integer iconURL;                            //the icon URL of the article
    //the set is to get the key set from the maps activity
    private Set<String> expiredKeySet = new HashSet<String>();
    //while the list is to store the set since set does not support get function
    private ArrayList<String> expiredKeyList;
    private ArrayList<String> commentKeyList = new ArrayList<String>();

    private ListView mCommentListView;      //an instance of the list view
    private TextView editText;              //the edit text to put in new comment
    private Button commentButton;           //the button to send the comment
    private Button routeButton;             //the button to trigger draw route to this article location
    private Button deleteButton;            //the button to delete the article manually
    //the image views for the photo and icon
    private ImageView photoImageView, avatarImageView;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_info_view);
        Log.i("InfoView", "running onCreate");

        Log.i("InfoView", "setup Firebase Database");
        //get instance for the database, storage and authentication
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStrorage = FirebaseStorage.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        //set the reference to specific on the "comments" child in the database
        mCommentsDatabaseReference = mFirebaseDatabase.getReference().child("comments");
        //set the reference to specific on the "articles" child in the database
        mArticleDatabaseReference = mFirebaseDatabase.getReference().child("articles");

        Log.i("InfoView", "setup Shared Preference");
        //initialize the shared preference
        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        //get the values from the previous activity
        //this is either from the Maps Activity or the Account Activity
        ownerName = sharedPref.getString("Article Owner", "anonymous");
        content = sharedPref.getString("Article Content", "This is a content");
        articleKey = sharedPref.getString("Article Key", "article");
        articleOwnerKey = sharedPref.getString("Article Owner ID", "anonymous");
        userKey = sharedPref.getString("User Key", "anonymous");
        photoURL = sharedPref.getString("Photo URL", null);
        iconURL = sharedPref.getInt("Icon URL", 0);
        //get the set from the shared preference
        expiredKeySet = sharedPref.getStringSet("Expired Key", null);
        Log.i("InfoView", "convert expired key list");
        //convert the set to the array list
        expiredKeyList = new ArrayList<String>(expiredKeySet);

        Log.i("InfoView", "setup layout contains");
        //set up the list view, the text view and the button
        mCommentListView = (ListView) findViewById(R.id.commentListView);

        //set up the adapter and add it to the list view
        mCommentAdapter = new CommentAdapter(this, R.layout.item_comment, commentList);
        mCommentListView.setAdapter(mCommentAdapter);

        Log.i("InfoView", "Add header to list view");
        // Inflate custom header and attach it to the list
        LayoutInflater inflater = getLayoutInflater();
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.custom_header_status, mCommentListView, false);
        mCommentListView.addHeaderView(header, null, false);

        // Declare elements for the header
        editText = (TextView) header.findViewById(R.id.editTextComment);
        header = (RelativeLayout)header.findViewById(R.id.header);
        commentButton = (Button)header.findViewById(R.id.buttonComment);
        routeButton = (Button)header.findViewById(R.id.buttonRoute);
        deleteButton = (Button)header.findViewById(R.id.buttonDelete);
        photoImageView = (ImageView)header.findViewById(R.id.imageViewPhoto);
        avatarImageView = (ImageView)header.findViewById(R.id.imageViewAvatar);

        //if the current user ID match with the article owner ID
        //than the delete button is enable, else it is disabled
        //since only the owner of the article can delete the article manually
        Log.i("InfoView", "Check if article belongs to current user");
        if(userKey.equals(articleOwnerKey)) deleteButton.setVisibility(View.VISIBLE);
        else deleteButton.setVisibility(View.GONE);

        //initialize the article
        TextView ownerText = (TextView)header.findViewById(R.id.ownerText);
        ownerText.setText(ownerName);
        TextView contentText = (TextView)header.findViewById(R.id.contentText);
        contentText.setText(content);
        avatarImageView.setImageResource(iconURL);

        mCommentListView.setOnScrollListener(this);

        //if there is a photo in the article, display it in the image view
        //else, hide the image view
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

        final MediaPlayer buttonSound = MediaPlayer.create(this, R.raw.sound);

        Log.i("InfoView", "setup Comment button");
        //when user send the comment
        commentButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSound.start();
                if(!editText.getText().toString().replace(" ","").isEmpty()) {
                    //set new comment with: comment content, current user, the article key, and the date
                    Comment newComment = new Comment(editText.getText().toString(), username, articleKey, new Date());
                    editText.setText("");       //empty the comment edit text
                    //push the new data to the database
                    mCommentsDatabaseReference.push().setValue(newComment);
                    Log.i("InfoView", "Comment pushed");
                }
            }
        });

        Log.i("InfoView", "setup Route button");
        //if the user want to get the route to this article location
        routeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSound.start();
                //set draw route flag to true
                sharedPref.edit().putBoolean("Draw Route", true).apply();
                Log.i("InfoView", "Draw route enabled, move to MapsActivity");
                //go back to map
                Intent intent = new Intent(InfoView.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        Log.i("InfoView", "setup Delete button");
        //when user choose to delete this article
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSound.start();
                Log.i("InfoView", "delete all comments");
                //delete all the comment in the article
                for(int key = 0; key < commentKeyList.size(); key++) {
                    mCommentsDatabaseReference.child(commentKeyList.get(key)).removeValue();
                }
                //if there is a photo, delete it in the storage
                if(photoURL != null) {
                    Log.i("InfoView", "delete photo");
                    StorageReference photoRef = mFirebaseStrorage.getReferenceFromUrl(photoURL);
                    photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            // File deleted successfully
                            Log.d("Info View", "onSuccess: deleted file");
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception exception) {
                            // Uh-oh, an error occurred!
                            Log.d("Info View", "onFailure: did not delete file");
                        }
                    });
                }
                //delete the article
                mArticleDatabaseReference.child(articleKey).removeValue();
                Log.i("InfoView", "article deleted");
                Log.i("InfoView", "move to MapsActivity");
                //go back to map, since this article is deleted
                Intent intent = new Intent(InfoView.this, MapsActivity.class);
                startActivity(intent);
            }
        });

        //set up authentication
        Log.i("InfoView", "setup Firebase Authentication");
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            //when the authentication state changed (eg. sign in, sign out)
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //get the user from the database
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //if user is signed in
                if (user != null) {
                    username = user.getDisplayName();
                    Log.i("Info View", "Signed In");
                }
                else {
                    Log.i("Info View", "Signed Out");
                    //stop the database acitivity
                    //create a sign in menu
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder()            //create a sign in instance
                                    .setIsSmartLockEnabled(false)           //disable smart lock feature
                                    .setProviders(AuthUI.EMAIL_PROVIDER)    //set the sign in type to email
                                    .build(), RC_SIGN_IN);                  //build the sign in menu
                }
            }
        };

        //attach database listener
        attachDatabaseReadListener();
    }

    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

    }

    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//        Rect rect = new Rect();
//        editText.getLocalVisibleRect(rect);
//        if (lastTopValue != rect.top) {
//            lastTopValue = rect.top;
//            editText.setY((float) (rect.top / 2.0));
//        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("InfoView", "running onResume");
        if(mAuthStateListener != null) {
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("InfoView", "running onPause");
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    private void attachDatabaseReadListener() {
        Log.i("InfoView", "Attach comment database listener");
        if (mCommentChildEventListener == null) {
            mCommentChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    comment = dataSnapshot.getValue(Comment.class);
                    //if there is expired key in the set
                    if(expiredKeySet.size() != 0) {
                        //check if any comment belongs to an expired article
                        for(int key = 0; key < expiredKeyList.size(); key++) {
                            if(expiredKeyList.get(key).equals(comment.getArticleKey())) {
                                //remove the comment in the expired article
                                mCommentsDatabaseReference.child(dataSnapshot.getKey()).removeValue();
                                Log.i("InfoView", "Comment from expired article, delete comment");
                            }
                        }
                    }
                    //if the comment is from the current article
                    if(comment.getArticleKey().equals(articleKey)) {
                        //add the comment to the adapter to display
                        mCommentAdapter.add(comment);
                        commentKeyList.add(dataSnapshot.getKey());
                        Log.i("InfoView", "article comment added");
                    }
                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //since the newly updated one will be check for censorship
                    comment = dataSnapshot.getValue(Comment.class);
                    //it will be located at the end of the adapter
                    if(mCommentAdapter.getCount() > 0) {
                        //since this function is called on every change
                        //the app needs to check if the comment belong to the article or not
                        if(comment.getArticleKey().equals(articleKey)) {
                            Comment changedComment = mCommentAdapter.getItem(mCommentAdapter.getCount() - 1);
                            //remove the uncensored comment with the censored one
                            mCommentAdapter.remove(changedComment);
                            mCommentAdapter.add(comment);
                            //notify the adapter to refresh the view
                            mCommentAdapter.notifyDataSetChanged();
                            Log.i("InfoView", "article comment censorship checked");
                        }
                    }

                }
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            mCommentsDatabaseReference.addChildEventListener(mCommentChildEventListener);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("InfoView", "running onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("InfoView", "running onRestart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("InfoView", "running onStop");
    }

    @Override
    protected void onDestroy() {
        Log.i("InfoView", "running onDestroy");
        super.onDestroy();
    }
}