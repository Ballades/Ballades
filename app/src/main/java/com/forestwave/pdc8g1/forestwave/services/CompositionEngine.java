package com.forestwave.pdc8g1.forestwave.services;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import com.forestwave.pdc8g1.forestwave.R;

public class CompositionEngine extends Service {
    public static final String TAG = "CompositionEngine";

    public CompositionEngine() {
    }

        @Override
    public void onCreate() {
            Log.d(TAG, "here");

            // Premiers sons
            MediaPlayer mp1 = MediaPlayer.create(this, R.raw.guitar);
            MediaPlayer mp2 = MediaPlayer.create(this, R.raw.mallet);
            //MediaPlayer mp3 = MediaPlayer.create(this, R.raw.some);
            mp1.setLooping(true);
            mp2.setLooping(true);
            //mp3.setLooping(true);
            mp1.start();
            mp2.start();
            //mp3.start();

        }


        @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


}
