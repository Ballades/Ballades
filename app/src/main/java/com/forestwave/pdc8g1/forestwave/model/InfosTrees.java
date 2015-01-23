package com.forestwave.pdc8g1.forestwave.model;

import android.location.Location;

import java.util.ArrayList;

/**
 * Cette classe a pour objectif de stocker des informations de traitement pour le compositionEngine, de manière pratique
 */
public class InfosTrees {

    private Integer count;
    private Double score;
    private Double[] location;
    private Double volume;

    public InfosTrees(Integer count, Double score, Double[] location, Double volume) {
        this.count = count;
        this.score = score;
        this.location = location;
        this.volume = volume;
    }

    public Integer getCount() {
        if (count == null) {
            count = 0;
        }
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public Double getScore() {
        if (score == null) {
            score = 0.0;
        }
        return score;
    }

    public void setScore(Double score) {
        this.score = score;
    }

    public Double[] getLocation() {
        if (location == null) {
            location = new Double[]{0.0, 0.0};
        }
        return location;
    }

    public void setLocation(Double[] location) {
        this.location = location;
    }

    public Double getVolume() {
        if (volume == null) {
            volume = 0.0;
        }
        return volume;
    }

    public void setVolume(Double volume) {
        this.volume = volume;
    }
}
