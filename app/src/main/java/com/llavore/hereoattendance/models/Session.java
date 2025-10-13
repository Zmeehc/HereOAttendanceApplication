package com.llavore.hereoattendance.models;

public class Session {
    private String id;
    private String courseId;
    private String courseCode;
    private String sessionName;
    private String classStartTime;
    private String classEndTime;
    private String lateAttendanceTime;
    private String date; // Format: "yyyy-MM-dd"
    private String teacherId;
    private long createdAt;
    private boolean isActive;

    // Default constructor required for Firebase
    public Session() {}

    public Session(String id, String courseId, String courseCode, String sessionName, 
                   String classStartTime, String classEndTime, String lateAttendanceTime, 
                   String date, String teacherId) {
        this.id = id;
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.sessionName = sessionName;
        this.classStartTime = classStartTime;
        this.classEndTime = classEndTime;
        this.lateAttendanceTime = lateAttendanceTime;
        this.date = date;
        this.teacherId = teacherId;
        this.createdAt = System.currentTimeMillis();
        this.isActive = true;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public String getClassStartTime() { return classStartTime; }
    public void setClassStartTime(String classStartTime) { this.classStartTime = classStartTime; }

    public String getClassEndTime() { return classEndTime; }
    public void setClassEndTime(String classEndTime) { this.classEndTime = classEndTime; }

    public String getLateAttendanceTime() { return lateAttendanceTime; }
    public void setLateAttendanceTime(String lateAttendanceTime) { this.lateAttendanceTime = lateAttendanceTime; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTeacherId() { return teacherId; }
    public void setTeacherId(String teacherId) { this.teacherId = teacherId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}
