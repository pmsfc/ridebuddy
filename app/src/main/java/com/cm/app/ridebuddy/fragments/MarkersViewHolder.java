package com.cm.app.ridebuddy.fragments;

import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.cm.app.ridebuddy.R;
import com.cm.app.ridebuddy.firebase.ExerciseMapMarker;



public class MarkersViewHolder extends RecyclerView.ViewHolder {
    public final View mView;
    public final TextView mNameView;
    public final ImageView mThumbView;

    public ExerciseMapMarker mItem;

    public MarkersViewHolder(View view) {
        super(view);
        mView = view;
        mNameView = (TextView) view.findViewById(R.id.name);
        mThumbView = (ImageView) view.findViewById(R.id.thumb);

    }

    @Override
    public String toString() {
        return super.toString() + " '" + mNameView.getText() + "'";
    }
}