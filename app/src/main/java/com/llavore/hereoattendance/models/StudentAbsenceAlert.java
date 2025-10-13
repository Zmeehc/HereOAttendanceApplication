package com.llavore.hereoattendance.models;

import java.util.List;

public class StudentAbsenceAlert {
    private String edpNumber;
    private String firstName;
    private String lastName;
    private String profileImageUrl;
    private int totalAbsences;
    private List<CourseAbsence> courseAbsences;

    // Default constructor required for Firebase
    public StudentAbsenceAlert() {}

    public StudentAbsenceAlert(String edpNumber, String firstName, String lastName, 
                              String profileImageUrl, int totalAbsences, List<CourseAbsence> courseAbsences) {
        this.edpNumber = edpNumber;
        this.firstName = firstName;
        this.lastName = lastName;
        this.profileImageUrl = profileImageUrl;
        this.totalAbsences = totalAbsences;
        this.courseAbsences = courseAbsences;
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

    public int getTotalAbsences() { return totalAbsences; }
    public void setTotalAbsences(int totalAbsences) { this.totalAbsences = totalAbsences; }

    public List<CourseAbsence> getCourseAbsences() { return courseAbsences; }
    public void setCourseAbsences(List<CourseAbsence> courseAbsences) { this.courseAbsences = courseAbsences; }

    public String getFullName() {
        return (firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "");
    }

    // Inner class for course-specific absence information
    public static class CourseAbsence {
        private String courseCode;
        private String courseName;
        private String courseSchedule;
        private int absenceCount;

        public CourseAbsence() {}

        public CourseAbsence(String courseCode, String courseName, String courseSchedule, int absenceCount) {
            this.courseCode = courseCode;
            this.courseName = courseName;
            this.courseSchedule = courseSchedule;
            this.absenceCount = absenceCount;
        }

        // Getters and Setters
        public String getCourseCode() { return courseCode; }
        public void setCourseCode(String courseCode) { this.courseCode = courseCode; }

        public String getCourseName() { return courseName; }
        public void setCourseName(String courseName) { this.courseName = courseName; }

        public String getCourseSchedule() { return courseSchedule; }
        public void setCourseSchedule(String courseSchedule) { this.courseSchedule = courseSchedule; }

        public int getAbsenceCount() { return absenceCount; }
        public void setAbsenceCount(int absenceCount) { this.absenceCount = absenceCount; }
    }
}
