package com.forestwave.pdc8g1.forestwave;

import android.app.Application;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

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

import java.util.Iterator;

/**
 * Created by leo on 12/01/15.
 */
public class App extends Application {

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
        String url ="http://rencontres-arbres.herokuapp.com/api/trees/";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener() {

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
                                Tree tree = new Tree(null, species, height);

                                treeDao.insert(tree);
                                Log.d("FORESTWAVES", "insert tree with species : " + tree.getSpecies());
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

        queue.add(stringRequest);
    }
}
