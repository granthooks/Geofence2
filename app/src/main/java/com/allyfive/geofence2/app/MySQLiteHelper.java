package com.allyfive.geofence2.app;

import android.content.ContentValues;
import android.content.Context;
import android.content.IntentSender;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.location.Geofence;

import java.util.LinkedList;
import java.util.List;

class MySQLiteHelper extends SQLiteOpenHelper {

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "geofencedb";
    private static final String GEOFENCE_TABLE = "geofencetable";

    // Table Columns names
    //private static final String KEY_ID = "_id";
    private static final String KEY_LABEL = "label";
    private static final String KEY_LATITUDE = "latitude";
    private static final String KEY_LONGITUDE = "longitude";
    private static final String KEY_TOTALTIME = "totaltime";

    //private static final String[] COLUMNS = {KEY_ID,KEY_LABEL,KEY_LATITUDE,KEY_LONGITUDE,KEY_TOTALTIME};

    public MySQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {

        String CREATE_GEOFENCE_TABLE = "CREATE TABLE " + GEOFENCE_TABLE + " ( " +
                "_id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "label TEXT, "+
                "latitude DOUBLE, "+
                "longitude DOUBLE," +
                "totaltime INT )";

        db.execSQL(CREATE_GEOFENCE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older geofences if they exist
        db.execSQL("DROP TABLE IF EXISTS "+ GEOFENCE_TABLE);

        // create fresh geofences table
        this.onCreate(db);
    }

    public void insertGeofenceToDB(String label, double latitude, double longitude, int totaltime){

        Log.d(GeofenceUtils.APPTAG, "Inserting geofence named: "+label);

        // 1. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        // 2. create ContentValues to add key "column"/value
        ContentValues values = new ContentValues();
        values.put(KEY_LABEL, label); // name of the geofence
        values.put(KEY_LATITUDE, latitude);
        values.put(KEY_LONGITUDE, longitude);
        values.put(KEY_TOTALTIME, totaltime);

        try {
            db.insert(GEOFENCE_TABLE, null, values); // key/value -> keys = column names/ values = column values
        } catch(SQLException e) {
            // Log the error
            e.printStackTrace();
        }
        db.close();

    }

    public List<Geofence> getAllGeofencesFromDB() {
        List<Geofence> geofences = new LinkedList<Geofence>();
        Cursor cursor = null;

        // 1. build the query
        String query = "SELECT * FROM " + GEOFENCE_TABLE;

        // 2. get reference to writable DB
        SQLiteDatabase db = this.getWritableDatabase();

        try {
            cursor = db.rawQuery(query, null);
        } catch(SQLException e) {
            e.printStackTrace();
        }

        // 3. build a geofence from each row, then add it to list
        Geofence geofence;
        if (cursor.moveToFirst()) {
            do {
                geofence = new Geofence() {
                    @Override
                    public String getRequestId() {
                        return null;
                    }
                };
                //book.setId(Integer.parseInt(cursor.getString(0)));
                //book.setTitle(cursor.getString(1));
                //book.setAuthor(cursor.getString(2));

                // Add book to books
                geofences.add(geofence);
            } while (cursor.moveToNext());
        }

        Log.d(GeofenceUtils.APPTAG, "Retrieved the following geofences: "+ geofences.toString());

        return geofences;
    }
}