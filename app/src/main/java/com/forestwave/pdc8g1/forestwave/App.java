package com.forestwave.pdc8g1.forestwave;

import android.app.Application;
import android.content.Context;
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

/**
 * Created by leo on 12/01/15.
 */
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

    public static void  initDatabase() {

        Log.d("FORESTWAVES", "init Database");

        RequestQueue queue = Volley.newRequestQueue(mContext);
        String url;

        for(int i = 1 ; i <= NB_PAGES_API ; i++) {

            url = "http://rencontres-arbres.herokuapp.com/api/trees/?page="+Integer.toString(i);
            StringRequest stringRequest = new StringRequest(Request.Method.GET, url,new Response.Listener() {

                @Override
                public void onResponse(Object response) {

                    JSONObject jTrees;
                    try {
                        jTrees = new JSONObject(response.toString());

                        for(int cpt = 0 ; cpt < jTrees.getJSONArray("results").length() ; cpt++) {

                            DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(mContext, "forestWaves-db", null);
                            SQLiteDatabase db = helper.getWritableDatabase();
                            DaoMaster daoMaster = new DaoMaster(db);
                            DaoSession daoSession = daoMaster.newSession();
                            TreeDao treeDao = daoSession.getTreeDao();
                            SpeciesDao speciesDao = daoSession.getSpeciesDao();

                            JSONObject jTree = new JSONObject(jTrees.getJSONArray("results").get(cpt).toString());
                            String speciesName = jTree.getString("name");
                            Integer height = jTree.getInt("height");
                            Double latitude = jTree.getDouble("latitude");
                            Double longitude = jTree.getDouble("longitude");
                            if(latitude != null && longitude != null) {
                                // Ajouter l'espèce si elle n'est pas présente
                                Log.d(TAG, "inserting species : " + speciesName);
                                Integer track = 1;
                                Integer count = 100;
                                Species species = new Species(null, speciesName, track, count);
                                Long speciesId = speciesDao.insert(species);
                                species.setId(speciesId);
                                Log.d(TAG, "species Id : " + speciesId);

                                Tree tree = new Tree(null, species, height, latitude, longitude);

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
