package com.develoop.travelbook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    ListView listView;
    ArrayAdapter arrayAdapter;
    ArrayList<String> placesListArray;
    ArrayList<Integer> placesIDArray;
    SQLiteDatabase database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        placesListArray = new ArrayList<String>();

        placesIDArray = new ArrayList<Integer>();

        database = openOrCreateDatabase("Places", MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS places(id INTEGER PRIMARY KEY, name VARCHAR, latitude DOUBLE, longitude DOUBLE)");
        final Cursor cursor = database.rawQuery("SELECT * FROM places",null);

        final int nameIndex = cursor.getColumnIndex("name");
        final int IdIndex = cursor.getColumnIndex("id");

        while (cursor.moveToNext()) {
            placesListArray.add("○      " + cursor.getString(nameIndex));
            placesIDArray.add(cursor.getInt(IdIndex));
        }
        cursor.close();

        //Dizi ile ListView'i bağlama
        arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, placesListArray);
        listView.setAdapter(arrayAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                intent.putExtra("placeID", placesIDArray.get(position));
                intent.putExtra("info", "getPlace");
                startActivity(intent);
            }
        });
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.addplace, menu);
        return super.onCreateOptionsMenu(menu);
    }

    public boolean onOptionsItemSelected(@NonNull MenuItem menuItem){
        if (menuItem.getItemId() == R.id.add_place_item) {
            Intent intent = new Intent(MainActivity.this, MapsActivity.class);
            intent.putExtra("info", "newPlace");
            startActivity(intent);
        }
        return super.onOptionsItemSelected(menuItem);
    }

}
