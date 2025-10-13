package com.llavore.hereoattendance.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.StudentAbsenceAlertAdapter;
import com.llavore.hereoattendance.models.AttendanceRecord;
import com.llavore.hereoattendance.models.Course;
import com.llavore.hereoattendance.models.StudentAbsenceAlert;
import com.llavore.hereoattendance.SemaphoreSmsSender;
import android.widget.Toast;
import android.widget.PopupMenu;
import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class SmsAlertsActivity extends AppCompatActivity {

    private ImageView backToDashboardButton;
    private RecyclerView absenceAlertsRecyclerView;
    private LinearLayout noAlertsLayout;
    private TextView noAlertsText;
    private TextView noAlertsSubtext;
    
    private StudentAbsenceAlertAdapter adapter;
    private List<StudentAbsenceAlert> absenceAlerts;
    private DatabaseReference mDatabase;
    private String currentTeacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sms_alerts);

        initializeViews();
        setupClickListeners();
        initializeFirebase();
        loadAbsenceAlerts();
    }

    private void initializeViews() {
        backToDashboardButton = findViewById(R.id.backDashArrow);
        absenceAlertsRecyclerView = findViewById(R.id.absenceAlertsRecyclerView);
        noAlertsLayout = findViewById(R.id.noAlertsLayout);
        noAlertsText = findViewById(R.id.noAlertsText);
        noAlertsSubtext = findViewById(R.id.noAlertsSubtext);
        
        absenceAlerts = new ArrayList<>();
        adapter = new StudentAbsenceAlertAdapter(absenceAlerts, this);
        
        absenceAlertsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        absenceAlertsRecyclerView.setAdapter(adapter);
        
        // Set up adapter click listeners
        adapter.setOnSendSmsClickListener((alert, holder) -> {
            sendSmsToParent(alert, holder);
        });
        
        adapter.setOnOverflowMenuClickListener((alert, view) -> {
            showOverflowMenu(alert, view);
        });
    }

    private void setupClickListeners() {
        backToDashboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(SmsAlertsActivity.this, TeacherDashboard.class);
            startActivity(intent);
            finish();
        });
    }

    private void initializeFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentTeacherId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private void loadAbsenceAlerts() {
        if (currentTeacherId == null) {
            showNoAlertsMessage("Not logged in", "Please log in to view absence alerts");
            return;
        }

        // Load teacher's courses first
        mDatabase.child("users").child("teachers").child(currentTeacherId).child("courses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot coursesSnapshot) {
                        if (!coursesSnapshot.exists()) {
                            showNoAlertsMessage("No courses found", "Create courses to track student attendance");
                            return;
                        }

                        List<String> courseCodes = new ArrayList<>();
                        Map<String, Course> courseMap = new HashMap<>();

                        // Collect course codes and course information
                        for (DataSnapshot courseSnapshot : coursesSnapshot.getChildren()) {
                            Course course = courseSnapshot.getValue(Course.class);
                            if (course != null && course.code != null) {
                                courseCodes.add(course.code);
                                courseMap.put(course.code, course);
                            }
                        }

                        if (courseCodes.isEmpty()) {
                            showNoAlertsMessage("No courses found", "Create courses to track student attendance");
                            return;
                        }

                        // Load attendance data for all courses
                        loadAttendanceDataForCourses(courseCodes, courseMap);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        showNoAlertsMessage("Error loading courses", "Please try again later");
                    }
                });
    }

    private void loadAttendanceDataForCourses(List<String> courseCodes, Map<String, Course> courseMap) {
        Map<String, Map<String, Integer>> studentAbsenceCounts = new HashMap<>();
        Map<String, Map<String, StudentAbsenceAlert.CourseAbsence>> studentCourseAbsences = new HashMap<>();

        AtomicInteger coursesProcessed = new AtomicInteger(0);
        int totalCourses = courseCodes.size();

        for (String courseCode : courseCodes) {
            Course course = courseMap.get(courseCode);
            if (course == null) continue;

            // Load all sessions for this course
            mDatabase.child("courses").child(courseCode).child("sessions")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot sessionsSnapshot) {
                            int processed = coursesProcessed.incrementAndGet();
                            
                            if (sessionsSnapshot.exists()) {
                                // Process each session
                                for (DataSnapshot sessionSnapshot : sessionsSnapshot.getChildren()) {
                                    DataSnapshot attendanceSnapshot = sessionSnapshot.child("attendance");
                                    if (attendanceSnapshot.exists()) {
                                        processSessionAttendance(attendanceSnapshot, course, studentAbsenceCounts, studentCourseAbsences);
                                    }
                                }
                            }

                            // Check if we've processed all courses
                            if (processed == totalCourses) {
                                generateAbsenceAlerts(studentAbsenceCounts, studentCourseAbsences);
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            int processed = coursesProcessed.incrementAndGet();
                            if (processed == totalCourses) {
                                generateAbsenceAlerts(studentAbsenceCounts, studentCourseAbsences);
                            }
                        }
                    });
        }
    }

    private void processSessionAttendance(DataSnapshot attendanceSnapshot, Course course, 
                                        Map<String, Map<String, Integer>> studentAbsenceCounts,
                                        Map<String, Map<String, StudentAbsenceAlert.CourseAbsence>> studentCourseAbsences) {
        
        for (DataSnapshot studentSnapshot : attendanceSnapshot.getChildren()) {
            AttendanceRecord record = studentSnapshot.getValue(AttendanceRecord.class);
            if (record == null || !"ABSENT".equals(record.getStatus())) {
                continue; // Only count ABSENT records
            }

            String edpNumber = record.getEdpNumber();
            String courseCode = course.code;

            // Initialize maps if needed
            if (!studentAbsenceCounts.containsKey(edpNumber)) {
                studentAbsenceCounts.put(edpNumber, new HashMap<>());
                studentCourseAbsences.put(edpNumber, new HashMap<>());
            }

            // Count absences per course
            Map<String, Integer> courseCounts = studentAbsenceCounts.get(edpNumber);
            int currentCount = courseCounts.getOrDefault(courseCode, 0);
            courseCounts.put(courseCode, currentCount + 1);

            // Store course absence information
            Map<String, StudentAbsenceAlert.CourseAbsence> courseAbsences = studentCourseAbsences.get(edpNumber);
            if (!courseAbsences.containsKey(courseCode)) {
                String schedule = course.startTime + " - " + course.endTime + " | " + course.scheduleDays;
                StudentAbsenceAlert.CourseAbsence courseAbsence = new StudentAbsenceAlert.CourseAbsence(
                        courseCode, course.name, schedule, 0);
                courseAbsences.put(courseCode, courseAbsence);
            }

            // Update absence count for this course
            StudentAbsenceAlert.CourseAbsence courseAbsence = courseAbsences.get(courseCode);
            courseAbsence.setAbsenceCount(courseCounts.get(courseCode));
        }
    }

    private void generateAbsenceAlerts(Map<String, Map<String, Integer>> studentAbsenceCounts,
                                     Map<String, Map<String, StudentAbsenceAlert.CourseAbsence>> studentCourseAbsences) {
        
        absenceAlerts.clear();
        
        for (String edpNumber : studentAbsenceCounts.keySet()) {
            Map<String, Integer> courseCounts = studentAbsenceCounts.get(edpNumber);
            Map<String, StudentAbsenceAlert.CourseAbsence> courseAbsences = studentCourseAbsences.get(edpNumber);
            
            // Check if student has 3+ absences in any course
            boolean hasViolation = false;
            List<StudentAbsenceAlert.CourseAbsence> violationCourses = new ArrayList<>();
            int totalAbsences = 0;
            
            for (String courseCode : courseCounts.keySet()) {
                int absenceCount = courseCounts.get(courseCode);
                totalAbsences += absenceCount;
                
                if (absenceCount >= 3) {
                    hasViolation = true;
                    violationCourses.add(courseAbsences.get(courseCode));
                }
            }
            
            if (hasViolation) {
                // Get student information (we'll need to load this from the database)
                loadStudentInformation(edpNumber, totalAbsences, violationCourses);
            }
        }
        
        if (absenceAlerts.isEmpty()) {
            showNoAlertsMessage("No SMS alerts yet", "SMS alerts will appear here when students have 3+ absences");
        } else {
            showAbsenceAlerts();
        }
    }

    private void loadStudentInformation(String edpNumber, int totalAbsences, List<StudentAbsenceAlert.CourseAbsence> violationCourses) {
        // Load student information from the database
        mDatabase.child("users").child("students").orderByChild("edpNumber").equalTo(edpNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                                String firstName = studentSnapshot.child("firstName").getValue(String.class);
                                String lastName = studentSnapshot.child("lastName").getValue(String.class);
                                String profileImageUrl = studentSnapshot.child("profileImageUrl").getValue(String.class);
                                
                                if (firstName != null && lastName != null) {
                                    StudentAbsenceAlert alert = new StudentAbsenceAlert(
                                            edpNumber, firstName, lastName, profileImageUrl, 
                                            totalAbsences, violationCourses);
                                    absenceAlerts.add(alert);
                                    
                                    // Update adapter
                                    adapter.notifyDataSetChanged();
                                    
                                    // Show alerts if this was the last student being processed
                                    if (absenceAlerts.size() > 0) {
                                        showAbsenceAlerts();
                                    }
                                }
                                break; // Only process the first match
                            }
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        // Continue without this student's information
                    }
                });
    }

    private void showAbsenceAlerts() {
        noAlertsLayout.setVisibility(View.GONE);
        absenceAlertsRecyclerView.setVisibility(View.VISIBLE);
    }

    private void showNoAlertsMessage(String title, String subtitle) {
        noAlertsText.setText(title);
        noAlertsSubtext.setText(subtitle);
        noAlertsLayout.setVisibility(View.VISIBLE);
        absenceAlertsRecyclerView.setVisibility(View.GONE);
    }
    
    private void sendSmsToParent(StudentAbsenceAlert alert, StudentAbsenceAlertAdapter.ViewHolder holder) {
        // Show loading message
        Toast.makeText(this, "Sending SMS to parent...", Toast.LENGTH_SHORT).show();
        
        // Load student's parent information
        mDatabase.child("users").child("students").orderByChild("edpNumber").equalTo(alert.getEdpNumber())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                                String guardianContactNumber = studentSnapshot.child("guardianContactNumber").getValue(String.class);
                                String studentFirstName = studentSnapshot.child("firstName").getValue(String.class);
                                String studentLastName = studentSnapshot.child("lastName").getValue(String.class);
                                
                                if (guardianContactNumber != null && !guardianContactNumber.trim().isEmpty()) {
                                    // Create SMS message
                                    String message = createSmsMessage(alert, studentFirstName, studentLastName);
                                    
                                    // Send SMS
                                    SemaphoreSmsSender.sendSMS(guardianContactNumber, message);
                                    
                                    // Update button state to success
                                    adapter.setButtonSuccessState(holder);
                                    
                                    Toast.makeText(SmsAlertsActivity.this, 
                                            "SMS sent to parent successfully!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(SmsAlertsActivity.this, 
                                            "Parent contact number not found for this student", Toast.LENGTH_LONG).show();
                                }
                                break; // Only process the first match
                            }
                        } else {
                            Toast.makeText(SmsAlertsActivity.this, 
                                    "Student information not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        Toast.makeText(SmsAlertsActivity.this, 
                                "Failed to load student information", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
    private String createSmsMessage(StudentAbsenceAlert alert, String firstName, String lastName) {
        StringBuilder message = new StringBuilder();
        message.append("Good day, Here-O Attendance has detected that your son/daughter ");
        message.append(firstName != null ? firstName : "").append(" ");
        message.append(lastName != null ? lastName : "");
        message.append(" have been absent more than 3 times in these subjects: ");
        
        // Add course names (without schedule)
        List<StudentAbsenceAlert.CourseAbsence> courseAbsences = alert.getCourseAbsences();
        for (int i = 0; i < courseAbsences.size(); i++) {
            if (i > 0) {
                message.append(", ");
            }
            message.append(courseAbsences.get(i).getCourseName());
        }
        
        return message.toString();
    }
    
    private void showOverflowMenu(StudentAbsenceAlert alert, View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add("Clear violation");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().equals("Clear violation")) {
                showClearViolationConfirmation(alert);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    private void showClearViolationConfirmation(StudentAbsenceAlert alert) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Clear Violation");
        builder.setMessage("Are you sure you want to clear the violation for " + alert.getFullName() + "? This action cannot be undone.");
        
        builder.setPositiveButton("Yes, Clear", (dialog, which) -> {
            clearStudentViolation(alert);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void clearStudentViolation(StudentAbsenceAlert alert) {
        // Remove the alert from the list
        absenceAlerts.remove(alert);
        adapter.notifyDataSetChanged();
        
        // Show success message
        Toast.makeText(this, "Violation cleared for " + alert.getFullName(), Toast.LENGTH_SHORT).show();
        
        // Check if no more alerts
        if (absenceAlerts.isEmpty()) {
            showNoAlertsMessage("No SMS alerts yet", "SMS alerts will appear here when students have 3+ absences");
        }
    }
    
}
