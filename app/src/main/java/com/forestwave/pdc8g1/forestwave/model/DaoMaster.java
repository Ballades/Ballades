package com.forestwave.pdc8g1.forestwave.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.android.volley.RequestQueue;
import com.android.volley.toolbox.Volley;
import com.forestwave.pdc8g1.forestwave.R;

import org.json.JSONObject;

import java.io.InputStream;
import java.util.HashMap;

import de.greenrobot.dao.AbstractDaoMaster;
import de.greenrobot.dao.identityscope.IdentityScopeType;

public class DaoMaster extends AbstractDaoMaster {

    public static final int PROGRESS_MAX = 800;
    public static final int SCHEMA_VERSION = 1000;
    private static final String TAG = "DaoMaster";
    public static HashMap<Long, Species> speciesKeys = new HashMap<>();
    public Context mContext= null;

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

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void initDatabase(final Context mContext) {
        Log.d(TAG, "init Database");
        RequestQueue queue = Volley.newRequestQueue(mContext);
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "forestWaves-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        TreeDao treeDao = daoSession.getTreeDao();
        SpeciesDao speciesDao = daoSession.getSpeciesDao();

        SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.sp_loading), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        int value = sharedPref.getInt(mContext.getString(R.string.sp_loading_done), 0);

        // Ajouter les species à la base de données
        Resources res = mContext.getResources();
        InputStream in1 = res.openRawResource(R.raw.species);
        String jsonSpecies = convertStreamToString(in1);
        try {
            JSONObject json = new JSONObject(jsonSpecies);
            for(int cpt = 0; cpt < json.getJSONArray("results").length(); cpt++) {
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
                if (cpt%10 == 0) {
                    value++;
                    editor.putInt(mContext.getString(R.string.sp_loading_done), value);
                    editor.commit();
                    Log.d(TAG, "progessbar value : " + value);
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
            for(int cpt = 0; cpt < json.getJSONArray("results").length(); cpt++) {
                JSONObject jTree = new JSONObject(json.getJSONArray("results").get(cpt).toString());

                long speciesId = jTree.getLong("species_id");
                Species species = speciesKeys.get(speciesId);
                Integer height = jTree.getInt("height");
                Double latitude = jTree.getDouble("latitude");
                Double longitude = jTree.getDouble("longitude");

                if(species != null && latitude != null && longitude != null) {
                    Tree tree = new Tree(null, species, height, latitude, longitude);
                    treeDao.insert(tree);
                    Log.d(TAG, "Tree inserted : speciesId : " + speciesId + ", " + species.getName());
                }
                if(species == null) {
                    Log.d(TAG, "null speciesId : " + speciesId);
                }

                // Barre de progression
                if (cpt%10 == 0) {
                    editor.putInt(mContext.getString(R.string.sp_loading_done), ++value);
                    editor.commit();
                    Log.d(TAG, "progessbar value : " + value);
                }
            }

        } catch (Exception e) {
            Log.d("JSONException", e.getMessage());
        }
        db.close();
    }

    public static abstract class OpenHelper extends SQLiteOpenHelper {


        public OpenHelper(Context context, String name, SQLiteDatabase.CursorFactory factory) {
            super(context, name, factory, SCHEMA_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            Log.i("greenDAO", "Creating tables for schema version " + SCHEMA_VERSION);
            createAllTables(db, false);

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
