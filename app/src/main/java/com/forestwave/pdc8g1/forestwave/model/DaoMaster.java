package com.forestwave.pdc8g1.forestwave.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;
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
    public static final int DATABASE_UNINITIALIZED=0;
    public static final int DATABASE_INCOMPLETE=1;
    public static final int DATABASE_INITIALIZED=2;

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
