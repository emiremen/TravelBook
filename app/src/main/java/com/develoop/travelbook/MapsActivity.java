package com.develoop.travelbook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.ActionBar;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.io.IOException;
import java.io.PushbackInputStream;
import java.util.List;
import java.util.Locale;

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleMap.OnMapLongClickListener {

    private GoogleMap mMap;
    public LocationListener locationListener;
    public LocationManager locationManager;
    public SQLiteDatabase database;
    public SharedPreferences sharedPreferences;
    public String placeDialogText;
    public double latitudeLocation, longitudeLocation;
    public double selectedLocLat, selectedLocLon;
    public int placeID;
    public Boolean newPlace, canSave;
    public Intent intentGet;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.mapmenu, menu);

        if (newPlace) {
            menu.findItem(R.id.edit_place).setVisible(false);
            menu.findItem(R.id.share_place).setVisible(false);
            menu.findItem(R.id.delete_place).setVisible(false);
        } else {
            menu.findItem(R.id.save_place).setVisible(false);
        }

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.share_place) {
            String placeUri = "http://maps.google.com/maps/place/" + latitudeLocation + "," + longitudeLocation;
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            String shareSub = "Konum";
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, shareSub);
            shareIntent.putExtra(Intent.EXTRA_TEXT, placeUri);
            startActivity(Intent.createChooser(shareIntent, "Şununla Paylaş:"));
        } else if (item.getItemId() == R.id.save_place) {
            if (canSave){
                showMyCustomDialog(selectedLocLat, selectedLocLon);
            } else {
                Toast.makeText(getApplicationContext(), "Please Select Location.", Toast.LENGTH_LONG).show();
            }

        } else if (item.getItemId() == R.id.delete_place) {
            String sqlString = "DELETE FROM places WHERE id = ?";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindDouble(1, placeID);
            sqLiteStatement.execute();
            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        } else if (item.getItemId() == R.id.edit_place) {
            String sqlString = "UPDATE places SET latitude = ?, longitude = ? WHERE id = ?";
            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
            sqLiteStatement.bindDouble(1, selectedLocLat);
            sqLiteStatement.bindDouble(2, selectedLocLon);
            sqLiteStatement.bindDouble(3, placeID);
            sqLiteStatement.execute();
            Toast.makeText(getApplicationContext(), "Location Updated.", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
            intent.putExtra("isUpdate", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(intent);
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        intentGet = getIntent();
        if (intentGet.getStringExtra("info").matches("newPlace")) {
            newPlace = true;
        } else {
            newPlace = false;
        }

        sharedPreferences = getSharedPreferences("com.develoop.travelbook", MODE_PRIVATE);
        sharedPreferences.edit().putBoolean("isFirstEnter", true).apply();

        database = openOrCreateDatabase("Places", MODE_PRIVATE, null);
        database.execSQL("CREATE TABLE IF NOT EXISTS places(id INTEGER PRIMARY KEY, name VARCHAR, latitude DOUBLE, longitude DOUBLE)");
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.setOnMapLongClickListener(this);

        canSave = false;

        if (newPlace) {
            locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
            locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(Location location) {
                    boolean isFirstEnter = sharedPreferences.getBoolean("isFirstEnter", true);

                    if (isFirstEnter) {
                        mMap.clear();
                        mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title("Your Location"));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(location.getLatitude(), location.getLongitude()), 15));
                        sharedPreferences.edit().putBoolean("isFirstEnter", false).apply();
                    }


                }

                @Override
                public void onStatusChanged(String provider, int status, Bundle extras) {

                }

                @Override
                public void onProviderEnabled(String provider) {

                }

                @Override
                public void onProviderDisabled(String provider) {

                }
            };

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(MapsActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            } else {
                locationManager.requestLocationUpdates(locationManager.GPS_PROVIDER, 1000, 1, locationListener);

                Location lastLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                LatLng userLastLocation = new LatLng(lastLocation.getLatitude(), lastLocation.getLongitude());
                mMap.addMarker(new MarkerOptions().title("Last Location").position(userLastLocation));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLastLocation, 15));
            }
        } else { //GET PLACE
            mMap.clear();
            placeID = intentGet.getIntExtra("placeID", 0);

            Cursor cursor = database.rawQuery("SELECT * FROM places WHERE id = ?", new String[]{String.valueOf(placeID)});

            int nameIndex = cursor.getColumnIndex("name");
            int latitudeIndex = cursor.getColumnIndex("latitude");
            int longitudeIndex = cursor.getColumnIndex("longitude");

            while (cursor.moveToNext()) {
                latitudeLocation = cursor.getDouble(latitudeIndex);
                longitudeLocation = cursor.getDouble(longitudeIndex);
                LatLng latLng = new LatLng(cursor.getDouble(latitudeIndex), cursor.getDouble(longitudeIndex));
                mMap.addMarker(new MarkerOptions().title(cursor.getString(nameIndex)).position(latLng));
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
            }
        }


    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                finish();
            } else {
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 1, locationListener);
                Toast.makeText(this, "Refresh Map",Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    public void onMapLongClick(LatLng latLng) {
        mMap.clear();
        selectedLocLat = latLng.latitude;
        selectedLocLon = latLng.longitude;

        Geocoder geocoder = new Geocoder(MapsActivity.this, Locale.getDefault());
        List<Address> addresses = null;
        try {
            addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
        } catch (IOException e) {
            e.printStackTrace();
        }
        String subLocality = addresses.get(0).getSubLocality();
        String subAdminArea = addresses.get(0).getSubAdminArea();
        String thoroughfare = addresses.get(0).getThoroughfare();
        String subThoroughfare = addresses.get(0).getSubThoroughfare();

        mMap.addMarker(new MarkerOptions().position(new LatLng(latLng.latitude, latLng.longitude)).title(subLocality + " " + thoroughfare + " No: " + subThoroughfare + " " + subAdminArea));
        canSave = true;
    }

    public void showMyCustomDialog(final double lat, final double lng){
        if (intentGet.getStringExtra("info").matches("newPlace")) {
            Context context = this;
            final Dialog dialog = new Dialog(context);
            dialog.setContentView(R.layout.dialog);
            dialog.setTitle("Set Location Name");
            dialog.setCancelable(false);

            Button btnCancel = dialog.findViewById(R.id.btnCancel);
            Button btnDialog = dialog.findViewById(R.id.btnDialog);
            final EditText dialogEditText = dialog.findViewById(R.id.dialogEditText);

            btnCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dialogEditText.getHint().toString().matches("Please Enter Name!")) {
                        dialogEditText.setHint("Place Name");
                    }
                    dialog.dismiss();
                }
            });
            btnDialog.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (dialogEditText.getText().toString().matches("") || dialogEditText.getText().toString().isEmpty()) {
                        dialogEditText.setHint("Please Enter Name!");
                    } else {
                        placeDialogText = dialogEditText.getText().toString();
                        try {
                            String sqlString = "INSERT INTO places (name, latitude, longitude) VALUES(?, ?, ?)";
                            SQLiteStatement sqLiteStatement = database.compileStatement(sqlString);
                            sqLiteStatement.bindString(1, placeDialogText);
                            sqLiteStatement.bindDouble(2, lat);
                            sqLiteStatement.bindDouble(3, lng);
                            sqLiteStatement.execute();

                            Intent intent = new Intent(MapsActivity.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                            startActivity(intent);
                            Toast.makeText(getApplicationContext(), "Place Saved.", Toast.LENGTH_LONG).show();
                            dialog.dismiss();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
            dialog.getWindow().setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            dialog.show();
        }
    }

}
