package com.forestwave.pdc8g1.forestwave.Model;

import android.database.sqlite.SQLiteDatabase;

import java.util.Map;

import de.greenrobot.dao.AbstractDao;
import de.greenrobot.dao.AbstractDaoSession;
import de.greenrobot.dao.identityscope.IdentityScopeType;
import de.greenrobot.dao.internal.DaoConfig;

/**
 * Created by leo on 12/01/15.
 */
public class DaoSession extends AbstractDaoSession {

    private final DaoConfig treeDaoConfig;

    private final TreeDao treeDao;

    public DaoSession(SQLiteDatabase db, IdentityScopeType type, Map<Class<? extends AbstractDao<?, ?>>, DaoConfig>
            daoConfigMap) {
        super(db);

        treeDaoConfig = daoConfigMap.get(TreeDao.class).clone();
        treeDaoConfig.initIdentityScope(type);

        treeDao = new TreeDao(treeDaoConfig, this);

        registerDao(Tree.class, treeDao);
    }

    public void clear() {
        treeDaoConfig.getIdentityScope().clear();
    }

    public TreeDao getTreeDao() {
        return treeDao;
    }
}
