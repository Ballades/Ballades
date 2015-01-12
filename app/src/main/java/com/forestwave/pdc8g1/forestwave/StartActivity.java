package com.forestwave.pdc8g1.forestwave;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;


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

        // Premiers sons
        MediaPlayer mp1 = MediaPlayer.create(this, R.raw.guitar);
        MediaPlayer mp2 = MediaPlayer.create(this, R.raw.mallet);
        MediaPlayer mp3 = MediaPlayer.create(this, R.raw.flute_piano);
        MediaPlayer mp4 = MediaPlayer.create(this, R.raw.rhodes);
        MediaPlayer mp5 = MediaPlayer.create(this, R.raw.pad_synth);
        mp1.setLooping(true);
        mp2.setLooping(true);
        mp3.setLooping(true);
        mp4.setLooping(true);
        mp5.setLooping(true);
        mp1.start();
        mp2.start();
        mp3.start();
        mp4.start();
        mp5.start();
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
