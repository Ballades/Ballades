package com.forestwave.pdc8g1.forestwave.model;

import android.location.Location;

import java.util.ArrayList;

/**
 * Created by nicorr on 21/01/15.
 */
public class InfosTrees {

    private Integer count;
    private Double score;
    private Double[] location;

    public InfosTrees(Integer count, Double score, Double[] location) {
        this.count = count;
        this.score = score;
        this.location = location;
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
}
