package com.allyfive.geofence2.app;

import com.google.android.gms.location.Geofence;

public class TimedGeofence implements Geofence {

    private String label;
    private double latitude;
    private double longitude;
    private int totaltime;

    public TimedGeofence (String label, double latitude, double longitude, int totaltime) {
        this.label = label;
        this.latitude = latitude;
        this.longitude = longitude;
        this.totaltime = totaltime;
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
    // because ArrayAdapter calls toString on each item and places the contents in a TextView
    // this is only necessary if using the ArrayAdapter for the ListView instead of a custom adapter
    @Override
    public String toString() {
        return label;
    }





}
