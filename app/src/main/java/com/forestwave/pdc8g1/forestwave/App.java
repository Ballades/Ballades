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

import com.forestwave.pdc8g1.forestwave.Model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.Model.DaoSession;
import com.forestwave.pdc8g1.forestwave.Model.Tree;
import com.forestwave.pdc8g1.forestwave.Model.TreeDao;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Created by leo on 12/01/15.
 */
public class App extends Application {

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

                            JSONObject jTree = new JSONObject(jTrees.getJSONArray("results").get(cpt).toString());
                            String species = jTree.getString("name");
                            Integer height = jTree.getInt("height");
                            Double latitude = jTree.getDouble("latitude");
                            Double longitude = jTree.getDouble("longitude");
                            if(latitude != null && longitude != null) {

                                Tree tree = new Tree(null, species, height, latitude, longitude);
                                treeDao.insert(tree);
                                Log.d("FORESTWAVES", "insert " + Integer.toString(cpt) + " ème tree with species : " + tree.getSpecies());
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
