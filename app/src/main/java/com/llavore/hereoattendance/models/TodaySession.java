package com.llavore.hereoattendance.models;

public class TodaySession {
    private String courseId;
    private String courseCode;
    private String courseName;
    private String room;
    private String sessionName;
    private String startTime;
    private String endTime;
    private String lateTime;
    private String scheduleDays;

    // Default constructor required for Firebase
    public TodaySession() {}

    public TodaySession(String courseId, String courseCode, String courseName, String room, 
                       String sessionName, String startTime, String endTime, String lateTime, String scheduleDays) {
        this.courseId = courseId;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.room = room;
        this.sessionName = sessionName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.lateTime = lateTime;
        this.scheduleDays = scheduleDays;
    }

    // Getters and Setters
    public String getCourseId() { return courseId; }
    public void setCourseId(String courseId) { this.courseId = courseId; }

    public String getCourseCode() { return courseCode; }
    public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

    public String getCourseName() { return courseName; }
    public void setCourseName(String courseName) { this.courseName = courseName; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getSessionName() { return sessionName; }
    public void setSessionName(String sessionName) { this.sessionName = sessionName; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public String getLateTime() { return lateTime; }
    public void setLateTime(String lateTime) { this.lateTime = lateTime; }

    public String getScheduleDays() { return scheduleDays; }
    public void setScheduleDays(String scheduleDays) { this.scheduleDays = scheduleDays; }

    public String getCourseInfo() {
        return courseName + " | " + room;
    }

    public String getFormattedStartTime() {
        return "Start: " + startTime;
    }

    public String getFormattedLateTime() {
        return "Late: " + lateTime;
    }

    public String getFormattedEndTime() {
        return "End: " + endTime;
    }
}
