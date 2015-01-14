package com.forestwave.pdc8g1.forestwave;

import android.app.Activity;
import android.app.Fragment;
import android.location.Location;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ConnectionResult;
import android.util.Log;
import android.widget.TextView;
import Location.LocationProvider;
import android.os.Handler;
import java.util.Date;



public class StartActivity extends Activity {

    LocationProvider provider;
    Handler mHandler = new Handler();
    TextView tvLocation;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }
        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()) == ConnectionResult.SUCCESS) {
            Log.v("LocationTest", "Play Services available");
            provider = new LocationProvider(this);
        }
        else{
            Log.v("LocationTest", "Play Services unavailable, " +GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()));
        }

        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                {   tvLocation = (TextView) findViewById(R.id.tvLocation);
                    if(tvLocation != null) {
                        tvLocation.setText(updateString(provider.getLocation(),tvLocation.getText().toString()));
                        mHandler.postDelayed(this, 1000);
                    }else{
                        mHandler.postDelayed(this, 100);
                    }


                }
            }
        };
        mHandler.post(runnable);
    }

    private String updateString(Location location, String old){
        String locationResult;
        if( location != null){
            double latitude = location.getLatitude();
            double longitude = location.getLongitude();
            float accuracy = location.getAccuracy();
            float speed = location.getSpeed();
            Date date = new Date(location.getTime());
            String provider = location.getProvider();
            double altitude = location.getAltitude();
            locationResult = "Latitude: " + latitude + "\n" +
                    "Longitude: " + longitude + "\n" +
                    "Altitude: " + altitude + "\n" +
                    "Accuracy: " + accuracy + "\n" +
                    "Speed: " + speed + "m/s" + "\n" +
                    "Date: " + date.toString() + "\n" +
                    "Provider: " + provider + "\n";

        }
        else{
            locationResult = "Location unavailabe \n";
        }

        if(old.endsWith("\\"))
            locationResult += "/";
        else
            locationResult += "\\";

        return locationResult;
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
