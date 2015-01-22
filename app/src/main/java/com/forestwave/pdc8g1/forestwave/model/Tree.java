package com.forestwave.pdc8g1.forestwave.model;

import android.location.Location;
import android.util.Log;

import java.lang.reflect.Array;

/**
 * Created by leo on 12/01/15.
 */
public class Tree {

    private Long id;
    private String species;
    private Integer height;
    private Double latitude;
    private Double longitude;

    public Tree() {
    }

    public Tree(Long id) {
        this.id = id;
    }

    public Tree(Long id, String species, Integer height, double latitude, double longitude) {
        this.id = id;
        this.species = species;
        this.height = height;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSpecies() {
        return species;
    }

    public void setSpecies(String species) {
        this.species = species;
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

    public float computeUserRange(Double latitude, Double longitude) {

        float[] results = new float[3];
        Location.distanceBetween(latitude, longitude, this.latitude, this.longitude, results);
        return results[0];
    }
}
