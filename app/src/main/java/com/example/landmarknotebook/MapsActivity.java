package com.example.landmarknotebook;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    static SQLiteDatabase database;
    LocationManager locationManager;
    LocationListener locationListener;
    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        assert mapFragment != null;
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setOnMapLongClickListener(this);//Uzun basma listener'ı eklendi
        Intent intent = getIntent();
        String initializationMethod = intent.getStringExtra("info");
        if (initializationMethod != null && initializationMethod.matches("add")) {
            //Ekleme sayfa düzeni
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);    //Konum servislerine erişim
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    SharedPreferences sharedPreferences = MapsActivity.this.getSharedPreferences("com.somercelik.landmarknotebook", MODE_PRIVATE);
                    boolean firstTimeCheck = sharedPreferences.getBoolean("notFirstTime", false);
                    if (!firstTimeCheck) {
                        LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());    //Gelen konumu paralel,meridyene ayır
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                        sharedPreferences.edit().putBoolean("notFirstTime", true).apply();
                    }

                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }
            };


            //İzin, ardından konum alma işlemleri

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                mMap.clear();
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastLocation != null) {
                    LatLng userLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
                }

            }
        } else {
            //İnceleme sayfa düzeni
            mMap.clear();
            int position = intent.getIntExtra("position", 0);
            LatLng selectedLocation = new LatLng(MainActivity.locations.get(position).latitude, MainActivity.locations.get(position).longitude);
            String selectedPlaceName = MainActivity.names.get(position);
            mMap.addMarker(new MarkerOptions().title(selectedPlaceName).position(selectedLocation));
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(selectedLocation, 15));
        }

    }


    //Kullanıcı izin verdikten sonra yapılacak işlemler
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length != 0 && requestCode == 1 && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mMap.clear();
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastLocation != null) {
                LatLng userLatLng = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15));
            }

        }
    }

    //Kullanıcı haritada bir yere uzun bastığında çağırılacak metod
    @Override
    public void onMapLongClick(LatLng latLng) {
        Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
        String address = "";
        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addressList != null && addressList.size() > 0) {
                if (addressList.get(0).getThoroughfare() != null) {
                    address += addressList.get(0).getThoroughfare();        //Caddeyi adrese ekle
                }
                if (addressList.get(0).getSubThoroughfare() != null) {
                    address += " " + addressList.get(0).getSubThoroughfare();     //Sokağı adrese ekle
                }
            } else {
                address = "Select new place!";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMap.clear();
        mMap.addMarker(new MarkerOptions().title(address).position(latLng));


        if (!address.isEmpty()) {

            MainActivity.names.add(address);
            MainActivity.locations.add(latLng);
            MainActivity.arrayAdapter.notifyDataSetChanged();
            Toast.makeText(getApplicationContext(), "New Place Added", Toast.LENGTH_LONG).show();
            try {
                String latitude = String.valueOf(latLng.latitude);
                String longitude = String.valueOf(latLng.longitude);

                database = this.openOrCreateDatabase("Places", MODE_PRIVATE, null);
                database.execSQL("CREATE TABLE IF NOT EXISTS places (id INTEGER PRIMARY KEY, name VARCHAR, latitude VARCHAR, longitude VARCHAR)");
                String databaseSqlScript = "INSERT INTO places (name, latitude, longitude) VALUES (?, ?, ?)";
                SQLiteStatement statement = database.compileStatement(databaseSqlScript);
                statement.bindString(1, address);
                statement.bindString(2, latitude);
                statement.bindString(3, longitude);
                statement.execute();


            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            Toast.makeText(getApplicationContext(), "Select another location!", Toast.LENGTH_LONG).show();
        }

    }
}