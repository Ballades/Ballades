package com.forestwave.pdc8g1.forestwave.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

/**
 * Created by leo on 12/01/15.
 */
public class TreeDao extends AbstractDao<Tree, Long>{

    public static final String TABLENAME = "Tree";

    /**
     * Properties of entity Tree.<br/>
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property SpeciesId = new Property(1, long.class, "speciesid", false, "SPECIES_ID");
        public final static Property Height = new Property(2, Integer.class, "height", false, "HEIGHT");
        public final static Property Latitude = new Property(3, Double.class, "latitude", false, "LATITUDE");
        public final static Property Longitude = new Property(4, Double.class, "longitude", false, "LONGITUDE");
    };

    public TreeDao(DaoConfig config) {
        super(config);
    }

    public TreeDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'TREE' (" + //
                "'_id' INTEGER PRIMARY KEY ," + // 0: id
                "'SPECIES_ID' INTEGER NOT NULL ," + // 1: species
                "'HEIGHT' INTEGER," + // 2: height
                "'LATITUDE' REAL," + // 3: latitude
                "'LONGITUDE' REAL);"); // 4: longitude
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'TREE'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, Tree entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }

        Long speciesId = entity.getSpeciesId();
        if (speciesId != null) {
            stmt.bindLong(2, speciesId);
        }

        Integer height = entity.getHeight();
        if (height != null) {
            stmt.bindLong(3, height);
        }

        Double latitude = entity.getLatitude();
        if (latitude != null) {
            stmt.bindDouble(4, latitude);
        }

        Double longitude = entity.getLongitude();
        if (longitude != null) {
            stmt.bindDouble(5, longitude);
        }
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }

    /** @inheritdoc */
    @Override
    public Tree readEntity(Cursor cursor, int offset) {
        Tree entity = new Tree(
                cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0), // id
                cursor.getLong(offset + 1), // species
                cursor.isNull(offset + 2) ? null : cursor.getInt(offset + 2),
                cursor.isNull(offset + 3) ? null : cursor.getDouble(offset + 3),
                cursor.isNull(offset + 4) ? null : cursor.getDouble(offset + 4)
        );
        return entity;
    }

    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Tree entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setSpeciesId(cursor.getLong(offset + 1));
        entity.setHeight(cursor.isNull(offset + 2) ? null : cursor.getInt(offset + 2));
        entity.setLatitude(cursor.isNull(offset + 3) ? null : cursor.getDouble(offset + 3));
        entity.setLongitude(cursor.isNull(offset + 4) ? null : cursor.getDouble(offset + 4));
    }

    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(Tree entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }

    /** @inheritdoc */
    @Override
    public Long getKey(Tree entity) {
        if(entity != null) {
            return entity.getId();
        } else {
            return null;
        }
    }

    /** @inheritdoc */
    @Override
    protected boolean isEntityUpdateable() {
        return true;
    }
}
