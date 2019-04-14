package com.example.stray_animal;

import android.Manifest;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import com.esri.arcgisruntime.concurrent.ListenableFuture;
import com.esri.arcgisruntime.data.Feature;
import com.esri.arcgisruntime.data.FeatureEditResult;
import com.esri.arcgisruntime.data.ServiceFeatureTable;
import com.esri.arcgisruntime.geometry.GeometryEngine;
import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.popup.Popup;
import com.esri.arcgisruntime.mapping.popup.PopupAttachmentManager;
import com.esri.arcgisruntime.mapping.popup.PopupField;
import com.esri.arcgisruntime.mapping.popup.PopupManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

public class PhotoActivity extends AppCompatActivity {

    private Button button;
    private Spinner spinner;
    private Spinner spinner2;
    private ImageView imageView;
    private FloatingActionButton fab;
    private Intent intent;
    private Bitmap bitmap;
    private TextView textView;

    private FusedLocationProviderClient fusedLocationClient;
    private static final int PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;

    private PopupField condition = null;
    private PopupField species = null;
    private PopupField dateSighted = null;
    private PopupField description = null;

    private Popup popup;
    private PopupManager popupManager;
    private PopupAttachmentManager popupAttachmentManager;

    private int conditionVal;
    private int speciesVal;
    private String descriptionVal;

    public  static final int RequestPermissionCode  = 1 ;
    public static final int REQUEST_IMAGE = 100;
    public static final int REQUEST_PERMISSION = 200;
    private String imageFilePath = "";
    private double longitude, latitude;
    private boolean mLocationPermissionGranted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo);

        spinner = (Spinner) findViewById(R.id.list);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.animal_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner.setAdapter(adapter);

        spinner2 = (Spinner) findViewById(R.id.list2);
        // Create an ArrayAdapter using the string array and a default spinner layout
        ArrayAdapter<CharSequence> adapter2 = ArrayAdapter.createFromResource(this,
                R.array.status_array, android.R.layout.simple_spinner_item);
        // Specify the layout to use when the list of choices appears
        adapter2.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // Apply the adapter to the spinner
        spinner2.setAdapter(adapter2);

        button = findViewById(R.id.button);
        imageView = findViewById(R.id.image);
        fab = findViewById(R.id.fab);
        textView = findViewById(R.id.plain_text_input);


        popup = MainActivity.currentPopup;
        popupManager = new PopupManager(this, popup);
        popupAttachmentManager = popupManager.getAttachmentManager();

        List<PopupField> popupFields = popupManager.getEditableFields();
        for(PopupField pf : popupFields){
            if(pf.getFieldName().equals("condition")){
                condition = pf;
            } else if(pf.getFieldName().equals("species")){
                species = pf;
            } else if(pf.getFieldName().equals("date_sighted")){
                dateSighted = pf;
            } else if(pf.getFieldName().equals("description")){
                description = pf;
            }
        }
        Log.d("PhotoActivity", "got " + popupFields.size() + " popupFields");

        EnableRuntimePermission();

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(PhotoActivity.this);
        getLocationPermission();
        getDeviceLocation();

        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                intent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);

                startActivityForResult(intent, 7);

            }
        });

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("Photo Activity", "Clicked");

                popupManager.startEditing();
                popupManager.updateValue(spinner.getSelectedItem().toString(), species);
                popupManager.updateValue(spinner2.getSelectedItem().toString(), condition);
                popupManager.updateValue(textView.getText().toString(), description);
                try {
                    popupAttachmentManager.addAttachment("attachment", bitmap, Bitmap.CompressFormat.PNG);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                popupManager.finishEditingAsync().addDoneListener(new Runnable() {
                    @Override
                    public void run() {


                    }
                });
                popup = popupManager.getPopup();
                Log.d("Photo Activity", "got popup");
                Feature feature = (Feature)popup.getGeoElement();
                Point p = new Point(longitude, latitude, SpatialReferences.getWgs84());
                feature.setGeometry(p);
                Log.d("Photo Activity", feature.getAttributes().toString());
                ServiceFeatureTable table = (ServiceFeatureTable)feature.getFeatureTable();
                table.addFeatureAsync(feature).addDoneListener(() -> applyEdits(table));
//                    @Override
//                    public void run() {
//
//                    }
//                });
//
//                Log.d("Photo Activity", "did a thing");
//                table.applyEditsAsync().addDoneListener(new Runnable() {
//                    @Override
//                    public void run() {
//                        table.applyEditsAsync();
//                        Log.d("Photo Activity", "ran");
//                    }
//                });
//                Map<String, Object> attributes = new HashMap<>();
//                attributes.put("condition", spinner2.getSelectedItem().toString());
//                attributes.put("species", spinner.getSelectedItem().toString());
//                attributes.put("description", textView.getText().toString());
//                ServiceFeatureTable table = (ServiceFeatureTable)((Feature)popup.getGeoElement()).getFeatureTable();
//                Point p = new Point(longitude, latitude, SpatialReferences.getWgs84());
//                Log.d("Photo Activity", "Longitude: " + longitude + " Latitude: " + latitude);
//                Feature feature = table.createFeature(attributes, p);
//                Log.d("Photo Activity", feature.getAttributes().toString());
//                table.addFeatureAsync(feature).addDoneListener(() -> applyEdits(table));

            }
        });

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

    private void applyEdits(ServiceFeatureTable featureTable){
        ListenableFuture<List<FeatureEditResult>> editResult = featureTable.applyEditsAsync();
        editResult.addDoneListener(() -> {
            try {
                List<FeatureEditResult> edits = editResult.get();
                // check if the server edit was successful
                if (edits != null && edits.size() > 0) {
                    if (!edits.get(0).hasCompletedWithErrors()) {
                        Log.d("Apply Edits", "Feature successfully added");
                    } else {
                        throw edits.get(0).getError();
                    }
                }
            } catch (InterruptedException | ExecutionException e) {
                Log.d("Apply Edits", e.getCause().getMessage());
            }
        });
    }


    public void EnableRuntimePermission(){

        if (ActivityCompat.shouldShowRequestPermissionRationale(PhotoActivity.this,
                Manifest.permission.CAMERA))
        {

            Toast.makeText(PhotoActivity.this,"CAMERA permission allows us to Access CAMERA app", Toast.LENGTH_LONG).show();

        } else {

            ActivityCompat.requestPermissions(PhotoActivity.this,new String[]{
                    Manifest.permission.CAMERA}, RequestPermissionCode);

        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == 7 && resultCode == RESULT_OK) {

            bitmap = (Bitmap) data.getExtras().get("data");

            imageView.setImageBitmap(bitmap);
        }
    }

    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {

        switch (RC) {

            case RequestPermissionCode:

                if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {

                    Toast.makeText(PhotoActivity.this,"Permission Granted, Now your application can access CAMERA.", Toast.LENGTH_LONG).show();

                } else {

                    Toast.makeText(PhotoActivity.this,"Permission Canceled, Now your application cannot access CAMERA.", Toast.LENGTH_LONG).show();

                }
                break;
        }
    }
}