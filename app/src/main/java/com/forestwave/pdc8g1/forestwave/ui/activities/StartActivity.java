package com.forestwave.pdc8g1.forestwave.ui.activities;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

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

import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import com.forestwave.pdc8g1.forestwave.R;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.service.SoundService;



public class StartActivity extends Activity implements OnClickListener, OnEditorActionListener,SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "StartActivity";

    private Button play;
    private CheckBox ckBass, ckSnare, ckMelody, ckKick, ckHighHat;
    private EditText msg;
    private Button prefs;
    private TextView logs;
    private SeekBar seekBarTempo;
    private SeekBar seekBarStyle;
    private TextView tvChooseStyle;
    private TextView tvLoading;
    private GridLayout gridLayout;
    private ProgressBar pbLoading;
    public SoundService pdService = null;

    private Toast toast = null;


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
            toast("Pure Data says, \"" + msg + "\"");
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
            pdService = (SoundService) ((PdService.PdBinder)service).getService();
            if(!pdService.isRunning()){
                initPd();
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


    private void initGui() {
        setContentView(R.layout.activity_start);
        play = (Button) findViewById(R.id.play_button);
        play.setOnClickListener(this);
        ckBass = (CheckBox) findViewById(R.id.bass_box);
        ckBass.setOnClickListener(this);
        ckSnare = (CheckBox) findViewById(R.id.snare_box);
        ckSnare.setOnClickListener(this);
        ckMelody = (CheckBox) findViewById(R.id.melody_box);
        ckMelody.setOnClickListener(this);
        ckKick = (CheckBox) findViewById(R.id.kick_box);
        ckKick.setOnClickListener(this);
        ckHighHat = (CheckBox) findViewById(R.id.hihat_box);
        ckHighHat.setOnClickListener(this);

        msg = (EditText) findViewById(R.id.msg_box);
        msg.setOnEditorActionListener(this);
        prefs = (Button) findViewById(R.id.pref_button);
        prefs.setOnClickListener(this);
        logs = (TextView) findViewById(R.id.log_box);
        logs.setMovementMethod(new ScrollingMovementMethod());
        seekBarTempo = (SeekBar) findViewById(R.id.seekBarTempo);
        seekBarTempo.setMax(100);
        seekBarTempo.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                PdBase.sendFloat("sqp11r", 50.0f + progresValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        seekBarStyle = (SeekBar) findViewById(R.id.seekBarStyle);
        seekBarStyle.setMax(32);
        seekBarStyle.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
                PdBase.sendFloat("style",progress);
                StartActivity.this.tvChooseStyle.setText(getResources().getText(R.string.style)+" "+String.valueOf(progress));
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
        tvChooseStyle = (TextView) findViewById(R.id.textViewStyle);
        tvChooseStyle.setText(getResources().getText(R.string.style) + " " + String.valueOf(seekBarStyle.getProgress()));
        tvLoading = (TextView) findViewById(R.id.tv_loading);
        pbLoading = (ProgressBar) findViewById(R.id.pb_loading);
        if(value!=DaoMaster.NB_PAGES_API) {
            pbLoading.setProgress(value);
            DaoMaster.initDatabase(this);
        }else {
            disableLoadingView();

        }
    }
    public void disableLoadingView(){
        play.setVisibility(View.VISIBLE);
        msg.setVisibility(View.VISIBLE);
        prefs.setVisibility(View.VISIBLE);
        logs.setVisibility(View.VISIBLE);
        seekBarTempo.setVisibility(View.VISIBLE);
        seekBarStyle.setVisibility(View.VISIBLE);
        tvChooseStyle.setVisibility(View.VISIBLE);
        tvLoading.setVisibility(View.GONE);
        gridLayout = (GridLayout) findViewById(R.id.glayout);
        gridLayout.setVisibility(View.VISIBLE);
        pbLoading.setVisibility(View.GONE);
        Intent serviceIntent = new Intent(this, SoundService.class);
        bindService(serviceIntent, pdConnection, BIND_AUTO_CREATE);
    }

    // Initialize pd ressources : wav samples required and pd patches
    private void initPd() {
        Resources res = getResources();
        File patchFile = null;
        try {
            PdBase.setReceiver(receiver);
            PdBase.subscribe("android");
            InputStream in1 = res.openRawResource(R.raw.acoustic_guitar);
            patchFile = IoUtils.extractResource(in1, "acoustic_guitar.wav", getCacheDir());
            InputStream in2 = res.openRawResource(R.raw.rhodes);
            patchFile = IoUtils.extractResource(in2, "rhodes.wav", getCacheDir());

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
            pdService.initAudio(44100, -1, -1, -1);   // negative values will be replaced with defaults/preferences
            pdService.startAudio(new Intent(this, StartActivity.class), R.drawable.ic_launcher, name, "Return to " + name + ".");
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
            case R.id.bass_box:
                PdBase.sendFloat("togle_b", ckBass.isChecked() ? 1 : 0);
                break;
            case R.id.snare_box:
                PdBase.sendFloat("togle_sn",ckSnare.isChecked() ? 1 : 0);
                break;
            case R.id.melody_box:
                PdBase.sendFloat("togle_m", ckMelody.isChecked() ? 1 : 0);
                break;
            case R.id.kick_box :
                PdBase.sendFloat("togle_k", ckKick.isChecked() ? 1 : 0);
                break;
            case R.id.hihat_box :
                PdBase.sendFloat("togle_hh", ckHighHat.isChecked() ? 1 : 0);
                break;
            case R.id.pref_button:
                startActivity(new Intent(this, PdPreferences.class));
                break;
            default:
                break;
        }
    }

    @Override
    public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
        evaluateMessage(msg.getText().toString());
        return true;
    }

    private void evaluateMessage(String s) {
        String dest = "test", symbol = null;
        boolean isAny = s.length() > 0 && s.charAt(0) == ';';
        Scanner sc = new Scanner(isAny ? s.substring(1) : s);
        if (isAny) {
            if (sc.hasNext()) dest = sc.next();
            else {
                toast("Message not sent (empty recipient)");
                return;
            }
            if (sc.hasNext()) symbol = sc.next();
            else {
                toast("Message not sent (empty symbol)");
            }
        }
        List<Object> list = new ArrayList<Object>();
        while (sc.hasNext()) {
            if (sc.hasNextInt()) {
                list.add(Float.valueOf(sc.nextInt()));
            } else if (sc.hasNextFloat()) {
                list.add(sc.nextFloat());
            } else {
                list.add(sc.next());
            }
        }
        if (isAny) {
            PdBase.sendMessage(dest, symbol, list.toArray());
        } else {
            switch (list.size()) {
                case 0:
                    PdBase.sendBang(dest);
                    break;
                case 1:
                    Object x = list.get(0);
                    if (x instanceof String) {
                        PdBase.sendSymbol(dest, (String) x);
                    } else {
                        PdBase.sendFloat(dest, (Float) x);
                    }
                    break;
                default:
                    PdBase.sendList(dest, list.toArray());
                    break;
            }
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
