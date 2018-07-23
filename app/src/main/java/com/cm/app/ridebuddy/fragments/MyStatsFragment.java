package com.cm.app.ridebuddy.fragments;


import android.os.Bundle;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.firebase.Exercise;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.helper.DateAsXAxisLabelFormatter;
import com.jjoe64.graphview.series.DataPoint;
import com.jjoe64.graphview.series.LineGraphSeries;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;


public class MyStatsFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener {

    private SwipeRefreshLayout swipeLayout;
    private TextView textlastExercise;
    private TextView textTime;
    private TextView textDistance;
    private TextView textAvgSpeed;
    private TextView textElevationGain;
    private TextView textCalories;
    private GraphView graph;
    private NavigationView navigationView;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;
    private DatabaseReference mDatabase;
    private long totalDuration = 0;
    private float totalDistance = 0;
    private float totalavgSpeed = 0;
    private double totalElevation = 0;
    private double totalcalories = 0;

    public MyStatsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
    }

    @Override
    public void onResume() {
        super.onResume();
        // navigationView = (NavigationView) getActivity().findViewById(R.id.nav_view);
        // navigationView.getMenu().getItem(4).setChecked(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        View view = inflater.inflate(R.layout.fragment_my_stats, container, false);
        swipeLayout = (SwipeRefreshLayout) view.findViewById(R.id.swipe_stats);
        swipeLayout.setOnRefreshListener(this);
        graph = (GraphView) view.findViewById(R.id.graph);
        textlastExercise = (TextView) view.findViewById(R.id.lastExercise);
        textTime = (TextView) view.findViewById(R.id.text_time);
        textDistance = (TextView) view.findViewById(R.id.text_distance);
        textAvgSpeed = (TextView) view.findViewById(R.id.text_avgspeed);
        textElevationGain = (TextView) view.findViewById(R.id.text_elevation);
        textCalories = (TextView) view.findViewById(R.id.text_calories);


        DatabaseReference myRefm = FirebaseDatabase.getInstance().getReference("exercises");
        Query queryRefm = myRefm.orderByChild("gID").equalTo(mFirebaseUser.getUid());

        queryRefm.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                // MyUtils.INSTANCE.showToast(getApplicationContext(), "changed!");

                List<DataPoint> dataPoints = new ArrayList<DataPoint>();
                int count = 1;
                for (DataSnapshot mdataSnapshot : dataSnapshot.getChildren()) {
                    Exercise exercise = mdataSnapshot.getValue(Exercise.class);
                    totalavgSpeed += exercise.getAvgSpeed();
                    totalcalories += exercise.getCalories();
                    totalDistance += exercise.getTotalDistance();
                    totalDuration += exercise.getDuration();
                    totalElevation += exercise.getElevationGain();

                    if (count < 15) {
                        dataPoints.add(new DataPoint(exercise.getDate(), exercise.getCalories()));
                    }

                    Log.d("MyStatsFragment DEBUG", "onDataChange: " + count + "  " + totalcalories);
                    count++;
                }

                Date d = new Date(totalDuration * 1000L);
                SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); // HH for 0-23
                df.setTimeZone(TimeZone.getTimeZone("GMT"));
                String timetoshow = df.format(d);
                textTime.setText(String.format("%s: %s", "Total Time",
                        timetoshow));

                double kilometers = totalDistance / 1000;
                double kilometersRound = Math.round(kilometers * 100.0) / 100.0;
                textDistance.setText(String.format("%s: %s %s", "Total Distance",
                        kilometersRound, " Km"));

                int speed = (int) ((totalavgSpeed * 3600) / 1000);
                textAvgSpeed.setText(String.format("%s: %s %s", "Total Speed",
                        speed, "Km/h"));
                double elevation = Math.round(totalElevation * 100.0) / 100.0;


                textElevationGain.setText(String.format("%s: %s %s", "Total Elevation",
                        elevation, "m"));
                textCalories.setText(String.format("%s: %s", "Total Calories",
                        Math.round(totalcalories)));

                DataPoint[] dataPointsToG = new DataPoint[dataPoints.size()];
                dataPoints.toArray(dataPointsToG);

                for (int i = 0; i < dataPointsToG.length; i++) {
                    Log.d("ARRAY TESTE", "onDataChange: " + dataPointsToG[i].toString());
                }
                LineGraphSeries<DataPoint> series = new LineGraphSeries<>(dataPointsToG);
                series.setDrawDataPoints(true);
                series.setDataPointsRadius(10);
                series.setThickness(8);

                graph.addSeries(series);
                graph.getGridLabelRenderer().setLabelFormatter(new DateAsXAxisLabelFormatter(getActivity()));
                graph.getGridLabelRenderer().setNumHorizontalLabels(2); // only 4 because of the space


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });


        return view;
    }

    @Override
    public void onRefresh() {
        swipeLayout.setRefreshing(false);
    }
}
