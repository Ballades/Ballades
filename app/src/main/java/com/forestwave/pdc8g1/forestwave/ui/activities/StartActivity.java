package com.forestwave.pdc8g1.forestwave.ui.activities;

import java.io.DataInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import org.puredata.android.service.PdService;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.IBinder;
import android.os.Messenger;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.forestwave.pdc8g1.forestwave.R;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.service.MyDownloadService;
import com.forestwave.pdc8g1.forestwave.service.SoundService;
import com.forestwave.pdc8g1.forestwave.ui.dialogs.AboutDialog;
import com.google.android.vending.expansion.downloader.Constants;
import com.google.android.vending.expansion.downloader.DownloadProgressInfo;
import com.google.android.vending.expansion.downloader.DownloaderClientMarshaller;
import com.google.android.vending.expansion.downloader.DownloaderServiceMarshaller;
import com.google.android.vending.expansion.downloader.Helpers;
import com.google.android.vending.expansion.downloader.IDownloaderClient;
import com.google.android.vending.expansion.downloader.IDownloaderService;
import com.google.android.vending.expansion.downloader.IStub;


public class StartActivity extends Activity implements OnClickListener,SharedPreferences.OnSharedPreferenceChangeListener, IDownloaderClient {

    private static final String TAG = "StartActivity";

    private ImageButton play;
    private ProgressBar pbLoading;
    private TextView tvLoading;

    private SeekBar seekBarEquality;
    private SeekBar seekBarDistance;
    private TextView tvEquality;
    private TextView tvScore;


    public SoundService pdService = null;
    private ProgressBar mPB;

    private TextView mStatusText;
    private TextView mProgressFraction;
    private TextView mProgressPercent;
    private TextView mAverageSpeed;
    private TextView mTimeRemaining;

    private View mDashboard;
    private View mCellMessage;

    private Button mPauseButton;
    private Button mWiFiSettingsButton;

    private boolean mStatePaused;
    private int mState;


    private IDownloaderService mRemoteService;

    private IStub mDownloaderClientStub;

    private void setState(int newState) {
        if (mState != newState) {
            mState = newState;
            mStatusText.setText(Helpers.getDownloaderStringResourceIDFromState(newState));
        }
    }

    private void setButtonPausedState(boolean paused) {
        mStatePaused = paused;
        int stringResourceID = paused ? R.string.text_button_resume :
                R.string.text_button_pause;
        mPauseButton.setText(stringResourceID);
    }


    /**
     * This is a little helper class that demonstrates simple testing of an
     * Expansion APK file delivered by Market. You may not wish to hard-code
     * things such as file lengths into your executable... and you may wish to
     * turn this code off during application development.
     */
    private static class XAPKFile {
        public final boolean mIsMain;
        public final int mFileVersion;
        public final long mFileSize;

        XAPKFile(boolean isMain, int fileVersion, long fileSize) {
            mIsMain = isMain;
            mFileVersion = fileVersion;
            mFileSize = fileSize;
        }
    }
    /**
     * Here is where you place the data that the validator will use to determine
     * if the file was delivered correctly. This is encoded in the source code
     * so the application can easily determine whether the file has been
     * properly delivered without having to talk to the server. If the
     * application is using LVL for licensing, it may make sense to eliminate
     * these checks and to just rely on the server.
     */
    private static final XAPKFile[] xAPKS = {
            new XAPKFile(
                    true, // true signifies a main file
                    3, // the version of the APK that the file was uploaded
                    // against
                    73171756L // the length of the file in bytes

            )
    };

    /**
     * Go through each of the APK Expansion files defined in the structure above
     * and determine if the files are present and match the required size. Free
     * applications should definitely consider doing this, as this allows the
     * application to be launched for the first time without having a network
     * connection present. Paid applications that use LVL should probably do at
     * least one LVL check that requires the network to be present, so this is
     * not as necessary.
     *
     * @return true if they are present.
     */
    boolean expansionFilesDelivered() {
        for (XAPKFile xf : xAPKS) {
            String fileName = Helpers.getExpansionAPKFileName(this, xf.mIsMain, xf.mFileVersion);
            if (!Helpers.doesFileExist(this, fileName, xf.mFileSize, false))
                return false;
        }
        return true;
    }

    /**
     * Calculating a moving average for the validation speed so we don't get
     * jumpy calculations for time etc.
     */
    static private final float SMOOTHING_FACTOR = 0.005f;

