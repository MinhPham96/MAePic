package com.example.hi.maepic;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class MapsActivity extends AppCompatActivity implements
        OnMapReadyCallback,                                     //for map set up
        GoogleApiClient.ConnectionCallbacks,                    //to connect to Google API Client for Google Map
        GoogleApiClient.OnConnectionFailedListener,             //checker for Google API Client
        com.google.android.gms.location.LocationListener {      //for current location retrieval

    private GoogleMap mMap;                         //an instance for Google Map
    private Marker myMarker;                        //an instance to create marker on Google Map
    private GoogleApiClient mGoogleApiClient;       //an instance for Google API Client
    private LocationRequest mLocationRequest;       //an instance for Location setup

    //set up the name for the map
    public static final String TAG = MapsActivity.class.getSimpleName();
    private final static int CONNECTION_FAILURE_RESOLUTION_REQUEST = 9000;

    private FirebaseDatabase mFirebaseDatabase;         //an instance for Firebase Database
    private DatabaseReference mDatabaseReference;       //an instance for the database listener
    private ChildEventListener mChildEventListener;     //an instance for the child listener in the database
    private FirebaseStorage mFirebaseStrorage;

    public static final int RC_SIGN_IN = 1;                     //a constance for sign in
    private String mUsername;                                   //an instance that holds the username
    public static final String ANONYMOUS = "anonymous";         //default username
    private FirebaseAuth mFirebaseAuth;                         //an instance for the authentication
    //an instance for the authentication state listener
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private EditText searchBar;                 //an instance of the search bar
    private Button buttonLeft;                  //an instance of button left
    private Button buttonRight;                 //an instance of button right

    private SharedPreferences sharedPref;       //an instance for the shared preference
    ArrayList<String> keyList = new ArrayList<String>();
    ArrayList<Article> articleList = new ArrayList<Article>();
    //Shared preference only support set so set is used to store the expired key
    Set<String> expiredKey = new HashSet<String>();
    //set the expired time of the article, in this case 2 days
    private static final int expiredTime = 2;

    ArrayList<Marker> markers = new ArrayList<Marker>();         // array of all markers on map
    ArrayList<Marker> searchedMarkers = new ArrayList<Marker>(); // array of searched markers
    int searchedMarkersIndex = 0;               //the index of the searched marker

    private double currentLatitude;             //the latitude of the current user
    private double currentLongitude;            //the longitude of the current user
    private double desLatitude;                 //the latitude of the destination article
    private double desLongitude;                //the longitude of the destination article
    private boolean drawRoute = false;          //a flag to indicate is draw route is enabled or not

    @Override
    public void onBackPressed() {
        //prevent user from pressing back
        //so user can only get back to main menu by signing out
        moveTaskToBack(true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps2);
        Log.i("MapsActivity", "Running onCreate");

        Log.i("MapsActivity", "setup map");
        setUpMapIfNeeded();

        Log.i("MapsActivity", "setup Google API Client");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)           //add the connection call backs
                .addOnConnectionFailedListener(this)    //add the connection failed notice
                .addApi(LocationServices.API)           //add location services
                .build();                               //build the new Google API Client

        Log.i("MapsActivity", "setup Location Request");
        mLocationRequest = LocationRequest.create()
                //set the priority to high accuracy to have more precise result
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                //set the interval to update the location
                .setInterval(1000);

        Log.i("MapsActivity", "setup Firebase Database");
        //get instance the database, storage and authentication
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseStrorage = FirebaseStorage.getInstance();
        //set the reference to specific on the "articles" child in the database
        mDatabaseReference = mFirebaseDatabase.getReference().child("articles");

        Log.i("MapsActivity", "setup Shared Preference");
        //initialize the shared preference
        sharedPref = this.getSharedPreferences("com.example.app", Context.MODE_PRIVATE);

        Log.i("MapsActivity", "setup Firebase Authentication");
        mAuthStateListener = new FirebaseAuth.AuthStateListener() {
            @Override
            //when the authentication state changed (eg. sign in, sign out)
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                //get the user from the database
                FirebaseUser user = firebaseAuth.getCurrentUser();
                //if user is signed in
                if (user != null) {
                    //Initialize the database
                    onSignedInInitialize(user.getDisplayName());
                    Log.i("Maps Activity", "Signed In");
                    Toast.makeText(MapsActivity.this, "Signed in", Toast.LENGTH_SHORT).show();
                }
                else {
                    Log.i("Maps Activity", "Signed Out");
                    //stop the database acitivity
                    onSignedOutCleanUp();
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

        final MediaPlayer buttonSound = MediaPlayer.create(this, R.raw.sound);

        searchBar = (EditText) findViewById(R.id.search); // EditText variable for search bar
        Log.i("MapsActivity", "setup Search button");
        final Button buttonSearch = findViewById(R.id.buttonSearch);
        //search the name if the button is clicked
        buttonSearch.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonSound.start();
                searchArticles();
                Log.i("MapsActivity", "Search button pressed");
            }
        });

        Log.i("MapsActivity", "setup Left button");
        buttonLeft = findViewById(R.id.buttonLeft);
        //move to next street if the button is clicked
        buttonLeft.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonSound.start();
                movetoPrevArticle();
            }
        });
        //initially this button is not available
        buttonLeft.setVisibility(View .GONE);

        Log.i("MapsActivity", "setup Right button");
        buttonRight = findViewById(R.id.buttonRight);
        //move to previous street if the button is clicked
        buttonRight.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                buttonSound.start();
                movetoNextArticle();
            }
        });
        //initially this button is not available
        buttonRight.setVisibility(View .GONE);

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i("MapsActivity", "Check authentication");
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == RC_SIGN_IN) {
            if(resultCode == RESULT_OK) {
                Log.i("MapsActivity", "Signed in");
                Toast.makeText(MapsActivity.this, "Signed in", Toast.LENGTH_SHORT).show();
            }
            else if (resultCode == RESULT_CANCELED) {
                Log.i("MapsActivity", "Sign in canceled");
                Toast.makeText(MapsActivity.this, "Sign in canceled", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("MapsActivity", "Running onResume");
        setUpMapIfNeeded();             //set up the map if needed
        //hide the button left and right
        buttonLeft.setVisibility(View .GONE);
        buttonRight.setVisibility(View .GONE);
        //these are required if any article is deleted in other activity to refresh the map
        markers.clear();                //clear the markers array list
        searchedMarkers.clear();        //clear the searched markers array list
        searchBar.setText("");          //empty the search bar
        mGoogleApiClient.connect();     //connect to Google API Client
        attachDatabaseReadListener();   //attach database listener again

        //add new authentication state listener if the current is null
        if(mAuthStateListener != null) {
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MapsActivity", "Running onPause");
        //disable the draw route, store this value to the shared preference
        sharedPref.edit().putBoolean("Draw Route", false).apply();
        //detach the database listener
        detachDatabaseReadListener();
        mMap.clear();       //clear the map
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        //disconnect the Google API Client
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
    }

    //when user close the app, clear all the draw route instances store in the shared preference
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.i("MapsActivity", "Running onDestroy");
        //empty the draw route values in the shared preference
        //prevent the draw route is trigger accidentally on next use
        sharedPref.edit().putBoolean("Draw Route", false).apply();
        sharedPref.edit().putFloat("Des Latitude", 0).apply();
        sharedPref.edit().putFloat("Des Longitude", 0).apply();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i("MapsActivity", "Map is ready");
        //get the Google Map
        mMap = googleMap;
        CustomInfoWindowAdapter adapter = new CustomInfoWindowAdapter(MapsActivity.this);
        mMap.setInfoWindowAdapter(adapter);

        //set to activate a method when click on the title of the marker
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                Log.i("MapsActivity", "Marker title tapped, searching article");
                //scan if the tag in the marker the article key in the list
                for (int i = 0; i < keyList.size(); i++) {
                    try {
                        if (marker != null && marker.getTag().equals(keyList.get(i))) {
                            Log.i("MapsActivity", "Article found, sending values");
                            //send the necessary values to the shared preference
                            //these values are used in the info view of the selected article
                            sharedPref.edit().putString("Article Key", keyList.get(i)).apply();
                            sharedPref.edit().putString("Article Owner", articleList.get(i).getOwner()).apply();
                            sharedPref.edit().putString("Article Owner ID", articleList.get(i).getUid()).apply();
                            sharedPref.edit().putString("Article Content", articleList.get(i).getText()).apply();
                            //the expired article key is sent as well
                            //to delete any comment that is related to the expired article
                            sharedPref.edit().putStringSet("Expired Key", expiredKey).apply();
                            sharedPref.edit().putInt("Icon URL", articleList.get(i).getIconURL()).apply();
                            sharedPref.edit().putString("User Key", mFirebaseAuth.getCurrentUser().getUid()).apply();
                            //the photo is optional for each article so there need to be a check
                            if (articleList.get(i).getPhotoURL() != null) {
                                sharedPref.edit().putString("Photo URL", articleList.get(i).getPhotoURL()).apply();
                            } else {
                                sharedPref.edit().putString("Photo URL", null).apply();
                            }

                            //mark the chosen article latitude and longitude
                            sharedPref.edit().putFloat("Des Latitude", (float) articleList.get(i).getLatitude()).apply();
                            sharedPref.edit().putFloat("Des Longitude", (float) articleList.get(i).getLongitude()).apply();

                            Log.i("MapsActivity", "Done, moving to InfoView");
                            //Move to the info page of the selected street
                            Intent intent = new Intent(MapsActivity.this, InfoView.class);
                            startActivity(intent);
                        }
                    }
                    catch (NullPointerException e) {
                        e.printStackTrace();
                    }
                }
            }
        });

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.i(TAG, "Location services connected.");

        //perform permission check to ensure that user allow location
        if(ContextCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 1600);
        }
        //get current location
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        //if no location retrieved, request for location update
        if(location == null) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
        else {
            //setup current location on map
            Log.i("MapsActivity", "Setup current location");
            handleNewLocation(location);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(TAG, "Location services suspended. Please reconnect");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if(connectionResult.hasResolution()) {
            try {
                //try to run the resolution if the connection to Location services is failed
                connectionResult.startResolutionForResult(this, CONNECTION_FAILURE_RESOLUTION_REQUEST);
            }
            catch (IntentSender.SendIntentException e) {
                e.printStackTrace();
            }
        }
        else {
            Log.i(TAG, "Location Services connection failed with code " + connectionResult.getErrorMessage());
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        handleNewLocation(location);
    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            Log.i("MapsActivity", "Map is null, requesting map");
            // Try to obtain the map from the SupportFragmentManager.
            SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFrag.getMapAsync(this);
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                Log.i("MapsActivity", "Setup Map");
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        //set up map by create a new marker
        myMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(0, 0))
                .title("marker"));
    }

    private void handleNewLocation(Location location) {
        //get current location latitude and longitude
        currentLatitude = location.getLatitude();
        currentLongitude = location.getLongitude();
        Log.i("Maps Activity", "Current Latitude: " + String.valueOf(currentLatitude));
        Log.i("Maps Activity", "Current Longitude: " + String.valueOf(currentLongitude));

        //save the current latitude and longitude to the shared preference
        sharedPref.edit().putFloat("Current Latitude", (float)currentLatitude).apply();
        sharedPref.edit().putFloat("Current Longitude", (float)currentLongitude).apply();

        //create new latlng instance based on latitude and longitude
        LatLng origin = new LatLng(currentLatitude, currentLongitude);

        //move current camera to marker position, with zoom value of 14
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(origin, 14.0f));

        //get the draw route flag signal from the info view
        drawRoute = sharedPref.getBoolean("Draw Route",  false);
        //if draw route is enabled
        if(drawRoute) {
            Log.i("MapsActivity", "Drawing route");
            //get the destination latitude and longitude stored in the shared preference
            desLatitude = (double) sharedPref.getFloat("Des Latitude", 0);
            desLongitude = (double) sharedPref.getFloat("Des Longitude", 0);
            Log.i("Maps Activity", "Des Latitude: " + String.valueOf(desLatitude));
            Log.i("Maps Activity", "Des Longitude: " + String.valueOf(desLongitude));
            //set new LatLng for the the destination
            LatLng des = new LatLng(desLatitude, desLongitude);

            Log.i("MapsActivity", "Create user current location marker");
            //create a custom icon to differentiate the user location with the other street point marker
            //get the resource image
            BitmapDrawable bitmapdraw =(BitmapDrawable)getResources().getDrawable(R.drawable.user_location);
            Bitmap mBitmap = bitmapdraw.getBitmap();        //add the image to the bitmap
            //create a custom bitmap with smaller size to fit the map using the above bitmap
            Bitmap smallMarker = Bitmap.createScaledBitmap(mBitmap, 130, 130, false);

            mMap.addMarker(new MarkerOptions()
                    .position(origin)           //specify marker location
                    .title("Your Location")     //marker title
                    //add the custom icon to the marker
                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
            Log.i("MapsActivity", "Route drawn");
            String url = getUrl(origin, des);       //convert the two points into JSON URL
            FetchUrl fetchUrl = new FetchUrl();     //fetch the JSON URL
            fetchUrl.execute(url);                  //execute the URL to draw the route
        }
    }

    //create a function to listen to the user input
    private void attachDatabaseReadListener() {
        Log.i("MapsActivity", "create Database Listener for current child");
        if(mChildEventListener == null) {
            mChildEventListener = new ChildEventListener() {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    //get the data from the database and add them to the streets array list
                    Article article1 = dataSnapshot.getValue(Article.class);

                    //create 2 string of date
                    //this format should have the same format in the Article class
                    SimpleDateFormat myFormat = new SimpleDateFormat("dd MM yyyy");
                    String inputString1 = article1.printDate();         //the date from the article
                    String inputString2 = myFormat.format(new Date());  //current date
                    try {
                        //create 2 date instances based on the string
                        Date date1 = myFormat.parse(inputString1);
                        Date date2 = myFormat.parse(inputString2);
                        //get the difference of time between the two
                        long diff = date2.getTime() - date1.getTime();
                        //convert the difference into days
                        diff = TimeUnit.DAYS.convert(diff, TimeUnit.MILLISECONDS);
                        Log.i("Maps Activity", article1.getOwner() + "'s article day difference is " +
                        String.valueOf(diff) + " days");
                        //if this article exceeds the expired day
                        if (diff >= expiredTime) {
                            //store the expired article key
                            expiredKey.add(dataSnapshot.getKey());
                            //if the article has a photo
                            //delete the photo in the storage
                            if(article1.getPhotoURL() != null) {
                                StorageReference photoRef = mFirebaseStrorage.getReferenceFromUrl(article1.getPhotoURL());
                                photoRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        // File deleted successfully
                                        Log.d(TAG, "onSuccess: deleted file");
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception exception) {
                                        // Uh-oh, an error occurred!
                                        Log.d(TAG, "onFailure: did not delete file");
                                    }
                                });
                            }
                            //remove the article from the database
                            mDatabaseReference.child(dataSnapshot.getKey()).removeValue();
                        }
                        else {
                            //add the article to the list
                            articleList.add(article1);
                            //get the article id and store it in another list
                            String key = dataSnapshot.getKey();
                            keyList.add(key);

                            BitmapDrawable bitmapDraw =(BitmapDrawable)getResources().getDrawable(article1.getIconURL());
                            Bitmap mBitmap = bitmapDraw.getBitmap();        //add the image to the bitmap
                            //create a custom bitmap with smaller size to fit the map using the above bitmap
                            Bitmap smallMarker = Bitmap.createScaledBitmap(mBitmap, 130, 130, false);

                            //create a new marker based on the article information and location
                            Marker m = mMap.addMarker(new MarkerOptions()
                                    //the position is based on the pre-defined latitude and longitude of the article
                                    .position(new LatLng(article1.getLatitude(), article1.getLongitude()))
                                    //the title of the marker is the owner name, snippet as the text content
                                    //and set the marker icon as the selected icon
                                    .title(article1.getOwner())
                                    .snippet(article1.getText())
                                    .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
                            m.setTag(key);      //the tag of the marker is the article ID
                            markers.add(m);     //add the marker to the marker array list
                            Log.i("MapsActivity", article1.getOwner() + "'s Article added");
                        }
                    } catch (ParseException e) {
                        e.printStackTrace();
                    }

                }
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {}
                public void onChildRemoved(DataSnapshot dataSnapshot) {}
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {}
                public void onCancelled(DatabaseError databaseError) {}
            };
            //set this child event listener to the database reference
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        Log.i("MapsActivity", "detach database read listener");
        articleList.clear();
        keyList.clear();
        if(mChildEventListener != null) {
            //remove the data read listener
            mDatabaseReference.removeEventListener(mChildEventListener);
            mChildEventListener = null;
        }
    }

    private void onSignedInInitialize(String username) {
        mUsername = username;
        //attach the database read listener after signed in
        attachDatabaseReadListener();
    }

    private void onSignedOutCleanUp() {
        mUsername = ANONYMOUS;          //set username back to default
        detachDatabaseReadListener();   //detach the listener
    }

    // function to search for articles
    private void searchArticles(){
        boolean articleFound = false; // boolean whether any article is found
        searchedMarkers.clear(); // clear the array of markers from previously searched markers
        Log.i("MapsActivity", "Searching for articles");
        String searchTitle = searchBar.getText().toString(); // obtain the text in the search bar
        // match the text in the search bar against the article owners
        for(int i = 0; i < articleList.size(); i++){
            // if a match is found
            if (searchTitle.toLowerCase().equals(articleList.get(i).getOwner().toLowerCase())){
                // add the respective markers of the articles found into a separate array
                searchedMarkers.add(markers.get(i));
                // and set the boolean to true
                articleFound = true;
            }
        }
        if (articleFound == true){ // if an article with the matching owner is found
            //show the button left and right
            buttonLeft.setVisibility(View .VISIBLE);
            buttonRight.setVisibility(View .VISIBLE);
            //get the position of the first found article
            LatLng latLng = searchedMarkers.get(0).getPosition();
            // show the information of the marker so that the user can click
            searchedMarkers.get(0).showInfoWindow();
            //set the camera to the found street
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));
            // prompt informing the user of a successful search
            Log.i("MapsActivity", "Found " + searchedMarkers.get(0).getTitle() + "'s article");
            Toast.makeText(this, "Found " + searchedMarkers.get(0).getTitle() + "'s article", Toast.LENGTH_LONG).show();
        }
        else {
            //hide the button left and right
            buttonLeft.setVisibility(View .GONE);
            buttonRight.setVisibility(View .GONE);
            // prompt informing the user of an unsuccessful search
            Log.i("MapsActivity", "Cannot find " + searchBar.getText().toString() + "'s article");
            Toast.makeText(this, "Cannot find " + searchBar.getText().toString() + "'s article", Toast.LENGTH_LONG).show();
        }
        searchBar.setText("");
    }

    // method executed for onClick of the left button
    private void movetoPrevArticle(){
        // only executed if there are articles found
        if (searchedMarkers.size() != 0){
            // change the index of the marker displayed
            searchedMarkersIndex -= 1;
            // in case the button is pressed at the first marker, move to last marker
            if (searchedMarkersIndex < 0){
                searchedMarkersIndex = searchedMarkers.size() - 1;
            }
            // move camera to marker and display its information
            LatLng latLng = searchedMarkers.get(searchedMarkersIndex).getPosition();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));
            searchedMarkers.get(searchedMarkersIndex).showInfoWindow();
            Log.i("MapsActivity", "Move to previous marker");
            Log.i("MapsActivity", "Showing marker's info");
        }
    }
    // method executed for onClick of the right button
    private void movetoNextArticle(){
        // only executed if there are articles found
        if (searchedMarkers.size() != 0){
            // change the index of the marker to be displayed
            searchedMarkersIndex += 1;
            // in case the button is pressed at the last marker, move to first article
            if (searchedMarkersIndex > searchedMarkers.size() - 1){
                searchedMarkersIndex = 0;
            }
            // move camera to marker and display its information
            LatLng latLng = searchedMarkers.get(searchedMarkersIndex).getPosition();
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));
            searchedMarkers.get(searchedMarkersIndex).showInfoWindow();
            Log.i("MapsActivity", "Move to next marker");
            Log.i("MapsActivity", "Showing marker's info");
        }
    }

    // Action bar for MapsActivity
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.i("MapsActivity", "Create Option menu");
        //get the menu layout for the option menu
        getMenuInflater().inflate(R.menu.main_actions, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //if any of the option is selected in the option menu
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {     //check which option is selected
            case R.id.signOut:          //sign out option
                Log.i("MapsActivity", "Sign out option selected");
                AuthUI.getInstance().signOut(MapsActivity.this)
                        //when the sign out is completed
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //go back to the main menu
                                Log.i("MapView", "Signed out, back to Main Menu");
                                startActivity(new Intent(MapsActivity.this, MainActivity.class));
                                finish();       //finish this activity
                            }});
                return true;
            case R.id.home:             //home option
                Log.i("MapsActivity", "Home option selected");
                //send the current user name and ID to the shared preference
                sharedPref.edit().putString("Current User", mUsername).apply();
                sharedPref.edit().putString("User Key", mFirebaseAuth.getCurrentUser().getUid()).apply();
                //move to Account Activity
                Log.i("MapsActivity", "Move to AccountActivity");
                Intent intent = new Intent(MapsActivity.this, AccountActivity.class);
                startActivity(intent);
                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //Methods for route drawing
    private String getUrl(LatLng origin, LatLng dest) {
        Log.i("MapsActivity", "Get URL from origin and destination LatLng");
        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;
        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;
        // Sensor enabled
        String sensor = "sensor=false";
        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + sensor;
        // Output format
        String output = "json";
        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        Log.i("MapsActivity", "Download formatted URL");
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);
            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();
            // Connecting to url
            urlConnection.connect();
            // Reading data from url
            iStream = urlConnection.getInputStream();
            //create a buffered reader to scan the URL string
            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
            //create a string buffer
            StringBuffer sb = new StringBuffer();
            //create an empty string of each line
            String line = "";
            //add the line read from the buffered reader
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            //convert the string buffer to string
            data = sb.toString();
            Log.d("downloadUrl", data.toString());
            br.close();     //turn off buffered reader

        } catch (Exception e) {
            Log.d("Exception", e.toString());
        } finally {
            //close the connection stream and disconnect the URL
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
                Log.d("Background Task data", data.toString());
            } catch (Exception e) {
                Log.d("Background Task", e.toString());
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                Log.d("ParserTask",jsonData[0].toString());
                DataParser parser = new DataParser();
                Log.d("ParserTask", parser.toString());

                // Starts parsing data
                routes = parser.parse(jObject);
                Log.d("ParserTask","Executing routes");
                Log.d("ParserTask",routes.toString());

            } catch (Exception e) {
                Log.d("ParserTask",e.toString());
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            PolylineOptions lineOptions = null;

            // Traversing through all the routes
            for (int i = 0; i < result.size(); i++) {
                points = new ArrayList<>();
                lineOptions = new PolylineOptions();

                // Fetching i-th route
                List<HashMap<String, String>> path = result.get(i);

                // Fetching all the points in i-th route
                for (int j = 0; j < path.size(); j++) {
                    HashMap<String, String> point = path.get(j);

                    double lat = Double.parseDouble(point.get("lat"));
                    double lng = Double.parseDouble(point.get("lng"));
                    LatLng position = new LatLng(lat, lng);

                    points.add(position);
                }

                // Adding all the points in the route to LineOptions
                lineOptions.addAll(points);
                lineOptions.width(10);
                lineOptions.color(Color.RED);

                Log.d("onPostExecute","onPostExecute lineoptions decoded");

            }

            // Drawing polyline in the Google Map for the i-th route
            if(lineOptions != null) {
                mMap.addPolyline(lineOptions);
            }
            else {
                Log.d("onPostExecute","without Polylines drawn");
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.i("MapsActivity", "running onStart");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.i("MapsActivity", "running onRestart");
    }

    @Override
    protected void onStop() {
        Log.i("MapsActivity", "running onStop");
        super.onStop();
    }
}
