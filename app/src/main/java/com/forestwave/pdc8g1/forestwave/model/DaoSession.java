package com.forestwave.pdc8g1.forestwave.model;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

public class DaoSession extends AbstractDaoSession {

    private final DaoConfig treeDaoConfig;
    private final DaoConfig speciesDaoConfig;

    private final TreeDao treeDao;
    private final SpeciesDao speciesDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        treeDaoConfig = daoConfigMap.get(TreeDao.class).clone();
        treeDaoConfig.initIdentityScope(type);

        treeDao = new TreeDao(treeDaoConfig, this);

        registerDao(Tree.class, treeDao);

        speciesDaoConfig = daoConfigMap.get(SpeciesDao.class).clone();
        speciesDaoConfig.initIdentityScope(type);

        speciesDao = new SpeciesDao(speciesDaoConfig, this);

        registerDao(Tree.class, treeDao);
    }

    public void clear() {
        treeDaoConfig.getIdentityScope().clear();
        speciesDaoConfig.getIdentityScope().clear();
    }

    public TreeDao getTreeDao() {
        return treeDao;
    }

    public SpeciesDao getSpeciesDao() {
        return speciesDao;
    }
}
