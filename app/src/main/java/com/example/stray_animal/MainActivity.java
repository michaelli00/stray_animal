package com.example.stray_animal;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.PopupWindow;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureTable;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.layers.FeatureLayer;
import com.esri.arcgisruntime.mapping.ArcGISMap;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.LayerList;
import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.popup.PopupDefinition;
import com.esri.arcgisruntime.mapping.view.DefaultMapViewOnTouchListener;
import com.esri.arcgisruntime.mapping.view.IdentifyLayerResult;
import com.esri.arcgisruntime.mapping.view.MapView;
import com.esri.arcgisruntime.portal.Portal;
import com.esri.arcgisruntime.portal.PortalItem;
import com.google.android.gms.auth.api.signin.internal.Storage;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutionException;
import com.esri.arcgisruntime.geometry.Point;


public class MainActivity extends AppCompatActivity {

    static Popup currentPopup = null;
    static MapView mappy = null;
    private double longitude, latitude;
    private boolean mLocationPermissionGranted = false;
    private FusedLocationProviderClient fusedLocationClient;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private class MapTouchListener extends DefaultMapViewOnTouchListener {

        private MapView mMapView;

        private FeatureLayer mAnimalLayer;
        public MapTouchListener(final Context context, final MapView mapView) {
            super(context, mapView);
            Log.d("Constructor", "After Super");
            this.mMapView = mapView;
            mappy = mMapView;
        }

        /**
         * When a user taps on the map, an identify action is initiated and
         * any features found are displayed in a  view.
         *
         * @param e - MotionEvent
         * @return boolean
         */
        @Override
        public boolean onSingleTapConfirmed(final MotionEvent e) {
            final long time = System.currentTimeMillis();
            LayerList layers = mMapView.getMap().getOperationalLayers();
            mAnimalLayer = (FeatureLayer)layers.get(0);
            Log.d("Constructor", "Found Layer: " + mAnimalLayer.getName());
            // Hide bottom sheet
//            if (getActivity() != null) {
//                hideBottomSheet();
//            }
//
//            if (mSingleTapEnabled) {
//                // set the scale
//                mPresenter.setMapScale(mMapView.getMapScale());
//                // clear any previous selections
//                clearSelections();

            // get the screen point where user tapped
            final android.graphics.Point screenPoint = new android.graphics.Point((int) e.getX(), (int) e.getY());

            // Convert the screen point to a location
            final Point locationPoint = mMapView.screenToLocation(screenPoint);

            Log.d("Touch Location", "" + locationPoint.getX() + ", " + locationPoint.getY());

            final ListenableFuture<IdentifyLayerResult> identifyFuture =
                    mMapView.identifyLayerAsync(mAnimalLayer, screenPoint, 20, true, 1);
            // add a listener to the future
            identifyFuture.addDoneListener(new Runnable() {

                @Override
                public void run() {
                    IdentifyLayerResult identifyLayerResult = null;
                    mAnimalLayer.clearSelection();
                    currentPopup = null;
                    try {
                        // get the identify results from the future - returns when the operation is complete
                        identifyLayerResult = identifyFuture.get();
                        Log.d("identify layer", "Identified Layers " + identifyLayerResult.getPopups());

                    } catch (InterruptedException | ExecutionException ex) {
                        // must deal with checked exceptions thrown from the async identify operation
                        ex.printStackTrace();
                    }
                    List<Popup> popups = identifyLayerResult.getPopups();
                    if(popups.size() > 0) {
                        Popup popup = popups.get(0);
                        currentPopup = popup;
                        Feature feature = (Feature)popup.getGeoElement();
                        //ServiceFeatureTable table = (ServiceFeatureTable)feature.getFeatureTable();
                        //table.addFeatureAsync()
                        //apply updates
                        mAnimalLayer.selectFeature(feature);
                        startActivity(new Intent(MainActivity.this, PopupViewActivity.class));
                    }

                }
            });

            FloatingActionButton fab = findViewById(R.id.fab);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ServiceFeatureTable featureTable = (ServiceFeatureTable)mAnimalLayer.getFeatureTable();
                    Feature newFeature = featureTable.createFeature();
                    //newFeature.setGeometry( GeometryEngine.normalizeCentralMeridian(mMapView.screenToLocation(new android.graphics.Point(0,0))));
                    //Log.d("Main Activity", newFeature.getGeometry().toString());
                    Point p = new Point(longitude, latitude, SpatialReferences.getWgs84());
                    newFeature.setGeometry(p);
                    Log.d("Main Activity", "Test");
                    PopupDefinition definition = featureTable.getPopupDefinition();
                    Popup newPopup = new Popup(newFeature, definition);
                    currentPopup = newPopup;
                    startActivity(new Intent(MainActivity.this, PhotoActivity.class));
                }
            });

