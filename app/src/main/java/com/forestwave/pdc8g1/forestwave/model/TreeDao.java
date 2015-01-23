package com.forestwave.pdc8g1.forestwave.model;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.Property;
import de.greenrobot.dao.internal.DaoConfig;
import de.greenrobot.dao.internal.SqlUtils;
import de.greenrobot.dao.query.Query;
import de.greenrobot.dao.query.QueryBuilder;

/**
 * Created by leo on 12/01/15.
 */
public class TreeDao extends AbstractDao<Tree, Long>{

    public static final String TABLENAME = "Tree";
    private Query<Tree> species_TreesQuery;
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

    private DaoSession daoSession;

    public TreeDao(DaoConfig config) {
        super(config);
    }

    public TreeDao(DaoConfig config, DaoSession daoSession) {
        super(config, daoSession);
        this.daoSession = daoSession;
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

        stmt.bindLong(2, entity.getSpeciesId());

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

    @Override
    protected void attachEntity(Tree entity) {
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

    /** Internal query to resolve the "trees" to-many relationship of Species. */
    public List<Tree> _querySpecies_Trees(long speciesId) {
        synchronized (this) {
            if (species_TreesQuery == null) {
                QueryBuilder<Tree> queryBuilder = queryBuilder();
                queryBuilder.where(Properties.SpeciesId.eq(null));
                queryBuilder.orderRaw("DATE ASC");
                species_TreesQuery = queryBuilder.build();
            }
        }
        Query<Tree> query = species_TreesQuery.forCurrentThread();
        query.setParameter(0, speciesId);
        return query.list();
    }
    private String selectDeep;
    protected String getSelectDeep() {
        if (selectDeep == null) {
            StringBuilder builder = new StringBuilder("SELECT ");
            SqlUtils.appendColumns(builder, "T", getAllColumns());
            builder.append(',');
            SqlUtils.appendColumns(builder, "T0", daoSession.getSpeciesDao().getAllColumns());
            builder.append(" FROM TREE T");
            builder.append(" LEFT JOIN SPECIES T0 ON T.'SPECIES_ID'=T0.'_id'");
            builder.append(' ');
            selectDeep = builder.toString();
        }
        return selectDeep;
    }
    protected Tree loadCurrentDeep(Cursor cursor, boolean lock) {
        Tree entity = loadCurrent(cursor, 0, lock);
        int offset = getAllColumns().length;
        Species species = loadCurrentOther(daoSession.getSpeciesDao(), cursor, offset);
        if(species != null) {
            entity.setSpecies(species);
        }
        return entity;
    }
    public Tree loadDeep(Long key) {
        assertSinglePk();
        if (key == null) {
            return null;
        }
        StringBuilder builder = new StringBuilder(getSelectDeep());
        builder.append("WHERE ");
        SqlUtils.appendColumnsEqValue(builder, "T", getPkColumns());
        String sql = builder.toString();
        String[] keyArray = new String[] { key.toString() };
        Cursor cursor = db.rawQuery(sql, keyArray);
        try {
            boolean available = cursor.moveToFirst();
            if (!available) {
                return null;
            } else if (!cursor.isLast()) {
                throw new IllegalStateException("Expected unique result, but count was " + cursor.getCount());
            }
            return loadCurrentDeep(cursor, true);
        } finally {
            cursor.close();
        }
    }
    /** Reads all available rows from the given cursor and returns a list of new ImageTO objects. */
    public List<Tree> loadAllDeepFromCursor(Cursor cursor) {
        int count = cursor.getCount();
        List<Tree> list = new ArrayList<Tree>(count);
        if (cursor.moveToFirst()) {
            if (identityScope != null) {
                identityScope.lock();
                identityScope.reserveRoom(count);
            }
            try {
                do {
                    list.add(loadCurrentDeep(cursor, false));
                } while (cursor.moveToNext());
            } finally {
                if (identityScope != null) {
                    identityScope.unlock();
                }
            }
        }
        return list;
    }
    protected List<Tree> loadDeepAllAndCloseCursor(Cursor cursor) {
        try {
            return loadAllDeepFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }
    /** A raw-style query where you can pass any WHERE clause and arguments. */
    public List<Tree> queryDeep(String where, String... selectionArg) {
        Cursor cursor = db.rawQuery(getSelectDeep() + where, selectionArg);
        return loadDeepAllAndCloseCursor(cursor);
    }
}
