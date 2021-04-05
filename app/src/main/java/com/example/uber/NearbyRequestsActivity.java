package com.example.uber;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.parse.FindCallback;
import com.parse.ParseException;
import com.parse.ParseGeoPoint;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;

import java.util.ArrayList;
import java.util.List;

public class NearbyRequestsActivity extends AppCompatActivity {

    ListView nearbyRequestsListView;
    ArrayList<String> requests = new ArrayList<>();
    ArrayList<Double> requestLatitude = new ArrayList<>();
    ArrayList<Double> requestLongitude = new ArrayList<>();
    ArrayList<String> userNames = new ArrayList<>();
    ArrayAdapter arrayAdapter;

    LocationManager locationManager;
    LocationListener locationListener;

    public void updateListView(Location location) {
        if (location != null) {

            ParseQuery<ParseObject> query = new ParseQuery<ParseObject>("Request");
            ParseGeoPoint parseGeoPoint = new ParseGeoPoint(location.getLatitude(),location.getLongitude());

            query.whereNear("location",parseGeoPoint);
            query.whereDoesNotExist("driverUsername");
            query.setLimit(10);
            query.findInBackground(new FindCallback<ParseObject>() {
                @Override
                public void done(List<ParseObject> objects, ParseException e) {
                    if(e==null){
                        requests.clear();
                        requestLatitude.clear();
                        requestLongitude.clear();
                        if(objects.size()>0){
                            for(ParseObject object : objects){
                                ParseGeoPoint requestGeoPoint = (ParseGeoPoint) object.get("location");
                                if(requestGeoPoint!=null) {
                                    double distanceInMiles = parseGeoPoint.distanceInMilesTo(requestGeoPoint);
                                    requests.add(Double.toString((double) Math.round(distanceInMiles * 10) / 10));
                                    requestLatitude.add(requestGeoPoint.getLatitude());
                                    requestLongitude.add(requestGeoPoint.getLongitude());
                                    userNames.add(object.getString("username"));
                                }
                            }
                        }
                    }
                    else {
                        requests.add("No Active Nearby Requests!");
                    }
                    arrayAdapter.notifyDataSetChanged();
                }
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode==1){
            if(grantResults.length>0 && grantResults[0]== PackageManager.PERMISSION_GRANTED){
                if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 1, locationListener);
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    updateListView(lastKnownLocation);
                }
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nearby_requests);
        setTitle("Nearby Requests");

        nearbyRequestsListView = (ListView) findViewById(R.id.nearbyRequestsListView);
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, requests);
        requests.clear();
        requests.add("Searching Nearby Requests...");
        nearbyRequestsListView.setAdapter(arrayAdapter);

        nearbyRequestsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                if(Build.VERSION.SDK_INT<23 || ContextCompat.checkSelfPermission(NearbyRequestsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)== PackageManager.PERMISSION_GRANTED) {
                    Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (requestLatitude.size() > i && requestLongitude.size() > i && userNames.size() > i && lastKnownLocation != null) {
                        Intent intent = new Intent(getApplicationContext(),DriverLocationActivity.class);

                        intent.putExtra("requestLatitude",requestLatitude.get(i));
                        intent.putExtra("requestLongitude",requestLongitude.get(i));
                        intent.putExtra("driverLatitude",lastKnownLocation.getLatitude());
                        intent.putExtra("driverLongitude",lastKnownLocation.getLongitude());
                        intent.putExtra("username",userNames.get(i));
                        startActivity(intent);
                    }
                }
            }
        });

        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                updateListView(location);

                ParseUser.getCurrentUser().put("location",new ParseGeoPoint(location.getLatitude(),location.getLongitude()));
                ParseUser.getCurrentUser().saveInBackground();
            }

            @Override
            public void onStatusChanged(String s, int i, Bundle bundle) {}
            @Override
            public void onProviderEnabled(String s) {}
            @Override
            public void onProviderDisabled(String s) {}
        };

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1);
        }
        else {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,1,locationListener);
            Location lastKnownLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if(lastKnownLocation!=null){
                updateListView(lastKnownLocation);
            }
        }
    }
}