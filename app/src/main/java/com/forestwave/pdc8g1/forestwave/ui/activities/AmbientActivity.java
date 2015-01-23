package com.forestwave.pdc8g1.forestwave.ui.activities;
/**
 *
 * @author Peter Brinkmann (peter.brinkmann@gmail.com)
 *
 * For information on usage and redistribution, and for a DISCLAIMER OF ALL
 * WARRANTIES, see the file, "LICENSE.txt," in this distribution.
 *
 * simple test case for {@link PdService}
 *
 */

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.service.PdPreferences;
import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.PdReceiver;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;

import com.forestwave.pdc8g1.forestwave.App;
import com.forestwave.pdc8g1.forestwave.location.LocationProvider;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.model.DaoSession;
import com.forestwave.pdc8g1.forestwave.model.InfosTrees;
import com.forestwave.pdc8g1.forestwave.model.Tree;
import com.forestwave.pdc8g1.forestwave.model.TreeDao;
import com.forestwave.pdc8g1.forestwave.R;

import com.forestwave.pdc8g1.forestwave.services.CompositionEngineService;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.ConnectionResult;

import de.greenrobot.dao.query.Query;

public class AmbientActivity extends Activity implements OnClickListener, OnEditorActionListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "AmbientActivity";
    //private static final String[] trackNames = {"acoustic_guitar", "ballons", "banjo", "clarinet", "ds", "ebow", "electribe", "electric_bass",
    //                                            "flute_piano", "guitar", "harmon_trumpet", "information", "love", "mallet", "omnichord_qchord",
    //                                             "pad_synth", "piano_tender", "rhodes", "strings", "vocals"};
    private static final String[] trackNames = {"acoustic_guitar"}; // TEMP

    private EditText msg;
    private Button play;
    private TextView logs;

    private PdService pdService = null;

    private Toast toast = null;

    LocationProvider provider;
    Handler handler;

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (toast == null) {
                    toast = Toast.makeText(getApplicationContext(), "", Toast.LENGTH_SHORT);
                }
                toast.setText(TAG + ": " + msg);
                toast.show();
            }
        });
    }

    private void post(final String s) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                logs.append(s + ((s.endsWith("\n")) ? "" : "\n"));
            }
        });
    }

    private PdReceiver receiver = new PdReceiver() {

        private void pdPost(String msg) {
            Log.d(TAG, "Pure Data says, \"" + msg + "\"");
        }

        @Override
        public void print(String s) {
            post(s);
        }

        @Override
        public void receiveBang(String source) {
            pdPost("bang");
        }

        @Override
        public void receiveFloat(String source, float x) {
            pdPost("float: " + x);
        }

        @Override
        public void receiveList(String source, Object... args) {
            pdPost("list: " + Arrays.toString(args));
        }

        @Override
        public void receiveMessage(String source, String symbol, Object... args) {
            pdPost("message: " + Arrays.toString(args));
        }

        @Override
        public void receiveSymbol(String source, String symbol) {
            pdPost("symbol: " + symbol);
        }
    };

    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = ((PdService.PdBinder)service).getService();
            initPd();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // this method will never be called
        }
    };

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AudioParameters.init(this);
        PdPreferences.initPreferences(getApplicationContext());
        PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).registerOnSharedPreferenceChangeListener(this);
        initGui();

        Intent serviceIntent = new Intent(this, PdService.class);
        bindService(serviceIntent, pdConnection, BIND_AUTO_CREATE);
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "forestWaves-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        TreeDao treeDao = daoSession.getTreeDao();

        if (GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()) == ConnectionResult.SUCCESS) {
            Log.v("LocationTest", "Play Services available");
            provider = new LocationProvider(this, (SensorManager) getSystemService(Context.SENSOR_SERVICE));
            handler = new Handler();
            final CompositionEngineService compositionEngineService = new CompositionEngineService();
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
                        double userOrientation = 0;

                        Map<Integer, InfosTrees> desiredState = compositionEngineService.calculateDesiredState(trees, provider);
                        this.applyState(desiredState);
                    }
                    handler.postDelayed(this, 120);
                }

                /**
                 * Applique l'état désiré sur la sortie sonore
                 */
                private void applyState(Map<Integer, InfosTrees> desiredState) {

                    for (Map.Entry<Integer, InfosTrees> entry : desiredState.entrySet())
                    {
                        int track = entry.getKey();
                        InfosTrees infos = entry.getValue();
                        Log.v(TAG, "track : " + track + ", volume : " + infos.getVolume());
                    }
                }
            };
            handler.post(runnable);
        }
        else{
            Log.v("LocationTest", "Play Services unavailable, " +GooglePlayServicesUtil.isGooglePlayServicesAvailable(this.getApplicationContext()));
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (pdService.isRunning()) {
            startAudio();
        }
    }

    private void initGui() {
    }

    /**
     * Initialise la librairie PureData et charge les tracks
     */
    private void initPd() {
        Resources res = getResources();
        File patchFile = null;
        try {
            PdBase.setReceiver(receiver);
            PdBase.subscribe("android");

            // Charger les tracks
            for (String trackName : trackNames) {
                //InputStream in2 = res.openRawResource(getResources().getIdentifier(trackName, "raw" , this.getClass().getPackage().getName()));
                //patchFile = IoUtils.extractResource(in2, trackName+".wav", getCacheDir());
            }
            //TEMP
            InputStream in2 = res.openRawResource(R.raw.acoustic_guitar);
            patchFile = IoUtils.extractResource(in2, "acoustic_guitar.wav", getCacheDir());

            InputStream in = res.openRawResource(R.raw.groovebox1r3);
            patchFile = IoUtils.extractResource(in, "groovebox1r3.pd", getCacheDir());
            PdBase.openPatch(patchFile);
        } catch (IOException e) {
            Log.e(TAG, e.toString());
            finish();
        } finally {
            if (patchFile != null) patchFile.delete();
        }
        startAudio();
    }

    private void startAudio() {
        String name = getResources().getString(R.string.app_name);
        try {
            pdService.initAudio(-1, -1, -1, -1);   // negative values will be replaced with defaults/preferences
            pdService.startAudio(new Intent(this, AmbientActivity.class), R.drawable.icon, name, "Return to " + name + ".");
        } catch (IOException e) {
            toast(e.toString());
        }
    }

    private void stopAudio() {
        pdService.stopAudio();
    }

    private void cleanup() {
        try {
            unbindService(pdConnection);
        } catch (IllegalArgumentException e) {
            // already unbound
            pdService = null;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.play_button:
                if (pdService.isRunning()) {
                    stopAudio();
                } else {
                    startAudio();
                }
            default:
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        return true;
    }

}
