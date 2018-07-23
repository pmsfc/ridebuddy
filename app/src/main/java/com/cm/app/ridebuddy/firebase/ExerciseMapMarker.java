package com.cm.app.ridebuddy.firebase;



public class ExerciseMapMarker {

    private double Latitude;
    private double Longitude;
    private String markerName;
    private String exerciseID;
    private String userID;
    private String markerID;

    public ExerciseMapMarker() {
    }

    public ExerciseMapMarker(double latitude, double longitude, String markerName, String exerciseID, String userID, String markerID) {
        Latitude = latitude;
        Longitude = longitude;
        this.markerName = markerName;
        this.exerciseID = exerciseID;
        this.userID = userID;
        this.markerID = markerID;
    }

    public double getLatitude() {
        return Latitude;
    }

    public void setLatitude(double latitude) {
        Latitude = latitude;
    }

    public double getLongitude() {
        return Longitude;
    }

    public void setLongitude(double longitude) {
        Longitude = longitude;
    }

    public String getMarkerName() {
        return markerName;
    }

    public void setMarkerName(String markerName) {
        this.markerName = markerName;
    }

    public String getExerciseID() {
        return exerciseID;
    }

    public void setExerciseID(String exerciseID) {
        this.exerciseID = exerciseID;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getMarkerID() {
        return markerID;
    }

    public void setMarkerID(String markerID) {
        this.markerID = markerID;
    }
}