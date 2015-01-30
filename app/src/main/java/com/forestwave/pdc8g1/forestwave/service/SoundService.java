package com.forestwave.pdc8g1.forestwave.service;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
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

public class SoundService extends PdService implements SensorEventListener {

    private final static String TAG = "SoundService";
    public LocationProvider provider;
    public Handler handler;

    public static int SPECIES_EQUALITY_FACTOR = 750;
    public static int SCORE_FACILITY = 500;
    public static int SOUND_DISTANCE_DEACREASE_SLOWNESS = 15;

    public Map<Integer, InfosTrees> desiredState = new HashMap<>();
    public Map<Integer, InfosTrees> actualState = new HashMap<>();

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private float[] mLastAccelerometer = new float[3];
    private float[] mLastMagnetometer = new float[3];
    private boolean mLastAccelerometerSet = false;
    private boolean mLastMagnetometerSet = false;
    private float[] mR = new float[9];
    private float[] mOrientation = new float[3];

    private float mCurrentDegree = 0f;

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

            mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
            mSensorManager.registerListener(this, mMagnetometer, SensorManager.SENSOR_DELAY_GAME);

            provider = new LocationProvider(this);

            handler = new Handler();
            final TreeFinder treeFinder =new TreeFinder(this);
            handler.post(treeFinder);

            handler = new Handler();
            final SoundPlayer soundPlayer =new SoundPlayer(this);
            handler.post(soundPlayer);
            initSystemServices();

        }
        else{
            Log.v("LocationTest", "Play Services unavailable, " +GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()));
        }
    }

    private void initSystemServices() {
        TelephonyManager telephonyManager =  (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                if (this == null) return;
                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    //TODO: restart properly
                } else {
                    stopAudio();
                }
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);
    }

    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this, mAccelerometer);
        mSensorManager.unregisterListener(this, mMagnetometer);
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor == mAccelerometer) {
            System.arraycopy(event.values, 0, mLastAccelerometer, 0, event.values.length);
            mLastAccelerometerSet = true;
        } else if (event.sensor == mMagnetometer) {
            System.arraycopy(event.values, 0, mLastMagnetometer, 0, event.values.length);
            mLastMagnetometerSet = true;
        }
        if (mLastAccelerometerSet && mLastMagnetometerSet) {
            SensorManager.getRotationMatrix(mR, null, mLastAccelerometer, mLastMagnetometer);
            SensorManager.getOrientation(mR, mOrientation);
            float azimuthInRadians = mOrientation[0];
            float azimuthInDegress = (float)(Math.toDegrees(azimuthInRadians)+360)%360;
            mCurrentDegree = azimuthInDegress;
            Log.v(TAG, "mCurrentDegree : " + mCurrentDegree);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

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

    public float getmCurrentDegree() {
        return mCurrentDegree;
    }

}
