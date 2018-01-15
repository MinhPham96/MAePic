package com.example.hi.maepic;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.media.MediaPlayer;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.Spinner;

import com.firebase.ui.auth.AuthUI;
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
    private FirebaseStorage mFirebaseStorage;           //an instance for Firebase Storage (for photo)
    private StorageReference mPhotoStorageReference;    //an instance for the folder reference in the storage

    private FirebaseAuth mFirebaseAuth;                 //an instance for the authentication
    //an instance for the authentication state listener
    private FirebaseAuth.AuthStateListener mAuthStateListener;
    public static final int RC_SIGN_IN = 1;             //a constance for sign in

    private ArticleAdapter mArticleAdapter;             //the article adapter is used as the same as a list
    private Article article;                            //a article instance
    //this is the array list used as the reference for the article adapter
    private ArrayList<Article> articleList = new ArrayList<Article>();
    //the key list stores all the ID of the article belong to this user
    //this is used to parse to the Info view when user tap into the article in their article list
    private ArrayList<String> keyList = new ArrayList<String>();

    private SharedPreferences sharedPref;   //an instance for the shared preference

    private Uri selectedImageUri;           //an instance that store the location of a photo in the device
    private Button clearButton;             //an instance of the clear button
    private Button postButton;
    private Button cameraButton;
    private Button galleryButton;
    private ImageView imageView;            //an instance of the image view

    double latitude = 0.0;      //an instance to store the user current latitude
    double longitude = 0.0;     //an instance to store the user current longitude
    String username;            //an instance to store the current user name
    String userKey;             //an instance to store the current user ID

    //this text array is used to display on the spinner
    String[] textArray = { "Default","Important", "Eating", "Shopping", "Place" };
    //this Integer array contains all the image ID in the drawable
    //these IDs are corresponded with the selection in the spinner
    //the spinner provide user the option to select their own marker icon on the map for their articles
    Integer[] imageArray = { R.drawable.ic_default, R.drawable.ic_star, R.drawable.ic_forkknife,
            R.drawable.ic_shopping, R.drawable.ic_camera };
    private Spinner spinner;    //an instance of the spinner


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);
        Log.i("AccountActivity", "running onCreate");

        Log.i("AccountActivity", "setup Firebase Database");
        //get instance for both the database and authentication
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseStorage = FirebaseStorage.getInstance();
        //set the reference to specific on the "articles" child in the database
        mDatabaseReference = mFirebaseDatabase.getReference().child("articles");
        //set the reference to the "article_photos" folder in the storage
        mPhotoStorageReference = mFirebaseStorage.getReference().child("article_photos");
        //get the instance for the authentication
        //since the storage require authentication to access
        mFirebaseAuth = FirebaseAuth.getInstance();

        Log.i("AccountActivity", "setup Shared Preference");
        //initialize the shared preference
        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);
        //update the instance from the values parsed from the Maps Activity
        latitude = (double) sharedPref.getFloat("Current Latitude", 0);
        Log.i("AccountActivity", String.valueOf(latitude));
        longitude = (double) sharedPref.getFloat("Current Longitude", 0);
        Log.i("AccountActivity", String.valueOf(longitude));
        username = sharedPref.getString("Current User", "anonymous");
        userKey = sharedPref.getString("User Key", "anonymous");

        Log.i("AccountActivity", "setup layout contains");
        //initialize all the layout variables
        final EditText editText = (EditText) findViewById(R.id.commentEditText);
        final ListView listView = (ListView) findViewById(R.id.statusListView);

        cameraButton = this.findViewById(R.id.buttonCamera);
        galleryButton = this.findViewById(R.id.buttonGallery);
        clearButton = this.findViewById(R.id.buttonClear);
        postButton = this.findViewById(R.id.buttonPost);

        imageView = this.findViewById(R.id.imageViewPhoto);
        //hide the clear button and the image view for user photo selection
        //since initially there is no photo selected or clear yet
        imageView.setVisibility(View.GONE);
        clearButton.setVisibility(View.GONE);

        //set up the article adapter
        //this require the context of this activity, the item layout xml and the array list that stores the data
        mArticleAdapter = new ArticleAdapter(this, R.layout.item_status, articleList);
        //set this adapter for the list view
        listView.setAdapter(mArticleAdapter);

        final MediaPlayer buttonSound = MediaPlayer.create(this, R.raw.sound);

        Log.i("AccountActivity", "setup Camera button");
        //when user tap on the camera button
        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switch to the camera intent
                buttonSound.start();
                Log.i("AccountActivity", "move to Camera");
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(cameraIntent, 0);
            }
        });

        Log.i("AccountActivity", "setup Gallery button");
        //when user tap on the gallery button
        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //switch to the gallery intent
                buttonSound.start();
                Log.i("AccountActivity", "move to Gallery");
                Intent galleryIntent = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(galleryIntent, 1);
            }
        });

        Log.i("AccountActivity", "setup Clear button");
        //when user tap on the gallery button
        clearButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSound.start();
                Log.i("AccountActivity", "Clear image");
                selectedImageUri = null;
                imageView.setVisibility(View.GONE);
                clearButton.setVisibility(View.GONE);
            }
        });

        Log.i("AccountActivity", "setup Spinner");
        spinner = (Spinner) findViewById(R.id.spinner);     //initialize the spinner
        //setup the adapter for the spinner, this requires the context of this activity, the item layout xml
        //and the array lists that stores the data
        SpinnerAdapter adapter = new SpinnerAdapter(this, R.layout.row, textArray, imageArray);
        spinner.setAdapter(adapter);        //set the adapter for the spinner



        Log.i("AccountActivity", "setup Post button");
        //when user tap on the button post
        final Button buttonPost = findViewById(R.id.buttonPost);
        buttonPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            //get the current selected icon option in the spinner
            final int row = spinner.getSelectedItemPosition();
            //get the text from the edit text, and remove all the space
            String articleText = editText.getText().toString().replace(" ","");

            //this app requires user to have a text content in their article
            //the photo is optional
            //if there is text content and photo selected
            //the process of uploading the photo may take some times to finish
            //when tap the post button, please wait for a moment before everything is updated
            if (!articleText.isEmpty()) {
                // while posting, the buttons are set  to be gone to avoid changing data halfway
                buttonPost.setVisibility(View.GONE);
                cameraButton.setVisibility(View.GONE);
                galleryButton.setVisibility(View.GONE);
                clearButton.setVisibility(View.GONE);

                buttonSound.start();
                if(selectedImageUri != null) {
                    //get the reference of the last position in the "article_photos" folder
                    StorageReference photoRef = mPhotoStorageReference.child(selectedImageUri.getLastPathSegment());
                    photoRef.putFile(selectedImageUri)      //upload the photo to the storage
                            .addOnSuccessListener(AccountActivity.this, new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                    // When the image has successfully uploaded, we get its download URL
                                    Uri downloadUrl = taskSnapshot.getDownloadUrl();
                                    //get the text content from the edit text
                                    String content = editText.getText().toString();

                                    //create new article with current user's name, key, location
                                    //the photo url that just uploaded, the icon ID value, and new datae
                                    Article newArticle = new Article(content, username, userKey, latitude, longitude, downloadUrl.toString(), imageArray[row], new Date());
                                    //push the new article to the database
                                    mDatabaseReference.push().setValue(newArticle);
                                    Log.i("AccountActivity", "Article pushed");
                                    //empty the edit text, hide the clear button and image view
                                    editText.setText("");
                                    imageView.setVisibility(View.GONE);
                                    clearButton.setVisibility(View.GONE);
                                    selectedImageUri = null;
                                }
                            });
                }
                else {
                    buttonPost.setVisibility(View.GONE);       //disable the button
                    //get the content in the edit text
                    String content = editText.getText().toString();
                    //create new article with the same property as above, but the photo URL is null
                    Article newArticle = new Article(content, username, userKey, latitude, longitude, null, imageArray[row], new Date());
                    //push the new article to the database
                    mDatabaseReference.push().setValue(newArticle);
                    Log.i("AccountActivity", "Article pushed");
                    editText.setText("");       //empty the edit text
                }
            }
            //if there is only text

            }
        });

        //set up user authentication
        Log.i("AccountActivity", "setup Firebase Authentication");
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            //when the authentication state changed (eg. sign in, sign out)
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //get the user from the database
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //if user is signed in
                if (user != null) {
                    Log.i("AccountActivity", "Signed In");
                }
                else {
                    Log.i("AccountActivity", "Signed Out");
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

        attachDatabaseReadListener();       //attach the listener for child "articles"

        //when the user tap on the article in the list
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Log.i("AccountActivity", "Article selected");
                //send all the necessary values to the shared preference
                //these values are needed for the functions in the info view
                //the position identify the row number that is selected
                sharedPref.edit().putString("Article Key", keyList.get(position)).apply();
                sharedPref.edit().putString("Article Owner", articleList.get(position).getOwner()).apply();
                sharedPref.edit().putString("Article Owner ID", articleList.get(position).getUid()).apply();
                sharedPref.edit().putString("Article Content", articleList.get(position).getText()).apply();
                sharedPref.edit().putString("User Key", mFirebaseAuth.getCurrentUser().getUid()).apply();
                sharedPref.edit().putInt("Icon URL", articleList.get(position).getIconURL()).apply();
                if(articleList.get(position).getPhotoURL() != null) {
                    sharedPref.edit().putString("Photo URL", articleList.get(position).getPhotoURL()).apply();
                }
                else {
                    sharedPref.edit().putString("Photo URL", null).apply();
                }
                //mark the chosen article latitude and longitude
                sharedPref.edit().putFloat("Des Latitude", (float) articleList.get(position).getLatitude()).apply();
                sharedPref.edit().putFloat("Des Longitude", (float) articleList.get(position).getLongitude()).apply();
                Log.i("AccountActivity", "Move to InfoView");
                //move to the info view of the selected article
                Intent intent = new Intent(AccountActivity.this,InfoView.class);
                startActivity(intent);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("AccountActivity", "running onResume");
        if(mAuthStateListener != null) {
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("AccountActivity", "running onPause");
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
    }

    // onActivityResult function called depending on the request code sent
    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        // super initialization
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);
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
                        Log.i("AccountActivity", "Get image");
                        // obtaining Uri from flushed file
                        selectedImageUri = Uri.fromFile(file);
                    }
                    Log.i("AccountActivity", "Display image");
                    //display the image view and cancel button
                    imageView.setVisibility(View.VISIBLE);
                    clearButton.setVisibility(View.VISIBLE);
                    // display the captured image
                    imageView.setImageURI(selectedImageUri);
                }
                break;
            case 1: // the case where the image is selected from gallery
                if(resultCode == RESULT_OK){
                    Log.i("AccountActivity", "Get image");
                    // get the Uri and again, display the image
                    selectedImageUri = imageReturnedIntent.getData();
                    //display the image view and cancel button
                    imageView.setVisibility(View.VISIBLE);
                    clearButton.setVisibility(View.VISIBLE);
                    Log.i("AccountActivity", "Display image");
                    // display the captured image
                    imageView.setImageURI(selectedImageUri);
                }
                break;
        }
    }


    private void attachDatabaseReadListener() {
        Log.i("AccountActivity", "Attach database listener");
        if (mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    article = dataSnapshot.getValue(Article.class);
                    //if the article is from the current user
                    if(article.getUid().equals(userKey)) {
                        //add the article to the adapter to display
                        mArticleAdapter.add(article);
                        keyList.add(dataSnapshot.getKey());
                        Log.i("AccountActivity", "Add article");
                    }
                    //re-enable the post, camera and gallery buttons when a new post is successfully added
                    postButton.setVisibility(View.VISIBLE);
                    cameraButton.setVisibility(View.VISIBLE);
                    galleryButton.setVisibility(View.VISIBLE);
                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                    //since the newly updated one will be check for censorship
                    article = dataSnapshot.getValue(Article.class);
                    //it will be located at the end of the adapter
                    if(mArticleAdapter.getCount() > 0) {
                        //since this function is called on every change
                        //the app needs to check if the article being change belong to this user or not
                        if(article.getUid().equals(userKey)) {
                            Article changedArticle = mArticleAdapter.getItem(mArticleAdapter.getCount() - 1);
                            //remove the uncensored article text with the censored one
                            mArticleAdapter.remove(changedArticle);
                            mArticleAdapter.add(article);
                            //notify the adapter to refresh the view
                            mArticleAdapter.notifyDataSetChanged();
                            Log.i("AccountActivity", "Checked article censorship");
                        }
                    }
                }
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    // function to create temporary file
    public File createImageFile() {
        Log.i("AccountActivity", "convert camera photo to image file");
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

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("AccountActivity", "running onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("AccountActivity", "running onRestart");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.i("AccountActivity", "running onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("AccountActivity", "running onDestroy");
    }
}
