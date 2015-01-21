package com.forestwave.pdc8g1.forestwave.model;

/**
 * Created by leo on 12/01/15.
 */
public class Tree {

    private Long id;
    private Species species;
    private java.lang.Integer height;
    private Double latitude;
    private Double longitude;

    public Tree() {
    }

    public Tree(Long id) {
        this.id = id;
    }

    public Tree(Long id, Species species, java.lang.Integer height, double latitude, double longitude) {
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

    public Species getSpecies() {
        return species;
    }

    public void setSpecies(Species species) {
        this.species = species;
    }

    public java.lang.Integer getHeight() {
        return height;
    }

    public void setHeight(java.lang.Integer height) {
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

    public Double[] getLocation() {
        return new Double[]{latitude, longitude};
    }
}
