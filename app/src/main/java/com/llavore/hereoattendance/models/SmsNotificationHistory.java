package com.llavore.hereoattendance.models;

public class SmsNotificationHistory {
    private String studentEdpNumber;
    private String studentFirstName;
    private String studentLastName;
    private String studentProfileImageUrl;
    private String guardianContactNumber;
    private String message;
    private long timestamp;
    private String courseCode;
    private String courseName;

    // Default constructor required for Firebase
    public SmsNotificationHistory() {
    }

    public SmsNotificationHistory(String studentEdpNumber, String studentFirstName, String studentLastName, 
                                 String studentProfileImageUrl, String guardianContactNumber, String message, 
                                 long timestamp, String courseCode, String courseName) {
        this.studentEdpNumber = studentEdpNumber;
        this.studentFirstName = studentFirstName;
        this.studentLastName = studentLastName;
        this.studentProfileImageUrl = studentProfileImageUrl;
        this.guardianContactNumber = guardianContactNumber;
        this.message = message;
        this.timestamp = timestamp;
        this.courseCode = courseCode;
        this.courseName = courseName;
    }

    // Getters and Setters
    public String getStudentEdpNumber() {
        return studentEdpNumber;
    }

    public void setStudentEdpNumber(String studentEdpNumber) {
        this.studentEdpNumber = studentEdpNumber;
    }

    public String getStudentFirstName() {
        return studentFirstName;
    }

    public void setStudentFirstName(String studentFirstName) {
        this.studentFirstName = studentFirstName;
    }

    public String getStudentLastName() {
        return studentLastName;
    }

    public void setStudentLastName(String studentLastName) {
        this.studentLastName = studentLastName;
    }

    public String getStudentProfileImageUrl() {
        return studentProfileImageUrl;
    }

    public void setStudentProfileImageUrl(String studentProfileImageUrl) {
        this.studentProfileImageUrl = studentProfileImageUrl;
    }

    public String getGuardianContactNumber() {
        return guardianContactNumber;
    }

    public void setGuardianContactNumber(String guardianContactNumber) {
        this.guardianContactNumber = guardianContactNumber;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getCourseCode() {
        return courseCode;
    }

    public void setCourseCode(String courseCode) {
        this.courseCode = courseCode;
    }

    public String getCourseName() {
        return courseName;
    }

    public void setCourseName(String courseName) {
        this.courseName = courseName;
    }

    public String getStudentFullName() {
        StringBuilder fullName = new StringBuilder();
        if (studentFirstName != null && !studentFirstName.isEmpty()) {
            fullName.append(studentFirstName);
        }
        if (studentLastName != null && !studentLastName.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(studentLastName);
        }
        return fullName.toString();
    }
}




