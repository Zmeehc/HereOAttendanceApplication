package com.llavore.hereoattendance.student;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.SessionStatusAdapter;
import com.llavore.hereoattendance.models.AttendanceRecord;
import com.llavore.hereoattendance.models.SessionStatus;
import com.llavore.hereoattendance.utils.SessionManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StudentCourseDetails extends AppCompatActivity {

    private ImageView backButton;
    private TextView courseTitleText, courseScheduleText, sessionsCountText;
    private MaterialButton refreshButton;
    private RecyclerView sessionsRecyclerView;
    
    private SessionStatusAdapter sessionStatusAdapter;
    private List<SessionStatus> sessionStatuses;
    
    private DatabaseReference mDatabase;
    private SessionManager sessionManager;
    
    private String courseId, courseName, courseRoom, courseSchedule, courseStartTime, courseEndTime, courseCode;
    private String currentStudentId;
    
    // Real-time listeners
    private ValueEventListener attendanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_course_details);

        // Initialize Firebase and session manager
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sessionManager = new SessionManager(this);
        currentStudentId = sessionManager.getUserId();

        // Get course data from intent
        getCourseDataFromIntent();

        // Initialize views
        initializeViews();

        // Setup RecyclerView
        setupRecyclerView();

        // Load course information
        loadCourseInformation();

        // Load session statuses
        loadSessionStatuses();

        // Setup real-time listeners
        setupRealTimeListeners();

        // Setup click listeners
        setupClickListeners();
    }

    private void getCourseDataFromIntent() {
        courseId = getIntent().getStringExtra("courseId");
        courseName = getIntent().getStringExtra("courseName");
        courseRoom = getIntent().getStringExtra("courseRoom");
        courseSchedule = getIntent().getStringExtra("courseSchedule");
        courseStartTime = getIntent().getStringExtra("courseStartTime");
        courseEndTime = getIntent().getStringExtra("courseEndTime");
        courseCode = getIntent().getStringExtra("courseCode");
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        courseTitleText = findViewById(R.id.courseTitleText);
        courseScheduleText = findViewById(R.id.courseScheduleText);
        sessionsCountText = findViewById(R.id.sessionsCountText);
        refreshButton = findViewById(R.id.refreshButton);
        sessionsRecyclerView = findViewById(R.id.sessionsRecyclerView);
    }

    private void setupRecyclerView() {
        sessionStatuses = new ArrayList<>();
        sessionStatusAdapter = new SessionStatusAdapter(this, sessionStatuses);
        sessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        sessionsRecyclerView.setAdapter(sessionStatusAdapter);
    }

    private void loadCourseInformation() {
        // Set course title
        courseTitleText.setText(courseName + " | " + courseRoom);
        
        // Set schedule
        courseScheduleText.setText(courseStartTime + " - " + courseEndTime + " | " + courseSchedule);
    }

    private void loadSessionStatuses() {
        if (courseCode == null || currentStudentId == null) {
            Toast.makeText(this, "Missing course or student information", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("StudentCourseDetails", "Loading session statuses for course: " + courseCode + ", student: " + currentStudentId);
        
        // Get the student's EDP number from Firebase
        mDatabase.child("users").child("students").child(currentStudentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                        if (studentSnapshot.exists()) {
                            String studentEdpNumber = studentSnapshot.child("edpNumber").getValue(String.class);
                            android.util.Log.d("StudentCourseDetails", "Student EDP Number: " + studentEdpNumber);
                            
                            // Now load session statuses with the correct EDP number
                            loadSessionStatusesWithEdp(studentEdpNumber);
                        } else {
                            android.util.Log.e("StudentCourseDetails", "Student data not found for ID: " + currentStudentId);
                            Toast.makeText(StudentCourseDetails.this, "Student data not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("StudentCourseDetails", "Failed to load student data: " + databaseError.getMessage());
                        Toast.makeText(StudentCourseDetails.this, "Failed to load student data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadSessionStatusesWithEdp(String studentEdpNumber) {
        if (courseCode == null || studentEdpNumber == null) {
            Toast.makeText(this, "Missing course or student EDP information", Toast.LENGTH_SHORT).show();
            return;
        }

        android.util.Log.d("StudentCourseDetails", "Loading session statuses for course: " + courseCode + ", EDP: " + studentEdpNumber);

        // Clear existing data
        sessionStatuses.clear();

        // First, get all sessions for this course
        mDatabase.child("courses").child(courseCode).child("sessions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot sessionsSnapshot) {
                        android.util.Log.d("StudentCourseDetails", "Sessions snapshot exists: " + sessionsSnapshot.exists());
                        
                        if (!sessionsSnapshot.exists()) {
                            android.util.Log.d("StudentCourseDetails", "No sessions found for course: " + courseCode);
                            sessionStatusAdapter.notifyDataSetChanged();
                            updateSessionsCount();
                            return;
                        }

                        final List<SessionStatus> newSessionStatuses = Collections.synchronizedList(new ArrayList<>());
                        final int totalSessions = (int) sessionsSnapshot.getChildrenCount();
                        final java.util.concurrent.atomic.AtomicInteger processedSessions = new java.util.concurrent.atomic.AtomicInteger(0);

                        for (DataSnapshot sessionSnapshot : sessionsSnapshot.getChildren()) {
                            String sessionId = sessionSnapshot.getKey();
                            String sessionDateValue = sessionSnapshot.child("date").getValue(String.class);
                            
                            // Make sessionDate effectively final
                            final String sessionDate = (sessionDateValue == null) ? sessionId : sessionDateValue;
                            
                            android.util.Log.d("StudentCourseDetails", "Processing session: " + sessionId + ", date: " + sessionDate);

                            // Debug: Let's see what's actually in the attendance node for this session
                            // The correct path is: /courses/{courseCode}/sessions/{date}/attendance/{edpNumber}
                            mDatabase.child("courses").child(courseCode).child("sessions").child(sessionId).child("attendance")
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot debugSnapshot) {
                                            android.util.Log.d("StudentCourseDetails", "DEBUG - All attendance records for session " + sessionId + ":");
                                            android.util.Log.d("StudentCourseDetails", "DEBUG - Snapshot exists: " + debugSnapshot.exists());
                                            android.util.Log.d("StudentCourseDetails", "DEBUG - Children count: " + debugSnapshot.getChildrenCount());
                                            
                                            for (DataSnapshot recordSnapshot : debugSnapshot.getChildren()) {
                                                String recordKey = recordSnapshot.getKey();
                                                String edpInRecord = recordSnapshot.child("edpNumber").getValue(String.class);
                                                String statusInRecord = recordSnapshot.child("status").getValue(String.class);
                                                android.util.Log.d("StudentCourseDetails", "DEBUG - Record: " + recordKey + ", EDP: " + edpInRecord + ", Status: " + statusInRecord);
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            android.util.Log.e("StudentCourseDetails", "DEBUG - Failed to load attendance debug: " + databaseError.getMessage());
                                        }
                                    });

                            // Now try to get attendance record for this student in this session
                            // The correct path is: /courses/{courseCode}/sessions/{date}/attendance/{edpNumber}
                            android.util.Log.d("StudentCourseDetails", "Searching attendance for session: " + sessionId + " with EDP: " + studentEdpNumber);
                            mDatabase.child("courses").child(courseCode).child("sessions").child(sessionId).child("attendance").child(studentEdpNumber)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                                            String status = "ABSENT"; // Default status
                                            
                                            android.util.Log.d("StudentCourseDetails", "Attendance snapshot exists: " + attendanceSnapshot.exists() + " for session: " + sessionId + " and EDP: " + studentEdpNumber);
                                            
                                            if (attendanceSnapshot.exists()) {
                                                AttendanceRecord record = attendanceSnapshot.getValue(AttendanceRecord.class);
                                                if (record != null) {
                                                    status = record.getStatus();
                                                    android.util.Log.d("StudentCourseDetails", "Found attendance record: " + status + " for EDP: " + record.getEdpNumber());
                                                } else {
                                                    android.util.Log.d("StudentCourseDetails", "Attendance record exists but could not parse it");
                                                }
                                            } else {
                                                android.util.Log.d("StudentCourseDetails", "No attendance record found for session: " + sessionId + " and EDP: " + studentEdpNumber);
                                            }
                                            
                                            // Add to list
                                            newSessionStatuses.add(new SessionStatus(sessionDate, status));
                                            
                                            int currentProcessed = processedSessions.incrementAndGet();
                                            if (currentProcessed == totalSessions) {
                                                // Sort by date (newest first)
                                                Collections.sort(newSessionStatuses, new Comparator<SessionStatus>() {
                                                    @Override
                                                    public int compare(SessionStatus s1, SessionStatus s2) {
                                                        try {
                                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                                                            Date date1 = sdf.parse(s1.getDate());
                                                            Date date2 = sdf.parse(s2.getDate());
                                                            return date2.compareTo(date1); // Descending order
                                                        } catch (Exception e) {
                                                            return s2.getDate().compareTo(s1.getDate());
                                                        }
                                                    }
                                                });
                                                
                                                // Update UI
                                                sessionStatuses.clear();
                                                sessionStatuses.addAll(newSessionStatuses);
                                                sessionStatusAdapter.notifyDataSetChanged();
                                                updateSessionsCount();
                                                
                                                android.util.Log.d("StudentCourseDetails", "Loaded " + newSessionStatuses.size() + " session statuses with real attendance data");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            android.util.Log.e("StudentCourseDetails", "Failed to load attendance for session " + sessionId + ": " + databaseError.getMessage());
                                            int currentProcessed = processedSessions.incrementAndGet();
                                            if (currentProcessed == totalSessions) {
                                                sessionStatusAdapter.notifyDataSetChanged();
                                                updateSessionsCount();
                                            }
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("StudentCourseDetails", "Failed to load sessions: " + databaseError.getMessage());
                        Toast.makeText(StudentCourseDetails.this, "Failed to load session data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateSessionsCount() {
        sessionsCountText.setText("Sessions: " + sessionStatuses.size());
    }

    private void setupRealTimeListeners() {
        if (courseCode == null || currentStudentId == null) {
            return;
        }

        // Listen for real-time attendance changes
        attendanceListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Reload session statuses when attendance data changes
                loadSessionStatuses();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("StudentCourseDetails", "Real-time attendance listener cancelled: " + error.getMessage());
            }
        };

        // Listen to all attendance records for this course and student
        mDatabase.child("attendance").child(courseCode)
                .addValueEventListener(attendanceListener);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
        
        refreshButton.setOnClickListener(v -> {
            // Show loading state
            refreshButton.setText("Refreshing...");
            refreshButton.setEnabled(false);
            
            // Refresh the session statuses
            loadSessionStatuses();
            
            // Reset button state after a short delay
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                refreshButton.setText("Refresh");
                refreshButton.setEnabled(true);
            }, 2000);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove real-time listeners to prevent memory leaks
        if (attendanceListener != null && courseCode != null) {
            mDatabase.child("attendance").child(courseCode)
                    .removeEventListener(attendanceListener);
        }
    }
}
