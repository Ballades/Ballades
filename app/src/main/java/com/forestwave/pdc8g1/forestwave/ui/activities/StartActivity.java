package com.forestwave.pdc8g1.forestwave.ui.activities;

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

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.forestwave.pdc8g1.forestwave.R;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.service.SoundService;
import com.forestwave.pdc8g1.forestwave.ui.dialogs.AboutDialog;

import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


public class StartActivity extends Activity implements OnClickListener,SharedPreferences.OnSharedPreferenceChangeListener{

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
                initPd(pdService.getApplicationContext());
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

        if (!ExpansionFileDownloaderActivity.expansionFilesDelivered(this.getApplicationContext())){
            startActivity(new Intent(this, ExpansionFileDownloaderActivity.class));
            this.finish();
        }else {
            initGui();
        }
    }

    @Override
    protected void onDestroy() {
        cleanup();
        super.onDestroy();
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
                String[] TO = {"contact@ballad.es"};
                Intent emailIntent = new Intent(Intent.ACTION_SEND);
                emailIntent.setData(Uri.parse("mailto:"));
                emailIntent.setType("text/plain");

                emailIntent.putExtra(Intent.EXTRA_EMAIL, TO);
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Votre sujet");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Votre email");

                try {
                    startActivity(Intent.createChooser(emailIntent, "Envoyez le mail..."));
                    finish();
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
            int progress = 500;

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
            int progress = 10;

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
        if(value < DaoMaster.PROGRESS_MAX) {
            pbLoading.setProgress(value);
            DaoMaster.initDatabase(this);
        } else {
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
    private void initPd(Context appContext) {
        Resources res = getResources();
        File patchFile = null;
        try {
            PdBase.subscribe("android");
            // Get a ZipResourceFile representing a merger of both the main and patch files
            ZipResourceFile expansionFile = APKExpansionSupport.getAPKExpansionZipFile(appContext,
                            3, 0);
           // Get an input stream for a known file inside the expansion file ZIPs
            InputStream in1 = expansionFile.getInputStream("acoustic_guitar.wav");
            patchFile = IoUtils.extractResource(in1, "acoustic_guitar.wav", getCacheDir());
            InputStream in2 = expansionFile.getInputStream("ballons.wav");
            patchFile = IoUtils.extractResource(in2, "ballons.wav", getCacheDir());
            InputStream in3 = expansionFile.getInputStream("banjo.wav");
            patchFile = IoUtils.extractResource(in3, "banjo.wav", getCacheDir());
            InputStream in4 = expansionFile.getInputStream("clarinet.wav");
            patchFile = IoUtils.extractResource(in4, "clarinet.wav", getCacheDir());
            InputStream in5 = expansionFile.getInputStream("ds.wav");
            patchFile = IoUtils.extractResource(in5, "ds.wav", getCacheDir());
            InputStream in6 = expansionFile.getInputStream("ebow.wav");
            patchFile = IoUtils.extractResource(in6, "ebow.wav", getCacheDir());
            InputStream in7 = expansionFile.getInputStream("electribe.wav");
            patchFile = IoUtils.extractResource(in7, "electribe.wav", getCacheDir());
            InputStream in8 = expansionFile.getInputStream("electric_bass.wav");
            patchFile = IoUtils.extractResource(in8, "electric_bass.wav", getCacheDir());
            InputStream in9 = expansionFile.getInputStream("flute_piano.wav");
            patchFile = IoUtils.extractResource(in9, "flute_piano.wav", getCacheDir());
            InputStream in10 = expansionFile.getInputStream("guitar.wav");
            patchFile = IoUtils.extractResource(in10, "guitar.wav", getCacheDir());
            InputStream in11 = expansionFile.getInputStream("harmon_trumpet.wav");
            patchFile = IoUtils.extractResource(in11, "harmon_trumpet.wav", getCacheDir());
            InputStream in12 = expansionFile.getInputStream("information.wav");
            patchFile = IoUtils.extractResource(in12, "information.wav", getCacheDir());
            InputStream in13 = expansionFile.getInputStream("love.wav");
            patchFile = IoUtils.extractResource(in13, "love.wav", getCacheDir());
            InputStream in14 = expansionFile.getInputStream("mallet.wav");
            patchFile = IoUtils.extractResource(in14, "mallet.wav", getCacheDir());
            InputStream in15 = expansionFile.getInputStream("omnichord_qchord.wav");
            patchFile = IoUtils.extractResource(in15, "omnichord_qchord.wav", getCacheDir());
            InputStream in16 = expansionFile.getInputStream("pad_synth.wav");
            patchFile = IoUtils.extractResource(in16, "pad_synth.wav", getCacheDir());
            InputStream in17 = expansionFile.getInputStream("piano_tender.wav");
            patchFile = IoUtils.extractResource(in17, "piano_tender.wav", getCacheDir());
            InputStream in18 = expansionFile.getInputStream("rhodes.wav");
            patchFile = IoUtils.extractResource(in18, "rhodes.wav", getCacheDir());
            InputStream in19 = expansionFile.getInputStream("strings.wav");
            patchFile = IoUtils.extractResource(in19, "strings.wav", getCacheDir());
            InputStream in20 = expansionFile.getInputStream("vocals.wav");
            patchFile = IoUtils.extractResource(in20, "vocals.wav", getCacheDir());

            InputStream in = res.openRawResource(R.raw.sample_player);
            patchFile = IoUtils.extractResource(in, "sample_player.pd", getCacheDir());
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

            if(value < DaoMaster.PROGRESS_MAX) {
                pbLoading.setProgress(value);
            } else {
                disableLoadingView();
            }
        }
    }
}
