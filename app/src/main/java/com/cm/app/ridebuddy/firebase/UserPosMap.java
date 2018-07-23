package com.cm.app.ridebuddy.firebase;


public class UserPosMap {

    private double latitude;
    private double longitude;
    private String userID;
    private String userName;

    public UserPosMap() {
    }

    public UserPosMap(double latitude, double longitude, String userID, String userName) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.userID = userID;
        this.userName = userName;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
