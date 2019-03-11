package work.koumakan.locationclient;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Looper;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;

import static com.google.android.gms.location.LocationServices.getFusedLocationProviderClient;

public class ConnectedActivity extends AppCompatActivity {
    //declare socket object
    private Socket socket;
    private String username;

    // Buttons
    Button disconnBtn;
    Button startSendingBtn;

    // Location
    LocationRequest mLocationRequest;

    // Permission codes
    final int PERM_FINE_LOC = 100;
    final int PERM_COARSE_LOC = 101;

    int port = 9696;
    //    final String serverName = "http://aws.koumakan.work";
    final String serverName = "http://192.168.1.72";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        // Button references
        disconnBtn = (Button) findViewById(R.id.disconnect);
        startSendingBtn = (Button) findViewById(R.id.startsendlocation);

        // get username
        username = (String) getIntent().getExtras().getString(MainActivity.USERNAME);
        String serverNamePort = serverName + ":" + port;
        System.out.println("connecting to: " + serverNamePort);

        TextView user_banner = (TextView) findViewById(R.id.connected_user);
        user_banner.setText(username);

        try {
            socket = IO.socket(serverNamePort);
            socket.connect();
            Log.d("is connect", "isConnected? " + socket.connected());
        } catch (URISyntaxException e) {
            Log.e("error_tag", "failed connect");
            e.printStackTrace();
        }

        // Set listeners for buttons
        disconnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectedActivity.this.socket.connected()) {
//                    ConnectedActivity.this.socket.emit("disconnect"); //this doesn't work
                    socket.disconnect();
                }
                finish();
            }
        });
        startSendingBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectedActivity.this.socket.connected()) {
                    // If we're connected to the server
                    ConnectedActivity.this.socket.emit("userlogin", username, "fakepwd");
                    // Setup interval here
                    ConnectedActivity.this.startLocationUpdates();
                }
            }
        });
    }

    protected void startLocationUpdates() {
        // Create the location request to start receiving updates
        mLocationRequest = new LocationRequest();
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);

        // Create LocationSettingsRequest object using location request
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(mLocationRequest);
        LocationSettingsRequest locationSettingsRequest = builder.build();

        // Check whether location settings are satisfied
        // https://developers.google.com/android/reference/com/google/android/gms/location/SettingsClient
        SettingsClient settingsClient = LocationServices.getSettingsClient(this);
        settingsClient.checkLocationSettings(locationSettingsRequest);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            System.out.println("permission failed");
            // TODO: Consider calling
            ActivityCompat.requestPermissions(ConnectedActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERM_FINE_LOC);
        } else {
            // Permissions already set
            getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    // do work here
                    onLocationChanged(locationResult.getLastLocation());
                }
            },
            Looper.myLooper());
        }
    }

    private void onLocationChanged(Location lastLocation) {
        System.out.println("onlocationChanged called");
        double lng = lastLocation.getLongitude();
        double lat = lastLocation.getLatitude();
        Log.d("location_changed", "Location: " + lastLocation.getLatitude() + " : " + lastLocation.getLongitude());
        // Make JSON object
        JSONObject data = new JSONObject();
        try {
            data.put("lng", lng);
            data.put("lat", lat);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        ConnectedActivity.this.socket.emit("locationdata", data);
    }

    @Override
    public void onBackPressed() {
        socket.disconnect();
        finish();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch(requestCode) {
            case PERM_FINE_LOC: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    System.out.println("permission ok");
                    getFusedLocationProviderClient(this).requestLocationUpdates(mLocationRequest, new LocationCallback() {
                        @Override
                        public void onLocationResult(LocationResult locationResult) {
                            // do work here
                            onLocationChanged(locationResult.getLastLocation());
                        }
                    },
                    Looper.myLooper());
                }
                return;
            }
            // Add other cases here if need be
        }
    }
}