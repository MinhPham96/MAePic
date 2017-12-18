package com.example.hi.maepic;

import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.location.Location;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class MapsActivity extends FragmentActivity implements
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

    public static final int RC_SIGN_IN = 1;                     //a constance for sign in
    private String mUsername;                                   //an instance that holds the username
    public static final String ANONYMOUS = "anonymous";         //default username
    private FirebaseAuth mFirebaseAuth;                         //an instance for the authentication
    //an instance for the authentiation state listener
    private FirebaseAuth.AuthStateListener mAuthStateListener;

    private SharedPreferences sharedPref;       //an instance for the shared preference
    ArrayList<String> keyList = new ArrayList<String>();
    ArrayList<Article> articleList = new ArrayList<Article>();
    private static final String currentLocation  = "Your Location";

    @Override
    public void onBackPressed() {
        //prevent user from pressing back
        //so user can only get back by signing out
        moveTaskToBack(true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps2);

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
        //get instance for both the database and authentiaction
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        //set the reference to specific on the "streets" child in the database
        mDatabaseReference = mFirebaseDatabase.getReference().child("Users");

        Log.i("MapsActivity", "setup Firebase Database");
        //get instance for both the database and authentiaction
        mFirebaseDatabase = FirebaseDatabase.getInstance();
        mFirebaseAuth = FirebaseAuth.getInstance();
        //set the reference to specific on the "streets" child in the database
        mDatabaseReference = mFirebaseDatabase.getReference().child("articles");

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
                    Log.i("MapView", "Signed In");
                    Toast.makeText(MapsActivity.this, "Signed in", Toast.LENGTH_SHORT).show();
                }
                else {
                    Log.i("MapView", "Signed Out");
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

        Log.i("MapsActivity", "setup Sign Out Button");
        final Button buttonSignOut = findViewById(R.id.buttonSignOut);
        buttonSignOut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sign out off the activity
                AuthUI.getInstance().signOut(MapsActivity.this)
                        //when the sign out is completed
                        .addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                //go back to the main menu
                                Log.i("MapView", "Signed out, back to Main Menu");
                                startActivity(new Intent(MapsActivity.this, MainActivity.class));
                                finish();       //finish this activity
                            }
                        });
            }
        });
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
        Log.i("MapsActivity", "onResume is running");
        //set up the map if needed
        setUpMapIfNeeded();
        //connect to Google API Client
        mGoogleApiClient.connect();
        //add new authentication state listener if the current is null
        if(mAuthStateListener != null) {
            mFirebaseAuth.addAuthStateListener(mAuthStateListener);
        }
        Log.i("MapsActivity", "onResume finished");
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("MapsActivity", "onPause is running");
        mFirebaseAuth.removeAuthStateListener(mAuthStateListener);
        //disconnect the Google API Client
        if(mGoogleApiClient.isConnected()) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        Log.i("MapsActivity", "onPause finished");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.i("MapsActivity", "Map is ready");
        //get the Google Map
        mMap = googleMap;
        //set to activate a method when click on the title of the marker
        mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(Marker marker) {
                if(marker != null && marker.getTitle().equals(currentLocation)) {
                    Intent intent = new Intent(MapsActivity.this, AccountActivity.class);
                    startActivity(intent);
                }
                else {
                    //scan if the title match the street name in the array list
                    for (int i = 0; i < keyList.size(); i++) {
                        if (marker != null && marker.getSnippet().equals(keyList.get(i))) {
                            sharedPref.edit().putString("Article Key", keyList.get(i)).apply();
                            sharedPref.edit().putString("Article Owner", articleList.get(i).getOwner()).apply();
                            sharedPref.edit().putString("Article Content", articleList.get(i).getText()).apply();
                            //send the current username to the shared preference
                            sharedPref.edit().putString("Current User", mUsername).apply();

                            //Move to the info page of the selected street
                            Intent intent = new Intent(MapsActivity.this, InfoView.class);
                            startActivity(intent);
                        }
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
            Log.i("MapView", "Map is null, requesting map");
            // Try to obtain the map from the SupportFragmentManager.
            SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
            mapFrag.getMapAsync(this);
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                Log.i("MapView", "Setup Map");
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
        //create a custom icon to differentiate the user location with the other street point marker
        //get the resource image
        BitmapDrawable bitmapdraw =(BitmapDrawable)getResources().getDrawable(R.drawable.user_location);
        Bitmap mBitmap = bitmapdraw.getBitmap();        //add the image to the bitmap
        //create a custom bitmap with smaller size to fit the map using the above bitmap
        Bitmap smallMarker = Bitmap.createScaledBitmap(mBitmap, 130, 130, false);

        //get current location latitude and longitude
        double currentLatitude = location.getLatitude();
        double currentLongitude = location.getLongitude();
        Log.i("MapView", "Current Latitude: " + String.valueOf(currentLatitude));
        Log.i("MapView", "Current Longitude: " + String.valueOf(currentLongitude));

        //create new latlng instance based on latitude and longitude
        LatLng latLng = new LatLng(currentLatitude, currentLongitude);

        myMarker = mMap.addMarker(new MarkerOptions()
                .position(latLng)           //specify marker location
                .title(currentLocation)     //marker title
                //add the custom icon to the marker
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker)));
        //move current camera to marker position, with zoom value of 14
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 14.0f));
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
                    articleList.add(article1);
                    String key = dataSnapshot.getKey();
                    keyList.add(key);
                    mMap.addMarker(new MarkerOptions()
                            //the position is based on the pre-defined latitude and longitude of the street
                            .position(new LatLng(article1.getLatitude(), article1.getLongitude()))
                            //the title of the marker is the street name
                            .title(article1.getOwner())
                            .snippet(key));

                    Log.i("MapsActivity", article1.getOwner() + "'s Article added");

                }

                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                public void onCancelled(DatabaseError databaseError) {
                }
            };
            //set this child event listener to the database reference
            mDatabaseReference.addChildEventListener(mChildEventListener);
        }
    }

    private void detachDatabaseReadListener() {
        Log.i("MapsActivity", "detach database read listener");
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
}
