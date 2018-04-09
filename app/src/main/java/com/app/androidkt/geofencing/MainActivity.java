package com.app.androidkt.geofencing;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "MainActivity";
    private static final int REQUEST_LOCATION_PERMISSION_CODE = 101;

    private GoogleMap googleMap;

    private GeofencingRequest geofencingRequest;
    private GoogleApiClient googleApiClient;

    private boolean isMonitoring = false;

    private MarkerOptions markerOptions;

    private Marker currentLocationMarker;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);


        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this).build();

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_PERMISSION_CODE);
        }


    }

    private void startLocationMonitor() {
        Log.d(TAG, "start location monitor");
        LocationRequest locationRequest = LocationRequest.create()
                .setInterval(2000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        try {
            LocationServices.FusedLocationApi.requestLocationUpdates(googleApiClient, locationRequest, new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {

                    if (currentLocationMarker != null) {
                        currentLocationMarker.remove();
                    }
                    markerOptions = new MarkerOptions();
                    markerOptions.position(new LatLng(location.getLatitude(), location.getLongitude()));
                    markerOptions.title("Ubicación actual");
                    currentLocationMarker = googleMap.addMarker(markerOptions);
                    Log.d(TAG, "Location Change Lat Lng " + location.getLatitude() + " " + location.getLongitude());
                }
            });
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
        }

    }

    private void startGeofencing() {
        Log.d(TAG, "Start geofencing monitoring call");
        sendNotification("Start geofencing monitoring call");
        pendingIntent = getGeofencePendingIntent();
        geofencingRequest = new GeofencingRequest.Builder()
                .setInitialTrigger(Geofence.GEOFENCE_TRANSITION_ENTER)
                .addGeofence(getGeofence())
                .build();

        if (!googleApiClient.isConnected()) {
            Log.d(TAG, "Google API client not connected");
        } else {
            try {
                LocationServices.GeofencingApi.addGeofences(googleApiClient, geofencingRequest, pendingIntent).setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()) {
                            Log.d(TAG, "Successfully Geofencing Connected");
                            sendNotification("Successfully Geofencing Connected");
                        } else {
                            Log.d(TAG, "Failed to add Geofencing " + status.getStatus());
                            sendNotification("Failed to add Geofencing " + status.getStatus());
                        }
                    }
                });
            } catch (SecurityException e) {
                Log.d(TAG, e.getMessage());
            }
        }
        isMonitoring = true;
        invalidateOptionsMenu();
    }

    @NonNull
    private Geofence getGeofence() {
        LatLng latLng = Constants.AREA_LANDMARKS.get(Constants.SECOND_PLACE_ID);
        return new Geofence.Builder()
                .setRequestId(Constants.SECOND_PLACE_ID)
                .setExpirationDuration(Geofence.NEVER_EXPIRE)
                .setCircularRegion(latLng.latitude, latLng.longitude, Constants.GEOFENCE_RADIUS_IN_METERS)
                .setNotificationResponsiveness(1000)
                .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT)
                .build();
    }

    private PendingIntent getGeofencePendingIntent() {
        if (pendingIntent != null) {
            return pendingIntent;
        }
        Intent intent = new Intent(this, GeofenceRegistrationService.class);
        return PendingIntent.getService(this, 0, intent, PendingIntent.
                FLAG_UPDATE_CURRENT);
    }

    private void stopGeoFencing() {
        pendingIntent = getGeofencePendingIntent();
        LocationServices.GeofencingApi.removeGeofences(googleApiClient, pendingIntent)
                .setResultCallback(new ResultCallback<Status>() {
                    @Override
                    public void onResult(@NonNull Status status) {
                        if (status.isSuccess()){
                            Log.d(TAG, "Stop geofencing");
                        sendNotification("Stop geofencing");
                        }
                        else{
                            Log.d(TAG, "Not stop geofencing");
                        sendNotification("Not stop geofencing");}
                    }
                });
        isMonitoring = false;
        invalidateOptionsMenu();
    }

    @Override
    protected void onResume() {
        super.onResume();
        int response = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(MainActivity.this);
        if (response != ConnectionResult.SUCCESS) {
            Log.d(TAG, "Google Play Service Not Available");
            GoogleApiAvailability.getInstance().getErrorDialog(MainActivity.this, response, 1).show();
        } else {
            Log.d(TAG, "Google play service available");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.reconnect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        googleApiClient.disconnect();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.manu_map_activity, menu);
        if (isMonitoring) {
            menu.findItem(R.id.action_start_monitor).setVisible(false);
            menu.findItem(R.id.action_stop_monitor).setVisible(true);
        } else {
            menu.findItem(R.id.action_start_monitor).setVisible(true);
            menu.findItem(R.id.action_stop_monitor).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_start_monitor:
                startGeofencing();
                break;
            case R.id.action_stop_monitor:
                stopGeoFencing();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        this.googleMap = googleMap;
        LatLng latLng = Constants.AREA_LANDMARKS.get(Constants.FIRST_PLACE_ID);
        //googleMap.addMarker(new MarkerOptions().position(latLng).title("Uble"));
        //googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));
        LatLng latLng1 = Constants.AREA_LANDMARKS.get(Constants.SECOND_PLACE_ID);
        googleMap.addMarker(new MarkerOptions().position(latLng).title("Fac de Conta"));
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 17f));

        googleMap.setMyLocationEnabled(true);

        Circle circle = googleMap.addCircle(new CircleOptions()
                .center(new LatLng(latLng1.latitude, latLng1.longitude))
                .radius(Constants.GEOFENCE_RADIUS_IN_METERS)
                .strokeColor(Color.RED)
                .strokeWidth(4f));

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Google Api Client Connected");
        isMonitoring = true;
        startGeofencing();
        startLocationMonitor();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Google Connection Suspended");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        isMonitoring = false;
        Log.e(TAG, "Connection Failed:" + connectionResult.getErrorMessage());
        sendNotification("Connection Failed:" + connectionResult.getErrorMessage());
    }

    private void sendNotification(String notificationDetails) {
        // Create an explicit content Intent that starts the main Activity.
        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);

        // Construct a task stack.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);

        // Add the main Activity to the task stack as the parent.
        stackBuilder.addParentStack(MainActivity.class);

        // Push the content Intent onto the stack.
        stackBuilder.addNextIntent(notificationIntent);

        // Get a PendingIntent containing the entire back stack.
        PendingIntent notificationPendingIntent =
                stackBuilder.getPendingIntent(1, PendingIntent.FLAG_UPDATE_CURRENT);

        // Get a notification builder that's compatible with platform versions >= 4
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);

        // Define the notification settings.
        builder.setSmallIcon(R.mipmap.ic_launcher)
                // In a real app, you may want to use a library like Volley
                // to decode the Bitmap.
                .setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_launcher))
                .setColor(Color.RED)
                .setContentTitle(notificationDetails)
                .setContentText("Funciona")
                .setDefaults(Notification.DEFAULT_LIGHTS | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_SOUND)
                .setContentIntent(notificationPendingIntent);

        // Dismiss notification once the user touches it.
        builder.setAutoCancel(true);

        // Get an instance of the Notification manager
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Issue the notification
        mNotificationManager.notify(0, builder.build());
    }
}
