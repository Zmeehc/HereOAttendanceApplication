package com.llavore.hereoattendance.models;

public class AttendanceRecord {
    private String edpNumber;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private String timeIn;
    private String status; // PRESENT, LATE, ABSENT
    private long timestamp;

    // Default constructor required for Firebase
    public AttendanceRecord() {}

    public AttendanceRecord(String edpNumber, String firstName, String lastName, 
                           String profileImageUrl, String timeIn, String status) {
        this.edpNumber = edpNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileImageUrl = profileImageUrl;
        this.timeIn = timeIn;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getEdpNumber() { return edpNumber; }
    public void setEdpNumber(String edpNumber) { this.edpNumber = edpNumber; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }

    public String getTimeIn() { return timeIn; }
    public void setTimeIn(String timeIn) { this.timeIn = timeIn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }
}
