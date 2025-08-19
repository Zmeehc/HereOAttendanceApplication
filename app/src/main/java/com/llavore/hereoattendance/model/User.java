package com.llavore.hereoattendance.model;

public class User {
    private String idNumber;
    private String firstName;
    private String middleName;
    private String lastName;
    private String email;
    private String gender;
    private String birthdate;
    private String program;
    private String contactNumber;
    private String userType;
    private String profileImageUrl;
    private long createdAt;
    
    // Default constructor for Firebase
    public User() {}
    
    public User(String idNumber, String firstName, String middleName, String lastName, 
                String email, String gender, String birthdate, String program, String contactNumber, String userType) {
        this.idNumber = idNumber;
        this.firstName = firstName;
        this.middleName = middleName;
        this.lastName = lastName;
        this.email = email;
        this.gender = gender;
        this.birthdate = birthdate;
        this.program = program;
        this.contactNumber = contactNumber;
        this.userType = userType;
        this.createdAt = System.currentTimeMillis();
    }
    
    // Getters
    public String getIdNumber() { return idNumber; }
    public String getFirstName() { return firstName; }
    public String getMiddleName() { return middleName; }
    public String getLastName() { return lastName; }
    public String getEmail() { return email; }
    public String getGender() { return gender; }
    public String getBirthdate() { return birthdate; }
    public String getProgram() { return program; }
    public String getContactNumber() { return contactNumber; }
    public String getUserType() { return userType; }
    public String getProfileImageUrl() { return profileImageUrl; }
    public long getCreatedAt() { return createdAt; }
    
    // Setters
    public void setIdNumber(String idNumber) { this.idNumber = idNumber; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setMiddleName(String middleName) { this.middleName = middleName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEmail(String email) { this.email = email; }
    public void setGender(String gender) { this.gender = gender; }
    public void setBirthdate(String birthdate) { this.birthdate = birthdate; }
    public void setProgram(String program) { this.program = program; }
    public void setContactNumber(String contactNumber) { this.contactNumber = contactNumber; }
    public void setUserType(String userType) { this.userType = userType; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }
    
    // Helper method to get full name
    public String getFullName() {
        StringBuilder fullName = new StringBuilder();
        if (firstName != null && !firstName.isEmpty()) {
            fullName.append(firstName);
        }
        if (middleName != null && !middleName.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(middleName);
        }
        if (lastName != null && !lastName.isEmpty()) {
            if (fullName.length() > 0) fullName.append(" ");
            fullName.append(lastName);
        }
        return fullName.toString();
    }
} 