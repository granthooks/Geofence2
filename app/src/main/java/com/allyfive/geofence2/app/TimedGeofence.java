package com.allyfive.geofence2.app;

import com.google.android.gms.location.Geofence;

public class TimedGeofence implements Geofence {

    private String label;
    private int totaltime;

    public TimedGeofence () {
        label = null;
        totaltime = 0;
    }

    public void setLabel(String labelname) {
        label = labelname;
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

    public int getTotalTime() {
        return totaltime;
    }






}
