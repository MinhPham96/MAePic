package com.example.hi.maepic;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.felipecsl.gifimageview.library.GifImageView;

import org.apache.commons.io.IOUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.io.InputStream;


public class MainActivity extends AppCompatActivity implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    GoogleApiClient mGoogleApiClient;       //an instance of google API client
    private GifImageView gifImageView;      //an instance to run the gif
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.i("Main Activity", "Running onCreate");
        Log.i("Main Activity", "Initialize Google Play Services");
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                //Location Permission already granted
                buildGoogleApiClient();
            } else {
                //Request Location Permission
                checkLocationPermission();
            }
        }
        else {
            //even though this activity does not use the map
            //it is necessary to test the Google API Client in this activity before moving to the map
            //so that any bug can be fixed here to prevent the app from crashing
            buildGoogleApiClient();
        }

        Log.i("Main Activity", "Setup GIF");
        //get the view from the layout for the gif
        //this is a custom view import from a Git project
        //check the app gradle to find more about the Git project
        gifImageView = (GifImageView)findViewById(R.id.welcome_gif);
        try {
            //get the input gif from the assets folder
            InputStream inputStream = getAssets().open("welcome.gif");
            byte[] bytes = IOUtils.toByteArray(inputStream);    //convert the gif into an array of bytes
            gifImageView.setBytes(bytes);                       //set the frame
            gifImageView.startAnimation();                      //run the animation of the gif
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.i("Main Activity", "Running onPause");
        //stop the animation when this view is paused
        gifImageView.stopAnimation();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.i("Main Activity", "Running onResume");
        //run the animation when this view is resumed
        gifImageView.startAnimation();
    }

    //this will move to the Maps Activity
    public void moveToMap(View view) {
        Log.i("Main Activity", "Move to MapsActivity");
        Intent intent = new Intent(this, MapsActivity.class);
        startActivity(intent);
    }

    //setup Google API Client
    protected synchronized void buildGoogleApiClient() {
        Log.i("Main Activity", "Setup Google API Client");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    //
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        Log.i("Main Activity", "Request Permission");
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.i("Main Activity", "Permission granted");
                    // permission was granted, build the Google API Client
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                    }

                } else {
                    Log.i("Main Activity", "Permission denied");
                    // permission denied
                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }
    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;
    private void checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {
                Log.i("Main Activity", "Show permission request");
                // Show an explanation to the user asynchronously -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
                new AlertDialog.Builder(this)
                        .setTitle("Location Permission Needed")
                        .setMessage("This app needs the Location permission, please accept to use location functionality")
                        .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                //Prompt the user once explanation has been shown
                                ActivityCompat.requestPermissions(MainActivity.this,
                                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                                        MY_PERMISSIONS_REQUEST_LOCATION );
                            }
                        })
                        .create()
                        .show();


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION );
            }
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    protected void onStart() {
        Log.i("Main Activity", "Running onStart");
        super.onStart();
    }

    @Override
    protected void onRestart() {
        Log.i("Main Activity", "Running onRestart");
        super.onRestart();
    }

    @Override
    protected void onStop() {
        Log.i("Main Activity", "Running onStop");
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        Log.i("Main Activity", "Running onDestroy");
        super.onDestroy();
    }
}