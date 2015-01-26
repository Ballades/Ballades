package com.forestwave.pdc8g1.forestwave.model;

import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.forestwave.pdc8g1.forestwave.R;

import org.json.JSONException;
import org.json.JSONObject;
import org.puredata.core.utils.IoUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import de.greenrobot.dao.AbstractDaoMaster;
import de.greenrobot.dao.identityscope.IdentityScopeType;

public class DaoMaster extends AbstractDaoMaster {

    public static final int SCHEMA_VERSION = 1000;

    /** Creates underlying database table using DAOs. */
    public static void createAllTables(SQLiteDatabase db, boolean ifNotExists) {
        TreeDao.createTable(db, ifNotExists);
        SpeciesDao.createTable(db, ifNotExists);
    }

    /** Drops underlying database table using DAOs. */
    public static void dropAllTables(SQLiteDatabase db, boolean ifExists) {
        TreeDao.dropTable(db, ifExists);
        SpeciesDao.dropTable(db, ifExists);
    }

    public static abstract class OpenHelper extends SQLiteOpenHelper {
        private static final String TAG = "App";
        public static final int NB_PAGES_API = 17;
        private Context mContext;

        public OpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory, SCHEMA_VERSION);
            this.mContext=context;
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i("greenDAO", "Creating tables for schema version " + SCHEMA_VERSION);
            createAllTables(db, false);
            //Ici tu peux accéder aux ressources :
            Resources res = mContext.getResources();
            File patchFile = null;
            try {
                InputStream in1 = res.openRawResource(R.raw.acoustic_guitar);
                patchFile = IoUtils.extractResource(in1, "acoustic_guitar.wav", mContext.getCacheDir());


            } catch (IOException e) {
                Log.e(TAG, e.toString());
            }
            initDatabase();
        }

        static String convertStreamToString(java.io.InputStream is) {
            java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
            return s.hasNext() ? s.next() : "";
        }

        public void initDatabase() {
            Log.d(TAG, "init Database");

            RequestQueue queue = Volley.newRequestQueue(mContext);
            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "forestWaves-db", null);
            SQLiteDatabase db = helper.getWritableDatabase();
            DaoMaster daoMaster = new DaoMaster(db);
            DaoSession daoSession = daoMaster.newSession();
            TreeDao treeDao = daoSession.getTreeDao();
            SpeciesDao speciesDao = daoSession.getSpeciesDao();

            // Ajouter les species à la base de données
            Resources res = mContext.getResources();
            InputStream in1 = res.openRawResource(R.raw.species);
            String jsonSpecies = this.convertStreamToString(in1);
            try {
                JSONObject json = new JSONObject(jsonSpecies);
                for(int cpt = 0; cpt < json.getJSONArray("results").length(); cpt++) {
                    JSONObject jSpecies = new JSONObject(json.getJSONArray("results").get(cpt).toString());

                    long id = jSpecies.getInt("count");
                    String name = jSpecies.getString("name");
                    Integer count = jSpecies.getInt("count");
                    Integer track = jSpecies.getInt("track");

                    Species species = new Species(id, name, track, count);
                    speciesDao.insert(species);
                    Log.d(TAG, "Insterting species " + id + " : " + name);
                }
            } catch (Exception e) {
                Log.d("JSONException", e.getMessage());
            }
            db.close();


            for(int i = 1; i <= NB_PAGES_API; i++) {
                String url = "http://rencontres-arbres.herokuapp.com/api/trees/?page="+Integer.toString(i);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,new Response.Listener() {

                    @Override
                    public void onResponse(Object response) {
                        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "forestWaves-db", null);
                        SQLiteDatabase db = helper.getWritableDatabase();
                        DaoMaster daoMaster = new DaoMaster(db);
                        DaoSession daoSession = daoMaster.newSession();
                        TreeDao treeDao = daoSession.getTreeDao();

                        JSONObject jTrees;
                        try {
                            jTrees = new JSONObject(response.toString());

                            for(int cpt = 0; cpt < jTrees.getJSONArray("results").length(); cpt++) {

                                JSONObject jTree = new JSONObject(jTrees.getJSONArray("results").get(cpt).toString());
                                String speciesName = jTree.getString("name");
                                Integer height = jTree.getInt("height");
                                Double latitude = jTree.getDouble("latitude");
                                Double longitude = jTree.getDouble("longitude");
                                if(latitude != null && longitude != null) {

                                    Tree tree = new Tree(null, 1, height, latitude, longitude);

                                    treeDao.insert(tree);
                                    Log.d(TAG, "Tree inserted.");
                                }
                            }
                        } catch (JSONException e) {
                            Log.d("JSONException", e.getMessage());
                        }
                        db.close();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.d("ERROR", error.toString());
                    }
                });

                stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                        5000,
                        DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                        DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

                queue.add(stringRequest);
            }
        }
    }

    /** WARNING: Drops all table on Upgrade! Use only during development. */
    public static class DevOpenHelper extends OpenHelper {
        public DevOpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.i("greenDAO", "Upgrading schema from version " + oldVersion + " to " + newVersion + " by dropping all tables");
            dropAllTables(db, true);
            onCreate(db);
        }
    }

    public DaoMaster(SQLiteDatabase db) {
        super(db, SCHEMA_VERSION);
        registerDaoClass(TreeDao.class);
        registerDaoClass(SpeciesDao.class);
    }

    public DaoSession newSession() {
        return new DaoSession(db, IdentityScopeType.Session, daoConfigMap);
    }

    public DaoSession newSession(IdentityScopeType type) {
        return new DaoSession(db, type, daoConfigMap);
    }
}