//                List<IdentifyLayerResult> results = mPresenter.getResultsForPoint(locationPoint);
//
//                if (results.isEmpty()) {
//                    // Show a progress bar while querying
//                    showProgressBar(getString(R.string.tree_at_location), getString(R.string.searching));
//
//                    // Query Street Tree layer with derived screen point
//                    final ListenableFuture<List<IdentifyLayerResult>> identifyLayers = mMapView
//                            .identifyLayersAsync(screenPoint, 5d, false);
//
//                    identifyLayers.addDoneListener(new Runnable() {
//                        @Override
//                        public void run() {
//                            try {
//                                Log.i(TAG, "Timing map data returning " + ((System.currentTimeMillis() - time) ) + " milliseconds, " + ((System.currentTimeMillis() - time) / 1000) + " seconds");
//                                // Grab the result and submit to the presenter for additional processing
//                                final List<IdentifyLayerResult> results = identifyLayers.get();
//                                Log.i(TAG, "Results obtained from identify");
//                                mPresenter.processIdentifiedLayerResult(results, locationPoint);
//                                Log.i(TAG, "Presenter processed results");
//                            } catch (InterruptedException | ExecutionException ie) {
//                                Log.e(TAG, ActivityUtils.getExceptionMessageAndCause(ie));
//                            }
//                        }
//                    });
//
//                } else {
//                    // We've processed this point before, so let the presenter handle it
//                    mPresenter.processIdentifiedLayerResult(results, locationPoint);
//                }
//
//                return super.onSingleTapConfirmed(e);
//            } else {
//                return false;
//            }
            return false;
//
        }
    }

    private MapView mMapView;
    private FeatureLayer mFeatureLayer;
    FirebaseStorage storage = FirebaseStorage.getInstance();

    private void setupMap() {
        if (mMapView != null) {
            String itemId = "e3674afc1f41437d9e9d9ff2717d20b8";
            Portal portal = new Portal("https://www.arcgis.com", false);
            PortalItem portalItem = new PortalItem(portal, itemId);
            ArcGISMap map = new ArcGISMap(portalItem);
            mMapView.setMap(map);

            // Listen for map taps
            MapTouchListener mapTouchListener = new MapTouchListener(this, mMapView);

            // Attach the listener to the map
            mMapView.setOnTouchListener(mapTouchListener);
        }



    }

