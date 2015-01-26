package com.forestwave.pdc8g1.forestwave.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;

public class SpeciesDao extends AbstractDao<Species, Long> {

    public static final String TABLENAME = "Species";

    /**
     * Properties of entity Species.
     * Can be used for QueryBuilder and for referencing column names.
     */
    public static class Properties {
        public final static Property Id = new Property(0, Long.class, "id", true, "_id");
        public final static Property Name = new Property(1, String.class, "name", false, "NAME");
        public final static Property Track = new Property(2, Integer.class, "track", false, "TRACK");
        public final static Property Count = new Property(3, Integer.class, "count", false, "COUNT");
    };

    private DaoSession daoSession;

    public SpeciesDao(DaoConfig config) {
        super(config);
    }

    public SpeciesDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
        this.daoSession = daoSession;
    }

    /** Creates the underlying database table. */
    public static void createTable(SQLiteDatabase db, boolean ifNotExists) {
        String constraint = ifNotExists? "IF NOT EXISTS ": "";
        db.execSQL("CREATE TABLE " + constraint + "'SPECIES' (" + //
                "'_id' INTEGER PRIMARY KEY ," +
                "'NAME' TEXT," +
                "'TRACK' INTEGER," +
                "'COUNT' INTEGER);");
    }

    /** Drops the underlying database table. */
    public static void dropTable(SQLiteDatabase db, boolean ifExists) {
        String sql = "DROP TABLE " + (ifExists ? "IF EXISTS " : "") + "'SPECIES'";
        db.execSQL(sql);
    }

    /** @inheritdoc */
    @Override
    protected void bindValues(SQLiteStatement stmt, Species entity) {
        stmt.clearBindings();

        Long id = entity.getId();
        if (id != null) {
            stmt.bindLong(1, id);
        }

        String name = entity.getName();
        if (name != null) {
            stmt.bindString(2, name);
        }

        Integer track = entity.getTrack();
        if (track != null) {
            stmt.bindLong(3, track);
        }

        Integer count = entity.getCount();
        if (count != null) {
            stmt.bindLong(4, count);
        }
    }

    @Override
    protected void attachEntity(Species entity) {
        super.attachEntity(entity);
        entity.__setDaoSession(daoSession);
    }

    /** @inheritdoc */
    @Override
    public Long readKey(Cursor cursor, int offset) {
        return cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0);
    }

    /** @inheritdoc */
    @Override
    public Species readEntity(Cursor cursor, int offset) {
        Species entity = new Species(
                cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0),
                cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1),
                cursor.isNull(offset + 2) ? null : cursor.getInt(offset + 2),
                cursor.isNull(offset + 3) ? null : cursor.getInt(offset + 3)
        );
        return entity;
    }

    /** @inheritdoc */
    @Override
    public void readEntity(Cursor cursor, Species entity, int offset) {
        entity.setId(cursor.isNull(offset + 0) ? null : cursor.getLong(offset + 0));
        entity.setName(cursor.isNull(offset + 1) ? null : cursor.getString(offset + 1));
        entity.setTrack(cursor.isNull(offset + 2) ? null : cursor.getInt(offset + 2));
        entity.setCount(cursor.isNull(offset + 3) ? null : cursor.getInt(offset + 3));
    }

    /** @inheritdoc */
    @Override
    protected Long updateKeyAfterInsert(Species entity, long rowId) {
        entity.setId(rowId);
        return rowId;
    }

    /** @inheritdoc */
    @Override
    public Long getKey(Species entity) {
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
