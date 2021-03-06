package com.allyfive.geofence2.app;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.allyfive.geofence2.app.GeofenceUtils.REMOVE_TYPE;
import com.allyfive.geofence2.app.GeofenceUtils.REQUEST_TYPE;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.Geofence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * UI handler for the Location Services Geofence sample app.
 * */
 public class MainActivity extends FragmentActivity {
    /*
     * Remember to unregister a geofence when you're finished with it.
     * Otherwise, your app will use up battery. To continue monitoring
     * a geofence indefinitely, set the expiration time to Geofence#NEVER_EXPIRE
     */
    // private static final long GEOFENCE_EXPIRATION_IN_HOURS = 12;
    // private static final long GEOFENCE_EXPIRATION_IN_MILLISECONDS = GEOFENCE_EXPIRATION_IN_HOURS * DateUtils.HOUR_IN_MILLIS;
    // Sets the geofence radius to 10 meters
    private static final long GEOFENCE_RADIUS = 10;
    private static final long GPS_INTERVAL_IN_MINUTES = 1;
    private static final long GPS_INTERVAL = 60000 * GPS_INTERVAL_IN_MINUTES;
    private static final long GPS_MIN_DISTANCE = 10;

    // Store the current request
    private REQUEST_TYPE requestType;
    // Store the current type of removal
    private REMOVE_TYPE removeType;

    // Persistent storage for geofences
    // private SimpleGeofenceStore geofenceSharedPreferences;

    // Store a list of geofences to add
    List<Geofence> geofenceList;
    List<TimedGeofence> timedGeofenceList;

    // Add geofences handler
    private GeofenceAdder geofenceAdder;
    // Remove geofences handler
    private GeofenceRemover geofenceRemover;

    // Handle to fields in the UI
    private TextView myLatitude;
    private TextView myLongitude;
    private EditText myLocationLabel;

    // Internal lightweight geofence object
    // private SimpleGeofence mySimpleGeofence;

    // SQLite helper class
    private MySQLiteHelper myDBHelper;

    /*
     * An instance of an inner class that receives broadcasts from listeners and from the
     * IntentService that receives geofence transition events
     */
    private GeofenceSampleReceiver myBroadcastReceiver;

    // An intent filter for the broadcast receiver
    private IntentFilter myIntentFilter;
    // Store the list of geofences to remove
    private List<String> geofencesToRemove;

    // Timer variables
    private TextView current_duration_value;
    private long startTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;

    private boolean currently_in_geofence;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a new broadcast receiver to receive updates from the listeners and service
        myBroadcastReceiver = new GeofenceSampleReceiver();
        // Create an intent filter for the broadcast receiver
        myIntentFilter = new IntentFilter();
        // Action for broadcast Intents that report successful addition of geofences
        myIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_ADDED);
        myIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCES_REMOVED);
        myIntentFilter.addAction(GeofenceUtils.ACTION_GEOFENCE_ERROR);
        // All Location Services sample apps use this category
        myIntentFilter.addCategory(GeofenceUtils.CATEGORY_LOCATION_SERVICES);

        // Instantiate a new geofence storage area
        //geofenceSharedPreferences = new SimpleGeofenceStore(this);
        myDBHelper = new MySQLiteHelper(this);

        // Instantiate the current List of geofences
        geofenceList = new ArrayList<Geofence>();
        
        // Instantiate a Geofence requester and remover
        geofenceAdder = new GeofenceAdder(this);
        geofenceRemover = new GeofenceRemover(this);

        // Attach to the main UI
        setContentView(R.layout.activity_main);
        // Get handles to the text fields in the UI
        myLatitude = (TextView) findViewById(R.id.current_latitude_value);
        myLongitude = (TextView) findViewById(R.id.current_longitude_value);
        myLocationLabel = (EditText) findViewById(R.id.current_location_label_value);
        TextView myMessage = (TextView) findViewById(R.id.messages_value);
        current_duration_value = (TextView) findViewById(R.id.current_duration_value);


        /* Use the LocationManager class to obtain GPS locations */
        LocationManager myLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        // Location lastKnownLocation = myLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

        LocationListener locListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                //String locationMessage = "My current location is: " + "Latitude = " + location.getLatitude() + "Longitude = " + location.getLongitude();
                //Toast.makeText( getApplicationContext(), locationMessage, Toast.LENGTH_SHORT).show();
                myLatitude.setText(String.valueOf(location.getLatitude()));
                myLongitude.setText(String.valueOf(location.getLongitude()));
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {
            }

            @Override
            public void onProviderEnabled(String s) {
                Toast.makeText( getApplicationContext(),"Gps Enabled", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onProviderDisabled(String s) {
                Toast.makeText( getApplicationContext(), "Gps Disabled", Toast.LENGTH_SHORT ).show();
            }
        };

        // Request the location updates from GPS, time in milliseconds, min-distance in meters
        myLocationManager.requestLocationUpdates( LocationManager.GPS_PROVIDER, GPS_INTERVAL, GPS_MIN_DISTANCE, locListener);

        // SET DEFAULT VALUES FOR TESTING
        myLatitude.setText("111.111");
        myLongitude.setText("222.222");
        // START THE DURATION TIMER
        startDurationTimer();

        currently_in_geofence = false;
    }



    /*
     * Handle results returned to this Activity by other Activities started with
     * startActivityForResult(). In particular, the method onConnectionFailed() in
     * GeofenceRemover and GeofenceAdder may call startResolutionForResult() to
     * start an Activity that handles Google Play services problems. The result of this
     * call returns here, to onActivityResult.
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        // Choose what to do based on the request code
        switch (requestCode) {

            // If the request code matches the code sent in onConnectionFailed
            case GeofenceUtils.CONNECTION_FAILURE_RESOLUTION_REQUEST :

                switch (resultCode) {
                    // If Google Play services resolved the problem
                    case Activity.RESULT_OK:

                        // If the request was to add geofences
                        if (GeofenceUtils.REQUEST_TYPE.ADD == requestType) {

                            // Toggle the request flag and send a new request
                            geofenceAdder.setInProgressFlag(false);

                            // Restart the process of adding the current geofences
                            geofenceAdder.addGeofences(geofenceList);

                            // If the request was to remove geofences
                        } else if (GeofenceUtils.REQUEST_TYPE.REMOVE == requestType ){

                            // Toggle the removal flag and send a new removal request
                            geofenceRemover.setInProgressFlag(false);

                            // If the removal was by Intent
                            if (GeofenceUtils.REMOVE_TYPE.INTENT == removeType) {

                                // Restart the removal of all geofences for the PendingIntent
                                geofenceRemover.removeGeofencesByIntent(
                                        geofenceAdder.getRequestPendingIntent());

                                // If the removal was by a List of geofence IDs
                            } else {

                                // Restart the removal of the geofence list
                                geofenceRemover.removeGeofencesById(geofencesToRemove);
                            }
                        }
                        break;

                    // If any other result was returned by Google Play services
                    default:

                        // Report that Google Play services was unable to resolve the problem.
                        Log.d(GeofenceUtils.APPTAG, getString(R.string.no_resolution));
                }

                // If any other request code was received
            default:
                // Report that this Activity received an unknown requestCode
                Log.d(GeofenceUtils.APPTAG,
                        getString(R.string.unknown_activity_request_code, requestCode));

                break;
        }
    }

    /*
     * Whenever the Activity resumes, reconnect the client to Location
     * Services and reload the last geofences that were set
     */
    @Override
    protected void onResume() {
        super.onResume();
        // Register the broadcast receiver to receive status updates
        LocalBroadcastManager.getInstance(this).registerReceiver(myBroadcastReceiver, myIntentFilter);

        updateTable();
        // possibly use ArrayAdapter.notifyDataSetChanged() instead of calling
        // entire updateTable() function
    }

    // retrieve all geofences from SQLite db, then display them in a ListView
    private void updateTable() {
        Log.d(GeofenceUtils.APPTAG, "about to UpdateTable()");

        timedGeofenceList = myDBHelper.getAllGeofencesFromDB();

        // ArrayAdapter creates a view for each array item by calling toString() on each
        // item and placing the contents in a TextView
        /* Uses the built-in row
         ArrayAdapter<TimedGeofence> adapter = new ArrayAdapter<TimedGeofence>(
         this,android.R.layout.simple_list_item_1, timedGeofenceList );
        */
        // Use our custom adapter
        MyAdapter adapter = new MyAdapter(this, timedGeofenceList);

        ListView listOfGeofences = (ListView) findViewById(R.id.listOfAddedGeofences);

        listOfGeofences.setAdapter(adapter);

        // When you click on an item in the list, do something
        Log.d(GeofenceUtils.APPTAG, "about to listOfGeofences.setOnItemClickListener()");
        listOfGeofences.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
                //Toast.makeText(getApplicationContext(),((TextView) v).getText(), Toast.LENGTH_SHORT).show();

                // call a function to remove the selected list entry
                //ListEntry entry= (ListEntry) parent.getAdapter().getItem(position);
                //String message = entry.getMessage();

            }
        });

    }

    /*
     * Inflate the app menu
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);

        return true;
    }

    /*
     * Respond to menu item selections
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
           /* 
            // Request to clear the geofence2 settings in the UI
            case R.id.menu_item_clear_geofence2:
                myLatitude.setText(GeofenceUtils.EMPTY_STRING);
                myLongitude.setText(GeofenceUtils.EMPTY_STRING);
                //myRadius.setText(GeofenceUtils.EMPTY_STRING);
                return true;

            // Request to clear both geofence settings in the UI
            case R.id.menu_item_clear_geofences:
                myLatitude.setText(GeofenceUtils.EMPTY_STRING);
                myLongitude.setText(GeofenceUtils.EMPTY_STRING);
                myRadius.setText(GeofenceUtils.EMPTY_STRING);
                return true;
            */
            // Remove all geofences from storage
            case R.id.menu_item_clear_geofence_history:
               //geofenceSharedPreferences.clearGeofence("1");
               //geofenceSharedPreferences.clearGeofence("2");
                return true;

            // Pass through any other request
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    /*
     * Save the current geofence settings in SharedPreferences.
     */
    @Override
    protected void onPause() {
        super.onPause();
        // geofenceSharedPreferences.setGeofence("2", mySimpleGeofence);
    }

    /**
     * Verify that Google Play services is available before making a request.
     * @return true if Google Play services is available, otherwise false
     */
    private boolean servicesConnected() {
        // Check that Google Play services is available
        int resultCode =
                GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        // If Google Play services is available
        if (ConnectionResult.SUCCESS == resultCode) {

            // In debug mode, log the status
            Log.d(GeofenceUtils.APPTAG, getString(R.string.play_services_available));

            // Continue
            return true;

            // Google Play services was not available for some reason
        } else {

            // Display an error dialog
            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode, this, 0);
            if (dialog != null) {
                ErrorDialogFragment errorFragment = new ErrorDialogFragment();
                errorFragment.setDialog(dialog);
                errorFragment.show(getSupportFragmentManager(), GeofenceUtils.APPTAG);
            }
            return false;
        }
    }

    /**
     * Called when the user clicks the "Remove geofences" button
     * @param view The view that triggered this callback
     */
    public void RemoveAllGeofences(View view) {
        /*
         * Remove all geofences set by this app. To do this, get the PendingIntent that was added when the geofences were added
         * and use it as an argument to removeGeofences(). The removal happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in the current Activity) when the removal is done
         */

        // Record the type of removal
        removeType = GeofenceUtils.REMOVE_TYPE.INTENT;

        if (!servicesConnected()) {
            return;
        }

        // Try to make a removal request
        try {
        /*
         * Remove the geofences represented by the currently-active PendingIntent. If the
         * PendingIntent was removed for some reason, re-create it; since it's always
         * created with FLAG_UPDATE_CURRENT, an identical PendingIntent is always created.
         */
            geofenceRemover.removeGeofencesByIntent(geofenceAdder.getRequestPendingIntent());

        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error,
                    Toast.LENGTH_LONG).show();
        }

        // attempt to remove all the geofences from the database as well
        myDBHelper.RemoveAllGeofencesFromDB();

        updateTable();
    }


    public void RemoveSingleGeofence(View view) {
        /*
         * Remove the geofence by creating a List of geofences to
         * remove and sending it to Location Services. The List
         * contains the id of the geofence.
         * The removal happens asynchronously; Location Services calls
         * onRemoveGeofencesByPendingIntentResult() (implemented in
         * the current Activity) when the removal is done.
         */

        /*
         * Record the removal as remove by list. If a connection error occurs,
         * the app can automatically restart the removal if Google Play services
         * can fix the error
         */
        removeType = GeofenceUtils.REMOVE_TYPE.LIST;

        // Create a List of 1 Geofence with the ID "2" and store it in the global list
        geofencesToRemove = Collections.singletonList("2");

        /*
         * Check for Google Play services. Do this after
         * setting the request type. If connecting to Google Play services
         * fails, onActivityResult is eventually called, and it needs to
         * know what type of request was in progress.
         */
        if (!servicesConnected()) {
            return;
        }

        // Try to remove the geofence
        try {
            geofenceRemover.removeGeofencesById(geofencesToRemove);

            // Catch errors with the provided geofence IDs
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.remove_geofences_already_requested_error,
                    Toast.LENGTH_LONG).show();
        }

        // remove this geofence from the database as well

        // update the table
        updateTable();
    }

    /**
     * Called when the user clicks the "savelocation" button. Create the PendingIntent containing an Intent that
     * Location Services sends to this app's broadcast receiver when Location Services detects a geofence transition. Send the List
     * and the PendingIntent to Location Services.
     */
    public void SaveLocation(View view) {

        requestType = GeofenceUtils.REQUEST_TYPE.ADD;
        // Check for Google Play services and check input fields
        if (!servicesConnected() || !checkInputFields()) { return; }

        /*
        TODO
        Check to make sure this geofence doesn't overlap with an existing geofence
        Make sure this geofence name doesn't match an existing geofence
        */
        if(currently_in_geofence == true) {
            Toast.makeText(this, "ERROR: Cannot save this Geofence since you are already in a saved Geofence",
                    Toast.LENGTH_SHORT).show();
            updateTable();
            return;
        }

        // Create the Geofence object and add it to the Geofence List to be sent to Location Services
        Log.d(GeofenceUtils.APPTAG, "Creating Geofence object");
        geofenceList.add(
            // Build a new Geofence object based on the values of Lat and Long textViews
            // set the RequestId to the Current Location Label
             new Geofence.Builder()
                    .setRequestId(myLocationLabel.getText().toString())
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setCircularRegion(
                            Double.valueOf(myLatitude.getText().toString()),
                            Double.valueOf(myLongitude.getText().toString()),
                            GEOFENCE_RADIUS)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .build());

        /*
        Log.d(GeofenceUtils.APPTAG, "Creating TimedGeofence object");
        timedGeofenceList.add(
                // Build a new TimedGeofence object
                // set the RequestId to the Current Location Label
                (TimedGeofence) new TimedGeofence.Builder()
                    .setRequestId(myLocationLabel.getText().toString())
                    .setTransitionTypes(TimedGeofence.GEOFENCE_TRANSITION_ENTER | TimedGeofence.GEOFENCE_TRANSITION_EXIT)
                    .setCircularRegion(
                            Double.valueOf(myLatitude.getText().toString()),
                            Double.valueOf(myLongitude.getText().toString()),
                            GEOFENCE_RADIUS)
                    .setExpirationDuration(TimedGeofence.NEVER_EXPIRE)
                    .build());
        */

        // Start the request. Fail if there's already a request in progress
        try {
            // Try to add geofences
            geofenceAdder.addGeofences(geofenceList);
        } catch (UnsupportedOperationException e) {
            // Notify user that previous request hasn't finished.
            Toast.makeText(this, R.string.add_geofences_already_requested_error,
                    Toast.LENGTH_LONG).show();
            return;
        }

        // NOTE database add/remove is handled in handleGeofenceAddRemove()
        Log.d(GeofenceUtils.APPTAG, "Just added geofence "+myLocationLabel.getText().toString() + " to Location Services");

        currently_in_geofence = true;
        updateTable();
    }
    /**
     * Check all the input values and flag those that are incorrect
     * @return true if all the widget values are correct; otherwise false
     */
    private boolean checkInputFields() {
        // Start with the input validity flag set to true
        boolean inputOK = true;
        // LocationLabel cannot be empty
        if (TextUtils.isEmpty(myLocationLabel.getText())) {
            myLocationLabel.setBackgroundColor(Color.RED);
            Toast.makeText(this, R.string.geofence_input_error_missing, Toast.LENGTH_LONG).show();
            // Set the validity to "invalid" (false)
            inputOK = false;
        } else {
            // myLocationLabel.setBackgroundColor(Color.BLACK);
        }

        /*
         * If all the input fields have been entered, test to ensure that their values are within
         * the acceptable range. The tests can't be performed until it's confirmed that there are
         * actual values in the fields.
         */
        // if (inputOK) {
            /*
             * Get values from the latitude, longitude, and radius fields.
             */
            // double lat2 = Double.valueOf(myLatitude.getText().toString());
            // double lng2 = Double.valueOf(myLongitude.getText().toString());
            //float rd2 = Float.valueOf(myRadius.getText().toString());

            /*
             * Test latitude and longitude for minimum and maximum values. Highlight incorrect
             * values and set a Toast in the UI.
             */

            /*if (lat2 > GeofenceUtils.MAX_LATITUDE || lat2 < GeofenceUtils.MIN_LATITUDE) {
                myLatitude.setBackgroundColor(Color.RED);
                Toast.makeText(
                        this,
                        R.string.geofence_input_error_latitude_invalid,
                        Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                myLatitude.setBackgroundColor(Color.BLACK);
            }

            if ((lng2 > GeofenceUtils.MAX_LONGITUDE) || (lng2 < GeofenceUtils.MIN_LONGITUDE)) {
                myLongitude.setBackgroundColor(Color.RED);
                Toast.makeText(
                        this,
                        R.string.geofence_input_error_longitude_invalid,
                        Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                myLongitude.setBackgroundColor(Color.BLACK);
            }
            */
            /*
            if (rd2 < GeofenceUtils.MIN_RADIUS) {
                myRadius.setBackgroundColor(Color.RED);
                Toast.makeText(
                        this,
                        R.string.geofence_input_error_radius_invalid,
                        Toast.LENGTH_LONG).show();

                // Set the validity to "invalid" (false)
                inputOK = false;
            } else {

                myRadius.setBackgroundColor(Color.BLACK);
            }*/
        // }

        // If everything passes, the validity flag will still be true, otherwise it will be false.
        return inputOK;
    }



    // Start the duration timer in the main activity screen
    public void startDurationTimer()
    {
        Log.d(GeofenceUtils.APPTAG, "Starting current duration timer");

        startTime = SystemClock.uptimeMillis();
        // The customHandler will run the process updateTimerThread on a different thread
        customHandler.postDelayed(updateTimerThread, 0);
    }

    private Runnable updateTimerThread = new Runnable() {
        public void run() {
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;

            int secs = (int) (timeInMilliseconds / 1000);
            int mins = secs / 60;
            secs = secs % 60;

            current_duration_value.setText("" + mins + ":" + String.format("%02d", secs));
            customHandler.postDelayed(this, 0);
        }
    };

    /**
     * Define a Broadcast receiver that receives updates from connection listeners and
     * the geofence transition service.
     */
    public class GeofenceSampleReceiver extends BroadcastReceiver {
        /*
         * Define the required method for broadcast receivers
         * This method is invoked when a broadcast Intent triggers the receiver
         */
        @Override
        public void onReceive(Context context, Intent intent) {

            // Check the action code and determine what to do
            String action = intent.getAction();

            // Intent contains information about errors in adding or removing geofences
            if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_ERROR)) {
                handleGeofenceError(context, intent);

                // Intent contains information about successful addition or removal of geofences
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_ADDED)) {
                handleGeofenceAddRemove(context, intent);
                Toast.makeText(context, "Location Services: Geofence has been added!", Toast.LENGTH_SHORT).show();

            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCES_REMOVED)) {
                Toast.makeText(context, "Location Services: Geofences have been removed!", Toast.LENGTH_SHORT).show();

                // Intent contains information about a geofence transition
            } else if (TextUtils.equals(action, GeofenceUtils.ACTION_GEOFENCE_TRANSITION)) {
                handleGeofenceTransition(context, intent);

                // The Intent contained an invalid action
            } else {
                Log.e(GeofenceUtils.APPTAG, getString(R.string.invalid_action_detail, action));
                Toast.makeText(context, R.string.invalid_action, Toast.LENGTH_LONG).show();
            }
        }

        /**
         * If you want to perform actions AFTER adding or removing geofences, put them here.
         * @param context A Context for this component
         * @param intent The received broadcast Intent
         */
        private void handleGeofenceAddRemove(Context context, Intent intent) {
            Log.d(GeofenceUtils.APPTAG, "Entered handleGeofenceAddRemove()");

            if(TextUtils.equals(intent.getAction(), "ADD")) {

                // insert to database (label, latitude, longitude, totaltime)
                Log.d(GeofenceUtils.APPTAG, "Calling insertGeofenceToDB()");
                myDBHelper.insertGeofenceToDB(
                        myLocationLabel.getText().toString(),
                        Double.valueOf(myLatitude.getText().toString()),
                        Double.valueOf(myLongitude.getText().toString()),
                        0);

            } else if (TextUtils.equals(intent.getAction(),"REMOVE")) {
                // remove geofence information from database
            }
        }

        /**
         * Report geofence transitions to the UI
         * @param context A Context for this component
         * @param intent The Intent containing the transition
         */
        private void handleGeofenceTransition(Context context, Intent intent) {
            Log.d(GeofenceUtils.APPTAG,"About to handle a geofence transition with handleGeofenceTransition()");
            String transitionMessage = "A Geofence transition has occurred: " + intent.getAction();

            //Toast.makeText( context, transitionMessage, Toast.LENGTH_SHORT).show();
            Log.d(GeofenceUtils.APPTAG, transitionMessage);
        }

        /**
         * Report addition or removal errors to the UI, using a Toast
         * @param intent A broadcast Intent sent by ReceiveTransitionsIntentService
         */
        private void handleGeofenceError(Context context, Intent intent) {
            String msg = intent.getStringExtra(GeofenceUtils.EXTRA_GEOFENCE_STATUS);
            Log.e(GeofenceUtils.APPTAG, msg);
            Toast.makeText(context, msg, Toast.LENGTH_LONG).show();
        }
    }
    /**
     * Define a DialogFragment to display the error dialog generated in
     * showErrorDialog.
     */
    public static class ErrorDialogFragment extends DialogFragment {

        // Global field to contain the error dialog
        private Dialog mDialog;

        /**
         * Default constructor. Sets the dialog field to null
         */
        public ErrorDialogFragment() {
            super();
            mDialog = null;
        }

        /**
         * Set the dialog to display
         *
         * @param dialog An error dialog
         */
        public void setDialog(Dialog dialog) {
            mDialog = dialog;
        }

        /*
         * This method must return a Dialog to the DialogFragment.
         */
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            return mDialog;
        }
    }


}