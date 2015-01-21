package com.forestwave.pdc8g1.forestwave.utils;

import android.app.Activity;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.forestwave.pdc8g1.forestwave.App;
import com.forestwave.pdc8g1.forestwave.location.LocationProvider;
import com.forestwave.pdc8g1.forestwave.model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.model.DaoSession;
import com.forestwave.pdc8g1.forestwave.model.Tree;
import com.forestwave.pdc8g1.forestwave.model.TreeDao;

import java.util.List;

import de.greenrobot.dao.query.Query;

/**
 * Created by leo on 17/01/15.
 */
public class TreeFinder implements Runnable {

    private Activity activity;

    public TreeFinder(Activity activity) {

        this.activity = activity;
    }

    @Override
    public void run() {

        LocationProvider provider = new LocationProvider(activity);

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(App.getContext(), "forestWaves-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();
        TreeDao treeDao = daoSession.getTreeDao();

        Double latitude = provider.getLocation().getLatitude();
        Double longitude = provider.getLocation().getLongitude();

        Query query = treeDao.queryBuilder().where(TreeDao.Properties.Latitude.between(latitude - 0.01/111, latitude + 0.01/111), TreeDao.Properties.Longitude.between(longitude - 0.01/76, longitude + 0.01/76)).build();
        List<Tree> trees = query.list();
        Log.d("NB TREE", Integer.toString(trees.size()));
    }
}
