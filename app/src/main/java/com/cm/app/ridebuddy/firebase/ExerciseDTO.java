package com.cm.app.ridebuddy.firebase;



public class ExerciseDTO {

    private  float cMeters;

    public ExerciseDTO(float cMeters) {
        this.cMeters = cMeters;
    }

    public float getcMeters() {
        return cMeters;
    }

    public void setcMeters(float cMeters) {
        this.cMeters = cMeters;
    }
}
