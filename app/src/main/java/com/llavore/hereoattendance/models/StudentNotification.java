package com.llavore.hereoattendance.models;

import java.util.Date;

public class StudentNotification {
    public enum NotificationType {
        WARNING,    // 2 absences
        ALERT       // 3+ absences
    }

    private String id;
    private String studentId;
    private String courseId;
    private String courseCode;
    private String courseName;
    private String courseSchedule;
    private String teacherId;
    private String teacherName;
    private String teacherProfileImageUrl;
    private NotificationType type;
    private int absenceCount;
    private String title;
    private String message;
    private Date dateCreated;
    private boolean isRead;
    private long timestamp;

    // Default constructor required for Firebase
    public StudentNotification() {}

    public StudentNotification(String id, String studentId, String courseId, String courseCode, 
                              String courseName, String courseSchedule, String teacherId, 
                              String teacherName, String teacherProfileImageUrl, 
                              NotificationType type, int absenceCount, String title, 
                              String message, boolean isRead) {
        this.id = id;
        this.studentId = studentId;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.courseSchedule = courseSchedule;
        this.teacherId = teacherId;
        this.teacherName = teacherName;
        this.teacherProfileImageUrl = teacherProfileImageUrl;
        this.type = type;
        this.absenceCount = absenceCount;
        this.title = title;
        this.message = message;
        this.isRead = isRead;
        this.timestamp = System.currentTimeMillis();
        this.dateCreated = new Date();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getStudentId() { return studentId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getCourseSchedule() { return courseSchedule; }
    public void setCourseSchedule(String courseSchedule) { this.courseSchedule = courseSchedule; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }

    public String getTeacherProfileImageUrl() { return teacherProfileImageUrl; }
    public void setTeacherProfileImageUrl(String teacherProfileImageUrl) { this.teacherProfileImageUrl = teacherProfileImageUrl; }

    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }

    public int getAbsenceCount() { return absenceCount; }
    public void setAbsenceCount(int absenceCount) { this.absenceCount = absenceCount; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public Date getDateCreated() { return dateCreated; }
    public void setDateCreated(Date dateCreated) { this.dateCreated = dateCreated; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    // Helper methods
    public String getFormattedDate() {
        if (dateCreated != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault());
            return sdf.format(dateCreated);
        }
        return "";
    }
    
    public String getFormattedTime() {
        if (dateCreated != null) {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
            return sdf.format(dateCreated);
        }
        return "";
    }

    public String getCourseInfo() {
        return courseName + " | " + courseSchedule;
    }

    public static String getWarningTitle(int absenceCount) {
        return "Warning: You have " + absenceCount + " consecutive absences";
    }

    public static String getAlertTitle(int absenceCount) {
        return "3 Consecutive Absences Detected!";
    }

    public static String getWarningMessage() {
        return "This is a reminder that you have accumulated two (2) consecutive absences. " +
               "Please be advised that a third consecutive absence may result in a violation and require disciplinary action. " +
               "We strongly encourage you to attend your classes regularly and communicate with your instructor if you are experiencing any difficulties.";
    }

    public static String getAlertMessage() {
        return "This is to inform you that you have incurred three (3) consecutive absences. " +
               "You are required to report to the Office of the Director of Student Affairs (DSA) at your earliest convenience. " +
               "Please ensure that you settle your attendance violation prior to resuming your classes to avoid further disciplinary measures.";
    }
}
