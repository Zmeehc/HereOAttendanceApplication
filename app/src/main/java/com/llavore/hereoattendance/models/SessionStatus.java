package com.llavore.hereoattendance.models;

public class SessionStatus {
    private String date;
    private String status;

    public SessionStatus() {
        // Default constructor required for Firebase
    }

    public SessionStatus(String date, String status) {
        this.date = date;
        this.status = status;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
