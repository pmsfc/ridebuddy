package com.cm.app.ridebuddy.firebase;


public class Exercise {

    public String gID;
    public double elevationGain;
    public float totalDistance;
    public long date;
    public String name;
    public String snapshotname;
    public long duration;
    public float avgSpeed;
    public double calories;
    public int mapZoom;
    public String myID;
    public String userName;
    public String photoURL;

    public Exercise() {
    }

    public Exercise(String gID, double elevationGain, float totalDistance, long date, String name, String snapshotname, long duration, float avgSpeed, double calories, int mapZoom, String myID, String userName, String photoURL) {
        this.gID = gID;
        this.elevationGain = elevationGain;
        this.totalDistance = totalDistance;
        this.date = date;
        this.name = name;
        this.snapshotname = snapshotname;
        this.duration = duration;
        this.avgSpeed = avgSpeed;
        this.calories = calories;
        this.mapZoom = mapZoom;
        this.myID = myID;
        this.userName = userName;
        this.photoURL = photoURL;
    }

    public String getgID() {
        return gID;
    }

    public void setgID(String gID) {
        this.gID = gID;
    }

    public double getElevationGain() {
        return elevationGain;
    }

    public void setElevationGain(double elevationGain) {
        this.elevationGain = elevationGain;
    }

    public float getTotalDistance() {
        return totalDistance;
    }

    public void setTotalDistance(float totalDistance) {
        this.totalDistance = totalDistance;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSnapshotname() {
        return snapshotname;
    }

    public void setSnapshotname(String snapshotname) {
        this.snapshotname = snapshotname;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public float getAvgSpeed() {
        return avgSpeed;
    }

    public void setAvgSpeed(float avgSpeed) {
        this.avgSpeed = avgSpeed;
    }

    public double getCalories() {
        return calories;
    }

    public void setCalories(double calories) {
        this.calories = calories;
    }

    public int getMapZoom() {
        return mapZoom;
    }

    public void setMapZoom(int mapZoom) {
        this.mapZoom = mapZoom;
    }

    public String getMyID() {
        return myID;
    }

    public void setMyID(String myID) {
        this.myID = myID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhotoURL() {
        return photoURL;
    }

    public void setPhotoURL(String photoURL) {
        this.photoURL = photoURL;
    }
}