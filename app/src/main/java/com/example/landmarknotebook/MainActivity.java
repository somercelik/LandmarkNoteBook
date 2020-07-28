package com.example.landmarknotebook;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    static ArrayList<String> names = new ArrayList<>();
    static ArrayList<LatLng> locations = new ArrayList<>();
    static ArrayAdapter arrayAdapter;
    ListView listView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.listView);

        try {
            MapsActivity.database = openOrCreateDatabase("Places", MODE_PRIVATE, null);
            Cursor cursor = MapsActivity.database.rawQuery("SELECT * FROM places", null);
            int nameIndex = cursor.getColumnIndex("name");
            int latitudeIndex = cursor.getColumnIndex("latitude");
            int longitudeIndex = cursor.getColumnIndex("longitude");

            while (cursor.moveToNext()) {
                String receivedName = cursor.getString(nameIndex);
                String receivedLatitude = cursor.getString(latitudeIndex);
                String receivedLongitude = cursor.getString(longitudeIndex);
                names.add(receivedName);
                locations.add(new LatLng(Double.parseDouble(receivedLatitude), Double.parseDouble(receivedLongitude)));
            }
            cursor.close();

        } catch (Exception e) {
            e.printStackTrace();
        }

        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, names);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                Intent intentToView = new Intent(getApplicationContext(), MapsActivity.class);
                intentToView.putExtra("position", position);
                intentToView.putExtra("info", "view");
                startActivity(intentToView);
            }
        });


    }

    //Menüyü activity'ye bağlıyoruz
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.add_place, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //Menü item'ına tıklandığında ne olacak?
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add_place) {           //Tıklanan Add place ise
            Intent intentToAdd = new Intent(getApplicationContext(), MapsActivity.class);
            intentToAdd.putExtra("info", "add");
            startActivity(intentToAdd);
        }
        return super.onOptionsItemSelected(item);
    }
}