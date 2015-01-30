package com.forestwave.pdc8g1.forestwave.utils;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.os.AsyncTask;
import android.util.Log;

import com.forestwave.pdc8g1.forestwave.R;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.model.DaoSession;
import com.forestwave.pdc8g1.forestwave.model.Species;
import com.forestwave.pdc8g1.forestwave.model.SpeciesDao;
import com.forestwave.pdc8g1.forestwave.model.Tree;
import com.forestwave.pdc8g1.forestwave.model.TreeDao;
import com.forestwave.pdc8g1.forestwave.ui.activities.StartActivity;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Created by Sylvain on 30/01/15.
 */
public class InitDatabaseTask extends AsyncTask<Void, Integer, Long> {
    private static final String TAG="InitDatabaseTask";
    private HashMap<Long, Species> speciesKeys = new HashMap<>();
    private StartActivity mStartActivity;

    public InitDatabaseTask(StartActivity startActivity){
        this.mStartActivity=startActivity;
    }

    protected Long doInBackground(Void... voids) {
        if(mStartActivity!=null) {
            Context mContext=mStartActivity.getApplicationContext();
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "forestWaves-db", null);
            SQLiteDatabase db = helper.getWritableDatabase();
            DaoMaster daoMaster = new DaoMaster(db);
            DaoSession daoSession = daoMaster.newSession();
            TreeDao treeDao = daoSession.getTreeDao();
            SpeciesDao speciesDao = daoSession.getSpeciesDao();
            SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.sp_loading), Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putInt(mContext.getString(R.string.sp_loading_done), DaoMaster.DATABASE_INCOMPLETE);
            editor.commit();
            // Ajouter les species à la base de données
            Resources res = mContext.getResources();
            InputStream in1 = res.openRawResource(R.raw.species);
            String jsonSpecies = convertStreamToString(in1);
            int value = 0;
            try {
                JSONObject json = new JSONObject(jsonSpecies);
                for (int cpt = 0; cpt < json.getJSONArray("results").length(); cpt++) {
                    JSONObject jSpecies = new JSONObject(json.getJSONArray("results").get(cpt).toString());

                    long id = jSpecies.getInt("id");
                    String name = jSpecies.getString("name");
                    Integer count = jSpecies.getInt("count");
                    Integer track = jSpecies.getInt("track");

                    Species species = new Species(id, name, track, count);
                    speciesDao.insert(species);
                    speciesKeys.put(id, species);
                    Log.d(TAG, "Insterting species " + id + " : " + name);

                    // Barre de progression
                    if (cpt % 10 == 0) {
                        publishProgress(++value);
                    }
                }
            } catch (Exception e) {
                Log.d("JSONException", e.getMessage());
            }

            // Ajouter les arbres
            InputStream in = res.openRawResource(R.raw.trees);
            String jsonTrees = convertStreamToString(in);
            try {
                JSONObject json = new JSONObject(jsonTrees);
                for (int cpt = 0; cpt < json.getJSONArray("results").length(); cpt++) {
                    JSONObject jTree = new JSONObject(json.getJSONArray("results").get(cpt).toString());

                    long speciesId = jTree.getLong("species_id");
                    Species species = speciesKeys.get(speciesId);
                    Integer height = jTree.getInt("height");
                    Double latitude = jTree.getDouble("latitude");
                    Double longitude = jTree.getDouble("longitude");

                    if (species != null && latitude != null && longitude != null) {
                        Tree tree = new Tree(null, species, height, latitude, longitude);
                        treeDao.insert(tree);
                        Log.d(TAG, "Tree inserted : speciesId : " + speciesId + ", " + species.getName());
                    }
                    if (species == null) {
                        Log.d(TAG, "null speciesId : " + speciesId);
                    }

                    // Barre de progression
                    if (cpt % 10 == 0) {
                        publishProgress(++value);
                    }
                }

            } catch (Exception e) {
                Log.d("JSONException", e.getMessage());
            }
            db.close();
        }
        return 0L;
    }
    protected static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    protected void onProgressUpdate(Integer... progress) {
        int value=progress[0];
        if(mStartActivity!=null){
            mStartActivity.updateProgress(value);
        }
        Log.d(TAG, "progessbar value : " + value);

    }



    protected void onPostExecute(Long result) {
        Context mContext= mStartActivity.getApplicationContext();
        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.sp_loading), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putInt(mContext.getString(R.string.sp_loading_done), DaoMaster.DATABASE_INITIALIZED);
        editor.commit();
        if(mStartActivity!=null){
            mStartActivity.startSoundService();
        }
    }
}