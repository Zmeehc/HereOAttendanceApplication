package com.llavore.hereoattendance.teacher;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.StudentsPresentAdapter;
import com.llavore.hereoattendance.models.AttendanceRecord;
import com.llavore.hereoattendance.models.Session;
import com.llavore.hereoattendance.utils.TransitionManager;

import java.util.ArrayList;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CourseDetails extends AppCompatActivity {

    private TextView courseTitleDetail, courseScheduleDetail, classCodeDetail;
    private TextView studentsTextDetail, sessionsTextDetail;
    private TextView dateTextDetail, dayTextDetail;
    private ImageView backArrow, dateDropdownIcon;
    private Timer dateUpdateTimer;
    private MaterialButton btnSetAttendance, btnScanQR, btnViewRecord, btnEndAttendance;
    private RecyclerView studentsPresentRecyclerView;
    private DatabaseReference mDatabase;
    private String courseId;
    private String courseCode;
    private boolean hasSessionForToday = false;
    private boolean attendanceEnded = false;
    private String selectedDate; // Format: yyyy-MM-dd
    private boolean isArchived = false;
    
    // Attendance tracking
    private List<AttendanceRecord> presentStudents;
    private StudentsPresentAdapter studentsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_course_details);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });



        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        
        initializeViews();
        loadCourseData();
        setCurrentDate();
        restoreAttendanceState();
        handleArchivedCourseState();
        setupBackButton();
        startDateUpdateTimer();
        setupRealTimeStudentCountListener();
        setupRealTimeSessionCountListener();
        checkSessionForDate(selectedDate);
        setupStudentsRecyclerView();
        loadAttendanceForDate(selectedDate);
        restoreAttendanceState();

        btnSetAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(CourseDetails.this, SetAttendanceActivity.class);
            // Pass all course data to SetAttendanceActivity
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseCode", courseCode);
            intent.putExtra("courseName", getIntent().getStringExtra("courseName"));
            intent.putExtra("courseRoom", getIntent().getStringExtra("courseRoom"));
            intent.putExtra("courseSchedule", getIntent().getStringExtra("courseSchedule"));
            intent.putExtra("courseStartTime", getIntent().getStringExtra("courseStartTime"));
            intent.putExtra("courseEndTime", getIntent().getStringExtra("courseEndTime"));
            intent.putExtra("courseStudentCount", getIntent().getIntExtra("courseStudentCount", 0));
            intent.putExtra("courseSessionCount", getIntent().getIntExtra("courseSessionCount", 0));
            intent.putExtra("selectedDate", selectedDate);
            startActivity(intent);
        });
        
        // Setup QR scan button
        btnScanQR.setOnClickListener(v -> {
            if (hasSessionForToday && !attendanceEnded) {
                // Navigate to QR scanner activity
                Intent intent = new Intent(CourseDetails.this, QRScannerActivity.class);
                intent.putExtra("courseId", courseId);
                intent.putExtra("courseCode", courseCode);
                intent.putExtra("selectedDate", selectedDate);
                startActivity(intent);
                android.util.Log.d("CourseDetails", "Navigating to QR scanner");
            } else if (attendanceEnded) {
                android.util.Log.d("CourseDetails", "QR scan button clicked - attendance session ended");
            } else {
                android.util.Log.d("CourseDetails", "QR scan button clicked - no session for today");
            }
        });
        
        btnViewRecord.setOnClickListener(v -> {
            // Navigate to attendance record activity
            Intent intent = new Intent(CourseDetails.this, AttendanceRecordActivity.class);
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseCode", courseCode);
            intent.putExtra("courseName", getIntent().getStringExtra("courseName"));
            intent.putExtra("courseRoom", getIntent().getStringExtra("courseRoom"));
            intent.putExtra("courseSchedule", getIntent().getStringExtra("courseSchedule"));
            intent.putExtra("courseStartTime", getIntent().getStringExtra("courseStartTime"));
            intent.putExtra("courseEndTime", getIntent().getStringExtra("courseEndTime"));
            intent.putExtra("courseStudentCount", getIntent().getIntExtra("courseStudentCount", 0));
            intent.putExtra("courseSessionCount", getIntent().getIntExtra("courseSessionCount", 0));
            intent.putExtra("selectedDate", selectedDate);
            intent.putExtra("isArchived", isArchived);
            startActivity(intent);
        });
        
        btnEndAttendance.setOnClickListener(v -> {
            // End attendance session - disable QR scanning
            endAttendanceSession();
        });
        
        // Setup date dropdown click listener
        dateDropdownIcon.setOnClickListener(v -> {
            if (!isArchived) {
                showDatePicker();
            }
        });
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh data when returning from other activities
        android.util.Log.d("CourseDetails", "CourseDetails resumed, refreshing data for date: " + selectedDate);
        checkSessionForDate(selectedDate);
        loadAttendanceForDate(selectedDate);
        
        // Immediately check if session should end when returning to activity
        checkSessionEndTime();
    }

    private void initializeViews() {
        courseTitleDetail = findViewById(R.id.courseTitleDetail);
        courseScheduleDetail = findViewById(R.id.courseScheduleDetail);
        classCodeDetail = findViewById(R.id.classCodeDetail);
        studentsTextDetail = findViewById(R.id.studentsTextDetail);
        sessionsTextDetail = findViewById(R.id.sessionsTextDetail);
        dateTextDetail = findViewById(R.id.dateTextDetail);
        dayTextDetail = findViewById(R.id.dayTextDetail);
        backArrow = findViewById(R.id.backActiveCourseArrow);
        dateDropdownIcon = findViewById(R.id.dateDropdownIcon);
        btnSetAttendance = findViewById(R.id.setAttendanceBtnDetail);
        btnScanQR = findViewById(R.id.scanQrbtn);
        btnViewRecord = findViewById(R.id.viewAttendanceBtn);
        btnEndAttendance = findViewById(R.id.stopAttendancebtn);
        studentsPresentRecyclerView = findViewById(R.id.studentsPresentRecyclerView);
    }

    private void loadCourseData() {
        Intent intent = getIntent();
        if (intent != null) {
            courseId = intent.getStringExtra("courseId");
            courseCode = intent.getStringExtra("courseCode");
            String courseName = intent.getStringExtra("courseName");
            String courseRoom = intent.getStringExtra("courseRoom");
            String courseSchedule = intent.getStringExtra("courseSchedule");
            String courseStartTime = intent.getStringExtra("courseStartTime");
            String courseEndTime = intent.getStringExtra("courseEndTime");
            int studentCount = intent.getIntExtra("courseStudentCount", 0);
            int sessionCount = intent.getIntExtra("courseSessionCount", 0);
            isArchived = intent.getBooleanExtra("isArchived", false);

            // Set course title (Name | Room)
            if (courseName != null && courseRoom != null) {
                courseTitleDetail.setText(String.format("%s | %s", courseName, courseRoom));
            }

            // Set schedule (Start - End | Days)
            if (courseStartTime != null && courseEndTime != null && courseSchedule != null) {
                courseScheduleDetail.setText(String.format("%s - %s | %s", courseStartTime, courseEndTime, courseSchedule));
            }

            // Set class code
            if (courseCode != null) {
                classCodeDetail.setText(String.format("Code: %s", courseCode));
            }

            // Set initial student and session counts
            studentsTextDetail.setText(String.format("Students: %d", studentCount));
            sessionsTextDetail.setText(String.format("Sessions: %d", sessionCount));
        }
    }
    
    private void handleArchivedCourseState() {
        if (isArchived) {
            // Disable attendance-related buttons
            btnSetAttendance.setEnabled(false);
            btnSetAttendance.setAlpha(0.5f);
            btnSetAttendance.setText("Archived Course");
            
            btnScanQR.setEnabled(false);
            btnScanQR.setAlpha(0.5f);
            btnScanQR.setText("Archived Course");
            
            btnEndAttendance.setEnabled(false);
            btnEndAttendance.setAlpha(0.5f);
            btnEndAttendance.setText("Archived Course");
            
            // Disable date picker
            dateDropdownIcon.setEnabled(false);
            dateDropdownIcon.setAlpha(0.5f);
            
            // Show archived indicator in title
            String currentTitle = courseTitleDetail.getText().toString();
            courseTitleDetail.setText(currentTitle + " (ARCHIVED)");
        }
    }
    
    private void setupRealTimeStudentCountListener() {
        if (courseCode == null) return;
        
        // Listen for real-time updates to student count from global courses registry
        mDatabase.child("courses").child(courseCode).child("studentCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer studentCount = snapshot.getValue(Integer.class);
                if (studentCount != null) {
                    studentsTextDetail.setText(String.format("Students: %d", studentCount));
                    android.util.Log.d("CourseDetails", "Updated student count to: " + studentCount);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("CourseDetails", "Failed to listen to student count updates: " + error.getMessage());
            }
        });
    }
    
    private void setupRealTimeSessionCountListener() {
        if (courseCode == null) return;
        
        // Listen for real-time updates to the actual sessions node to count sessions dynamically
        mDatabase.child("courses").child(courseCode).child("sessions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Count the actual number of sessions
                int actualSessionCount = (int) snapshot.getChildrenCount();
                sessionsTextDetail.setText(String.format("Sessions: %d", actualSessionCount));
                android.util.Log.d("CourseDetails", "Updated session count to: " + actualSessionCount + " (from actual sessions)");
                
                // Also update the sessionCount field in the global registry to keep it in sync
                mDatabase.child("courses").child(courseCode).child("sessionCount").setValue(actualSessionCount)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("CourseDetails", "Updated global sessionCount field to: " + actualSessionCount);
                            } else {
                                android.util.Log.e("CourseDetails", "Failed to update global sessionCount field: " + task.getException());
                            }
                        });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("CourseDetails", "Failed to listen to sessions updates: " + error.getMessage());
            }
        });
    }

    private void setCurrentDate() {
        // Only set to current date if no date is selected yet
        if (selectedDate == null) {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            
            String currentDate = dateFormat.format(calendar.getTime());
            String currentDay = dayFormat.format(calendar.getTime());
            selectedDate = dbDateFormat.format(calendar.getTime());
            
            dateTextDetail.setText(currentDate);
            dayTextDetail.setText("- " + currentDay);
        } else {
            // Update display for the selected date
            updateDateDisplay();
        }
    }
    
    private void updateDateDisplay() {
        if (selectedDate != null) {
            try {
                SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                
                Calendar calendar = Calendar.getInstance();
                calendar.setTime(dbDateFormat.parse(selectedDate));
                
                String displayDate = dateFormat.format(calendar.getTime());
                String displayDay = dayFormat.format(calendar.getTime());
                
                dateTextDetail.setText(displayDate);
                dayTextDetail.setText("- " + displayDay);
            } catch (Exception e) {
                android.util.Log.e("CourseDetails", "Error parsing selected date: " + e.getMessage());
            }
        }
    }

    private void setupBackButton() {
        backArrow.setOnClickListener(v -> {
            // Simply finish this activity to go back to the previous one
            TransitionManager.finishActivityBackward(this);
        });
    }

    private void startDateUpdateTimer() {
        // Check every 30 seconds for session end time and date changes
        dateUpdateTimer = new Timer();
        dateUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> {
                    // Only update date if we're on current date, don't reset selected date
                    if (selectedDate != null) {
                        SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                        String currentDateStr = dbDateFormat.format(Calendar.getInstance().getTime());
                        
                        // Only update if we're viewing current date
                        if (selectedDate.equals(currentDateStr)) {
                            setCurrentDate();
                        }
                    } else {
                        setCurrentDate();
                    }
                    checkSessionEndTime();
                });
            }
        }, 0, 30000); // Start immediately and check every 30 seconds
    }
    
    private void checkSessionEndTime() {
        if (courseCode == null || selectedDate == null || attendanceEnded) {
            return;
        }
        
        // Get session data to check class end time
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String classEndTime = snapshot.child("classEndTime").getValue(String.class);
                            if (classEndTime != null) {
                                checkIfSessionShouldEnd(classEndTime);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("CourseDetails", "Failed to check session end time: " + error.getMessage());
                    }
                });
    }
    
    private void checkIfSessionShouldEnd(String classEndTime) {
        try {
            // Get current time
            Calendar currentCalendar = Calendar.getInstance();
            String currentTimeStr = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(currentCalendar.getTime());
            
            android.util.Log.d("CourseDetails", "Checking session end time - Current: " + currentTimeStr + ", End: " + classEndTime);
            
            // Parse times
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date currentTime = timeFormat.parse(currentTimeStr);
            Date endTime = timeFormat.parse(classEndTime);
            
            // If current time is after class end time, automatically end the session
            if (currentTime.after(endTime) && !attendanceEnded) {
                android.util.Log.d("CourseDetails", "Class end time passed, automatically ending session");
                endAttendanceSession();
            } else if (currentTime.before(endTime)) {
                android.util.Log.d("CourseDetails", "Session still active - " + 
                    (endTime.getTime() - currentTime.getTime()) / 60000 + " minutes remaining");
            } else {
                android.util.Log.d("CourseDetails", "Current time equals end time");
            }
        } catch (Exception e) {
            android.util.Log.e("CourseDetails", "Error checking session end time: " + e.getMessage());
        }
    }

    private void checkTodaySession() {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null || courseId == null) {
            android.util.Log.e("CourseDetails", "User not authenticated or courseId is null");
            updateQRScanButtonState();
            return;
        }
        
        // Get current date in yyyy-MM-dd format
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String currentDate = dateFormat.format(Calendar.getInstance().getTime());
        
        android.util.Log.d("CourseDetails", "Checking for session on date: " + currentDate);
        
        // Check if session exists for today in course code (new structure: /courses/{courseCode}/sessions/{date}/)
        mDatabase.child("courses").child(courseCode).child("sessions")
                .child(currentDate)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        hasSessionForToday = snapshot.exists();
                        android.util.Log.d("CourseDetails", "Session exists for today: " + hasSessionForToday);
                        
                        // If no session exists for today, clear any previous attendance state
                        if (!hasSessionForToday) {
                            clearAttendanceState();
                        }
                        
                        updateQRScanButtonState();
                        updateEndAttendanceButtonState();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("CourseDetails", "Failed to check session: " + error.getMessage());
                        hasSessionForToday = false;
                        updateQRScanButtonState();
                        updateEndAttendanceButtonState();
                    }
                });
    }
    
    private void updateQRScanButtonState() {
        if (btnScanQR != null) {
            if (hasSessionForToday && !attendanceEnded) {
                btnScanQR.setEnabled(true);
                btnScanQR.setAlpha(1.0f);
                btnScanQR.setText("Scan QR Code");
                android.util.Log.d("CourseDetails", "QR scan button enabled");
            } else if (attendanceEnded) {
                btnScanQR.setEnabled(false);
                btnScanQR.setAlpha(0.5f);
                btnScanQR.setText("Attendance Ended");
                android.util.Log.d("CourseDetails", "QR scan button disabled - attendance ended");
            } else {
                btnScanQR.setEnabled(false);
                btnScanQR.setAlpha(0.5f);
                btnScanQR.setText("Create Session First");
                android.util.Log.d("CourseDetails", "QR scan button disabled");
            }
        }
    }
    
    private void updateEndAttendanceButtonState() {
        if (btnEndAttendance != null) {
            if (hasSessionForToday && !attendanceEnded) {
                btnEndAttendance.setEnabled(true);
                btnEndAttendance.setAlpha(1.0f);
                btnEndAttendance.setText("End Attendance");
                android.util.Log.d("CourseDetails", "End Attendance button enabled");
            } else if (attendanceEnded) {
                btnEndAttendance.setEnabled(false);
                btnEndAttendance.setAlpha(0.5f);
                btnEndAttendance.setText("Attendance Ended");
                android.util.Log.d("CourseDetails", "End Attendance button disabled - attendance ended");
            } else {
                btnEndAttendance.setEnabled(false);
                btnEndAttendance.setAlpha(0.5f);
                btnEndAttendance.setText("Create Session First");
                android.util.Log.d("CourseDetails", "End Attendance button disabled - no session");
            }
        }
    }
    
    private void endAttendanceSession() {
        attendanceEnded = true;
        saveAttendanceState();
        updateQRScanButtonState();
        updateEndAttendanceButtonState();
        
        // Mark unscanned students as ABSENT
        markUnscannedStudentsAsAbsent();
        
        android.util.Log.d("CourseDetails", "Attendance session ended - QR scanning disabled");
    }
    
    
    private void markUnscannedStudentsAsAbsent() {
        if (courseCode == null || selectedDate == null) {
            android.util.Log.e("CourseDetails", "Course code or selected date is null");
            return;
        }
        
        android.util.Log.d("CourseDetails", "Marking unscanned students as ABSENT for date: " + selectedDate);
        
        // Get all enrolled students for this course
        mDatabase.child("courses").child(courseCode).child("students")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot enrolledStudentsSnapshot) {
                        if (enrolledStudentsSnapshot.exists()) {
                            android.util.Log.d("CourseDetails", "Found " + enrolledStudentsSnapshot.getChildrenCount() + " enrolled students");
                            
                            // Get list of students who have already been scanned
                            mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                                    .child("attendance").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                                            // Create a set of already scanned EDP numbers
                                            java.util.Set<String> scannedEDPNumbers = new java.util.HashSet<>();
                                            for (DataSnapshot attendanceRecord : attendanceSnapshot.getChildren()) {
                                                AttendanceRecord record = attendanceRecord.getValue(AttendanceRecord.class);
                                                if (record != null) {
                                                    scannedEDPNumbers.add(record.getEdpNumber());
                                                }
                                            }
                                            
                                            android.util.Log.d("CourseDetails", "Found " + scannedEDPNumbers.size() + " already scanned students");
                                            
                                            // Mark unscanned students as ABSENT
                                            markUnscannedStudentsAsAbsentHelper(enrolledStudentsSnapshot, scannedEDPNumbers);
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            android.util.Log.e("CourseDetails", "Failed to get attendance records: " + error.getMessage());
                                        }
                                    });
                        } else {
                            android.util.Log.d("CourseDetails", "No enrolled students found for course: " + courseCode);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("CourseDetails", "Failed to get enrolled students: " + error.getMessage());
                    }
                });
    }
    
    private void markUnscannedStudentsAsAbsentHelper(DataSnapshot enrolledStudentsSnapshot, java.util.Set<String> scannedEDPNumbers) {
        int totalStudents = (int) enrolledStudentsSnapshot.getChildrenCount();
        int absentCount = 0;
        
        android.util.Log.d("CourseDetails", "Processing " + totalStudents + " enrolled students for ABSENT marking");
        android.util.Log.d("CourseDetails", "Already scanned EDP numbers: " + scannedEDPNumbers.toString());
        
        for (DataSnapshot studentSnapshot : enrolledStudentsSnapshot.getChildren()) {
            String studentId = studentSnapshot.getKey();
            android.util.Log.d("CourseDetails", "Processing student ID: " + studentId);
            
            // Get student details to check EDP number
            mDatabase.child("users").child("students").child(studentId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot studentDataSnapshot) {
                            if (studentDataSnapshot.exists()) {
                                String edpNumber = studentDataSnapshot.child("edpNumber").getValue(String.class);
                                String firstName = studentDataSnapshot.child("firstName").getValue(String.class);
                                String lastName = studentDataSnapshot.child("lastName").getValue(String.class);
                                String profileImageUrl = studentDataSnapshot.child("profileImageUrl").getValue(String.class);
                                
                                android.util.Log.d("CourseDetails", "Student data - EDP: " + edpNumber + ", Name: " + firstName + " " + lastName);
                                
                                if (edpNumber != null && firstName != null && lastName != null) {
                                    // Check if this student has already been scanned
                                    if (!scannedEDPNumbers.contains(edpNumber)) {
                                        // This student hasn't been scanned, mark as ABSENT
                                        String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(Calendar.getInstance().getTime());
                                        AttendanceRecord absentRecord = new AttendanceRecord(edpNumber, firstName, lastName, profileImageUrl, currentTime, "ABSENT");
                                        
                                        // Save to database
                                        saveAbsentRecordToDatabase(absentRecord);
                                        
                                        android.util.Log.d("CourseDetails", "Marked student as ABSENT: " + firstName + " " + lastName + " (EDP: " + edpNumber + ")");
                                    } else {
                                        android.util.Log.d("CourseDetails", "Student already scanned: " + firstName + " " + lastName + " (EDP: " + edpNumber + ")");
                                    }
                                } else {
                                    android.util.Log.e("CourseDetails", "Missing student data for ID: " + studentId);
                                }
                            } else {
                                android.util.Log.e("CourseDetails", "Student data not found for ID: " + studentId);
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            android.util.Log.e("CourseDetails", "Failed to get student data for ID " + studentId + ": " + error.getMessage());
                        }
                    });
        }
        
        android.util.Log.d("CourseDetails", "Completed processing enrolled students for ABSENT marking");
    }
    
    private void saveAbsentRecordToDatabase(AttendanceRecord record) {
        if (courseCode == null || selectedDate == null) return;
        
        // Save to the attendance records for the selected date
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                .child("attendance").child(record.getEdpNumber()).setValue(record)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("CourseDetails", "Successfully saved ABSENT record for: " + record.getFirstName() + " " + record.getLastName());
                        
                        // Check and increment absence count, send SMS if needed
                        checkAndIncrementAbsenceCount(record.getEdpNumber());
                    } else {
                        android.util.Log.e("CourseDetails", "Failed to save ABSENT record: " + task.getException());
                    }
                });
    }
    
    private void checkAndIncrementAbsenceCount(String studentEdpNumber) {
        // Get current teacher ID
        String teacherId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        
        if (teacherId == null) {
            android.util.Log.e("CourseDetails", "Teacher ID is null, cannot check absence count");
            return;
        }
        
        // Get course name for SMS
        mDatabase.child("courses").child(courseCode).child("name")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        String courseName = snapshot.getValue(String.class);
                        if (courseName == null) {
                            courseName = courseCode; // Fallback to course code
                        }
                        
                        // Call the SMS check method from SmsAlertsActivity
                        com.llavore.hereoattendance.teacher.SmsAlertsActivity.checkAndIncrementAbsenceCount(
                                teacherId, studentEdpNumber, courseCode, courseName, CourseDetails.this);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        android.util.Log.e("CourseDetails", "Failed to get course name: " + databaseError.getMessage());
                    }
                });
    }
    
    private void saveAttendanceState() {
        if (courseCode != null) {
            SharedPreferences prefs = getSharedPreferences("attendance_state", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String attendanceKey = "attendance_ended_" + courseCode + "_" + selectedDate;
            editor.putBoolean(attendanceKey, attendanceEnded);
            editor.putString("selected_date_" + courseCode, selectedDate);
            editor.apply();
            android.util.Log.d("CourseDetails", "Saved attendance state with key: " + attendanceKey);
            android.util.Log.d("CourseDetails", "Saved attendance state: " + attendanceEnded + " for date: " + selectedDate);
        }
    }
    
    private void restoreAttendanceState() {
        if (courseCode != null) {
            SharedPreferences prefs = getSharedPreferences("attendance_state", MODE_PRIVATE);
            String attendanceKey = "attendance_ended_" + courseCode + "_" + selectedDate;
            attendanceEnded = prefs.getBoolean(attendanceKey, false);
            
            android.util.Log.d("CourseDetails", "Restoring attendance state for key: " + attendanceKey);
            android.util.Log.d("CourseDetails", "Restored attendance state: " + attendanceEnded + " for date: " + selectedDate);
            
            if (attendanceEnded) {
                updateQRScanButtonState();
                updateEndAttendanceButtonState();
            }
        }
    }
    
    private void clearAttendanceState() {
        if (courseCode != null) {
            SharedPreferences prefs = getSharedPreferences("attendance_state", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            String attendanceKey = "attendance_ended_" + courseCode + "_" + selectedDate;
            editor.remove(attendanceKey);
            editor.apply();
            attendanceEnded = false;
            android.util.Log.d("CourseDetails", "Cleared attendance state with key: " + attendanceKey);
            android.util.Log.d("CourseDetails", "Cleared attendance state for date: " + selectedDate);
        }
    }
    
    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar selectedCalendar = Calendar.getInstance();
                    selectedCalendar.set(selectedYear, selectedMonth, selectedDay);
                    
                    // Format the selected date
                    SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
                    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    
                    String formattedDate = dateFormat.format(selectedCalendar.getTime());
                    String formattedDay = dayFormat.format(selectedCalendar.getTime());
                    selectedDate = dbDateFormat.format(selectedCalendar.getTime());
                    
                    // Update the UI
                    dateTextDetail.setText(formattedDate);
                    dayTextDetail.setText("- " + formattedDay);
                    
                    // Check session for selected date
                    checkSessionForDate(selectedDate);
                    
                    android.util.Log.d("CourseDetails", "Selected date: " + selectedDate);
                }, year, month, day);
        
        // Set maximum date to today (can't select future dates)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    private void checkSessionForDate(String date) {
        if (courseCode == null) return;
        
        android.util.Log.d("CourseDetails", "Checking session for date: " + date);
        
        // Update selectedDate before checking session
        selectedDate = date;
        
        // Check if session exists for selected date
        mDatabase.child("courses").child(courseCode).child("sessions").child(date)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        hasSessionForToday = snapshot.exists();
                        android.util.Log.d("CourseDetails", "Session exists for " + date + ": " + hasSessionForToday);
                        
                        // If no session exists for selected date, clear any previous attendance state
                        if (!hasSessionForToday) {
                            clearAttendanceState();
                        } else {
                            // If session exists, restore attendance state for this specific date
                            restoreAttendanceState();
                            
                            // Immediately check if session should end when session is found
                            checkSessionEndTime();
                        }
                        
                        updateQRScanButtonState();
                        updateEndAttendanceButtonState();
                        loadAttendanceForDate(date);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("CourseDetails", "Failed to check session for date: " + error.getMessage());
                        hasSessionForToday = false;
                        updateQRScanButtonState();
                        updateEndAttendanceButtonState();
                    }
                });
    }

    private void setupStudentsRecyclerView() {
        presentStudents = new ArrayList<>();
        studentsAdapter = new StudentsPresentAdapter(presentStudents);
        studentsPresentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        studentsPresentRecyclerView.setAdapter(studentsAdapter);
    }
    
    private void loadTodayAttendance() {
        // Load attendance for the currently selected date
        if (selectedDate != null) {
            loadAttendanceForDate(selectedDate);
        }
    }
    
    private void loadAttendanceForDate(String date) {
        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        if (userId == null || courseId == null || courseCode == null) {
            android.util.Log.e("CourseDetails", "User not authenticated or courseId/courseCode is null");
            return;
        }
        
        android.util.Log.d("CourseDetails", "Loading attendance for date: " + date);
        
        // Listen for real-time updates to attendance for selected date (new structure: /courses/{courseCode}/sessions/{date}/attendance/)
        mDatabase.child("courses").child(courseCode).child("sessions").child(date)
                .child("attendance").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        presentStudents.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            AttendanceRecord record = child.getValue(AttendanceRecord.class);
                            if (record != null) {
                                // Only add PRESENT and LATE students to the RecyclerView
                                // ABSENT students should not appear in the "Students Present" list
                                if (record.getStatus().equals("PRESENT") || record.getStatus().equals("LATE")) {
                                    presentStudents.add(record);
                                }
                            }
                        }
                        studentsAdapter.notifyDataSetChanged();
                        android.util.Log.d("CourseDetails", "Loaded " + presentStudents.size() + " scanned students (PRESENT/LATE) for " + date);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("CourseDetails", "Failed to load attendance: " + error.getMessage());
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dateUpdateTimer != null) {
            dateUpdateTimer.cancel();
        }
    }
}