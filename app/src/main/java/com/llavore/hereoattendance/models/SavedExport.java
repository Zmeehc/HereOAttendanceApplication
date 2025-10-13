package com.llavore.hereoattendance.models;

public class SavedExport {
    public String id;
    public String fileName;
    public String courseCode;
    public String courseName;
    public String startDate;
    public String endDate;
    public String dateRange;
    public String filePath;
    public long timestamp;
    public String teacherId;

    public SavedExport() {}

    public SavedExport(String id, String fileName, String courseCode, String courseName, 
                      String startDate, String endDate, String dateRange, String filePath, 
                      long timestamp, String teacherId) {
        this.id = id;
        this.fileName = fileName;
        this.courseCode = courseCode;
        this.courseName = courseName;
        this.startDate = startDate;
        this.endDate = endDate;
        this.dateRange = dateRange;
        this.filePath = filePath;
        this.timestamp = timestamp;
        this.teacherId = teacherId;
    }
}
