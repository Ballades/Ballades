package com.forestwave.pdc8g1.forestwave.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.android.vending.expansion.zipfile.APKExpansionSupport;
import com.android.vending.expansion.zipfile.ZipResourceFile;
import com.forestwave.pdc8g1.forestwave.R;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.model.DaoSession;
import com.forestwave.pdc8g1.forestwave.model.Species;
import com.forestwave.pdc8g1.forestwave.model.SpeciesDao;
import com.forestwave.pdc8g1.forestwave.model.Tree;
import com.forestwave.pdc8g1.forestwave.model.TreeDao;
import com.forestwave.pdc8g1.forestwave.ui.activities.StartActivity;

import org.json.JSONObject;
import org.puredata.core.PdBase;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;

public class InitPDTask extends AsyncTask<Void, Integer, Long> {
    private static final String TAG="InitDatabaseTask";
    private StartActivity mStartActivity;
    public static final int MAIN_VERSION = 3;
    public static final long MAIN_SIZE = 73171756L;

    public InitPDTask(StartActivity startActivity){
        this.mStartActivity=startActivity;
    }

    protected Long doInBackground(Void... voids) {
        if(mStartActivity!=null) {
            Resources res = mStartActivity.getResources();
            Context appContext=mStartActivity.getApplicationContext();
            File patchFile = null;
            int progress=0;
            publishProgress(progress++);
            try {
                PdBase.subscribe("android");
                // Get a ZipResourceFile representing a merger of both the main and patch files
                ZipResourceFile expansionFile = APKExpansionSupport.getAPKExpansionZipFile(appContext,
                        MAIN_VERSION, 0);
                // Get an input stream for a known file inside the expansion file ZIPs
                InputStream in1 = expansionFile.getInputStream("acoustic_guitar.wav");
                patchFile = IoUtils.extractResource(in1, "acoustic_guitar.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in2 = expansionFile.getInputStream("ballons.wav");
                patchFile = IoUtils.extractResource(in2, "ballons.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in3 = expansionFile.getInputStream("banjo.wav");
                patchFile = IoUtils.extractResource(in3, "banjo.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in4 = expansionFile.getInputStream("clarinet.wav");
                patchFile = IoUtils.extractResource(in4, "clarinet.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in5 = expansionFile.getInputStream("ds.wav");
                patchFile = IoUtils.extractResource(in5, "ds.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in6 = expansionFile.getInputStream("ebow.wav");
                patchFile = IoUtils.extractResource(in6, "ebow.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in7 = expansionFile.getInputStream("electribe.wav");
                patchFile = IoUtils.extractResource(in7, "electribe.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in8 = expansionFile.getInputStream("electric_bass.wav");
                patchFile = IoUtils.extractResource(in8, "electric_bass.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in9 = expansionFile.getInputStream("flute_piano.wav");
                patchFile = IoUtils.extractResource(in9, "flute_piano.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in10 = expansionFile.getInputStream("guitar.wav");
                patchFile = IoUtils.extractResource(in10, "guitar.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in11 = expansionFile.getInputStream("harmon_trumpet.wav");
                patchFile = IoUtils.extractResource(in11, "harmon_trumpet.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in12 = expansionFile.getInputStream("information.wav");
                patchFile = IoUtils.extractResource(in12, "information.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in13 = expansionFile.getInputStream("love.wav");
                patchFile = IoUtils.extractResource(in13, "love.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in14 = expansionFile.getInputStream("mallet.wav");
                patchFile = IoUtils.extractResource(in14, "mallet.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in15 = expansionFile.getInputStream("omnichord_qchord.wav");
                patchFile = IoUtils.extractResource(in15, "omnichord_qchord.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in16 = expansionFile.getInputStream("pad_synth.wav");
                patchFile = IoUtils.extractResource(in16, "pad_synth.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in17 = expansionFile.getInputStream("piano_tender.wav");
                patchFile = IoUtils.extractResource(in17, "piano_tender.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in18 = expansionFile.getInputStream("rhodes.wav");
                patchFile = IoUtils.extractResource(in18, "rhodes.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in19 = expansionFile.getInputStream("strings.wav");
                patchFile = IoUtils.extractResource(in19, "strings.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);
                InputStream in20 = expansionFile.getInputStream("vocals.wav");
                patchFile = IoUtils.extractResource(in20, "vocals.wav", mStartActivity.getCacheDir());
                publishProgress(progress++);

                InputStream in = res.openRawResource(R.raw.sample_player);
                patchFile = IoUtils.extractResource(in, "sample_player.pd", mStartActivity.getCacheDir());
                PdBase.openPatch(patchFile);

            } catch (IOException e) {
                Log.e(TAG, e.toString());
            } finally {
                if (patchFile != null) patchFile.delete();
            }
        }
        return 0L;
    }

    protected void onProgressUpdate(Integer... progress) {
        int value=progress[0];
        if(mStartActivity!=null){
            mStartActivity.updateProgress(value);
        }
        Log.d(TAG, "progessbar value : " + value);

    }



    protected void onPostExecute(Long result) {

        if(mStartActivity!=null){
            mStartActivity.disableLoadingView();
        }
    }
}