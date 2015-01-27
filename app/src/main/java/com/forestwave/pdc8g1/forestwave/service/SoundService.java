package com.forestwave.pdc8g1.forestwave.service;

import android.os.Handler;
import android.util.Log;

import com.forestwave.pdc8g1.forestwave.location.LocationProvider;

import com.forestwave.pdc8g1.forestwave.model.InfosTrees;
import com.forestwave.pdc8g1.forestwave.utils.SoundPlayer;
import com.forestwave.pdc8g1.forestwave.utils.TreeFinder;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;

import java.util.HashMap;
import java.util.Map;

public class SoundService extends PdService  {

    private final static String TAG = "SoundService";
    public LocationProvider provider;
    public Handler handler;

    public Map<Integer, InfosTrees> desiredState = new HashMap<>();
    public Map<Integer, InfosTrees> actualState = new HashMap<>();

    public SoundService() {

    }

    @Override
    /**
     * Initialise le provider et les runnables
     */
    public void onCreate() {
        super.onCreate();
        AudioParameters.init(this);
        PdPreferences.initPreferences(getApplicationContext());
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()) == ConnectionResult.SUCCESS) {
            Log.v("LocationTest", "Play Services available");
            provider = new LocationProvider(this);

            handler = new Handler();
            final TreeFinder treeFinder =new TreeFinder(this);
            handler.post(treeFinder);

            handler = new Handler();
            final SoundPlayer soundPlayer =new SoundPlayer(this);
            handler.post(soundPlayer);
        }
        else{
            Log.v("LocationTest", "Play Services unavailable, " +GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()));
        }
    }

    /**
     * Récupère de desiredState (ressource partagée entre le TreeFinder et le SoundPlayer)
     */
    public Map<Integer, InfosTrees> getDesiredState() {
        return desiredState;
    }

    /**
     * Enregistre le desiredState, avec vérification de non-concurrence
     */
    public void setDesiredState(Map<Integer, InfosTrees> desiredState) {
        this.desiredState = desiredState;
    }

    /**
     * Récupère de actualState (ressource partagée entre le TreeFinder et le SoundPlayer)
     */
    public Map<Integer, InfosTrees> getActualState() {
        return actualState;
    }


    /**
     * Enregistre le actualState, avec vérification de non-concurrence
     */
    public void setActualState(Map<Integer, InfosTrees> actualState) {
        this.actualState = actualState;
    }

}