//    private void storageTest(){
//        StorageReference storageRef = storage.getReference();
//        StorageReference testRef;
//        String jsonString = "Not Initialized";
//        FileOutputStream outputStream;
//        UploadTask uploadTask;
//
//        try{
//            JSONObject jsonObject = new JSONObject();
//            jsonObject.put("Name", "YEETICUS MAXIMUS");
//            jsonString = jsonObject.toString();
//        } catch(JSONException ex) {
//            ex.printStackTrace();
//        }
//
//        try{
//            outputStream = openFileOutput("testing.json", Context.MODE_PRIVATE);
//            outputStream.write(jsonString.getBytes());
//            outputStream.close();
//        } catch(Exception ex) {
//            ex.printStackTrace();
//        }
//
//        Uri file = Uri.fromFile(new File(this.getFilesDir() + "/testing.json"));
//        testRef = storageRef.child("tests/"+file.getLastPathSegment());
//        uploadTask = testRef.putFile(file);
//
//        uploadTask.addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.e("Failure", "failed");
//            }
//        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                Log.i("Not Failure", "Didn't fail");
//                downloadTest();
//            }
//        });
//
//    }
//
//    private void downloadTest(){
//        StorageReference storageRef = storage.getReference();
//        StorageReference downloadRef = storageRef.child("tests/jsontestthing.json");
//
//        File newFile = new File(this.getFilesDir(), "jsontestthing.json");
//
//        downloadRef.getFile(newFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
//                Log.i("Success", "big yeet");
//                overwriteTest();
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.e("Failure", "big oof");
//            }
//        });
//    }
//
//    private void overwriteTest(){
//        StorageReference storageRef = storage.getReference();
//        StorageReference overwriteRef;
//
//        String jsonData = "", newJsonData = "";
//        FileOutputStream outputStream;
//        UploadTask uploadTask;
//        try {
//            FileInputStream fileInputStream = new FileInputStream(new File(this.getFilesDir(), "/jsontestthing.json"));
//            InputStreamReader inputStreamReader = new InputStreamReader(fileInputStream);
//            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
//            StringBuilder builder = new StringBuilder();
//            String line = bufferedReader.readLine();
//
//            while(line != null){
//                builder.append(line);
//                line = bufferedReader.readLine();
//            }
//            jsonData = builder.toString();
//            Log.e("got string", jsonData);
//        } catch (Exception e) {
//            e.printStackTrace();
//            Log.e("No String", "boooooo");
//        }
//
//        try {
//            JSONObject jsonObject = new JSONObject(jsonData);
//            JSONArray jsonArray = jsonObject.getJSONArray("straysObjects");
//            JSONObject newStray = new JSONObject();
//            newStray.put("Biggus", "Dickus");
//            newStray.put("Largus", "Peanus");
//            jsonArray.put(newStray);
//            jsonObject.put("straysObjects", jsonArray);
//            newJsonData = jsonObject.toString();
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        try{
//            outputStream = openFileOutput("jsontestthing.json", Context.MODE_PRIVATE);
//            outputStream.write(newJsonData.getBytes());
//            outputStream.close();
//        } catch(Exception ex) {
//            ex.printStackTrace();
//        }
//
//        Uri file = Uri.fromFile(new File(this.getFilesDir() + "/jsontestthing.json"));
//        overwriteRef = storageRef.child("tests/"+file.getLastPathSegment());
//        uploadTask = overwriteRef.putFile(file);
//
//        uploadTask.addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception e) {
//                Log.e("Failure2", "failed2");
//            }
//        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
//            @Override
//            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
//                Log.i("Not Failure2", "Didn't fail2");
//            }
//        });
//
//    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mMapView = findViewById(R.id.mapView);
        setupMap();
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        getLocationPermission();
        getDeviceLocation();

        //storageTest();
    }

    @Override
    protected void onPause() {
        if (mMapView != null) {
            mMapView.pause();
        }
        super.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mMapView != null) {

            mMapView.resume();
        }
    }

    @Override
    protected void onDestroy() {
        if (mMapView != null) {
            mMapView.dispose();
        }
        super.onDestroy();
    }

    private void getDeviceLocation() {
        /*
         * Get the best and most recent location of the device, which may be null in rare
         * cases when a location is not available.
         */
        try {
            if (mLocationPermissionGranted) {
                Task<Location> locationResult = fusedLocationClient.getLastLocation();
                locationResult.addOnCompleteListener(this, new OnCompleteListener<Location>() {
                    @Override
                    public void onComplete(@NonNull Task<Location> task) {
                        if (task.isSuccessful()) {
                            // Set the map's camera position to the current location of the device.
                            Location location = task.getResult();
                            longitude = location.getLongitude();
                            latitude = location.getLatitude();
                            Log.d("getDeviceLocation", "Longitude: " + longitude + " Latitude: " + latitude);

                        } else {
                            Log.d("getDeviceLocation", "Current location is null. Using defaults.");
                            Log.e("getDeviceLocation", "Exception: %s", task.getException());
                        }
                    }
                });
            }
        } catch (SecurityException e)  {
            Log.e("Exception: %s", e.getMessage());
        }
    }

    private void getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                    PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
        }
    }
//    FloatingActionButton fab = findViewById(R.id.fab);



}