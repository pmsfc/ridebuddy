package com.cm.app.ridebuddy.currentexercise;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Display;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.activities.MainActivity;
import com.cm.app.ridebuddy.activities.ViewExerciseActivity;
import com.cm.app.ridebuddy.auxiliary.MyUtils;
import com.cm.app.ridebuddy.auxiliary.UserSession;
import com.cm.app.ridebuddy.firebase.Exercise;
import com.cm.app.ridebuddy.firebase.ExerciseDTO;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.ByteArrayOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;

import static android.R.attr.width;
import static com.cm.app.ridebuddy.R.id.map;
import static com.cm.app.ridebuddy.activities.ViewExerciseActivity.EXTRA_EX_ID;
import static java.lang.System.out;


public class RuningExerciseActivity extends AppCompatActivity implements
        ConnectionCallbacks,
        OnConnectionFailedListener,
        LocationListener,
        OnMapReadyCallback {

    protected static final String TAG = "location-service";
    public static final long UPDATE_INTERVAL_IN_MILLISECONDS = 1000;
    public static final long FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS = 1000;

    // Keys for storing activity state in the Bundle.
    protected final static String REQUESTING_LOCATION_UPDATES_KEY = "requesting-location-updates-key";
    protected final static String LOCATION_KEY = "location-key";
    protected final static String LAST_UPDATED_TIME_STRING_KEY = "last-updated-time-string-key";
    protected GoogleApiClient mGoogleApiClient;
    protected LocationRequest mLocationRequest;
    protected Location mCurrentLocation;

    // UI Widgets.
    protected Button mStartUpdatesButton;
    protected Button mStopUpdatesButton;

    protected TextView mSpeedView;
    protected TextView mTotalDistance;
    protected TextView mTotalTime;
    protected TextView savingTxt;


    protected Boolean mRequestingLocationUpdates;

    protected String mLastUpdateTime;
    private GoogleMap mMap;
    private ArrayList<LatLng> points; //added
    private ArrayList<MarkerOptions> markers;
    private Boolean exerciseStarted = false;

    private float totalDistanceMeters;
    private Location lastLocation;
    private double totalElevationGain;
    private UserSession userSession;
    private Exercise currentExercise;
    private Date startDate;
    private Date endDate;
    private String mapSnapshotName;
    private float totalSpeed;

    private static final double LN2 = 0.6931471805599453;
    private static final int WORLD_PX_HEIGHT = 256;
    private static final int WORLD_PX_WIDTH = 256;
    private static final int ZOOM_MAX = 21;

    private Thread myThread;
    private int currentDuration = 0;
    private int displayWidth;

    private Timer checkImmobile = new Timer();
    private TimerTask ok;

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mGyro;

    private FirebaseAuth mFirebaseAuth;
    private DatabaseReference mDatabase;
    private String currExerciseID;
    private FirebaseUser mFirebaseUser;
    private String exerciseName;
    private boolean canClose = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final LocationManager manager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            MyUtils.INSTANCE.showToast(this, "Turn on GPS service!");
            finish();
        } else {
            setContentView(R.layout.activity_runing_exercicise);


            Display display = getWindowManager().getDefaultDisplay();
            Point size = new Point();
            display.getSize(size);
            displayWidth = size.x;


            // Locate the UI widgets.
            mStartUpdatesButton = (Button) findViewById(R.id.start_updates_button);
            mStopUpdatesButton = (Button) findViewById(R.id.stop_updates_button);

            mTotalDistance = (TextView) findViewById(R.id.totaldistance);
            mSpeedView = (TextView) findViewById(R.id.Speed_text);
            mTotalTime = (TextView) findViewById(R.id.totalTime);
            savingTxt = (TextView) findViewById(R.id.saving_show_user);


            mRequestingLocationUpdates = false;
            mLastUpdateTime = "";

            // Update values using data stored in the Bundle.
            updateValuesFromBundle(savedInstanceState);

            // Kick off the process of building a GoogleApiClient and requesting the LocationServices
            // API.
            buildGoogleApiClient();

            SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(map);
            mapFragment.getMapAsync(this);

            ActionBar actionBar = getSupportActionBar();
            actionBar.setDisplayHomeAsUpEnabled(false);
            setTitle("All Checkpoints");

            points = new ArrayList<>(); //added
            markers = new ArrayList<>();
            userSession = new UserSession(this);

            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mGyro = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

            mFirebaseAuth = FirebaseAuth.getInstance();
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mFirebaseUser = mFirebaseAuth.getCurrentUser();

            EventBus.getDefault().register(this);

        }

    }


    private void updateValuesFromBundle(Bundle savedInstanceState) {
        Log.i(TAG, "Updating values from bundle");
        if (savedInstanceState != null) {
            // Update the value of mRequestingLocationUpdates from the Bundle, and make sure that
            // the Start Updates and Stop Updates buttons are correctly enabled or disabled.
            if (savedInstanceState.keySet().contains(REQUESTING_LOCATION_UPDATES_KEY)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                        REQUESTING_LOCATION_UPDATES_KEY);


            }

            // Update the value of mCurrentLocation from the Bundle and update the UI to show the
            // correct latitude and longitude.
            if (savedInstanceState.keySet().contains(LOCATION_KEY)) {
                // Since LOCATION_KEY was found in the Bundle, we can be sure that mCurrentLocation
                // is not null.
                mCurrentLocation = savedInstanceState.getParcelable(LOCATION_KEY);
            }

            // Update the value of mLastUpdateTime from the Bundle and update the UI.
            if (savedInstanceState.keySet().contains(LAST_UPDATED_TIME_STRING_KEY)) {
                mLastUpdateTime = savedInstanceState.getString(LAST_UPDATED_TIME_STRING_KEY);
            }
            //  updateUI();
        }
    }

    /**
     * Builds a GoogleApiClient. Uses the {@code #addApi} method to request the
     * LocationServices API.
     */
    protected synchronized void buildGoogleApiClient() {
        Log.i(TAG, "Building GoogleApiClient");
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        createLocationRequest();
    }

    /**
     * Sets up the location request. Android has two location request settings:
     * {@code ACCESS_COARSE_LOCATION} and {@code ACCESS_FINE_LOCATION}. These settings control
     * the accuracy of the current location. This sample uses ACCESS_FINE_LOCATION, as defined in
     * the AndroidManifest.xml.
     * <p/>
     * When the ACCESS_FINE_LOCATION setting is specified, combined with a fast update
     * interval (5 seconds), the Fused Location Provider API returns location updates that are
     * accurate to within a few feet.
     * <p/>
     * These settings are appropriate for mapping applications that show real-time location
     * updates.
     */
    protected void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setFastestInterval(FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    public void closeThis() {
        finish();
        Intent intent = new Intent(this, ViewExerciseActivity.class);
        // Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra(EXTRA_EX_ID, currExerciseID);
        startActivity(intent);
    }

    public void startUpdatesButtonHandler(View view) throws InterruptedException {
        if (!mRequestingLocationUpdates) {

            mStartUpdatesButton.setVisibility(View.GONE);
            mStopUpdatesButton.setVisibility(View.VISIBLE);
            mTotalDistance.setVisibility(View.VISIBLE);
            mSpeedView.setVisibility(View.VISIBLE);
            mTotalTime.setVisibility(View.VISIBLE);

            Exercise nExercise = new Exercise();
            DatabaseReference postRef = mDatabase.child("exercises");
            DatabaseReference newPostRef = postRef.push();
            newPostRef.setValue(nExercise);
            currExerciseID = newPostRef.getKey();
            mapSnapshotName = currExerciseID;

            mRequestingLocationUpdates = true;
            startLocationUpdates();
            MyUtils.INSTANCE.showToast(this, "Exercise Started!");
            exerciseStarted = true;


            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            startDate = new Date();


            HashMap<String, String> userDetails = userSession.getUserDetails();
            String gID = userDetails.get(UserSession.KEY_GID);

            Date dateNow = new Date();
            String exerciseName = "Afternoon Ride";


            // Exercise exercise = new Exercise(null, totalElevationGain, totalDistanceMeters, dateNow, exerciseName, mapSnapshotName, 0, 0, 0, 0, userID , gID);

            //  addMark("Begining");
            //  points.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

            totalSpeed += mCurrentLocation.getSpeed();

            myThread = new Thread() {

                @Override
                public void run() {
                    try {
                        while (!isInterrupted()) {
                            Thread.sleep(1000);
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    updateUI();
                                }
                            });
                        }
                    } catch (InterruptedException e) {
                    }
                }
            };

            myThread.start();

        }
        makeNotification(getApplicationContext()); // discrepancia entre este tempo e o real?! ms? nano?
        Intent intent = new Intent(getBaseContext(), RuningExerciseService.class);
        intent.putExtra(EXTRA_EX_ID, currExerciseID);
        startService(intent);

    }


    public void stopUpdatesButtonHandler(View view) throws InterruptedException {
        mStopUpdatesButton.setActivated(false);
        mStopUpdatesButton.setVisibility(View.GONE);
        mTotalDistance.setVisibility(View.GONE);
        mSpeedView.setVisibility(View.GONE);
        mTotalTime.setVisibility(View.GONE);


        savingTxt.setVisibility(View.VISIBLE);
        savingTxt.setTextColor(Color.DKGRAY);
        savingTxt.setText("Insert Name");
        // MyUtils.INSTANCE.showToast(this, "Processing data...");


        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        // Set up the input
        final EditText input = new EditText(this);

        // builder.setTitle("Exercise Name");

        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);
        String text = "";
        if (timeOfDay >= 0 && timeOfDay < 12) {
            text = "Morning Ride";
        } else if (timeOfDay >= 12 && timeOfDay < 16) {
            text = "Afternoon Ride";
        } else if (timeOfDay >= 16 && timeOfDay < 21) {
            text = "Evening Ride";
        } else if (timeOfDay >= 21 && timeOfDay < 24) {
            text = "Night Ride";
        }

        input.setText(text);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_CORRECT);
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                exerciseName = input.getText().toString();
                if (mRequestingLocationUpdates && !canClose) {
                    canClose = true;
                    myThread.interrupt();
                    savingTxt.setTextColor(Color.LTGRAY);
                    savingTxt.setText("Saving...");
                    endDate = new Date();
                    exerciseStarted = false;
                    if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                        return;
                    }
                    mMap.setMyLocationEnabled(false);
                    // addMark("End");
                    //  points.add(new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude()));

                    totalSpeed += mCurrentLocation.getSpeed();

                    mapRedraw();
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    mStartUpdatesButton.setEnabled(false);
                    mRequestingLocationUpdates = false;
                    stopLocationUpdates();



                    /*The METS values are provided by "The Compendium of Physical Activities 2011".

                    Energy Expended (kcal) = MET x Body Weight (kg) x Time (h)
                    bicycling, 12-13.9 mph, leisure, moderate effort  Met = 8.0
                    Data for this version: Male 75 kilos, 23 years old , 175 cm height
                    */
                    // exercise duration is in seconds
                    double durationHours = ((double) currentDuration / 3600);
                    double calories = 8 * 75 * durationHours;
                    LatLng initPoint = points.get(0);
                    LatLng midPoint = points.get((points.size() / 2));
                    LatLngBounds.Builder latbuilder = new LatLngBounds.Builder();
                    latbuilder.include(initPoint);
                    latbuilder.include(midPoint);
                    latbuilder.include(new LatLng(mCurrentLocation.getLatitude(),
                            mCurrentLocation.getLongitude()));
                    LatLngBounds bounds = latbuilder.build();


                    //these calculations will be checked!!!
                    int zoom = getBoundsZoomLevel(bounds, width, width / 2);
                    MyUtils.INSTANCE.showToast(getApplicationContext(), exerciseName);


                    Exercise nExercise = new Exercise(mFirebaseUser.getUid(), totalElevationGain,
                            totalDistanceMeters, new Date().getTime(), exerciseName, mapSnapshotName,
                            currentDuration, totalSpeed / points.size(), calories, zoom - 2, currExerciseID,mFirebaseUser.getDisplayName(),mFirebaseUser.getPhotoUrl().getPath());
                    DatabaseReference postRef = mDatabase.child("exercises");
                    postRef.child(currExerciseID).setValue(nExercise);
                  //  mDatabase.child("user_wall").child(mFirebaseUser.getUid()).child(nExercise.getMyID()).setValue(nExercise);


                    //  CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, padding);
                    CameraPosition camPos = new CameraPosition.Builder()
                            .target(bounds.getCenter())
                            .zoom((float) (zoom - 2.5))
                            .build();

                    mMap.moveCamera(CameraUpdateFactory.newCameraPosition(camPos));

                }

                stopService(new Intent(getBaseContext(), RuningExerciseService.class));
                NotificationManager notificationManager =
                        (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                notificationManager.cancel(123123);

                snapAndClose();
            }
        });
        input.setSelection(input.length());


        builder.setOnCancelListener(new DialogInterface.OnCancelListener() {

            @Override
            public void onCancel(DialogInterface dialog) {
                if (!canClose) {
                    mStopUpdatesButton.setActivated(true);
                    mStopUpdatesButton.setVisibility(View.VISIBLE);
                    mTotalDistance.setVisibility(View.VISIBLE);
                    mSpeedView.setVisibility(View.VISIBLE);
                    mTotalTime.setVisibility(View.VISIBLE);
                    savingTxt.setVisibility(View.GONE);
                }

            }
        });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {

            @Override
            public void onDismiss(DialogInterface dialog) {
                if (!canClose) {
                    mStopUpdatesButton.setActivated(true);
                    mStopUpdatesButton.setVisibility(View.VISIBLE);
                    mTotalDistance.setVisibility(View.VISIBLE);
                    mSpeedView.setVisibility(View.VISIBLE);
                    mTotalTime.setVisibility(View.VISIBLE);
                    savingTxt.setVisibility(View.GONE);
                }


            }
        });

        //  builder.setCancelable(false);


        builder.show();


    }

    private void snapAndClose() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mapSnap();
                closeThis();
            }
        }, 1000);
    }


    private void makeNotification(Context context) {
        Intent intent = new Intent(context, RuningExerciseActivity.class);

        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                123123, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification.Builder builder = new Notification.Builder(context)
                .setContentTitle("New Exercise")
                .setContentText("Your exercise is runing.")
                .setContentIntent(pendingIntent)
                .setSmallIcon(R.drawable.bikedark)
                .setWhen(System.currentTimeMillis())
                .setUsesChronometer(true)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.bikeorange));

        Notification n = builder.build();
        n.flags |= Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT;

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(123123, n);
    }


    @Override
    public void onBackPressed()
    {

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
      //  super.onBackPressed();
    }

    /**
     * Requests location updates from the FusedLocationApi.
     */
    protected void startLocationUpdates() {
        // The final argument to {@code requestLocationUpdates()} is a LocationListener
        // (http://developer.android.com/reference/com/google/android/gms/location/LocationListener.html).
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
    }


    /**
     * Updates the latitude, the longitude, and the last location time in the UI
     * runing one time per second - 1 FPS
     */
    private void updateUI() {

        //mapRedraw();
        currentDuration++;
        Date d = new Date(currentDuration * 1000L);
        SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss"); // HH for 0-23
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        String timetoshow = df.format(d);

        mTotalTime.setText(String.format("%s: %s", "Time",
                timetoshow));

        int speed = (int) ((mCurrentLocation.getSpeed() * 3600) / 1000);
        mSpeedView.setText(String.format("%s: %s %s", "Speed",
                speed, "Km/h"));

        if (totalDistanceMeters < 1000) {
            double meters = Math.round(totalDistanceMeters * 100.0) / 100.0;
            mTotalDistance.setText(String.format("%s: %s %s", "Distance",
                    meters, " m"));
        } else {
            double kilometers = totalDistanceMeters / 1000;
            double kilometersRound = Math.round(kilometers * 100.0) / 100.0;
            mTotalDistance.setText(String.format("%s: %s %s", "Distance",
                    kilometersRound, " Km"));
        }

    }

    /**
     * Removes location updates from the FusedLocationApi.
     */
    protected void stopLocationUpdates() {
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();

    }

    @Override
    public void onResume() {
        super.onResume();
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }


    }

    @Override
    protected void onPause() {
        super.onPause();
        // Stop location updates to save battery, but don't disconnect the GoogleApiClient object.
        if (mGoogleApiClient.isConnected() && !exerciseStarted) {
            stopLocationUpdates();

        }
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    @Override
    protected void onDestroy() {

        //Close the Text to Speech Library

        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }


    // Called in a separate thread
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(Object object) {

        if (object instanceof ArrayList<?>) {
            ArrayList<?> list = (ArrayList<?>) object;

            if (list.get(0) instanceof LatLng) {
                this.points = (ArrayList<LatLng>) list;
                mapRedraw();

            } else if (list.get(0) instanceof MarkerOptions) {
                this.markers = (ArrayList<MarkerOptions>) list;
                mapRedraw();
            }
        } else if (object instanceof ExerciseDTO) {
            ExerciseDTO exerciseDTO = (ExerciseDTO) object;
            this.totalDistanceMeters = exerciseDTO.getcMeters();

        }

    }


    /**
     * Runs when a GoogleApiClient object successfully connects.
     */
    @Override
    public void onConnected(Bundle connectionHint) {
        Log.i(TAG, "Connected to GoogleApiClient");

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            MyUtils.INSTANCE.showToast(getApplicationContext(), "Location Permission failed");
            return;
        }

        if (mCurrentLocation == null) {

            mCurrentLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
            mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());
          if(mCurrentLocation != null) {
              LatLng current = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
              mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 18));

          }

            //updateUI();
        }


        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    /**
     * Callback that fires when the location changes.
     */
    @Override
    public void onLocationChanged(Location location) {


        mCurrentLocation = location;
        mLastUpdateTime = DateFormat.getTimeInstance().format(new Date());


        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            MyUtils.INSTANCE.showToast(getApplicationContext(), "Location Permission failed");
            return;
        }
        LatLng current = new LatLng(mCurrentLocation.getLatitude(), mCurrentLocation.getLongitude());
        mMap.setMyLocationEnabled(true);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(current, 18));


        //updateUI();
        //Toast.makeText(this, getResources().getString(R.string.location_updated_message), Toast.LENGTH_SHORT).show();


        if (lastLocation != null) {
            totalDistanceMeters += lastLocation.distanceTo(location);
            totalElevationGain = location.getAltitude() - lastLocation.getAltitude();
        }
        // points.add(new LatLng(location.getLatitude(), location.getLongitude()));

       /* ExerciseMapPoint ePoint = new ExerciseMapPoint(location.getLatitude(),location.getLongitude(),location.getAltitude(),
                location.getSpeed(), currExerciseID);
        mDatabase.child("map_points").push().setValue(ePoint);*/

        //points.add(new LatLng(location.getLatitude(), location.getLongitude()));
        mapRedraw();

    }


    @Override
    public void onConnectionSuspended(int cause) {
        // The connection to Google Play services was lost for some reason. We call connect() to
        // attempt to re-establish the connection.
        Log.i(TAG, "Connection suspended");
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = " + result.getErrorCode());
    }


    /**
     * Stores activity data in the Bundle.
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putBoolean(REQUESTING_LOCATION_UPDATES_KEY, mRequestingLocationUpdates);
        savedInstanceState.putParcelable(LOCATION_KEY, mCurrentLocation);
        savedInstanceState.putString(LAST_UPDATED_TIME_STRING_KEY, mLastUpdateTime);
        super.onSaveInstanceState(savedInstanceState);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            MyUtils.INSTANCE.showToast(getApplicationContext(), "Location Permission failed");
            return;
        }
        mMap.setMyLocationEnabled(true);

    }


    private static Bitmap scaleBitmap(Bitmap bitmap, int adjustwidth, int adjustHeight, int wantedWidth, int wantedHeight) {

        //optimized later
        Bitmap resizedbitmap1 = Bitmap.createBitmap(bitmap, adjustwidth, adjustHeight, wantedWidth, wantedHeight);
        return resizedbitmap1;

    }

    private void mapSnap() {
        FirebaseStorage storage = FirebaseStorage.getInstance();
        StorageReference storageRef = storage.getReferenceFromUrl("gs://ridebuddy-cb318.appspot.com");
        final StorageReference imagesRef = storageRef.child("images");

        mMap.snapshot(new GoogleMap.SnapshotReadyCallback() {
            public void onSnapshotReady(Bitmap bitmap) {

                // 1440-800 = 640  640/2=320, this is start point

                Double cut = (displayWidth - (displayWidth * 0.5)) / 2;
                Double height = (displayWidth * 0.5);

                Bitmap newbit = scaleBitmap(bitmap, 0, cut.intValue(), displayWidth, height.intValue());
                newbit.compress(Bitmap.CompressFormat.JPEG, 90, out);


                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                newbit.compress(Bitmap.CompressFormat.JPEG, 70, baos);
                byte[] data = baos.toByteArray();

                StorageReference mountainsRef = imagesRef.child(mapSnapshotName + ".jpg");
                UploadTask uploadTask = mountainsRef.putBytes(data);
                uploadTask.addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception exception) {
                        // Handle unsuccessful uploads
                    }
                }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // taskSnapshot.getMetadata() contains file metadata such as size, content-type, and download URL.
                        Uri downloadUrl = taskSnapshot.getDownloadUrl();
                    }
                });

                canClose = true;


            }
        });
    }

    private void mapRedraw() {
        mMap.clear();
        PolylineOptions options = new PolylineOptions().width(14).color(Color.RED).geodesic(true);
        for (LatLng latng : points) {
            options.add(latng);
        }
        for (MarkerOptions mark : markers) {
            mMap.addMarker(mark);
            // MyUtils.INSTANCE.showToast(getApplicationContext(),mark.getTitle());
        }
        mMap.addPolyline(options);
        //  lastLocation = location;
        //  totalSpeed += location.getSpeed();

    }

    public int getBoundsZoomLevel(LatLngBounds bounds, int mapWidthPx, int mapHeightPx) {

        LatLng ne = bounds.northeast;
        LatLng sw = bounds.southwest;

        double latFraction = (latRad(ne.latitude) - latRad(sw.latitude)) / Math.PI;

        double lngDiff = ne.longitude - sw.longitude;
        double lngFraction = ((lngDiff < 0) ? (lngDiff + 360) : lngDiff) / 360;

        double latZoom = zoom(mapHeightPx, WORLD_PX_HEIGHT, latFraction);
        double lngZoom = zoom(mapWidthPx, WORLD_PX_WIDTH, lngFraction);

        int result = Math.min((int) latZoom, (int) lngZoom);
        return Math.min(result, ZOOM_MAX);
    }

    private double latRad(double lat) {
        double sin = Math.sin(lat * Math.PI / 180);
        double radX2 = Math.log((1 + sin) / (1 - sin)) / 2;
        return Math.max(Math.min(radX2, Math.PI), -Math.PI) / 2;
    }

    private double zoom(int mapPx, int worldPx, double fraction) {
        return Math.floor(Math.log(mapPx / worldPx / fraction) / LN2);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            if (mRequestingLocationUpdates)
                MyUtils.INSTANCE.showToast(this, "ERROR, Service not implemented! ");
            finish();
            return true;
        }
        return false;
    }


}
