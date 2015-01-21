package com.forestwave.pdc8g1.forestwave.location;

/**
 * Created by Quentin on 12/01/2015.
 */
import android.app.Activity;
import android.location.Location;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.FusedLocationProviderApi;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import android.util.Log;

public class LocationProvider implements
        LocationListener,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final long INTERVAL = 1000 * 2;
    private static final long FASTEST_INTERVAL = 1000 * 5;
    private static final long ONE_MIN = 1000 * 60;
    private static final long REFRESH_TIME = ONE_MIN * 5;
    private static final float MINIMUM_ACCURACY = 50.0f;

    Activity locationActivity;
    private LocationRequest locationRequest;
    private GoogleApiClient googleApiClient;
    private Location location;
    private FusedLocationProviderApi fusedLocationProviderApi = LocationServices.FusedLocationApi;
    private double userOrientation;

    public LocationProvider(Activity locationActivity) {

        Log.v("Locationprovider", "call to constructor");

        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        this.locationActivity = locationActivity;

        googleApiClient = new GoogleApiClient.Builder(locationActivity)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        if (googleApiClient != null) {
            googleApiClient.connect();
            Log.v("Locationprovider", "googleApiConnected");
        }
        Log.v("Locationprovider", "end of constructor");
    }

    @Override
    public void onConnected(Bundle connectionHint) {

        Log.v("Locationprovider", "onConnected called");

        Location currentLocation = fusedLocationProviderApi.getLastLocation(googleApiClient);

        if (currentLocation != null) {
            location = currentLocation;
            Log.v("LocationLocationLocation onConnected", location.toString());
        } else {
            Log.v("LocationLocationLocation onConnected", "no last location");
        }
        enable();
    }

    @Override
    public void onLocationChanged(Location newLocation) {
        Log.v("Locationprovider", "onLocationChanged called");

        if (newLocation != null  ) {
            this.location = newLocation;
            Log.v("LocationLocationLocation onLocationChanged",this.location.toString());
            userOrientation = location.getBearing();
        }
        else {
            Log.v("LocationlocationLocation onLocationChanged","bite");
        }
    }

    public Location getLocation() {
        return this.location;
    }


    @Override
    public void onConnectionSuspended(int i) {
        Log.v("OnconnectionSuspended","called");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.v("OnconnectionFailed","called");
    }

    private void enable(){
        fusedLocationProviderApi.requestLocationUpdates(googleApiClient, locationRequest, this);
    }

    private void disable(){
        fusedLocationProviderApi.removeLocationUpdates(googleApiClient,this);
    }

    public double getUserOrientation() {

        return userOrientation;
    }

}
