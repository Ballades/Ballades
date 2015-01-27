package com.forestwave.pdc8g1.forestwave.model;

import android.location.Location;

import de.greenrobot.dao.DaoException;


public class Tree {

    private Long id;
    private long speciesId;
    private Integer height;
    private Double latitude;
    private Double longitude;

    private transient DaoSession daoSession;

    private transient TreeDao myDao;
    private Species species;
    private Long species__resolvedKey;

    public Tree() {
    }

    public Tree(Long id) {
        this.id = id;
    }

    public Tree(Long id, long speciesId, Integer height, double latitude, double longitude) {
        this.id = id;
        this.speciesId = speciesId;
        this.height = height;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Tree(Long id, Species species, Integer height, double latitude, double longitude) {
        this.id = id;
        this.setSpecies(species);
        this.height = height;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    /** called by internal mechanisms, do not call yourself. */
    public void __setDaoSession(DaoSession daoSession) {
        this.daoSession = daoSession;
        myDao = daoSession != null ? daoSession.getTreeDao() : null;
    }

    public Species getSpecies() {
        long __key = this.speciesId;
        if (species__resolvedKey == null || !species__resolvedKey.equals(__key)) {
            if (daoSession == null) {
                throw new DaoException("Entity is detached from DAO context");
            }
            SpeciesDao targetDao = daoSession.getSpeciesDao();
            Species speciesNew = targetDao.load(__key);
            synchronized (this) {
                species = speciesNew;
                species__resolvedKey = __key;
            }
        }
        return species;
    }

    public void setSpecies(Species species) {
        if (species == null) {
            throw new DaoException("To-one property 'speciesId' has not-null constraint; cannot set to-one to null");
        }
        synchronized (this) {
            this.species = species;
            speciesId = species.getId();
            species__resolvedKey = speciesId;
        }
    }


    public void setId(Long id) {
        this.id = id;
    }

    public long getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(long speciesId) {
        this.speciesId = speciesId;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    /**
     * Retourne la distance en m au point donné
     */
    public float getDistance(Double latitude, Double longitude) {

        float[] results = new float[3];
        Location.distanceBetween(latitude, longitude, this.latitude, this.longitude, results);
        return results[0];
    }

    public Double[] getLocation() {
        Double[] location = {latitude, longitude};
        return location;
    }
}
