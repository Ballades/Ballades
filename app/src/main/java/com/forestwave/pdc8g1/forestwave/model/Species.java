package com.forestwave.pdc8g1.forestwave.model;

public class Species {

    private Long id;
    private String name;
    private Integer track;
    private Integer count;

    public Species() {
    }

    public Species(Long id) {
        this.id = id;
    }

    public Species(Long id, String name, Integer track, Integer count) {
        this.id = id;
        this.name = name;
        this.track = track;
        this.count = count;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTrack() {
        return track;
    }

    public void setTrack(Integer track) {
        this.track = track;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }
}
