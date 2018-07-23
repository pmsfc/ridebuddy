package com.cm.app.ridebuddy.firebase;



public class Friend {

    public String uid;
    public long date;
    public String userName;
    public String photo;

    public Friend() {
    }

    public Friend(String photo, String uid, long date, String userName) {
        this.photo = photo;
        this.uid = uid;
        this.date = date;
        this.userName = userName;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public long getDate() {
        return date;
    }

    public void setDate(long date) {
        this.date = date;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPhoto() {
        return photo;
    }

    public void setPhoto(String photo) {
        this.photo = photo;
    }
}
