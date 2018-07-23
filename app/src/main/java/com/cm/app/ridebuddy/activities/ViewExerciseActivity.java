package com.cm.app.ridebuddy.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.auxiliary.MyUtils;
import com.cm.app.ridebuddy.firebase.Exercise;
import com.cm.app.ridebuddy.firebase.ExerciseMapMarker;
import com.cm.app.ridebuddy.firebase.ExerciseMapPoint;
import com.cm.app.ridebuddy.processing.CircleTransform;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.GroundOverlayOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import static com.cm.app.ridebuddy.R.id.map;

public class ViewExerciseActivity extends AppCompatActivity implements OnMapReadyCallback {

    public final static String EXTRA_EX_ID = "com.cm.app.ridebuddy.EXID";
    private long exerciseID;
    private GoogleMap mMap;
    private Exercise exercise;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_exercise);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        actionBar.setDisplayHomeAsUpEnabled(true);
        Intent intent = getIntent();
        String ID = intent.getStringExtra(EXTRA_EX_ID);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(map);
        mapFragment.getMapAsync(this);
        setTitle("Exercise data review");



        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("exercises");
        Query queryRef = myRef.child(ID);

       // Log.d("HALP ME", "ples");

        queryRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                exercise = snapshot.getValue(Exercise.class);
                Log.w("HALP", snapshot.getKey());

                TextView username = (TextView) findViewById(R.id.name);
                ImageView thumb = (ImageView)  findViewById(R.id.thumb);
                TextView exerciseName = (TextView) findViewById(R.id.exercise_name);
                TextView exercisedate = (TextView) findViewById(R.id.exercise_date);
                TextView textTime = (TextView) findViewById(R.id.text_time);
                TextView textDistance = (TextView) findViewById(R.id.text_distance);
                TextView textAvgSpeed = (TextView) findViewById(R.id.text_avgspeed);
                TextView textElevationGain = (TextView) findViewById(R.id.text_elevation);
                TextView textCalories = (TextView) findViewById(R.id.text_calories);

                exerciseName.setText(exercise.getName());
                exercisedate.setText(new Date(exercise.getDate()).toString());

                Date d = new Date(exercise.getDuration() * 1000L);
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); // HH for 0-23
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                String timetoshow = df.format(d);
                textTime.setText(String.format("%s: %s", "Time",
                        timetoshow));

                double kilometers = exercise.getTotalDistance() / 1000;
                double kilometersRound = Math.round(kilometers * 100.0) / 100.0;
                textDistance.setText(String.format("%s: %s %s", "Distance",
                        kilometersRound, " Km"));

                int speed = (int) ((exercise.getAvgSpeed() * 3600) / 1000);
                textAvgSpeed.setText(String.format("%s: %s %s", "Speed",
                        speed, "Km/h"));
                double elevation = Math.round(exercise.getElevationGain() * 100.0) / 100.0;


                textElevationGain.setText(String.format("%s: %s %s", "Elev. Gain",
                        elevation, "m"));
                textCalories.setText(String.format("%s: %s", "Calories",
                        Math.round(exercise.getCalories())));

                username.setText(exercise.getUserName());

                Glide.with(getApplicationContext()).load("https://lh4.googleusercontent.com" + exercise.getPhotoURL())
                        .crossFade()
                        .thumbnail(0.5f)
                        .error(R.drawable.bikeorange)
                        .bitmapTransform(new CircleTransform(getApplicationContext()))
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(thumb);


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("HALP", "getUser:onCancelled", databaseError.toException());
            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        Intent intent = getIntent();
        String ID = intent.getStringExtra(EXTRA_EX_ID);


        DatabaseReference myRefm = FirebaseDatabase.getInstance().getReference("map_markers");
        Query queryRefm = myRefm.orderByChild("exerciseID").equalTo(ID);
        queryRefm.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot mdataSnapshot : dataSnapshot.getChildren()) {
                    ExerciseMapMarker mark = mdataSnapshot.getValue(ExerciseMapMarker.class);
                    mMap.addMarker(new MarkerOptions().title(mark.getMarkerName())
                            .position(new LatLng(mark.getLatitude(),mark.getLongitude())));
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        });

        mMap = googleMap;
        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("map_points");
        Query queryRef = myRef.orderByChild("exerciseID").equalTo(ID);

        queryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                List <ExerciseMapPoint> points = new ArrayList<>();
                for (DataSnapshot mdataSnapshot : dataSnapshot.getChildren()) {
                    ExerciseMapPoint exerciseMapPoint = mdataSnapshot.getValue(ExerciseMapPoint.class);
                    points.add(exerciseMapPoint);
                }

                PolylineOptions options = new PolylineOptions().width(14).color(Color.RED).geodesic(true);
                for (ExerciseMapPoint point : points) {
                    options.add(new LatLng(point.getLatitude(),point.getLongitude()));
                }

                //start
                LatLng NEWARK = new LatLng(points.get(0).getLatitude(), points.get(0).getLongitude());
                GroundOverlayOptions newarkMap = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromResource(R.drawable.bikedark))
                        .zIndex(9)
                        .position(NEWARK, 6f, 6f);
                mMap.addGroundOverlay(newarkMap);

                //end
                LatLng NEWARK2 = new LatLng(points.get(points.size()-1).getLatitude(), points.get(points.size()-1).getLongitude());
                GroundOverlayOptions newarkMap2 = new GroundOverlayOptions()
                        .image(BitmapDescriptorFactory.fromResource(R.drawable.myflag))
                        .zIndex(9)
                        .position(NEWARK2, 6f, 6f);
                mMap.addGroundOverlay(newarkMap2);
                mMap.addPolyline(options);

                if(points.size() != 0) {
                    LatLng initPoint = new LatLng(points.get(0).getLatitude(),points.get(0).getLongitude());
                    LatLng midPoint = new LatLng(points.get((points.size() / 2)).getLatitude(),
                            points.get((points.size() / 2)).getLongitude());
                    LatLng lastPoint = new LatLng(points.get((points.size()-1)).getLatitude(),
                            points.get((points.size()-1)).getLongitude());

                    LatLngBounds.Builder builder = new LatLngBounds.Builder();
                    builder.include(initPoint);
                    builder.include(midPoint);
                    builder.include(lastPoint);
                    LatLngBounds bounds = builder.build();

                    CameraPosition camPos = new CameraPosition.Builder()
                            .target(bounds.getCenter())
                            .zoom(exercise.getMapZoom())
                            .build();
                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));

                }else
                    MyUtils.INSTANCE.showToast(getApplicationContext(), "No DATA");
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });



    }


}
