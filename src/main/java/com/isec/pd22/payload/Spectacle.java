package com.isec.pd22.payload;

import java.io.Serializable;
import java.util.Date;

public class Spectacle implements Serializable {
    String designation;
    String type;
    Date data;
    Date time;
    int duration;
    String location;
    String town;
    String country;
    int ageRating;
    //TODO falta modelar os lugares


    public Spectacle(String designation, String type, Date data, Date time, int duration, String location, String town, String country, int ageRating) {
        this.designation = designation;
        this.type = type;
        this.data = data;
        this.time = time;
        this.duration = duration;
        this.location = location;
        this.town = town;
        this.country = country;
        this.ageRating = ageRating;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Date getData() {
        return data;
    }

    public void setData(Date data) {
        this.data = data;
    }

    public Date getTime() {
        return time;
    }

    public void setTime(Date time) {
        this.time = time;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getTown() {
        return town;
    }

    public void setTown(String town) {
        this.town = town;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public int getAgeRating() {
        return ageRating;
    }

    public void setAgeRating(int ageRating) {
        this.ageRating = ageRating;
    }

}
