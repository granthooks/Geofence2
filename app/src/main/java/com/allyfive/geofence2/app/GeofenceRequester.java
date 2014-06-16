package com.allyfive.geofence2.app;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.common.GooglePlayServicesClient.ConnectionCallbacks;
import com.google.android.gms.common.GooglePlayServicesClient.OnConnectionFailedListener;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationStatusCodes;
import com.google.android.gms.location.LocationClient.OnAddGeofencesResultListener;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Class for connecting to Location Services and requesting geofences.
 * 
 * Note: Clients must ensure that Google Play services is available before requesting geofences.
 *  Use GooglePlayServicesUtil.isGooglePlayServicesAvailable() to check.
 *
 * To use a GeofenceRequester, instantiate it and call AddGeofence(). Everything else is done
 * automatically.
 */
public class GeofenceRequester implements OnAddGeofencesResultListener, ConnectionCallbacks,
        OnConnectionFailedListener {

    // Storage for a reference to the calling client
    private final Activity myActivity;

    // Stores the PendingIntent used to send geofence transitions back to the app
    private PendingIntent myGeofencePendingIntent;

    // Stores the current list of geofences
    private ArrayList<Geofence> listOfGeofences;

    // Stores the current instantiation of the location client
    private LocationClient myLocationClient;

    /*
     * Flag that indicates whether an add or remove request is underway. Check this
     * flag before attempting to start a new request.
     */
    private boolean inProgress;

    public GeofenceRequester(Activity activityContext) {
        // Save the context
        myActivity = activityContext;

        // Initialize the globals to null
        myGeofencePendingIntent = null;
        myLocationClient = null;
        inProgress = false;
    }

    /**
     * Set the "in progress" flag from a caller. This allows callers to re-set a
     * request that failed but was later fixed.
     * @param flag Turn the in progress flag on or off.
     */
    public void setInProgressFlag(boolean flag) {
        // Set the "In Progress" flag.
        inProgress = flag;
    }

    /**
     * Get the current in progress status.
     * @return The current value of the in progress flag.
     */
    public boolean getInProgressFlag() {
        return inProgress;
    }

    /**
     * Returns the current PendingIntent to the caller.
     * @return The PendingIntent used to create the current set of geofences
     */
    public PendingIntent getRequestPendingIntent() {
        return createRequestPendingIntent();
    }

    /**
     * Start adding geofences. Save the geofences, then start adding them by requesting a
     * connection
     * @param geofences A List of one or more geofences to add
     */
    public void addGeofences(List<Geofence> geofences) throws UnsupportedOperationException {
        /*
         * Save the geofences so that they can be sent to Location Services once the
         * connection is available.
         */
        listOfGeofences = (ArrayList<Geofence>) geofences;

        // If a request is not already in progress
        if (!inProgress) {
            // Toggle the flag and continue
            inProgress = true;
            // Request a connection to Location Services
            requestConnection();
            // If a request is in progress
        } else {
            // Throw an exception and stop the request
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Request a connection to Location Services. This call returns immediately,
     * but the request is not complete until onConnected() or onConnectionFailure() is called.
     */
    private void requestConnection() {
        getLocationClient().connect();
    }
    /**
     * Get the current location client, or create a new one if necessary.
     * @return A LocationClient object
     */
    private GooglePlayServicesClient getLocationClient() {
        if (myLocationClient == null) {
            myLocationClient = new LocationClient(myActivity, this, this);
        }
        return myLocationClient;
    }
    /**
     * Once the connection is available, send a request to add the Geofences
     */
    private void continueAddGeofences() {
        // Get a PendingIntent that Location Services issues when a geofence transition occurs
        myGeofencePendingIntent = createRequestPendingIntent();

        // Send a request to add the current geofences
        myLocationClient.addGeofences(listOfGeofences, myGeofencePendingIntent, this);
    }

    /*
     * Handle the result of adding the geofences
     */
    @Override
    public void onAddGeofencesResult(int statusCode, String[] geofenceRequestIds) {
        // Create a broadcast Intent that notifies other components of success or failure
        Intent broadcastIntent = new Intent();

        // Temp storage for messages
        String msg;

        // If adding the geocodes was successful
        if (LocationStatusCodes.SUCCESS == statusCode) {

            // Create a message containing all the geofence IDs added.
            msg = myActivity.getString(R.string.add_geofences_result_success,
                    Arrays.toString(geofenceRequestIds));

            // In debug mode, log the result
            Log.d(GeofenceUtils.APPTAG, msg);
            // Create an Intent to broadcast to the app
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCES_ADDED)
                    .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
            // If adding the geofences failed
        } else {
            /*
             * Create a message containing the error code and the list
             * of geofence IDs you tried to add
             */
            msg = myActivity.getString(
                    R.string.add_geofences_result_failure,
                    statusCode,
                    Arrays.toString(geofenceRequestIds)
            );

            // Log an error
            Log.e(GeofenceUtils.APPTAG, msg);
            // Create an Intent to broadcast to the app
            broadcastIntent.setAction(GeofenceUtils.ACTION_GEOFENCE_ERROR)
                    .addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS, msg);
        }

        // Broadcast whichever result occurred
        LocalBroadcastManager.getInstance(myActivity).sendBroadcast(broadcastIntent);

        // Disconnect the location client
        requestDisconnection();
    }

    /**
     * Get a location client and disconnect from Location Services
     */
    private void requestDisconnection() {
        // A request is no longer in progress
        inProgress = false;
        getLocationClient().disconnect();
    }

    /*
     * Called by Location Services once the location client is connected.
     * Continue by adding the requested geofences.
     */
    @Override
    public void onConnected(Bundle arg0) {
        // If debugging, log the connection
        Log.d(GeofenceUtils.APPTAG, myActivity.getString(R.string.connected));
        // Continue adding the geofences
        continueAddGeofences();
    }

    /*
     * Called by Location Services once the location client is disconnected.
     */
    @Override
    public void onDisconnected() {
        // Turn off the request flag
        inProgress = false;

        // In debug mode, log the disconnection
        Log.d(GeofenceUtils.APPTAG, myActivity.getString(R.string.disconnected));
        // Destroy the current location client
        myLocationClient = null;
    }

    /**
     * Get a PendingIntent to send with the request to add Geofences. Location Services issues
     * the Intent inside this PendingIntent whenever a geofence transition occurs for the current
     * list of geofences.
     *
     * @return A PendingIntent for the IntentService that handles geofence transitions.
     */
    private PendingIntent createRequestPendingIntent() {

        // If the PendingIntent already exists
        if (null != myGeofencePendingIntent) {
            // Return the existing intent
            return myGeofencePendingIntent;

            // If no PendingIntent exists
        } else {
            // Create an Intent pointing to the IntentService
            Intent intent = new Intent(myActivity, ReceiveTransitionsIntentService.class);
            /*
             * Return a PendingIntent to start the IntentService.
             * Always create a PendingIntent sent to Location Services
             * with FLAG_UPDATE_CURRENT, so that sending the PendingIntent
             * again updates the original. Otherwise, Location Services
             * can't match the PendingIntent to requests made with it.
             */
            return PendingIntent.getService(
                    myActivity,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT);
        }
    }

    /*
     * Implementation of OnConnectionFailedListener.onConnectionFailed
     * If a connection or disconnection request fails, report the error
     * connectionResult is passed in from Location Services
     */
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // Turn off the request flag
        inProgress = false;

        /*
         * Google Play services can resolve some errors it detects.
         * If the error has a resolution, try sending an Intent to
         * start a Google Play services activity that can resolve
         * error.
         */
        if (connectionResult.hasResolution()) {
            try {
                // Start an Activity that tries to resolve the error
                connectionResult.startResolutionForResult(myActivity,
                        GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST);
            /*
             * Thrown if Google Play services canceled the original
             * PendingIntent
             */
            } catch (SendIntentException e) {
                // Log the error
                e.printStackTrace();
            }

        /*
         * If no resolution is available, put the error code in
         * an error Intent and broadcast it back to the main Activity.
         * The Activity then displays an error dialog.
         */
        } else {
            Intent errorBroadcastIntent = new Intent(GeofenceUtils.ACTION_CONNECTION_ERROR);
            errorBroadcastIntent.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES)
                    .putExtra(GeofenceUtils.EXTRA_CONNECTION_ERROR_CODE,
                            connectionResult.getErrorCode());
            LocalBroadcastManager.getInstance(myActivity).sendBroadcast(errorBroadcastIntent);
        }
    }
}
