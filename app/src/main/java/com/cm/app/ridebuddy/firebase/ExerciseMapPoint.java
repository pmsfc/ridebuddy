package com.cm.app.ridebuddy.firebase;


public class ExerciseMapPoint {

    public double latitude;
    public double longitude;
    public double altitude;
    public float speed;
    public String exerciseID;

    public ExerciseMapPoint(double latitude, double longitude, double altitude, float speed, String exerciseID) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.altitude = altitude;
        this.speed = speed;
        this.exerciseID = exerciseID;
    }

    public ExerciseMapPoint() {
    }

    public String getExerciseID() {
        return exerciseID;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public double getAltitude() {
        return altitude;
    }

    public float getSpeed() {
        return speed;
    }

}