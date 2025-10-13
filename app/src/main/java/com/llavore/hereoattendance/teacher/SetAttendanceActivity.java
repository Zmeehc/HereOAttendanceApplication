package com.llavore.hereoattendance.teacher;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.Session;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class SetAttendanceActivity extends AppCompatActivity {
    private ImageView btnBackToCourseDetails;
    private TextView courseTitleDetail, courseScheduleDetail, dateNDayTxt;
    private TextInputEditText sessionNameTxt, classStartTxt, classEndTxt, lateAttTxt;
    private MaterialButton btnSaveSetAttendance;
    
    // Course data
    private String courseId, courseCode, courseName, courseRoom, courseSchedule, courseStartTime, courseEndTime;
    private String selectedDate; // Format: yyyy-MM-dd
    private int studentCount, sessionCount;
    
    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_attendance);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        
        initializeViews();
        loadCourseData();
        setCurrentDate();
        setupBackButton();
        setupTimePickers();
        checkExistingSession();
        setupSaveButton();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void initializeViews() {
        btnBackToCourseDetails = findViewById(R.id.backToCourseDetails);
        courseTitleDetail = findViewById(R.id.courseTitleDetail);
        courseScheduleDetail = findViewById(R.id.courseScheduleDetail);
        dateNDayTxt = findViewById(R.id.dateNDayTxt);
        sessionNameTxt = findViewById(R.id.sessionNameTxt);
        classStartTxt = findViewById(R.id.classStartTxt);
        classEndTxt = findViewById(R.id.classEndTxt);
        lateAttTxt = findViewById(R.id.lateAttTxt);
        btnSaveSetAttendance = findViewById(R.id.btnSaveSetAttendance);
    }
    
    private void loadCourseData() {
        Intent intent = getIntent();
        if (intent != null) {
            courseId = intent.getStringExtra("courseId");
            courseCode = intent.getStringExtra("courseCode");
            courseName = intent.getStringExtra("courseName");
            courseRoom = intent.getStringExtra("courseRoom");
            courseSchedule = intent.getStringExtra("courseSchedule");
            courseStartTime = intent.getStringExtra("courseStartTime");
            courseEndTime = intent.getStringExtra("courseEndTime");
            studentCount = intent.getIntExtra("courseStudentCount", 0);
            sessionCount = intent.getIntExtra("courseSessionCount", 0);
            selectedDate = intent.getStringExtra("selectedDate");
            
            // If no selected date provided, use current date
            if (selectedDate == null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate = dateFormat.format(Calendar.getInstance().getTime());
            }

            // Set course title (Name | Room)
            if (courseName != null && courseRoom != null) {
                courseTitleDetail.setText(String.format("%s | %s", courseName, courseRoom));
            }

            // Set course schedule (Start Time - End Time | Days)
            if (courseStartTime != null && courseEndTime != null && courseSchedule != null) {
                courseScheduleDetail.setText(String.format("%s - %s | %s", courseStartTime, courseEndTime, courseSchedule));
            }
        }
    }
    
    private void setCurrentDate() {
        if (selectedDate == null) return;
        
        try {
            // Parse the selected date
            SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(dbDateFormat.parse(selectedDate));
            
            // Format for display
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            
            String date = dateFormat.format(calendar.getTime());
            String day = dayFormat.format(calendar.getTime());
            
            dateNDayTxt.setText(String.format("%s | %s", date, day));
        } catch (Exception e) {
            android.util.Log.e("SetAttendance", "Error parsing selected date: " + e.getMessage());
            // Fallback to current date
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            
            String date = dateFormat.format(calendar.getTime());
            String day = dayFormat.format(calendar.getTime());
            
            dateNDayTxt.setText(String.format("%s | %s", date, day));
        }
    }
    
    private void checkExistingSession() {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null || courseCode == null) {
            android.util.Log.e("SetAttendance", "User not authenticated or courseCode is null");
            return;
        }

        android.util.Log.d("SetAttendance", "Checking for existing session on date: " + selectedDate);

        // Check if session already exists for selected date
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            android.util.Log.d("SetAttendance", "Session already exists for today");
                            // Disable the save button and show message
                            btnSaveSetAttendance.setEnabled(false);
                            btnSaveSetAttendance.setText("Session Already Created Today");
                            btnSaveSetAttendance.setBackgroundColor(getResources().getColor(android.R.color.darker_gray));
                            
                            // Disable all input fields
                            sessionNameTxt.setEnabled(false);
                            classStartTxt.setEnabled(false);
                            classEndTxt.setEnabled(false);
                            lateAttTxt.setEnabled(false);
                            
                            Toast.makeText(SetAttendanceActivity.this, "Attendance session already created for today", Toast.LENGTH_LONG).show();
                        } else {
                            android.util.Log.d("SetAttendance", "No existing session found, allowing creation");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("SetAttendance", "Failed to check existing session: " + error.getMessage());
                    }
                });
    }
    
    private void setupBackButton() {
        btnBackToCourseDetails.setOnClickListener(v -> {
            Intent intent = new Intent(SetAttendanceActivity.this, CourseDetails.class);
            // Pass back all the course data
            intent.putExtra("courseId", courseId);
            intent.putExtra("courseCode", courseCode);
            intent.putExtra("courseName", courseName);
            intent.putExtra("courseRoom", courseRoom);
            intent.putExtra("courseSchedule", courseSchedule);
            intent.putExtra("courseStartTime", courseStartTime);
            intent.putExtra("courseEndTime", courseEndTime);
            intent.putExtra("courseStudentCount", studentCount);
            intent.putExtra("courseSessionCount", sessionCount);
            startActivity(intent);
            finish();
        });
    }
    
    private void setupTimePickers() {
        // Disable keyboard for picker-based fields
        disableKeyboard(classStartTxt);
        disableKeyboard(classEndTxt);
        disableKeyboard(lateAttTxt);

        // Time pickers for start, end, and late attendance
        classStartTxt.setOnClickListener(v -> showTimePicker(classStartTxt));
        classEndTxt.setOnClickListener(v -> showTimePicker(classEndTxt));
        lateAttTxt.setOnClickListener(v -> showTimePicker(lateAttTxt));
    }
    
    private void setupSaveButton() {
        btnSaveSetAttendance.setOnClickListener(v -> {
            android.util.Log.d("SetAttendance", "Save button clicked");
            
            if (validateInputs()) {
                android.util.Log.d("SetAttendance", "Validation passed, creating session");
                Toast.makeText(this, "Creating session...", Toast.LENGTH_SHORT).show();
                createSession();
            } else {
                android.util.Log.d("SetAttendance", "Validation failed");
                Toast.makeText(this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private boolean validateInputs() {
        String sessionName = sessionNameTxt.getText().toString().trim();
        String classStart = classStartTxt.getText().toString().trim();
        String classEnd = classEndTxt.getText().toString().trim();
        String lateAttendance = lateAttTxt.getText().toString().trim();
        
        android.util.Log.d("SetAttendance", "Validating inputs:");
        android.util.Log.d("SetAttendance", "Session name: '" + sessionName + "'");
        android.util.Log.d("SetAttendance", "Class start: '" + classStart + "'");
        android.util.Log.d("SetAttendance", "Class end: '" + classEnd + "'");
        android.util.Log.d("SetAttendance", "Late attendance: '" + lateAttendance + "'");
        
        if (sessionName.isEmpty()) {
            android.util.Log.d("SetAttendance", "Session name is empty");
            sessionNameTxt.setError("Session name is required");
            return false;
        }
        
        if (classStart.isEmpty()) {
            android.util.Log.d("SetAttendance", "Class start is empty");
            classStartTxt.setError("Class start time is required");
            return false;
        }
        
        if (classEnd.isEmpty()) {
            android.util.Log.d("SetAttendance", "Class end is empty");
            classEndTxt.setError("Class end time is required");
            return false;
        }
        
        if (lateAttendance.isEmpty()) {
            android.util.Log.d("SetAttendance", "Late attendance is empty");
            lateAttTxt.setError("Late attendance time is required");
            return false;
        }
        
        android.util.Log.d("SetAttendance", "All validations passed");
        return true;
    }
    
    private void createSession() {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null) {
            android.util.Log.e("SetAttendance", "User not authenticated");
            return;
        }
        
        // Create session data
        String sessionName = sessionNameTxt.getText().toString().trim();
        String classStart = classStartTxt.getText().toString().trim();
        String classEnd = classEndTxt.getText().toString().trim();
        String lateAttendance = lateAttTxt.getText().toString().trim();
        
        // Generate unique session ID
        String sessionId = mDatabase.child("sessions").push().getKey();
        
        // Create session object using selected date
        Session session = new Session(sessionId, courseId, courseCode, sessionName, 
                                    classStart, classEnd, lateAttendance, selectedDate, userId);
        
        // Save session to database under course code (new structure: /courses/{courseCode}/sessions/{date}/)
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate).setValue(session)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("SetAttendance", "Session created successfully under course code");
                        
                        // Also save to teacher's course sessions for easy access
                        mDatabase.child("users").child("teachers").child(userId)
                                .child("courses").child(courseId).child("sessions")
                                .child(selectedDate).setValue(session)
                                .addOnCompleteListener(task2 -> {
                                    if (task2.isSuccessful()) {
                                        android.util.Log.d("SetAttendance", "Session saved to teacher's course");
                                        
                                        // Update session count
                                        updateSessionCount(userId, courseId, courseCode);
                                        
                                        // Return to the previous CourseDetails activity
                                        finish();
                                    } else {
                                        android.util.Log.e("SetAttendance", "Failed to save session to teacher's course: " + task2.getException().getMessage());
                                    }
                                });
                    } else {
                        android.util.Log.e("SetAttendance", "Failed to create session: " + task.getException().getMessage());
                    }
                });
    }

    private void disableKeyboard(TextInputEditText editText) {
        if (editText == null) return;
        editText.setInputType(InputType.TYPE_NULL);
        editText.setFocusable(false);
        editText.setClickable(true);
    }

    private void showTimePicker(TextInputEditText targetField) {
        if (targetField == null) return;
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String formatted = formatTime(hourOfDay, minuteOfHour);
            targetField.setText(formatted);
        }, hour, minute, false);
        dialog.show();
    }

    private String formatTime(int hourOfDay, int minute) {
        int hour12 = hourOfDay % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        String ampm = hourOfDay < 12 ? "AM" : "PM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, ampm);
    }
    
    private void updateSessionCount(String userId, String courseId, String courseCode) {
        android.util.Log.d("SessionCount", "Updating session count - Teacher: " + userId + ", Course: " + courseId + ", Code: " + courseCode);
        
        // Count sessions from the new structure: /courses/{courseCode}/sessions/
        mDatabase.child("courses").child(courseCode).child("sessions").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int sessionCount = (int) snapshot.getChildrenCount();
                android.util.Log.d("SessionCount", "Current session count from /courses/{courseCode}/sessions/: " + sessionCount);

                // Update in teacher's course
                mDatabase.child("users").child("teachers").child(userId).child("courses")
                        .child(courseId).child("sessionCount").setValue(sessionCount)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("SessionCount", "Successfully updated session count in teacher's course to: " + sessionCount);
                            } else {
                                android.util.Log.e("SessionCount", "Failed to update session count in teacher's course: " + task.getException());
                            }
                        });

                // Update in global courses
                mDatabase.child("courses").child(courseCode).child("sessionCount").setValue(sessionCount)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("SessionCount", "Successfully updated session count in global courses to: " + sessionCount);
                            } else {
                                android.util.Log.e("SessionCount", "Failed to update session count in global courses: " + task.getException());
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("SessionCount", "Failed to read sessions from course:");
                android.util.Log.e("SessionCount", "  - Error code: " + error.getCode());
                android.util.Log.e("SessionCount", "  - Error message: " + error.getMessage());
            }
        });
    }
}