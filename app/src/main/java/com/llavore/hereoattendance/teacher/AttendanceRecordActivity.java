package com.llavore.hereoattendance.teacher;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import com.google.android.material.button.MaterialButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import androidx.annotation.NonNull;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.AttendanceRecordAdapter;
import com.llavore.hereoattendance.models.AttendanceRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AttendanceRecordActivity extends AppCompatActivity {
    
    private ImageView backArrow, toolbarOptionsMenu, dateDropdownIcon;
    private TextView courseTitleDetail, courseScheduleDetail, dateTxt, dayTxt;
    private TextView presentCount, absentCount, excusedCount, lateCount;
    private RecyclerView attendanceRecyclerView;
    private MaterialButton excuseStudentsButton;
    private String selectedDate; // Format: yyyy-MM-dd
    private boolean isExcuseMode = false;
    
    // Course data
    private String courseId, courseCode, courseName, courseRoom, courseSchedule, courseStartTime, courseEndTime;
    private int studentCount, sessionCount;
    private boolean isArchived = false;
    
    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    
    // Attendance data
    private List<AttendanceRecord> attendanceRecords;
    private AttendanceRecordAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_attendance_record);

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        
        initializeViews();
        loadCourseData();
        setCurrentDate();
        handleArchivedCourseState();
        setupBackButton();
        setupOptionsMenu();
        setupDatePicker();
        setupExcuseStudentsButton();
        setupRecyclerView();
        loadAttendanceData();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void initializeViews() {
        backArrow = findViewById(R.id.backArrow);
        toolbarOptionsMenu = findViewById(R.id.toolbarOptionsMenu);
        dateDropdownIcon = findViewById(R.id.dateDropdownIcon);
        courseTitleDetail = findViewById(R.id.courseTitleDetail);
        courseScheduleDetail = findViewById(R.id.courseScheduleDetail);
        dateTxt = findViewById(R.id.dateTxt);
        dayTxt = findViewById(R.id.dayTxt);
        presentCount = findViewById(R.id.presentCount);
        absentCount = findViewById(R.id.absentCount);
        excusedCount = findViewById(R.id.excusedCount);
        lateCount = findViewById(R.id.lateCount);
        attendanceRecyclerView = findViewById(R.id.attendanceRecyclerView);
        excuseStudentsButton = findViewById(R.id.btnExcuseStudents);
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
            isArchived = intent.getBooleanExtra("isArchived", false);
            
            if (courseName != null && courseRoom != null) {
                courseTitleDetail.setText(String.format("%s | %s", courseName, courseRoom));
            }
            
            if (courseStartTime != null && courseEndTime != null && courseSchedule != null) {
                courseScheduleDetail.setText(String.format("%s - %s | %s", courseStartTime, courseEndTime, courseSchedule));
            }
            
            // If no selected date provided, use current date
            if (selectedDate == null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate = dateFormat.format(Calendar.getInstance().getTime());
            }
        }
    }
    
    private void handleArchivedCourseState() {
        if (isArchived) {
            // Disable excuse students button
            excuseStudentsButton.setEnabled(false);
            excuseStudentsButton.setAlpha(0.5f);
            excuseStudentsButton.setText("Archived Course");
            
            // Disable date picker
            dateDropdownIcon.setEnabled(false);
            dateDropdownIcon.setAlpha(0.5f);
            
            // Show archived indicator in title
            String currentTitle = courseTitleDetail.getText().toString();
            courseTitleDetail.setText(currentTitle + " (ARCHIVED)");
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
            
            dateTxt.setText(date);
            dayTxt.setText(day);
        } catch (Exception e) {
            android.util.Log.e("AttendanceRecord", "Error parsing date: " + e.getMessage());
            // Fallback to current date
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
            
            String date = dateFormat.format(calendar.getTime());
            String day = dayFormat.format(calendar.getTime());
            
            dateTxt.setText(date);
            dayTxt.setText(day);
        }
    }
    
    private void setupBackButton() {
        backArrow.setOnClickListener(v -> {
            // Just finish this activity to return to the previous CourseDetails
            finish();
        });
    }
    
    private void setupOptionsMenu() {
        toolbarOptionsMenu.setOnClickListener(v -> {
            PopupMenu popupMenu = new PopupMenu(this, v);
            popupMenu.getMenuInflater().inflate(R.menu.attendance_record_options_menu, popupMenu.getMenu());
            
            popupMenu.setOnMenuItemClickListener(item -> {
                if (item.getItemId() == R.id.menu_export_record) {
                    // Navigate to ExportingReportActivity
                    Intent intent = new Intent(this, ExportingReportActivity.class);
                    intent.putExtra("courseCode", courseCode);
                    intent.putExtra("courseName", courseName);
                    startActivity(intent);
                    return true;
                }
                return false;
            });
            
            popupMenu.show();
        });
    }
    
    private void setupDatePicker() {
        dateDropdownIcon.setOnClickListener(v -> {
            if (!isArchived) {
                showDatePicker();
            }
        });
    }
    
    private void setupExcuseStudentsButton() {
        excuseStudentsButton.setOnClickListener(v -> {
            if (!isArchived) {
                if (!isExcuseMode) {
                    // Enter excuse mode
                    isExcuseMode = true;
                    excuseStudentsButton.setText("Save");
                    adapter.setExcuseMode(true);
                } else {
                    // Save changes and exit excuse mode
                    isExcuseMode = false;
                    excuseStudentsButton.setText("Excuse Students");
                    adapter.setExcuseMode(false);
                }
            }
        });
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
                    SimpleDateFormat dbDateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                    selectedDate = dbDateFormat.format(selectedCalendar.getTime());
                    
                    // Update the UI
                    setCurrentDate();
                    
                    // Load attendance data for selected date
                    loadAttendanceData();
                    
                    android.util.Log.d("AttendanceRecord", "Selected date: " + selectedDate);
                }, year, month, day);
        
        // Set maximum date to today (can't select future dates)
        datePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());
        datePickerDialog.show();
    }
    
    private void setupRecyclerView() {
        attendanceRecords = new ArrayList<>();
        adapter = new AttendanceRecordAdapter(attendanceRecords);
        attendanceRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        attendanceRecyclerView.setAdapter(adapter);
        
        // Set up excuse status change listener
        adapter.setOnExcuseStatusChangeListener((record, isExcused) -> {
            updateStudentStatus(record, isExcused);
        });
    }
    
    private void loadAttendanceData() {
        if (courseCode == null || selectedDate == null) {
            android.util.Log.e("AttendanceRecord", "Course code or selected date is null");
            return;
        }

        android.util.Log.d("AttendanceRecord", "Loading attendance for date: " + selectedDate);

        // Listen for real-time updates to attendance for selected date
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                .child("attendance").addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        attendanceRecords.clear();
                        int present = 0, absent = 0, excused = 0, late = 0;
                        
                        for (DataSnapshot child : snapshot.getChildren()) {
                            AttendanceRecord record = child.getValue(AttendanceRecord.class);
                            if (record != null) {
                                // Add ALL students to the attendance record (including ABSENT and EXCUSED)
                                // This is the complete attendance record
                                attendanceRecords.add(record);
                                
                                // Count by status
                                String status = record.getStatus();
                                if ("PRESENT".equals(status)) {
                                    present++;
                                } else if ("ABSENT".equals(status)) {
                                    absent++;
                                } else if ("EXCUSED".equals(status)) {
                                    excused++;
                                } else if ("LATE".equals(status)) {
                                    late++;
                                }
                            }
                        }
                        
                        // Update counts
                        presentCount.setText(String.valueOf(present));
                        absentCount.setText(String.valueOf(absent));
                        excusedCount.setText(String.valueOf(excused));
                        lateCount.setText(String.valueOf(late));
                        
                        adapter.notifyDataSetChanged();
                        android.util.Log.d("AttendanceRecord", "Loaded " + attendanceRecords.size() + " attendance records for " + selectedDate);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("AttendanceRecord", "Failed to load attendance: " + error.getMessage());
                    }
                });
    }
    
    private void updateStudentStatus(AttendanceRecord record, boolean isExcused) {
        if (courseCode == null || selectedDate == null) {
            android.util.Log.e("AttendanceRecord", "Course code or selected date is null");
            return;
        }
        
        String newStatus = isExcused ? "EXCUSED" : "ABSENT";
        android.util.Log.d("AttendanceRecord", "Updating student " + record.getFirstName() + " " + record.getLastName() + " status to: " + newStatus);
        
        // Update the record's status
        record.setStatus(newStatus);
        
        // Update in database
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                .child("attendance").child(record.getEdpNumber()).setValue(record)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("AttendanceRecord", "Successfully updated student status to: " + newStatus);
                        // Update the local list and refresh the adapter
                        updateLocalAttendanceRecords();
                    } else {
                        android.util.Log.e("AttendanceRecord", "Failed to update student status: " + task.getException());
                    }
                });
    }
    
    private void updateLocalAttendanceRecords() {
        // Update the local list to reflect the changes
        for (int i = 0; i < attendanceRecords.size(); i++) {
            AttendanceRecord record = attendanceRecords.get(i);
            // The record should already be updated by the updateStudentStatus method
        }
        
        // Notify adapter of changes
        adapter.notifyDataSetChanged();
        
        // Update counts
        updateStatusCounts();
    }
    
    private void updateStatusCounts() {
        int present = 0, absent = 0, excused = 0, late = 0;
        
        for (AttendanceRecord record : attendanceRecords) {
            switch (record.getStatus()) {
                case "PRESENT":
                    present++;
                    break;
                case "ABSENT":
                    absent++;
                    break;
                case "EXCUSED":
                    excused++;
                    break;
                case "LATE":
                    late++;
                    break;
            }
        }
        
        presentCount.setText(String.valueOf(present));
        absentCount.setText(String.valueOf(absent));
        excusedCount.setText(String.valueOf(excused));
        lateCount.setText(String.valueOf(late));
    }
}
