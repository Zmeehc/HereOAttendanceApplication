package com.llavore.hereoattendance.models;

public class Course {
    public String id;
    public String name;
    public String room;
    public String scheduleDays; // e.g., M-W-F
    public String startTime;    // e.g., 8:30 AM
    public String endTime;      // e.g., 10:00 AM
    public String lateAfter;    // e.g., 10:15 AM
    public String code;         // generated code
    public String teacherId;    // ID of the teacher who created this course
    public String teacherFirstName;  // Teacher's first name (cached for faster loading)
    public String teacherLastName;   // Teacher's last name (cached for faster loading)
    public String teacherProfileImageUrl;  // Teacher's profile image URL (cached for faster loading)
    public int studentCount;
    public int sessionCount;

    public Course() {}
}

