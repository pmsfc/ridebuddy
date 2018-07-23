package com.cm.app.ridebuddy.currentexercise;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.speech.tts.TextToSpeech;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import com.cm.app.ridebuddy.auxiliary.MyUtils;
import com.cm.app.ridebuddy.firebase.ExerciseDTO;
import com.cm.app.ridebuddy.firebase.ExerciseMapMarker;
import com.cm.app.ridebuddy.firebase.ExerciseMapPoint;
import com.cm.app.ridebuddy.firebase.UserPosMap;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.bumptech.glide.gifdecoder.GifHeaderParser.TAG;
import static com.cm.app.ridebuddy.activities.ViewExerciseActivity.EXTRA_EX_ID;

public class RuningExerciseService extends Service implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener,
        SensorEventListener, TextToSpeech.OnInitListener {


    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleApiClient;
    private static final String LOGSERVICE = "RuningExerciseService";
    private final int NUM_FALL_THRESHOLD = 16;
    private final double FALL_MAG_THRESHOLD = 40;
    private final int REST_THRESHOLD = 20;
    private int currRecordInd;
    private int accel_count; // fall occurs if accel_count >= NUM_ACCEL_THRESHOLD
    private int idle_count;
    private boolean cycle;
    private boolean isAYOActive;
    private final int MAX_RECORDS = 200;
    private float motionAbs;
    private boolean fallDetected = false;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyro;
    private float[] accel_data;

    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabase;
    private String currExerciseID;
    private FirebaseUser mFirebaseUser;

    private Location mCurrentLocation;
    private Location mLastLocation;
    private TextToSpeech tts;
    private boolean alreadyRun = false;
    private ArrayList<LatLng> points;
    private ArrayList<MarkerOptions> markers;
    private String curAddress;
    private float distanceMeters;
    private List<String> alreadySaid;
    private List<ExerciseMapMarker> myMarkers;

    @Override
    public void onCreate() {
        super.onCreate();
        buildGoogleApiClient();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
        mSensorManager.registerListener(this, mGyro, SensorManager.SENSOR_DELAY_GAME);

        isAYOActive = false;
        currRecordInd = 0;
        accel_count = 0;
        cycle = false;
        idle_count = 0;

        accel_data = new float[MAX_RECORDS];

        mFirebaseAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();

        mKnockDetector.init();
        tts = new TextToSpeech(getApplicationContext(), this);

        points = new ArrayList<>();
        markers = new ArrayList<>();

        Log.i(LOGSERVICE, "onCreate");
        alreadySaid = new ArrayList<>();
        myMarkers = new ArrayList<>();


    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(LOGSERVICE, "onStartCommand");

        if (!mGoogleApiClient.isConnected())
            mGoogleApiClient.connect();

        currExerciseID = intent.getStringExtra(EXTRA_EX_ID);

        return START_STICKY;
    }


    @Override
    public void onConnected(Bundle bundle) {
        Log.i(LOGSERVICE, "onConnected" + bundle);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        if (location != null) {
            ExerciseMapPoint ePoint = new ExerciseMapPoint(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                    location.getSpeed(), currExerciseID);
            mDatabase.child("map_points").push().setValue(ePoint);
            updateUserPos(location);
        }

        DatabaseReference myRef = FirebaseDatabase.getInstance().getReference("map_markers");
        Query queryRef = myRef;
        queryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for (DataSnapshot mdataSnapshot : dataSnapshot.getChildren()) {
                    ExerciseMapMarker exerciseMapMarker = mdataSnapshot.getValue(ExerciseMapMarker.class);
                    myMarkers.add(exerciseMapMarker);
                }


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }

        });

        startLocationUpdate();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.i(LOGSERVICE, "onConnectionSuspended " + i);

    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = mCurrentLocation;
        mCurrentLocation = location;
        //  updateUserPos();
        if (mLastLocation != null) {
            if (mLastLocation.distanceTo(location) > 1) { //reduce mem usage
                distanceMeters += mLastLocation.distanceTo(mCurrentLocation);
                ExerciseMapPoint ePoint = new ExerciseMapPoint(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                        location.getSpeed(), currExerciseID);
                mDatabase.child("map_points").push().setValue(ePoint);
                points.add(new LatLng(location.getLatitude(), location.getLongitude()));
                EventBus.getDefault().post(points);
                EventBus.getDefault().post(new ExerciseDTO(distanceMeters));
                updateUserPos(mCurrentLocation);


                for (ExerciseMapMarker mMarker : myMarkers) {
                    Location marker = new Location("marker");
                    marker.setLongitude(mMarker.getLongitude());
                    marker.setLatitude(mMarker.getLatitude());
                    if (mCurrentLocation.distanceTo(marker) < 15 &&
                            !alreadySaid.contains(mMarker.getMarkerName())) {
                        speak(mMarker.getMarkerName());
                        alreadySaid.add(mMarker.getMarkerName());
                    }
                }

            } else
                return;

        } else {
            ExerciseMapPoint ePoint = new ExerciseMapPoint(location.getLatitude(), location.getLongitude(), location.getAltitude(),
                    location.getSpeed(), currExerciseID);
            mDatabase.child("map_points").push().setValue(ePoint);
            points.add(new LatLng(location.getLatitude(), location.getLongitude()));
            EventBus.getDefault().post(points);
            updateUserPos(mCurrentLocation);
        }

    }

    private void updateUserPos(Location location) {
        UserPosMap userPosMap = new UserPosMap(location.getLatitude(),
                location.getLongitude(), mFirebaseUser.getUid(), mFirebaseUser.getDisplayName());
        mDatabase.child("user_on_map").child(mFirebaseUser.getUid()).setValue(userPosMap);
        //   MyUtils.INSTANCE.showToast(getApplicationContext(),"HALP");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopLocationUpdate();
        mGoogleApiClient.disconnect();
        if (mKnockDetector != null)
            mKnockDetector.stop();

        if (tts != null) {
            tts.stop();
            tts.shutdown();
            Log.d(TAG, "TTS Destroyed");
        }

        MyUtils.INSTANCE.showToast(getApplicationContext(), "Service closed!");
        Log.i(LOGSERVICE, "onDestroy service");

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.i(LOGSERVICE, "onConnectionFailed ");

    }

    private void initLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(5000);
        mLocationRequest.setFastestInterval(2000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);

    }

    private void addMark() {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(mCurrentLocation.getLatitude(),
                    mCurrentLocation.getLongitude(), 1);

            if (addresses != null) {
                Address returnedAddress = addresses.get(0);
                StringBuilder strReturnedAddress = new StringBuilder();
                for (int i = 0; i < returnedAddress.getMaxAddressLineIndex(); i++) {
                    strReturnedAddress.append(returnedAddress.getAddressLine(i)).append("");
                }
                curAddress = strReturnedAddress.toString();
            } else {
                curAddress = "marker";
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        ExerciseMapMarker eMarkerPre = new ExerciseMapMarker();
        DatabaseReference postRef = mDatabase.child("map_markers");
        DatabaseReference newPostRef = postRef.push();
        newPostRef.setValue(eMarkerPre);
        String markerID = newPostRef.getKey();

        ExerciseMapMarker eMarker = new ExerciseMapMarker(mCurrentLocation.getLatitude(),
                mCurrentLocation.getLongitude(), curAddress, currExerciseID, mFirebaseUser.getUid(), markerID);


        alreadySaid.add(curAddress);
        postRef.child(markerID).setValue(eMarker);
        markers.add(new MarkerOptions()
                .position(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()))
                .title(curAddress));

        EventBus.getDefault().post(markers);
    }

    private void startLocationUpdate() {
        initLocationRequest();


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
    }

    private void stopLocationUpdate() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addOnConnectionFailedListener(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {

            float mSensorX = event.values[0];
            float mSensorY = event.values[1];
            float mSensorZ = event.values[2];


            // big loop for checking threshold begins here

            // 1) get new accelerometer reading
            float accelValue = mSensorX * mSensorX + mSensorY * mSensorY + mSensorZ * mSensorZ;

            // 2) record accelerometer difference, then increment currRecordInd

            if (currRecordInd != 0) { // if not the very first record

                // 3) update accel_count
                // check if in cycle, and if so if existing record is above or below DIFF_THRESHOLD as well
                boolean newRecordTap = accelValue < FALL_MAG_THRESHOLD;
                boolean oldRecordTap = accel_data[currRecordInd] < FALL_MAG_THRESHOLD;
                if (newRecordTap) {
                    //boolean oldRecordTap = accel_diff[(currRecordInd + MAX_RECORDS - 1) % MAX_RECORDS] < FALL_THRESHOLD;
                    if (!oldRecordTap || !cycle) {
                        accel_count++;
                    }
                    idle_count = 0;
                } else {
                    //boolean oldRecordTap = accel_diff[(currRecordInd + MAX_RECORDS - 1) % MAX_RECORDS] < FALL_THRESHOLD;
                    if (oldRecordTap && cycle) {
                        accel_count--;
                    }
                    idle_count++;
                    if (idle_count >= REST_THRESHOLD)
                        accel_count = Math.max(0, accel_count - 2);
                }
            }
            accel_data[currRecordInd] = accelValue;
            currRecordInd = (currRecordInd + 1) % MAX_RECORDS;


            // 4) check if accel_count threshold is met, if so switch activity
            if (accel_count >= NUM_FALL_THRESHOLD) {


                //Need to check if the "are you okay is already called"
                if (!isAYOActive) {
                    isAYOActive = true;


                }
            }

        } else if (event.sensor.getType() == Sensor.TYPE_GYROSCOPE) {
              /*aqui se encontrar movimento brusco ira
                esperar 5 segundos, se apos 5 segundos os dados da acel do gyro
                estiverem perto de 0 a queda é confirmada.*/
            if (isAYOActive) {
                float mSensorX = event.values[0];
                float mSensorY = event.values[1];
                float mSensorZ = event.values[2];
                final float motionValue = mSensorX + mSensorY + mSensorZ;

                motionAbs = Math.abs(motionValue);
                Log.i(TAG, "MOTION VALUE " + motionAbs);
                Handler handler = new Handler();
                handler.postDelayed(new Runnable() {
                    public void run() {
                        if (motionAbs < 0.05 && isAYOActive) { //lower motion, person is laying on floor
                            Intent verification = new Intent(getApplicationContext(), Verification.class);
                            verification.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            if (!alreadyRun && !fallDetected) {
                                fallDetected = true;
                                startActivity(verification);
                                alreadyRun = true;
                            }
                            currRecordInd++;
                            isAYOActive = false;
                        } else {
                            isAYOActive = false;
                        }
                    }
                }, 5000); //time in millis

                alreadyRun = false;
            }

        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }


    //The abstract class KnockDetector requires the implementation of void knockDetected(int) method
    KnockDetector mKnockDetector = new KnockDetector(this) {
        @Override
        void knockDetected(int knockCount) {
            switch (knockCount) {
                case 2:
                    Log.d("KNOCK", "Double Knock");

                    speak("Added new location");

                    addMark();
                    break;
                case 3:
                  /*  Log.d("KNOCK", "Triple Knock");

                    Geocoder geocoder = new Geocoder(getApplicationContext(), Locale.getDefault());
                    try {
                        List<Address> addresses = geocoder.getFromLocation(mCurrentLocation.getLatitude(),
                                mCurrentLocation.getLongitude(), 1);

                        if (addresses != null) {
                            Address returnedAddress = addresses.get(0);
                            StringBuilder strReturnedAddress = new StringBuilder();

                            strReturnedAddress.append(returnedAddress.getAddressLine(0)).append(",");
                            String city = returnedAddress.getAddressLine(1);
                            city = city.replaceAll("\\d", "");
                            strReturnedAddress.append(city);
                            strReturnedAddress.append(returnedAddress.getAddressLine(2)).append(",");

                            speak("You'r on " + strReturnedAddress.toString());


                        }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
*/

                    break;
                default:
                    break;
            }
        }
    };

    private void speak(String txt) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ttsGreater21(txt);
        } else {
            ttsUnder20(txt);
        }
    }


    @SuppressWarnings("deprecation")
    private void ttsUnder20(String text) {
        HashMap<String, String> map = new HashMap<>();
        map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "MessageId");
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, map);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void ttsGreater21(String text) {
        String utteranceId = this.hashCode() + "";
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
    }


    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {

            int result = tts.setLanguage(Locale.US);

            if (result == TextToSpeech.LANG_MISSING_DATA
                    || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "This Language is not supported");
            }

        } else {
            Log.e("TTS", "Initilization Failed!");
        }
    }
}