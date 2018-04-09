package com.app.androidkt.geofencing;

import com.google.android.gms.maps.model.LatLng;

import java.util.HashMap;

/**
 * Created by brijesh on 15/4/17.
 */

public class Constants {


    public static final String FIRST_PLACE_ID = "UBLE";
    public static final String SECOND_PLACE_ID = "FAC_CONTA";
    public static final float GEOFENCE_RADIUS_IN_METERS = 150;

    /**
     * Map for storing information about stanford university in the Stanford.
     */
    public static final HashMap<String, LatLng> AREA_LANDMARKS = new HashMap<String, LatLng>();

    static {
        // youre location or locations.
        AREA_LANDMARKS.put(FIRST_PLACE_ID, new LatLng(19.324218, -99.184614));
        AREA_LANDMARKS.put(SECOND_PLACE_ID, new LatLng(19.324218, -99.184614));
    }
}
