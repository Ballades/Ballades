package com.forestwave.pdc8g1.forestwave.ui.activities;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.location.LocationManager;
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
import com.forestwave.pdc8g1.forestwave.ui.dialogs.HowToDialog;
import com.forestwave.pdc8g1.forestwave.ui.dialogs.NoGPSDialog;
import com.forestwave.pdc8g1.forestwave.ui.dialogs.NoLocationDialog;
import com.forestwave.pdc8g1.forestwave.ui.dialogs.WrongLocationDialog;
import com.forestwave.pdc8g1.forestwave.utils.InitDatabaseTask;
import com.forestwave.pdc8g1.forestwave.utils.InitPDTask;

import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;

import java.io.IOException;


public class StartActivity extends Activity implements OnClickListener{

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
            if(!pdService.isRunning() && StartActivity.this!=null ){
                StartActivity.this.pbLoading.setMax(21);
                StartActivity.this.tvLoading.setText(R.string.loading_sounds);
                InitPDTask initPDTask =new InitPDTask(StartActivity.this);
                initPDTask.execute();
            }else if(play!=null){
                play.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_pause));
                if (android.os.Build.VERSION.SDK_INT >= 17) {
                    play.setPaddingRelative(0, 0, 0, 0);
                }
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
            case R.id.action_how_to:
                HowToDialog dialog2 = new HowToDialog();
                dialog2.show(getFragmentManager(),"HowToDialog");
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

        // Debug uniquement, cachées sinon
        seekBarEquality = (SeekBar) findViewById(R.id.seekBarTempo);
        seekBarEquality.setMax(SoundService.SPECIES_EQUALITY_FACTOR*2);
        seekBarEquality.setProgress(SoundService.SPECIES_EQUALITY_FACTOR);
        seekBarEquality.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = SoundService.SPECIES_EQUALITY_FACTOR;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue;
                SoundService.SPECIES_EQUALITY_FACTOR = progressValue;
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
        seekBarDistance.setMax(SoundService.SOUND_DISTANCE_DECREASE_SLOWNESS*2);
        seekBarDistance.setProgress(SoundService.SOUND_DISTANCE_DECREASE_SLOWNESS);
        seekBarDistance.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = SoundService.SOUND_DISTANCE_DECREASE_SLOWNESS;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                progress = progressValue;
                tvEquality.setText(getResources().getText(R.string.choose_SDDS) + " " + String.valueOf(progress));
                SoundService.SOUND_DISTANCE_DECREASE_SLOWNESS = progressValue;
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
        SharedPreferences.Editor editor = sharedPref.edit();
        int value = sharedPref.getInt(this.getString(R.string.sp_loading_done), DaoMaster.DATABASE_UNINITIALIZED);

        tvEquality = (TextView) findViewById(R.id.textViewStyle);
        tvEquality.setText( String.valueOf(seekBarDistance.getProgress()));
        tvScore = (TextView) findViewById(R.id.tv_loading);
        pbLoading = (ProgressBar) findViewById(R.id.pb_loading);
        tvLoading = (TextView) findViewById(R.id.tv_loading);
        if(value == DaoMaster.DATABASE_UNINITIALIZED) {
            pbLoading.setProgress(value);
            InitDatabaseTask initDatabaseTask = new InitDatabaseTask(this);
            initDatabaseTask.execute();
        } else {
           startSoundService();
        }
    }

    public void disableLoadingView(){
        play.setVisibility(View.VISIBLE);
        seekBarEquality.setVisibility(View.GONE);
        seekBarDistance.setVisibility(View.GONE);
        tvEquality.setVisibility(View.GONE);
        tvScore.setVisibility(View.GONE);
        pbLoading.setVisibility(View.GONE);
        tvLoading.setVisibility(View.GONE);
    }

    public void startSoundService(){
        Intent serviceIntent = new Intent(this, SoundService.class);
        bindService(serviceIntent, pdConnection, BIND_AUTO_CREATE);
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
                        int dpAsPixels = (int) (6 * scale + 0.5f);
                        if (android.os.Build.VERSION.SDK_INT >= 17) {
                            play.setPaddingRelative(dpAsPixels, 0, 0, 0);
                        }
                    } else {
                        startAudio();
                        play.setImageDrawable(getResources().getDrawable(R.drawable.ic_av_pause));
                        if (android.os.Build.VERSION.SDK_INT >= 17) {
                            play.setPaddingRelative(0, 0, 0, 0);
                        }

                        this.sendWarningMessages();
                    }
            default:
                break;
        }
    }

    public void updateProgress(int progress){
        if(pbLoading!=null && pbLoading.getVisibility()==View.VISIBLE) {
            pbLoading.setProgress(progress);
        }
    }

    /**
     * Envoie si besoin est les messages d'avertissements liés au GPS et à la position de l'utilisateur
     */
    private void sendWarningMessages() {
        if (pdService.provider.userIsInParc() < 1) {
            if (pdService.provider.userIsInParc() == 3) {
                NoLocationDialog dialog = new NoLocationDialog();
                dialog.show(getFragmentManager(), "NoLocationDialog");
            } else {
                WrongLocationDialog dialog = new WrongLocationDialog();
                dialog.show(getFragmentManager(), "WrongLocationDialog");
            }
        } else {
            LocationManager mLocationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                NoGPSDialog dialog = new NoGPSDialog();
                dialog.show(getFragmentManager(), "NoGPSDialog");
            }
        }
    }

}
