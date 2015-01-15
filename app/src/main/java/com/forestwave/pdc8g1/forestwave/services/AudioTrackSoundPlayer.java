package com.forestwave.pdc8g1.forestwave.services;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioTrack;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

import static android.media.AudioFormat.*;
import static android.media.AudioManager.STREAM_MUSIC;
import static android.media.AudioTrack.MODE_STREAM;

/**
 * Created by steevenssegura on 13/01/15.
 */
public class AudioTrackSoundPlayer {
    private Context context;
    private HashMap<String, PlayThread> threadMap;
    public static final String TAG = "AudioTrackSoundPlayer";

    public AudioTrackSoundPlayer(Context context) {
        this.context = context;
        threadMap = new HashMap<String, PlayThread>();
    }

    public void play(String sound) {
        if (!isPlaying(sound)) {
            PlayThread thread = new PlayThread(sound);
            thread.start();
            threadMap.put(sound, thread);
        }
        // else if thread is over..?
    }

    public void stop(String sound) {
        PlayThread thread = threadMap.get(sound);
        if (thread != null) {
            thread.requestStop();
            threadMap.remove(sound);
        }
    }

    public boolean isPlaying(String sound) {
        return threadMap.containsKey(sound);
    }

    private class PlayThread extends Thread {
        String sound;
        String path;
        boolean stop = false;
        AudioTrack audioTrack = null;
        AssetManager assetManager = context.getAssets();
        int sampleRateInHz = 44100;
        int bufferSize = 16000;

        public PlayThread(String sound) {
            super();
            this.sound = sound;
            this.path = sound + ".wav";
        }

        public void run() {
            try {
                AssetFileDescriptor ad = assetManager.openFd(path);
                long fileSize = ad.getLength();

                byte[] buffer = new byte[bufferSize];

                audioTrack = new AudioTrack(STREAM_MUSIC, sampleRateInHz, CHANNEL_OUT_MONO, ENCODING_PCM_16BIT, bufferSize, MODE_STREAM);

                Log.d(TAG, "MIN BUFFER SIZE " + AudioTrack.getMinBufferSize(sampleRateInHz, CHANNEL_OUT_STEREO, ENCODING_PCM_16BIT));

                audioTrack.play();

                InputStream audioStream;

                int headerOffset = 0x2C;
                long bytesWritten;
                int bytesRead;

                while (!stop) { // loop sound
                    audioStream = assetManager.open(path);
                    bytesWritten = 0;
                    Log.d(TAG, "in loop " + path);

                    audioStream.read(buffer, 0, headerOffset);

                    // read until end of file
                    while (!stop && bytesWritten < fileSize - headerOffset) {
                        bytesRead = audioStream.read(buffer, 0, bufferSize);
                        bytesWritten += audioTrack.write(buffer, 0, bytesRead);
                        //Log.d(TAG, "bytesRead" +bytesRead+"bytesWritten" +bytesWritten);
                    }
                }

                audioTrack.stop();
                audioTrack.release();
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        public synchronized void requestStop() {
            stop = true;
        }
    }
}