    /**
     * Used by the async task
     */
    private boolean mCancelValidation;

    /**
     * Go through each of the Expansion APK files and open each as a zip file.
     * Calculate the CRC for each file and return false if any fail to match.
     *
     * @return true if XAPKZipFile is successful
     */
    void validateXAPKZipFiles() {
        AsyncTask<Object, DownloadProgressInfo, Boolean> validationTask = new AsyncTask<Object, DownloadProgressInfo, Boolean>() {

            @Override
            protected void onPreExecute() {
                mDashboard.setVisibility(View.VISIBLE);
                mCellMessage.setVisibility(View.GONE);
                mStatusText.setText(R.string.text_verifying_download);
                mPauseButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        mCancelValidation = true;
                    }
                });
                mPauseButton.setText(R.string.text_button_cancel_verify);
                super.onPreExecute();
            }

            @Override
            protected Boolean doInBackground(Object... params) {
                for (XAPKFile xf : xAPKS) {
                    String fileName = Helpers.getExpansionAPKFileName(
                            StartActivity.this,
                            xf.mIsMain, xf.mFileVersion);
                    if (!Helpers.doesFileExist(StartActivity.this, fileName,
                            xf.mFileSize, false))
                        return false;
                    fileName = Helpers
                            .generateSaveFileName(StartActivity.this, fileName);
                    ZipResourceFile zrf;
                    byte[] buf = new byte[1024 * 256];
                    try {
                        zrf = new ZipResourceFile(fileName);
                        ZipResourceFile.ZipEntryRO[] entries = zrf.getAllEntries();
                        /**
                         * First calculate the total compressed length
                         */
                        long totalCompressedLength = 0;
                        for (ZipResourceFile.ZipEntryRO entry : entries) {
                            totalCompressedLength += entry.mCompressedLength;
                        }
                        float averageVerifySpeed = 0;
                        long totalBytesRemaining = totalCompressedLength;
                        long timeRemaining;
                        /**
                         * Then calculate a CRC for every file in the Zip file,
                         * comparing it to what is stored in the Zip directory.
                         * Note that for compressed Zip files we must extract
                         * the contents to do this comparison.
                         */
                        for (ZipResourceFile.ZipEntryRO entry : entries) {
                            if (-1 != entry.mCRC32) {
                                long length = entry.mUncompressedLength;
                                CRC32 crc = new CRC32();
                                DataInputStream dis = null;
                                try {
                                    dis = new DataInputStream(
                                            zrf.getInputStream(entry.mFileName));

                                    long startTime = SystemClock.uptimeMillis();
                                    while (length > 0) {
                                        int seek = (int) (length > buf.length ? buf.length
                                                : length);
                                        dis.readFully(buf, 0, seek);
                                        crc.update(buf, 0, seek);
                                        length -= seek;
                                        long currentTime = SystemClock.uptimeMillis();
                                        long timePassed = currentTime - startTime;
                                        if (timePassed > 0) {
                                            float currentSpeedSample = (float) seek
                                                    / (float) timePassed;
                                            if (0 != averageVerifySpeed) {
                                                averageVerifySpeed = SMOOTHING_FACTOR
                                                        * currentSpeedSample
                                                        + (1 - SMOOTHING_FACTOR)
                                                        * averageVerifySpeed;
                                            } else {
                                                averageVerifySpeed = currentSpeedSample;
                                            }
                                            totalBytesRemaining -= seek;
                                            timeRemaining = (long) (totalBytesRemaining / averageVerifySpeed);
                                            this.publishProgress(
                                                    new DownloadProgressInfo(
                                                            totalCompressedLength,
                                                            totalCompressedLength
                                                                    - totalBytesRemaining,
                                                            timeRemaining,
                                                            averageVerifySpeed)
                                            );
                                        }
                                        startTime = currentTime;
                                        if (mCancelValidation)
                                            return true;
                                    }
                                    if (crc.getValue() != entry.mCRC32) {
                                        Log.e(Constants.TAG,
                                                "CRC does not match for entry: "
                                                        + entry.mFileName);
                                        Log.e(Constants.TAG,
                                                "In file: " + entry.getZipFileName());
                                        return false;
                                    }
                                } finally {
                                    if (null != dis) {
                                        dis.close();
                                    }
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return false;
                    }
                }
                return true;
            }

            @Override
            protected void onProgressUpdate(DownloadProgressInfo... values) {
                onDownloadProgress(values[0]);
                super.onProgressUpdate(values);
            }

            @Override
            protected void onPostExecute(Boolean result) {
                if (result) {
                    mDashboard.setVisibility(View.VISIBLE);
                    mCellMessage.setVisibility(View.GONE);
                    mStatusText.setText(R.string.text_validation_complete);
                    mPauseButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    });
                    mPauseButton.setText(android.R.string.ok);
                } else {
                    mDashboard.setVisibility(View.VISIBLE);
                    mCellMessage.setVisibility(View.GONE);
                    mStatusText.setText(R.string.text_validation_failed);
                    mPauseButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            finish();
                        }
                    });
                    mPauseButton.setText(android.R.string.cancel);
                }
                super.onPostExecute(result);
            }

        };
        validationTask.execute(new Object());
    }

    /**
     * If the download isn't present, we initialize the download UI. This ties
     * all of the controls into the remote service calls.
     */
    private void initializeDownloadUI() {
        mDownloaderClientStub = DownloaderClientMarshaller.CreateStub
                (this, MyDownloadService.class);
        setContentView(R.layout.main);

        mPB = (ProgressBar) findViewById(R.id.progressBar);
        mStatusText = (TextView) findViewById(R.id.statusText);
        mProgressFraction = (TextView) findViewById(R.id.progressAsFraction);
        mProgressPercent = (TextView) findViewById(R.id.progressAsPercentage);
        mAverageSpeed = (TextView) findViewById(R.id.progressAverageSpeed);
        mTimeRemaining = (TextView) findViewById(R.id.progressTimeRemaining);
        mDashboard = findViewById(R.id.downloaderDashboard);
        mCellMessage = findViewById(R.id.approveCellular);
        mPauseButton = (Button) findViewById(R.id.pauseButton);
        mWiFiSettingsButton = (Button) findViewById(R.id.wifiSettingsButton);

        mPauseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mStatePaused) {
                    mRemoteService.requestContinueDownload();
                } else {
                    mRemoteService.requestPauseDownload();
                }
                setButtonPausedState(!mStatePaused);
            }
        });

        mWiFiSettingsButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
            }
        });

        Button resumeOnCell = (Button) findViewById(R.id.resumeOverCellular);
        resumeOnCell.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRemoteService.setDownloadFlags(IDownloaderService.FLAGS_DOWNLOAD_OVER_CELLULAR);
                mRemoteService.requestContinueDownload();
                mCellMessage.setVisibility(View.GONE);
            }
        });

    }


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

        /**
         * Both downloading and validation make use of the "download" UI
         */
        initializeDownloadUI();

        /**
         * Before we do anything, are the files we expect already here and
         * delivered (presumably by Market) For free titles, this is probably
         * worth doing. (so no Market request is necessary)
         */
        if (!expansionFilesDelivered()) {

            try {
                Intent launchIntent = StartActivity.this
                        .getIntent();
                Intent intentToLaunchThisActivityFromNotification = new Intent(
                        StartActivity
                                .this, StartActivity.this.getClass());
                intentToLaunchThisActivityFromNotification.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK |
                        Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intentToLaunchThisActivityFromNotification.setAction(launchIntent.getAction());

                if (launchIntent.getCategories() != null) {
                    for (String category : launchIntent.getCategories()) {
                        intentToLaunchThisActivityFromNotification.addCategory(category);
                    }
                }

                // Build PendingIntent used to open this activity from
                // Notification
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        StartActivity.this,
                        0, intentToLaunchThisActivityFromNotification,
                        PendingIntent.FLAG_UPDATE_CURRENT);
                // Request to start the download
                int startResult = DownloaderClientMarshaller.startDownloadServiceIfRequired(this,
                        pendingIntent, MyDownloadService.class);

                if (startResult != DownloaderClientMarshaller.NO_DOWNLOAD_REQUIRED) {
                    // The DownloaderService has started downloading the files,
                    // show progress
                    initializeDownloadUI();
                    return;
                } // otherwise, download not needed so we fall through to
                // starting the movie
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Cannot find own package! MAYDAY!");
                e.printStackTrace();
            }

        } else {
            validateXAPKZipFiles();
        }

        initGui();
    }

    /**
     * Critical implementation detail. In onServiceConnected we create the
     * remote service and marshaler. This is how we pass the client information
     * back to the service so the client can be properly notified of changes. We
     * must do this every time we reconnect to the service.
     */
    @Override
    public void onServiceConnected(Messenger m) {
        mRemoteService = DownloaderServiceMarshaller.CreateProxy(m);
        mRemoteService.onClientUpdated(mDownloaderClientStub.getMessenger());
    }

    /**
     * The download state should trigger changes in the UI --- it may be useful
     * to show the state as being indeterminate at times. This sample can be
     * considered a guideline.
     */
    @Override
    public void onDownloadStateChanged(int newState) {
        setState(newState);
        boolean showDashboard = true;
        boolean showCellMessage = false;
        boolean paused;
        boolean indeterminate;
        switch (newState) {
            case IDownloaderClient.STATE_IDLE:
                // STATE_IDLE means the service is listening, so it's
                // safe to start making calls via mRemoteService.
                paused = false;
                indeterminate = true;
                break;
            case IDownloaderClient.STATE_CONNECTING:
            case IDownloaderClient.STATE_FETCHING_URL:
                showDashboard = true;
                paused = false;
                indeterminate = true;
                break;
            case IDownloaderClient.STATE_DOWNLOADING:
                paused = false;
                showDashboard = true;
                indeterminate = false;
                break;

            case IDownloaderClient.STATE_FAILED_CANCELED:
            case IDownloaderClient.STATE_FAILED:
            case IDownloaderClient.STATE_FAILED_FETCHING_URL:
            case IDownloaderClient.STATE_FAILED_UNLICENSED:
                paused = true;
                showDashboard = false;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_PAUSED_NEED_CELLULAR_PERMISSION:
            case IDownloaderClient.STATE_PAUSED_WIFI_DISABLED_NEED_CELLULAR_PERMISSION:
                showDashboard = false;
                paused = true;
                indeterminate = false;
                showCellMessage = true;
                break;

            case IDownloaderClient.STATE_PAUSED_BY_REQUEST:
                paused = true;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_PAUSED_ROAMING:
            case IDownloaderClient.STATE_PAUSED_SDCARD_UNAVAILABLE:
                paused = true;
                indeterminate = false;
                break;
            case IDownloaderClient.STATE_COMPLETED:
                showDashboard = false;
                paused = false;
                indeterminate = false;
                validateXAPKZipFiles();
                return;
            default:
                paused = true;
                indeterminate = true;
                showDashboard = true;
        }
        int newDashboardVisibility = showDashboard ? View.VISIBLE : View.GONE;
        if (mDashboard.getVisibility() != newDashboardVisibility) {
            mDashboard.setVisibility(newDashboardVisibility);
        }
        int cellMessageVisibility = showCellMessage ? View.VISIBLE : View.GONE;
        if (mCellMessage.getVisibility() != cellMessageVisibility) {
            mCellMessage.setVisibility(cellMessageVisibility);
        }

        mPB.setIndeterminate(indeterminate);
        setButtonPausedState(paused);
    }

    /**
     * Sets the state of the various controls based on the progressinfo object
     * sent from the downloader service.
     */
    @Override
    public void onDownloadProgress(DownloadProgressInfo progress) {
        mAverageSpeed.setText(getString(R.string.kilobytes_per_second,
                Helpers.getSpeedString(progress.mCurrentSpeed)));
        mTimeRemaining.setText(getString(R.string.time_remaining,
                Helpers.getTimeRemaining(progress.mTimeRemaining)));

        progress.mOverallTotal = progress.mOverallTotal;
        mPB.setMax((int) (progress.mOverallTotal >> 8));
        mPB.setProgress((int) (progress.mOverallProgress >> 8));
        mProgressPercent.setText(Long.toString(progress.mOverallProgress
                * 100 /
                progress.mOverallTotal) + "%");
        mProgressFraction.setText(Helpers.getDownloadProgressString
                (progress.mOverallProgress,
                        progress.mOverallTotal));
    }


    @Override
    protected void onResume() {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.connect(this);
        }
        super.onResume();
    }

    @Override
    protected void onStop() {
        if (null != mDownloaderClientStub) {
            mDownloaderClientStub.disconnect(this);
        }
        super.onStop();
    }



    @Override
    protected void onDestroy() {
        this.mCancelValidation = true;
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
