package com.forestwave.pdc8g1.forestwave.ui.activities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Resources;

import android.net.Uri;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.forestwave.pdc8g1.forestwave.R;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.service.SoundService;
import com.forestwave.pdc8g1.forestwave.ui.dialogs.AboutDialog;


public class StartActivity extends Activity implements OnClickListener,SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "StartActivity";

    private ImageButton play;
    private ProgressBar pbLoading;
    private TextView tvLoading;

    private SeekBar seekBarEquality;
    private SeekBar seekBarDistance;
    private TextView tvEquality;
    private TextView tvScore;


    public SoundService pdService = null;



    private final ServiceConnection pdConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            pdService = (SoundService) ((PdService.PdBinder)service).getService();
            if(!pdService.isRunning()){
                initPd();
            }else if(play!=null){
                play.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_pause));
                play.setPaddingRelative(0, 0, 0, 0);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            // this method will never be called
        }
    };

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initGui();
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cleanup();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_start, menu);
        return true;
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_about:
                AboutDialog dialog = new AboutDialog();
                dialog.show(getFragmentManager(),"AboutDialog");
                return true;
            case R.id.action_contact:
                String[] TO = {"sylvain.abadie2099@gmail.com"};
                String[] CC = {"reynolds.nicorr@gmail.com"};
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("text/plain");

                emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
                emailIntent.putExtra(Intent.EXTRA_CC, CC);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Votre sujet");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Votre email");

                try {
                    startActivity(Intent.createChooser(emailIntent, "Envoyez le mail..."));
                    finish();
                    Log.i("Email envoyé", "");
                } catch (android.content.ActivityNotFoundException ex) {
                    Toast.makeText(StartActivity.this,
                            "Il n'y a pas de client mail installé", Toast.LENGTH_SHORT).show();
                }
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
    private void initGui() {
        setContentView(R.layout.activity_start);
        play = (ImageButton) findViewById(R.id.play_button);
        play.setOnClickListener(this);
        seekBarEquality = (SeekBar) findViewById(R.id.seekBarTempo);
        seekBarEquality.setMax(1200);
        seekBarEquality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                SoundService.SPECIES_EQUALITY_FACTOR = progresValue;
                tvEquality.setText(getResources().getText(R.string.choose_SEF) + " " + String.valueOf(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBarDistance = (SeekBar) findViewById(R.id.seekBarStyle);
        seekBarDistance.setMax(200);
        seekBarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                tvEquality.setText(getResources().getText(R.string.choose_SDDS) + " " + String.valueOf(progress));
                SoundService.SOUND_DISTANCE_DEACREASE_SLOWNESS = progresValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                PdBase.sendBang("applystyle");

            }
        });
        SharedPreferences sharedPref = this.getSharedPreferences(this.getString(R.string.sp_loading), Context.MODE_PRIVATE);
        sharedPref.registerOnSharedPreferenceChangeListener(this);
        SharedPreferences.Editor editor = sharedPref.edit();
        int value = sharedPref.getInt(this.getString(R.string.sp_loading_done), 0);
        tvEquality = (TextView) findViewById(R.id.textViewStyle);
        tvEquality.setText(getResources().getText(R.string.style) + " " + String.valueOf(seekBarDistance.getProgress()));
        tvScore = (TextView) findViewById(R.id.tv_loading);
        pbLoading = (ProgressBar) findViewById(R.id.pb_loading);
        tvLoading = (TextView) findViewById(R.id.tv_loading);
        if(value!=DaoMaster.NB_PAGES_API) {
            pbLoading.setProgress(value);
            DaoMaster.initDatabase(this);
        }else {
            disableLoadingView();

        }
    }
    public void disableLoadingView(){
        play.setVisibility(View.VISIBLE);
        seekBarEquality.setVisibility(View.VISIBLE);
        seekBarDistance.setVisibility(View.VISIBLE);
        tvEquality.setVisibility(View.VISIBLE);
        tvScore.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.GONE);
        tvLoading.setVisibility(View.GONE);
        Intent serviceIntent = new Intent(this, SoundService.class);
        bindService(serviceIntent, pdConnection, BIND_AUTO_CREATE);
    }

    // Initialize pd ressources : wav samples required and pd patches
    private void initPd() {
        Resources res = getResources();
        File patchFile = null;
        try {
            PdBase.subscribe("android");
            InputStream in1 = res.openRawResource(R.raw.acoustic_guitar);
            patchFile = IoUtils.extractResource(in1, "acoustic_guitar.wav", getCacheDir());
            InputStream in2 = res.openRawResource(R.raw.ballons);
            patchFile = IoUtils.extractResource(in2, "ballons.wav", getCacheDir());
            InputStream in3 = res.openRawResource(R.raw.banjo);
            patchFile = IoUtils.extractResource(in3, "banjo.wav", getCacheDir());
            InputStream in4 = res.openRawResource(R.raw.clarinet);
            patchFile = IoUtils.extractResource(in4, "clarinet.wav", getCacheDir());
            InputStream in5 = res.openRawResource(R.raw.ds);
            patchFile = IoUtils.extractResource(in5, "ds.wav", getCacheDir());
            InputStream in6 = res.openRawResource(R.raw.ebow);
            patchFile = IoUtils.extractResource(in6, "ebow.wav", getCacheDir());
            InputStream in7 = res.openRawResource(R.raw.electribe);
            patchFile = IoUtils.extractResource(in7, "electribe.wav", getCacheDir());
            InputStream in8 = res.openRawResource(R.raw.electric_bass);
            patchFile = IoUtils.extractResource(in8, "electric_bass.wav", getCacheDir());
            InputStream in9 = res.openRawResource(R.raw.flute_piano);
            patchFile = IoUtils.extractResource(in9, "flute_piano.wav", getCacheDir());
            InputStream in10 = res.openRawResource(R.raw.guitar);
            patchFile = IoUtils.extractResource(in10, "guitar.wav", getCacheDir());
            InputStream in11 = res.openRawResource(R.raw.harmon_trumpet);
            patchFile = IoUtils.extractResource(in11, "harmon_trumpet.wav", getCacheDir());
            InputStream in12 = res.openRawResource(R.raw.information);
            patchFile = IoUtils.extractResource(in12, "information.wav", getCacheDir());
            InputStream in13 = res.openRawResource(R.raw.love);
            patchFile = IoUtils.extractResource(in13, "love.wav", getCacheDir());
            InputStream in14 = res.openRawResource(R.raw.mallet);
            patchFile = IoUtils.extractResource(in14, "mallet.wav", getCacheDir());
            InputStream in15 = res.openRawResource(R.raw.omnichord_qchord);
            patchFile = IoUtils.extractResource(in15, "omnichord_qchord.wav", getCacheDir());
            InputStream in16 = res.openRawResource(R.raw.pad_synth);
            patchFile = IoUtils.extractResource(in16, "pad_synth.wav", getCacheDir());
            InputStream in17 = res.openRawResource(R.raw.piano_tender);
            patchFile = IoUtils.extractResource(in17, "piano_tender.wav", getCacheDir());
            InputStream in18 = res.openRawResource(R.raw.rhodes);
            patchFile = IoUtils.extractResource(in18, "rhodes.wav", getCacheDir());
            InputStream in19 = res.openRawResource(R.raw.strings);
            patchFile = IoUtils.extractResource(in19, "strings.wav", getCacheDir());
            InputStream in20 = res.openRawResource(R.raw.vocals);
            patchFile = IoUtils.extractResource(in20, "vocals.wav", getCacheDir());


            InputStream in = res.openRawResource(R.raw.groovebox1r3);
            patchFile = IoUtils.extractResource(in, "groovebox1r3.pd", getCacheDir());
            PdBase.openPatch(patchFile);

        } catch (IOException e) {
            Log.e(TAG, e.toString());
            finish();
        } finally {
            if (patchFile != null) patchFile.delete();
        }
    }

    private void startAudio() {
        String name = getResources().getString(R.string.app_name);
        try {
            pdService.initAudio(16000, -1, -1, -1);   // negative values will be replaced with defaults/preferences
            pdService.startAudio(new Intent(this, StartActivity.class), R.drawable.ic_stat_hardware_headset, name, "Return to " + name + ".");
        } catch (IOException e) {
            Log.d(TAG,e.toString());
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
                    play.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_play_arrow));
                    float scale = getResources().getDisplayMetrics().density;
                    int dpAsPixels = (int) (6*scale + 0.5f);
                    play.setPaddingRelative(dpAsPixels,0,0,0);
                } else {
                    startAudio();
                    play.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_pause));
                    play.setPaddingRelative(0, 0, 0, 0);
                }
            default:
                break;
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if(key.equals(this.getString(R.string.sp_loading_done)) && pbLoading!= null && pbLoading.getVisibility()==View.VISIBLE){
            int value = sharedPreferences.getInt(this.getString(R.string.sp_loading_done), 0);
            if(value < DaoMaster.NB_PAGES_API){
                pbLoading.setProgress(value);
            }else{
                disableLoadingView();
            }
        }
    }
}
