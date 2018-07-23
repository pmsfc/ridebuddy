package com.cm.app.ridebuddy.fragments;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.firebase.Exercise;

public class ExerciseViewHolder extends RecyclerView.ViewHolder {
    public final View mView;
    public final TextView mNameView;
    public final ImageView mThumbView;
    public final TextView mExerciseNameView;
    public final TextView mMetaView;
    public final TextView mRideDataView;
    public final ImageView mRideImgView;
    public Exercise mItem;
    public Button undoButton;

    public ExerciseViewHolder(View view) {
        super(view);
        mView = view;
        mNameView = (TextView) view.findViewById(R.id.name);
        mThumbView = (ImageView) view.findViewById(R.id.thumb);
        mExerciseNameView = (TextView) view.findViewById(R.id.txt);
        mMetaView = (TextView) view.findViewById(R.id.meta);
        mRideDataView = (TextView) view.findViewById(R.id.ride_data);
        mRideImgView = (ImageView) view.findViewById(R.id.map_image);
    }

    @Override
    public String toString() {
        return super.toString() + " '" + mNameView.getText() + "'";
    }
}