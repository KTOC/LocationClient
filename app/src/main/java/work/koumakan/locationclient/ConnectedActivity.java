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
import android.widget.Toast;
import android.widget.TextView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.net.URISyntaxException;

import io.socket.client.IO;
import io.socket.client.Socket;
import io.socket.emitter.Emitter;

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
    FusedLocationProviderClient locationProviderClient;
    LocationCallback locationCallBack;

    // Permission codes
    final int PERM_FINE_LOC = 100;
    final int PERM_COARSE_LOC = 101;

    String serverName;
    String port;

//    String serverName = "http://aws.koumakan.work";
//    final String serverName = "http://192.168.1.72";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connected);

        // Button references
        disconnBtn = (Button) findViewById(R.id.disconnect);
        startSendingBtn = (Button) findViewById(R.id.startsendlocation);

        // get username
        username = (String) getIntent().getExtras().getString(MainActivity.USERNAME);
        port = (String) getIntent().getExtras().getString(MainActivity.PORT);
        serverName = (String) getIntent().getExtras().getString(MainActivity.ADDR);

        String serverNamePort = serverName + ":" + port;

        Toast.makeText(this, "Connecting to : " + serverNamePort, Toast.LENGTH_SHORT).show();

        TextView user_banner = (TextView) findViewById(R.id.connected_user);
        user_banner.setText(username);

        try {
            socket = IO.socket(serverNamePort);
            // Add event listener for socket
            socket.on(Socket.EVENT_CONNECT, onConnect).on(Socket.EVENT_DISCONNECT, onDisconnect).on(Socket.EVENT_CONNECT_ERROR, onConnectError);
            socket.connect();
        } catch (URISyntaxException e) {
            Toast.makeText(this, "Failed to connect: " + e, Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }

        // Set listeners for buttons
        disconnBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (ConnectedActivity.this.socket.connected()) {
                    socket.disconnect();
                    stopLocationUpdates();
                    finish();
                } else {
                    stopLocationUpdates();
                    finish();
                }
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
                } else {
                    System.out.println("Not connected");
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
            locationProviderClient = getFusedLocationProviderClient(this);
            locationCallBack = new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    // do work here
                    onLocationChanged(locationResult.getLastLocation());
                }
            };
            locationProviderClient.requestLocationUpdates(mLocationRequest, locationCallBack, Looper.myLooper());
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
        if (ConnectedActivity.this.socket.connected()) {
            Toast.makeText(ConnectedActivity.this, "Sending: " + lat + " : " + lng, Toast.LENGTH_SHORT).show();
            ConnectedActivity.this.socket.emit("locationdata", data);
        }
    }

    public void stopLocationUpdates() {
        if (locationProviderClient != null) {
            try {
                final Task<Void> voidTask = locationProviderClient.removeLocationUpdates(locationCallBack);
                if (voidTask.isSuccessful()) {
                    Log.d("LOC","StopLocation updates successful! ");
                } else {
                    Log.d("LOC","StopLocation updates unsuccessful! " + voidTask.toString());
                }
            }
            catch (SecurityException exp) {
                Log.d("LOC", " Security exception while removeLocationUpdates");
            }
        }
    }

    @Override
    public void onBackPressed() {
        socket.disconnect();
        stopLocationUpdates();
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


    // Make callback functions
    private Emitter.Listener onConnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ConnectedActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ConnectedActivity.this, "Connected to " + serverName, Toast.LENGTH_SHORT).show();
                }
            });
        }
    };
    private Emitter.Listener onDisconnect = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ConnectedActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ConnectedActivity.this, "Disconnected from " + serverName, Toast.LENGTH_SHORT).show();
                    socket.disconnect();
                }
            });
        }
    };
    private Emitter.Listener onConnectError = new Emitter.Listener() {
        @Override
        public void call(final Object... args) {
            ConnectedActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(ConnectedActivity.this, "Connect Error", Toast.LENGTH_SHORT).show();
                    socket.disconnect();
                    finish();
                }
            });
        }
    };
}