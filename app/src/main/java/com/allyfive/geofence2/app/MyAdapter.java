package com.allyfive.geofence2.app;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

class MyAdapter extends ArrayAdapter<TimedGeofence> {

    public MyAdapter(Context context, List<TimedGeofence> values){
        super(context, R.layout.location_row, values);
    }

    // Override getView which is responsible for creating the rows for our list
    // position represents the index we are in for the array.

    // convertView is a reference to the previous view that is available for reuse. As
    // the user scrolls the information is populated as needed to conserve memory.

    // A ViewGroup are invisible containers that hold a bunch of views and
    // define their layout properties.
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        // The LayoutInflator puts a layout into the right View
        LayoutInflater theInflater = LayoutInflater.from(getContext());

        // inflate takes the resource to load, the parent that the resource may be
        // loaded into and true or false if we are loading into a parent view.
        View theView = theInflater.inflate(R.layout.location_row, parent, false);

        // Set the Icon
        // Get the ImageView in the layout
        ImageView theImageView = (ImageView) theView.findViewById(R.id.icon);
        // We can set a ImageView like this
        theImageView.setImageResource(R.drawable.ic_launcher);

        // Set the Label
        // We retrieve the text from the array
        String label = getItem(position).getLabel();
        // Get the TextView we want to edit
        TextView theLocationLabel = (TextView) theView.findViewById(R.id.location_label);
        // Put the next TV Show into the TextView
        theLocationLabel.setText(label);

        // Set the total time
        int totaltime = getItem(position).getTotalTime();
        TextView theTotalTime = (TextView) theView.findViewById(R.id.totaltime);
        theTotalTime.setText(Integer.toString(totaltime));

        return theView;

    }
}