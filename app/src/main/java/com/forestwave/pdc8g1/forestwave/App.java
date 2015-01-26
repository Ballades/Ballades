package com.forestwave.pdc8g1.forestwave;

import android.app.Application;
import android.content.Context;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.model.DaoSession;
import com.forestwave.pdc8g1.forestwave.model.Species;
import com.forestwave.pdc8g1.forestwave.model.SpeciesDao;
import com.forestwave.pdc8g1.forestwave.model.Tree;
import com.forestwave.pdc8g1.forestwave.model.TreeDao;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;

public class App extends Application {

    private static final String TAG = "App";
    public static final int NB_PAGES_API = 17;
    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = getApplicationContext();
    }

    public App() {
    }

    public static Context getContext() {
        return mContext;
    }

    static String convertStreamToString(java.io.InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    public static void initDatabase() {
        Log.d(TAG, "init Database");

        RequestQueue queue = Volley.newRequestQueue(mContext);
        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "forestWaves-db", null);
        final SQLiteDatabase db = helper.getWritableDatabase();
        final DaoMaster daoMaster = new DaoMaster(db);
        final DaoSession daoSession = daoMaster.newSession();
        final TreeDao treeDao = daoSession.getTreeDao();
        final SpeciesDao speciesDao = daoSession.getSpeciesDao();

        // Ajouter les species à la base de données
        Resources res = getResources();
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
                            db.close();
                        }
                    } catch (JSONException e) {
                        Log.d("JSONException", e.getMessage());
                    }
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
