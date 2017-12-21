package com.example.hi.maepic;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class AccountActivity extends AppCompatActivity {

    private FirebaseDatabase mFirebaseDatabase;         //an instance for Firebase Database
    private DatabaseReference mDatabaseReference;       //an instance for the database listener
    private ChildEventListener mChildEventListener;     //an instance for the child listener in the database
    private FirebaseStorage mFirebaseStorage;
    private StorageReference mPhotoStorageReference;

    private ArticleAdapter mArticleAdapter;             //the comment adapter is used as the same as a list
    private Article article;                            //a comment instance
    //this is the array list used as the reference for the comment adapter
    private ArrayList<Article> articleList = new ArrayList<Article>();

    private SharedPreferences sharedPref;       //an instance for the shared preference

    private Uri selectedImageUri;

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
        mFirebaseStorage = mFirebaseStorage.getInstance();
        //set the reference to specific on the "streets" child in the database
        mDatabaseReference = mFirebaseDatabase.getReference().child("articles");
        mPhotoStorageReference = mFirebaseStorage.getReference().child("article_photos");

        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);

        username = sharedPref.getString("Current User", "anonymous");
        Log.i("AccountActivity", username);
        userKey = sharedPref.getString("User Key", "nothing");
        Log.i("AccountActivity", userKey);
        latitude = (double) sharedPref.getFloat("Current Latitude", 0);
        Log.i("AccountActivity", String.valueOf(latitude));
        longitude = (double) sharedPref.getFloat("Current Longitude", 0);
        Log.i("AccountActivity", String.valueOf(longitude));

        final EditText editText = (EditText) findViewById(R.id.commentEditText);
        final ListView listView = (ListView) findViewById(R.id.statusListView);
        final Button cameraButton = this.findViewById(R.id.buttonCamera);
        final Button galleryButton = this.findViewById(R.id.buttonGallery);
        final ImageView imageView = this.findViewById(R.id.imageViewPhoto);
        imageView.setVisibility(View.GONE);

        mArticleAdapter = new ArticleAdapter(this, R.layout.item_status, articleList);
        listView.setAdapter(mArticleAdapter);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 0);
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, 1);
            }
        });

        final Button buttonPost = findViewById(R.id.buttonPost);
        buttonPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            buttonPost.setEnabled(false);
            if(!editText.getText().toString().replace(" ","").isEmpty() || selectedImageUri != null) {
                if (selectedImageUri != null) {
                    StorageReference photoRef = mPhotoStorageReference.child(selectedImageUri.getLastPathSegment());
                    photoRef.putFile(selectedImageUri)
                            .addOnSuccessListener(AccountActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // When the image has successfully uploaded, we get its download URL
                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                    String content = editText.getText().toString();

                                    Article newArticle = new Article(content, username, userKey, latitude, longitude, downloadUrl.toString(), null, new Date());
                                    mDatabaseReference.push().setValue(newArticle);
                                    editText.setText("");
                                    imageView.setVisibility(View.GONE);
                                    selectedImageUri = null;
                                }
                            });
                } else {
                    String content = editText.getText().toString();
                    Article newArticle = new Article(content, username, userKey, latitude, longitude, null, null, new Date());
                    mDatabaseReference.push().setValue(newArticle);
                    editText.setText("");
                }
            }
            buttonPost.setEnabled(true);
            }
        });

        attachDatabaseReadListener();
    }

    // onActivityResult function called depending on the request code sent
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        // super initialization
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
        ImageView imageView = this.findViewById(R.id.imageViewPhoto);
        switch(requestCode) {
            case 0: // first case, image was taken from camera
                if(resultCode == RESULT_OK){ // if activity is successful
                    // create output stream
                    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                    // obtain bitmap from photo taken
                    Bitmap photo = (Bitmap) imageReturnedIntent.getExtras().get("data");
                    // compress photo
                    photo.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                    // create temporary image file from the photo taken
                    File file = createImageFile();
                    if (file != null) {
                        FileOutputStream fout;
                        try {
                            // compress and then flush output file
                            fout = new FileOutputStream(file);
                            photo.compress(Bitmap.CompressFormat.PNG, 70, fout);
                            fout.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        // obtaining Uri from flushed file
                        selectedImageUri = Uri.fromFile(file);
                    }
                    // make the imageView visible
                    imageView.setVisibility(View.VISIBLE);
                    // display the captured image
                    imageView.setImageURI(selectedImageUri);
                }
                break;
            case 1: // the case where the image is selected from gallery
                if(resultCode == RESULT_OK){
                    // get the Uri and again, display the image
                    selectedImageUri = imageReturnedIntent.getData();
                    imageView.setVisibility(View.VISIBLE);
                    imageView.setImageURI(selectedImageUri);
                }
                break;
        }
    }


    private void attachDatabaseReadListener() {
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    article = dataSnapshot.getValue(Article.class);
                    //if the comment is from the current article
                    if(article.getUid().equals(userKey)) {
                        //add the comment to the adapter to display
                        mArticleAdapter.add(article);
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

    // function to create temporary file
    public File createImageFile() {
        // create timestamp for file
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        // create file name
        String imageFileName = "JPEG_" + timeStamp + "_";
        // create empty file
        File mFileTemp = null;
        // create string story file directory
        String root=AccountActivity.this.getDir("my_sub_dir",Context.MODE_PRIVATE).getAbsolutePath();
        // create file representing directory
        File myDir = new File(root + "/Img");
        // create directory if it does not exist
        if(!myDir.exists()){
            myDir.mkdirs();
        }
        try {
            //  store file at directory into temporary file
            mFileTemp=File.createTempFile(imageFileName,".jpg",myDir.getAbsoluteFile());
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        // return the temporary file
        return mFileTemp;
    }
}
