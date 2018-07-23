package com.cm.app.ridebuddy.auxiliary;


import android.content.Context;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.widget.Toast;


/**
 * Thread safe singleton
 */
public enum MyUtils {
    INSTANCE;



    private String GID;

    public String getGID() {
        return GID;
    }

    public void setGID(String GID) {
        this.GID = GID;
    }

    MyUtils() {
    }



    public void showToast(Context context, String txt){
        Toast.makeText(context, txt, Toast.LENGTH_SHORT).show();
    }

    public void showSnackbar(View view, String txt){
        Snackbar.make(view, txt,Snackbar.LENGTH_INDEFINITE ).setAction("Action", null).setDuration(2000).show();
    }



}
