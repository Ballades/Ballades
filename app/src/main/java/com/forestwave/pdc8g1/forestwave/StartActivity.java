package com.forestwave.pdc8g1.forestwave;

import android.app.Activity;
import android.app.Fragment;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ConnectionResult;
import com.forestwave.pdc8g1.forestwave.Location.LocationProvider;
import com.forestwave.pdc8g1.forestwave.Model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.Model.DaoSession;
import com.forestwave.pdc8g1.forestwave.Model.Tree;
import com.forestwave.pdc8g1.forestwave.Model.TreeDao;

import java.util.List;

import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.WhereCondition;

public class StartActivity extends Activity {

	LocationProvider provider;
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "forestWaves-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        TreeDao treeDao = daoSession.getTreeDao();

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()) == ConnectionResult.SUCCESS) {
            Log.v("LocationTest", "Play Services available");
            provider = new LocationProvider(this);
            handler = new Handler();
            final Runnable runnable = new Runnable() {

                @Override
                public void run() {
                    DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(App.getContext(), "forestWaves-db", null);
                    SQLiteDatabase db = helper.getWritableDatabase();
                    DaoMaster daoMaster = new DaoMaster(db);
                    DaoSession daoSession = daoMaster.newSession();
                    TreeDao treeDao = daoSession.getTreeDao();

                    if(provider.getLocation() != null) {

                        Double latitude = provider.getLocation().getLatitude();
                        Double longitude = provider.getLocation().getLongitude();

                        Query query = treeDao.queryBuilder().where(TreeDao.Properties.Latitude.between(latitude - 0.01/111, latitude + 0.01/111), TreeDao.Properties.Longitude.between(longitude - 0.01/76, longitude + 0.01/76)).build();
                        List<Tree> trees = query.list();
                        Log.d("NB TREE", Integer.toString(trees.size()));
                    }
                    handler.postDelayed(this, 1000);
                }
            };
            handler.post(runnable);
        }
        else{
            Log.v("LocationTest", "Play Services unavailable, " +GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()));
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
