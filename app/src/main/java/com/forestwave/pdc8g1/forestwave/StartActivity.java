package com.forestwave.pdc8g1.forestwave;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.os.Build;

import com.forestwave.pdc8g1.forestwave.Model.DaoMaster;
import com.forestwave.pdc8g1.forestwave.Model.DaoSession;
import com.forestwave.pdc8g1.forestwave.Model.Tree;
import com.forestwave.pdc8g1.forestwave.Model.TreeDao;

import java.util.List;

import de.greenrobot.dao.query.QueryBuilder;

public class StartActivity extends Activity {

    private DaoMaster daoMaster;
    private DaoSession daoSession;
    private TreeDao treeDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();
        }

        DaoMaster.DevOpenHelper helper = new DaoMaster.DevOpenHelper(this, "forestWaves-db", null);
        SQLiteDatabase db = helper.getWritableDatabase();
        DaoMaster daoMaster = new DaoMaster(db);
        DaoSession daoSession = daoMaster.newSession();

        TreeDao treeDao = daoSession.getTreeDao();
        Tree tree = new Tree(null, "chêne", 17, 4.532453, 48.346436);
        Tree tree2 = new Tree(null, "chêne", 18, 5.532453, 48.676767);
        Tree tree3 = new Tree(null, "chêne", 19, 6.532453, 48.1212121);
        treeDao.insert(tree);
        treeDao.insert(tree2);
        treeDao.insert(tree3);

        List trees = treeDao.queryBuilder()
                                    .where(TreeDao.Properties.Height.eq(17))
                                    .list();
        Log.d("DaoExample", "species : " + trees.size());
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_start, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_start, container, false);
            return rootView;
        }
    }
}
