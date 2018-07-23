package com.cm.app.ridebuddy.fragments;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.firebase.User;

public class UserViewHolder extends RecyclerView.ViewHolder {
    public final View mView;
    public final TextView mNameView;
    public final ImageView mThumbView;
    public final Button myButton;

    public User mItem;
    public Button undoButton;

    public UserViewHolder(View view) {
        super(view);
        mView = view;
        mNameView = (TextView) view.findViewById(R.id.name);
        mThumbView = (ImageView) view.findViewById(R.id.thumb);
        myButton = (Button) view.findViewById(R.id.button2);

    }

    @Override
    public String toString() {
        return super.toString() + " '" + mNameView.getText() + "'";
    }
}