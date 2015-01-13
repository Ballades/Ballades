package com.forestwave.pdc8g1.forestwave;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;


public class StartActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        AudioTrackSoundPlayer audioTrackSoundPlayer = new AudioTrackSoundPlayer(this);
        audioTrackSoundPlayer.playNote("ds");
        audioTrackSoundPlayer.playNote("clarinet");

    }

    public class AudioTrackSoundPlayer {
        private HashMap<String, PlayThread> threadMap = null;
        private Context context;
        public static final String TAG = "AudioTrackSoundPlayer";

        public AudioTrackSoundPlayer(Context context) {
            this.context = context;
            threadMap = new HashMap<String, PlayThread>();
        }

        public void playNote(String note)
        {
            if (!isNotePlaying(note))
            {
                PlayThread thread = new PlayThread(note);
                thread.start();
                threadMap.put(note, thread);

            }
        }

        public void stopNote(String note)
        {
            PlayThread thread = threadMap.get(note);
            if (thread != null)
            {
                thread.requestStop();
                threadMap.remove(note);
            }
        }

        public boolean isNotePlaying(String note)
        {
            return threadMap.containsKey(note);
        }

        private class PlayThread extends Thread {
            String note;
            boolean stop = false;
            AudioTrack audioTrack = null;

            public PlayThread(String note) {
                super();
                this.note = note;
            }

            public void run() {
                try {
                    String path = note + ".wav";

                    AssetManager assetManager = context.getAssets();
                    AssetFileDescriptor ad = assetManager.openFd(path);
                    long fileSize = ad.getLength();
                    int bufferSize = 16000;
                    byte[] buffer = new byte[bufferSize];

                    audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC, 44100, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize, AudioTrack.MODE_STREAM);

                    Log.d(TAG, "MIN BUFFER SIZE " + audioTrack.getMinBufferSize(44100, AudioFormat.CHANNEL_OUT_STEREO, AudioFormat.ENCODING_PCM_16BIT));

                    audioTrack.play();

                    InputStream audioStream = null;

                    int headerOffset = 0x2C;
                    long bytesWritten = 0;
                    int bytesRead = 0;

                    while (!stop) // loop sound
                    {
                        audioStream = assetManager.open(path);
                        bytesWritten = 0;
                        bytesRead = 0;
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
            public synchronized void requestStop()
            {
                stop = true;
            }
        }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_start, container, false);
            return rootView;
        }
    }
}
