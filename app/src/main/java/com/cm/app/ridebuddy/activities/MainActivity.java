package com.cm.app.ridebuddy.activities;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.auxiliary.MyUtils;
import com.cm.app.ridebuddy.auxiliary.UserSession;
import com.cm.app.ridebuddy.currentexercise.RuningExerciseActivity;
import com.cm.app.ridebuddy.firebase.Exercise;
import com.cm.app.ridebuddy.fragments.ContactsListFragment;
import com.cm.app.ridebuddy.fragments.MyMarkersFragment;
import com.cm.app.ridebuddy.fragments.MyStatsFragment;
import com.cm.app.ridebuddy.fragments.WallFragment;
import com.cm.app.ridebuddy.processing.CircleTransform;
import com.google.android.gms.appinvite.AppInviteInvitation;
import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;

import static com.cm.app.ridebuddy.activities.ViewExerciseActivity.EXTRA_EX_ID;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener,
        GoogleApiClient.OnConnectionFailedListener,
        WallFragment.OnListFragmentInteractionListener,
        ContactsListFragment.OnContactsInteractionListener {

    private View navHeader;
    private UserSession userSession;
    private GoogleApiClient mGoogleApiClient;
    private boolean doubleBackToExitPressedOnce = false;

    private boolean permissionToRecordAccepted = false;
    private boolean permissionToWriteAccepted = false;
    private String[] permissions = {
            "android.permission.RECORD_AUDIO",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.MODIFY_AUDIO_SETTINGS",
            "android.permission.INTERNET",
            "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION",
            "android.permission.READ_CONTACTS",
            "android.permission.WRITE_CONTACTS",
            "android.permission.READ_PROFILE",
            "android.permission.GET_ACCOUNTS",
            "android.permission.SEND_SMS",
            "android.permission.WAKE_LOCK",
            "android.permission.WRITE_SETTINGS",
            "android.permission.CALL_PHONE",
            "android.permission.WRITE_EXTERNAL_STORAGE",
            "android.permission.READ_EXTERNAL_STORAGE"};
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseUser;


    /*
    this is needed after android 6
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case 200:
                permissionToRecordAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                permissionToWriteAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                break;
        }
        if (!permissionToRecordAccepted) MainActivity.super.finish();
        if (!permissionToWriteAccepted) MainActivity.super.finish();

    }


    @Override
    public void onBackPressed() {

        Fragment f = getSupportFragmentManager().findFragmentById(R.id.content_main);
        if (f instanceof WallFragment) {//the fragment on which you want to handle your back press

            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
                Intent a = new Intent(Intent.ACTION_MAIN);
                a.addCategory(Intent.CATEGORY_HOME);
                a.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(a);
            }

            Log.i("BACK PRESSED", "BACK PRESSED");
            setTitle("Ridebuddy Wall");

            this.doubleBackToExitPressedOnce = true;
            MyUtils.INSTANCE.showToast(this, "Please click BACK again to exit");

            new Handler().postDelayed(new Runnable() {

                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);

        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        setTheme(R.style.AppTheme);
        super.onCreate(savedInstanceState);
        setTheme(R.style.AppTheme_NoActionBar);
        setContentView(R.layout.activity_main);
        //FirebaseDatabase.getInstance().setPersistenceEnabled(true);
        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseUser = mFirebaseAuth.getCurrentUser();


        userSession = new UserSession(this);
        if (mFirebaseUser == null) {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
            finish();
        } else {

            setContentView(R.layout.activity_main);

            GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestEmail()
                    .build();
            mGoogleApiClient = new GoogleApiClient.Builder(this)
                    .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                    .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                    .build();

            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);


            FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
            fab.setImageResource(R.drawable.bike);
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    //onclick plus button
                    startActivity(new Intent(getApplicationContext(), RuningExerciseActivity.class));

                }
            });


            DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.setDrawerListener(toggle);
            toggle.syncState();

            setNavHeader();

            setTitle("Ridebuddy Wall");
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_main, new WallFragment());
            ft.commit();


            // Add the following code to your onCreate
            int requestCode = 200;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(permissions, requestCode);
            }

        }
    }


    private void setNavHeader() {
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        navigationView.setCheckedItem(R.id.nav_wall);
        navHeader = navigationView.getHeaderView(0);
        // Navigation header


        HashMap<String, String> userDetails = userSession.getUserDetails();

        String username = mFirebaseUser.getDisplayName();
        String imageUrl = mFirebaseUser.getPhotoUrl().getPath();

        // String username = "ok";
        //String imageUrl = "ok";
        TextView txtName = (TextView) navHeader.findViewById(R.id.username_extra);
        //  TextView txtEmail = (TextView) navHeader.findViewById(R.id.mail_extra);


        txtName.setText(username);
        //  txtEmail.setText(intent.getStringExtra(SignInActivity.EXTRA_EMAIL));
        ImageView imgProfile = (ImageView) navHeader.findViewById(R.id.imageProfile);
        // Toast.makeText(RuningExerciseActivity.this, intent.getStringExtra(SignInActivity.EXTRA_IMGURL),Toast.LENGTH_LONG).show();

        Glide.with(this).load("https://lh4.googleusercontent.com" + imageUrl)
                .crossFade()
                .thumbnail(0.5f)
                .bitmapTransform(new CircleTransform(this))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imgProfile);
    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        // Log.d(TAG, "onConnectionFailed:" + connectionResult);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void setActionBarTitle(String title) {
        setTitle(title);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_wall) {
            setTitle("RideBuddy Wall");

            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_main, new WallFragment());
            ft.addToBackStack(null).commit();

        } else if (id == R.id.nav_friends) {
           /*  setTitle("Friends List");
             FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
             ft.replace(R.id.content_main, new ContactsListFragment());
             ft.addToBackStack(null).commit();*/
            startActivity(new Intent(getApplicationContext(), MyFriendsActivity.class));

        } else if (id == R.id.nav_fullmap_friends) {
            startActivity(new Intent(getApplicationContext(), FriendsOnMapActivity.class));

        } else if (id == R.id.nav_fullmap_markers) {
            startActivity(new Intent(getApplicationContext(), AllMarkersMapActivity.class));

        } /*else if (id == R.id.nav_manage) {
            startActivity(new Intent(getApplicationContext(), SettingsActivity.class));

        } */ else if (id == R.id.nav_my_markers) {

            setTitle("My Markers List");
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
            ft.replace(R.id.content_main, new MyMarkersFragment());
            ft.addToBackStack(null).commit();


        }else if (id == R.id.nav_stats) {

                setTitle("My Stats");
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.content_main, new MyStatsFragment());
                ft.addToBackStack(null).commit();


        } else if (id == R.id.nav_invite) {

            sendInvitation();

        } else if (id == R.id.nav_logout) {

            mFirebaseAuth.signOut();
            Auth.GoogleSignInApi.signOut(mGoogleApiClient);
            userSession.logoutUser();
            startActivity(new Intent(this, SignInActivity.class));

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


    @Override
    public void onContactSelected(Uri contactUri) {
        Intent intent = new Intent(this, ContactDetailActivity.class);
        intent.setData(contactUri);
        startActivity(intent);

    }

    @Override
    public void onSelectionCleared() {

    }

    @Override
    public void onListFragmentInteraction(Exercise item) {
        Intent intent = new Intent(this, ViewExerciseActivity.class);
        intent.putExtra(EXTRA_EX_ID, item.getMyID() + "");
        startActivity(intent);

    }

    private void sendInvitation() {
        Intent intent = new AppInviteInvitation.IntentBuilder("Ridebuddy, your cycling companion!")
                .setMessage("Check out this awesome cycling app, it even detects if you have fallen!")
                .setCallToActionText("okay")
                .build();
        startActivityForResult(intent, 123);
    }


}
