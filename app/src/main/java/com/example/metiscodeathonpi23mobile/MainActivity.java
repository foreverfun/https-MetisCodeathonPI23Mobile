package com.example.metiscodeathonpi23mobile;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity implements LocationUpdateListener, PictureTakerCallback {
    private Button btnStart;
    private Button btnTakePicture;
    private TextView tvLocation;
    private boolean isCollecting = false;

    private LocationTracker tracker;
    private Compass compass;
    private PictureTaker pictureTaker;

    private TrackedPath trackedPath;

    private GoogleMap myMap;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        tracker = new LocationTracker(this, this);
        compass = new Compass(this);
        pictureTaker = new PictureTaker(this, this);

        setContentView(R.layout.activity_main);
        btnStart = findViewById(R.id.btnStart);
        btnTakePicture = findViewById(R.id.btnTakePicture);
        tvLocation = findViewById(R.id.tvLocation);

        btnStart.setOnClickListener(view -> handleStartClick());

        btnTakePicture.setOnClickListener(view -> handleTakePictureClick());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        pictureTaker.onActivityResult(requestCode, resultCode, data);
    }

    public void onLocationUpdate(Location location) {
        TrackedPoint trackedPoint = new TrackedPoint();

        trackedPoint.longitude = location.getLongitude();
        trackedPoint.latitude = location.getLatitude();
        trackedPoint.azimuth = compass.azimuth;
        trackedPoint.direction = compass.direction;

        // TODO: create a new Location object and add it to TrackedPath.locationList
        String text = "(" + trackedPoint.longitude + ", " + trackedPoint.latitude + ") " + compass.azimuth + " : " + compass.direction;
        trackedPath.locationList.add(trackedPoint);
        tvLocation.setText(text);

        //To track current path
        LatLng current = new LatLng(trackedPoint.latitude, trackedPoint.longitude);
        myMap.addMarker(new MarkerOptions().position(current).title(trackedPath.localTime.toString()));
    }

    public void onPictureTaken(String imageUri) {
        Log.d("MainActivity", "onPictureTaken - Image captured and saved to: " + imageUri.toString());
    }

    @SuppressLint("SetTextI18n")
    private void handleStartClick() {

        if (isCollecting) {
            isCollecting = false;
            btnStart.setText("Start");
            tracker.stop();
            compass.stop();
        } else {
            trackedPath = new TrackedPath();
            isCollecting = true;
            btnStart.setText("Stop");
            tracker.start();
            compass.start();
        }
    }

    @SuppressLint("SetTextI18n")
    private void handleTakePictureClick() {
        pictureTaker.takePicture();
    }

    private void postEndpoint(String url, TrackedPath trackedPath)
    {
        Gson gson = new Gson();
        String trackedPathString = gson.toJson(trackedPath);
        new Thread(new Runnable() {

            @Override
            public void run() {
                RequestBody postBody = new FormBody.Builder()
                        .add("TrackedPathString", trackedPathString)
                        .build();
                Request request = new Request.Builder()
                        .url(url)
                        .post(postBody)
                        .build();
                OkHttpClient client = new OkHttpClient();
                Call call = client.newCall(request);

                Response response = null;
                try {
                    response = call.execute();
                    String serverResponse = response.body().string();
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            //display to serverResponse to UI
                        }
                    });
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }

            }
        }).start();
    }

    private void getEndpoint(String url)
    {
        //get
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url)
                .build();
        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (response.isSuccessful())
                {
                    String responseString = response.body().string();
                    Gson gson = new Gson();
                    Type listType = new TypeToken<ArrayList<TrackedPath>>(){}.getType();
                    ArrayList<TrackedPath> trackedPaths = gson.fromJson(responseString, listType);
                    runOnUiThread(new Runnable() {

                        @Override
                        public void run() {
                            //display trackedPath to UI component
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                e.printStackTrace();
            }
        });

    }
}
