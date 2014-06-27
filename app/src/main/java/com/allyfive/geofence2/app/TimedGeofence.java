package com.allyfive.geofence2.app;

import com.google.android.gms.location.Geofence;

public class TimedGeofence implements Geofence {

    private String label;
    private double latitude;
    private double longitude;
    private int totaltime;

    public TimedGeofence () {
        label = null;
        latitude = 0;
        longitude = 0;
        totaltime = 0;
    }

    public void setLabel(String labelname) {
        label = labelname;
    }

    public void setLatitude(double newlatitude) {
        latitude = newlatitude;
    }

    public void setLongitude(double newlongitude) {
        longitude = newlongitude;
    }

    public void setTotalTime(int newtime) {
        totaltime = newtime;
    }

    @Override
    public String getRequestId() {
        return label;
    }

    public String getLabel() {
        return label;
    }

    public Double getLatitude() {
        return latitude;
    }
    public Double getLongitude() {
        return longitude;
    }

    public int getTotalTime() {
        return totaltime;
    }

    // added this to control what is displayed in the ListView
    @Override
    public String toString() {
        return label;
    }





}
