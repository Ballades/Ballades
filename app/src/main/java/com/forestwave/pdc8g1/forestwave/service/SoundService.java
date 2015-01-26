package com.forestwave.pdc8g1.forestwave.service;

import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.forestwave.pdc8g1.forestwave.location.LocationProvider;

import com.forestwave.pdc8g1.forestwave.utils.TreeFinder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;

import java.util.ArrayList;

public class SoundService extends PdService  {
    private final static String TAG="SoundService";
    public LocationProvider provider;
    public Handler handler;

    public ArrayList<Integer> playingTracks = new ArrayList<>();

    public SoundService() {

    }

    @Override
    public void onCreate() {
        super.onCreate();
        AudioParameters.init(this);
        PdPreferences.initPreferences(getApplicationContext());
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()) == ConnectionResult.SUCCESS) {
            Log.v("LocationTest", "Play Services available");
            provider = new LocationProvider(this);
            handler = new Handler();
            final TreeFinder runnable =new TreeFinder(this);
            handler.post(runnable);
        }
        else{
            Log.v("LocationTest", "Play Services unavailable, " +GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()));
        }
    }


}
