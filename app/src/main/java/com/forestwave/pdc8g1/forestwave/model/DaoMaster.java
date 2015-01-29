package com.forestwave.pdc8g1.forestwave.model;

import android.content.Context;
import android.content.SharedPreferences;
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
import java.io.InputStream;
import java.util.HashMap;

import de.greenrobot.dao.AbstractDaoMaster;
import de.greenrobot.dao.identityscope.IdentityScopeType;

public class DaoMaster extends AbstractDaoMaster {

    public static final int SCHEMA_VERSION = 1000;
    private static final String TAG = "DaoMaster";
    public static final int NB_PAGES_API = 17;
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
            }
        } catch (Exception e) {
            Log.d("JSONException", e.getMessage());
        }
        db.close();
        for( int i = 1; i <= NB_PAGES_API; i++) {
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

                        for(long cpt = 0; cpt < jTrees.getJSONArray("results").length(); cpt++) {

                            JSONObject jTree = new JSONObject(jTrees.getJSONArray("results").get((int)cpt).toString());
                            String speciesURL = jTree.getString("genre").substring(0, jTree.getString("genre").length()-1);
                            long speciesId = Long.parseLong(speciesURL.substring(speciesURL.lastIndexOf("/")+1, speciesURL.length()));
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
                        }
                    } catch (JSONException e) {
                        Log.d("JSONException", e.getMessage());
                    }
                    db.close();
                    SharedPreferences sharedPref = mContext.getSharedPreferences(mContext.getString(R.string.sp_loading),Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = sharedPref.edit();
                    int value = sharedPref.getInt(mContext.getString(R.string.sp_loading_done), 0);
                    editor.putInt(mContext.getString(R.string.sp_loading_done), value+1);
                    editor.commit();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.d("ERROR", error.toString());
                }
            });

            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    30000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

            queue.add(stringRequest);
        }
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
