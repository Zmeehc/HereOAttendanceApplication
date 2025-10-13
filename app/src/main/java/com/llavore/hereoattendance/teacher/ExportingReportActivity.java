package com.llavore.hereoattendance.teacher;

import android.Manifest;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.AttendanceRecord;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ExportingReportActivity extends AppCompatActivity {
    
    // Views
    private ImageView backArrow, optionsMenu, dateRangeDropdown, excelIcon;
    private TextView fileNameTxt, dateRangeTxt, progressText;
    private LinearLayout dateHeadersContainer, studentsContainer, progressSection;
    private ProgressBar exportProgressBar;
    private com.google.android.material.button.MaterialButton exportBtn;
    
    // Loading state management
    private View loadingLayout;
    private View tableScrollView;
    private int loadingTasksCompleted = 0;
    private final int totalLoadingTasks = 3; // Course data, session dates, and attendance data
    
    // Data
    private String courseCode, courseName;
    private String startDate, endDate;
    private String fileName = "attendance_report.xlsx";
    private List<String> sessionDates = new ArrayList<>();
    private List<String> studentNames = new ArrayList<>();
    private Map<String, Map<String, String>> attendanceData = new HashMap<>();
    private Map<String, String> studentNameToEdpMap = new HashMap<>(); // Maps student name to EDP number
    
    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    
    // Permission launcher
    private ActivityResultLauncher<String> requestPermissionLauncher;
    
    // Notification
    private NotificationManager notificationManager;
    private static final String CHANNEL_ID = "export_progress";
    private static final int NOTIFICATION_ID = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_exporting_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        
        // Initialize permission launcher
        requestPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.RequestPermission(),
                isGranted -> {
                    android.util.Log.d("ExportingReport", "Permission result received: " + isGranted);
                    
                    // Double-check permission status
                    boolean hasPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                            == PackageManager.PERMISSION_GRANTED;
                    android.util.Log.d("ExportingReport", "Permission double-check result: " + hasPermission);
                    
                    if (isGranted && hasPermission) {
                        android.util.Log.d("ExportingReport", "Storage permission confirmed, starting export");
                        exportToExcel();
                    } else {
                        android.util.Log.d("ExportingReport", "Storage permission denied or not properly granted");
                        Toast.makeText(this, "Storage permission is required to export files. Please enable it in Settings > Apps > HereOAttendance > Permissions > Storage", Toast.LENGTH_LONG).show();
                    }
                }
        );
        
        // Initialize notification
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        createNotificationChannel();
        requestNotificationPermission();
        
        initializeViews();
        loadCourseData();
        setupClickListeners();
        setDefaultDateRange();
        loadSessionDates();
    }
    
    private void initializeViews() {
        backArrow = findViewById(R.id.backArrow);
        optionsMenu = findViewById(R.id.optionsMenu);
        dateRangeDropdown = findViewById(R.id.dateRangeDropdown);
        excelIcon = findViewById(R.id.excelIcon);
        fileNameTxt = findViewById(R.id.fileNameTxt);
        dateRangeTxt = findViewById(R.id.dateRangeTxt);
        dateHeadersContainer = findViewById(R.id.dateHeadersContainer);
        studentsContainer = findViewById(R.id.studentsContainer);
        exportBtn = findViewById(R.id.exportBtn);
        progressSection = findViewById(R.id.progressSection);
        progressText = findViewById(R.id.progressText);
        exportProgressBar = findViewById(R.id.exportProgressBar);
        
        // Initialize loading views
        loadingLayout = findViewById(R.id.loadingLayout);
        tableScrollView = findViewById(R.id.tableScrollView);
        
        // Show loading initially
        showLoading();
    }
    
    private void showLoading() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }
        if (tableScrollView != null) {
            tableScrollView.setVisibility(View.GONE);
        }
    }
    
    private void hideLoading() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }
        if (tableScrollView != null) {
            tableScrollView.setVisibility(View.VISIBLE);
        }
    }
    
    private void onLoadingTaskCompleted() {
        loadingTasksCompleted++;
        if (loadingTasksCompleted >= totalLoadingTasks) {
            hideLoading();
        }
    }
    
    private void loadCourseData() {
        Intent intent = getIntent();
        if (intent != null) {
            courseCode = intent.getStringExtra("courseCode");
            courseName = intent.getStringExtra("courseName");
            
            if (courseCode != null && courseName != null) {
                // Generate default filename based on course and current date
                SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy", Locale.getDefault());
                String currentDate = dateFormat.format(new Date());
                fileName = courseName.replaceAll("[^a-zA-Z0-9]", "_") + "_" + currentDate + ".xlsx";
                fileNameTxt.setText("File name: " + fileName);
            }
        }
        onLoadingTaskCompleted();
    }
    
    private void setupClickListeners() {
        backArrow.setOnClickListener(v -> finish());
        
        optionsMenu.setOnClickListener(v -> showRenameDialog());
        
        dateRangeDropdown.setOnClickListener(v -> showDateRangePicker());
        
        exportBtn.setOnClickListener(v -> {
            // For Android 11+, we need to use a different approach
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                // Android 11+ - use scoped storage or request MANAGE_EXTERNAL_STORAGE
                if (Environment.isExternalStorageManager()) {
                    android.util.Log.d("ExportingReport", "External storage manager permission granted");
                    exportToExcel();
                } else {
                    android.util.Log.d("ExportingReport", "External storage manager permission not granted, requesting...");
                    showStoragePermissionDialog();
                }
            } else {
                // Android 10 and below - use traditional storage permission
                boolean hasStoragePermission = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                        == PackageManager.PERMISSION_GRANTED;
                
                android.util.Log.d("ExportingReport", "Storage permission check result: " + hasStoragePermission);
                
                if (!hasStoragePermission) {
                    android.util.Log.d("ExportingReport", "Storage permission not granted, requesting...");
                    requestPermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE);
                } else {
                    android.util.Log.d("ExportingReport", "Storage permission already granted, starting export");
                    exportToExcel();
                }
            }
        });
    }
    
    private void setDefaultDateRange() {
        // Set default date range to last 30 days
        Calendar calendar = Calendar.getInstance();
        endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        
        calendar.add(Calendar.DAY_OF_MONTH, -30);
        startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.getTime());
        
        updateDateRangeDisplay();
    }
    
    private void updateDateRangeDisplay() {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            
            Date start = inputFormat.parse(startDate);
            Date end = inputFormat.parse(endDate);
            
            String startStr = outputFormat.format(start);
            String endStr = outputFormat.format(end);
            
            dateRangeTxt.setText(startStr + " - " + endStr);
        } catch (Exception e) {
            android.util.Log.e("ExportingReport", "Error formatting date range: " + e.getMessage());
        }
    }
    
    private void showDateRangePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog startDatePicker = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar startCalendar = Calendar.getInstance();
                    startCalendar.set(selectedYear, selectedMonth, selectedDay);
                    startDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(startCalendar.getTime());
                    
                    // Show end date picker
                    showEndDatePicker();
                }, year, month, day);
        
        startDatePicker.setTitle("Select Start Date");
        startDatePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        startDatePicker.show();
    }
    
    private void showEndDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        
        DatePickerDialog endDatePicker = new DatePickerDialog(this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    Calendar endCalendar = Calendar.getInstance();
                    endCalendar.set(selectedYear, selectedMonth, selectedDay);
                    endDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(endCalendar.getTime());
                    
                    updateDateRangeDisplay();
                    loadSessionDates();
                }, year, month, day);
        
        endDatePicker.setTitle("Select End Date");
        endDatePicker.getDatePicker().setMaxDate(System.currentTimeMillis());
        endDatePicker.show();
    }
    
    private void showRenameDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename File");
        
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(fileName);
        builder.setView(input);
        
        builder.setPositiveButton("OK", (dialog, which) -> {
            String newFileName = input.getText().toString().trim();
            if (!newFileName.isEmpty()) {
                if (!newFileName.endsWith(".xlsx")) {
                    newFileName += ".xlsx";
                }
                fileName = newFileName;
                fileNameTxt.setText("File name: " + fileName);
            }
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
    
    private void loadSessionDates() {
        if (courseCode == null) return;
        
        android.util.Log.d("ExportingReport", "Loading session dates for course: " + courseCode);
        android.util.Log.d("ExportingReport", "Date range: " + startDate + " to " + endDate);
        
        mDatabase.child("courses").child(courseCode).child("sessions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        sessionDates.clear();
                        
                        for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                            String date = dateSnapshot.getKey();
                            if (date != null && isDateInRange(date)) {
                                sessionDates.add(date);
                            }
                        }
                        
                        Collections.sort(sessionDates);
                        android.util.Log.d("ExportingReport", "Found " + sessionDates.size() + " session dates");
                        
                        loadAttendanceData();
                        onLoadingTaskCompleted();
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("ExportingReport", "Failed to load session dates: " + error.getMessage());
                        onLoadingTaskCompleted();
                    }
                });
    }
    
    private boolean isDateInRange(String date) {
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            Date checkDate = dateFormat.parse(date);
            Date start = dateFormat.parse(startDate);
            Date end = dateFormat.parse(endDate);
            
            return !checkDate.before(start) && !checkDate.after(end);
        } catch (Exception e) {
            android.util.Log.e("ExportingReport", "Error parsing date: " + e.getMessage());
            return false;
        }
    }
    
    private void loadAttendanceData() {
        if (courseCode == null || sessionDates.isEmpty()) {
            android.util.Log.d("ExportingReport", "No session dates to load attendance for");
            return;
        }
        
        android.util.Log.d("ExportingReport", "Loading attendance data for " + sessionDates.size() + " dates");
        
        // Clear previous data
        studentNames.clear();
        attendanceData.clear();
        studentNameToEdpMap.clear();
        
        // Get all enrolled students first
        mDatabase.child("courses").child(courseCode).child("students")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentsSnapshot) {
                        List<String> studentIds = new ArrayList<>();
                        
                        // Collect all student IDs
                        for (DataSnapshot studentSnapshot : studentsSnapshot.getChildren()) {
                            String studentId = studentSnapshot.getKey();
                            if (studentId != null) {
                                studentIds.add(studentId);
                            }
                        }
                        
                        android.util.Log.d("ExportingReport", "Found " + studentIds.size() + " enrolled students");
                        
                        // Load student data and attendance for all students
                        loadAllStudentsData(studentIds, 0);
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("ExportingReport", "Failed to get enrolled students: " + error.getMessage());
                    }
                });
    }
    
    private void loadAllStudentsData(List<String> studentIds, int index) {
        if (index >= studentIds.size()) {
            // All students loaded, now load attendance data
            android.util.Log.d("ExportingReport", "All student data loaded, now loading attendance data");
            loadAllAttendanceData();
            return;
        }
        
        String studentId = studentIds.get(index);
        mDatabase.child("users").child("students").child(studentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentDataSnapshot) {
                        if (studentDataSnapshot.exists()) {
                            String firstName = studentDataSnapshot.child("firstName").getValue(String.class);
                            String lastName = studentDataSnapshot.child("lastName").getValue(String.class);
                            String edpNumber = studentDataSnapshot.child("edpNumber").getValue(String.class);
                            
                            if (firstName != null && lastName != null && edpNumber != null) {
                                String fullName = lastName + ", " + firstName;
                                studentNames.add(fullName);
                                
                                // Initialize attendance data for this student
                                attendanceData.put(edpNumber, new HashMap<>());
                                
                                // Map student name to EDP number for easy lookup
                                studentNameToEdpMap.put(fullName, edpNumber);
                                
                                android.util.Log.d("ExportingReport", "Loaded student: " + fullName + " (EDP: " + edpNumber + ")");
                            }
                        }
                        
                        // Load next student
                        loadAllStudentsData(studentIds, index + 1);
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("ExportingReport", "Failed to get student data for " + studentId + ": " + error.getMessage());
                        // Continue with next student even if this one failed
                        loadAllStudentsData(studentIds, index + 1);
                    }
                });
    }
    
    private void loadAllAttendanceData() {
        if (studentNames.isEmpty() || sessionDates.isEmpty()) {
            android.util.Log.d("ExportingReport", "No students or session dates to load attendance for");
            displayAttendanceTable();
            return;
        }
        
        android.util.Log.d("ExportingReport", "Loading attendance data for " + studentNames.size() + " students and " + sessionDates.size() + " dates");
        
        // Create a counter to track how many attendance records we need to load
        int totalRecords = studentNames.size() * sessionDates.size();
        final int[] loadedRecords = {0};
        
        // Load attendance for each student and each date
        for (String edpNumber : attendanceData.keySet()) {
            for (String date : sessionDates) {
                mDatabase.child("courses").child(courseCode).child("sessions").child(date)
                        .child("attendance").child(edpNumber)
                        .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                String status = "Absent"; // Default status
                                
                                if (snapshot.exists()) {
                                    AttendanceRecord record = snapshot.getValue(AttendanceRecord.class);
                                    if (record != null && record.getStatus() != null) {
                                        status = record.getStatus();
                                    }
                                }
                                
                                // Store attendance data
                                if (attendanceData.containsKey(edpNumber)) {
                                    attendanceData.get(edpNumber).put(date, status);
                                }
                                
                                android.util.Log.d("ExportingReport", "Loaded attendance for EDP " + edpNumber + " on " + date + ": " + status);
                                
                                // Increment counter
                                loadedRecords[0]++;
                                
                                // Check if all records are loaded
                                if (loadedRecords[0] >= totalRecords) {
                                    android.util.Log.d("ExportingReport", "All attendance data loaded, displaying table");
                                    displayAttendanceTable();
                                }
                            }
                            
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                android.util.Log.e("ExportingReport", "Failed to get attendance for EDP " + edpNumber + " on " + date + ": " + error.getMessage());
                                
                                // Still increment counter and check if done
                                loadedRecords[0]++;
                                if (loadedRecords[0] >= totalRecords) {
                                    android.util.Log.d("ExportingReport", "All attendance data loaded (with some errors), displaying table");
                                    displayAttendanceTable();
                                }
                            }
                        });
            }
        }
    }
    
    
    private void displayAttendanceTable() {
        // Clear existing views
        dateHeadersContainer.removeAllViews();
        studentsContainer.removeAllViews();
        
        // Calculate cell width based on longest status text
        int cellWidth = calculateCellWidth();
        
        // Add date headers
        for (String date : sessionDates) {
            TextView dateHeader = new TextView(this);
            dateHeader.setText(formatDateForDisplay(date));
            dateHeader.setTextColor(getResources().getColor(R.color.white));
            dateHeader.setTextSize(14);
            // Convert dp to pixels for padding
            float density = getResources().getDisplayMetrics().density;
            int paddingHorizontal = (int) (12 * density); // 12dp
            int paddingVertical = (int) (8 * density); // 8dp
            dateHeader.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            dateHeader.setWidth(cellWidth);
            dateHeader.setGravity(android.view.Gravity.CENTER);
            dateHeader.setBackgroundColor(getResources().getColor(R.color.green));
            dateHeader.setTypeface(getResources().getFont(R.font.poppins_semibold));
            dateHeadersContainer.addView(dateHeader);
        }
        
        // Add student rows
        Collections.sort(studentNames);
        for (String studentName : studentNames) {
            View studentRow = createStudentRow(studentName, cellWidth);
            studentsContainer.addView(studentRow);
        }
        
        // Mark attendance data loading as completed
        onLoadingTaskCompleted();
    }
    
    private int calculateCellWidth() {
        // Use a more reasonable width that's still large enough for text
        float density = getResources().getDisplayMetrics().density;
        return (int) (120 * density); // 120dp width - closer columns but still readable
    }
    
    private String formatDateForDisplay(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("M-d-yy", Locale.getDefault());
            Date dateObj = inputFormat.parse(date);
            return outputFormat.format(dateObj);
        } catch (Exception e) {
            return date;
        }
    }
    
    private View createStudentRow(String studentName, int cellWidth) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View row = inflater.inflate(R.layout.attendance_export_row, studentsContainer, false);
        
        TextView studentNameTxt = row.findViewById(R.id.studentName);
        LinearLayout attendanceContainer = row.findViewById(R.id.attendanceContainer);
        
        studentNameTxt.setText(studentName);
        
        // Add attendance status for each date
        for (String date : sessionDates) {
            TextView statusTxt = new TextView(this);
            statusTxt.setWidth(cellWidth);
            statusTxt.setHeight((int) (60 * getResources().getDisplayMetrics().density)); // 60dp height
            // Convert dp to pixels for padding
            float density = getResources().getDisplayMetrics().density;
            int paddingHorizontal = (int) (12 * density); // 12dp
            int paddingVertical = (int) (8 * density); // 8dp
            statusTxt.setPadding(paddingHorizontal, paddingVertical, paddingHorizontal, paddingVertical);
            statusTxt.setGravity(android.view.Gravity.CENTER);
            statusTxt.setTextSize(16); // Increased text size for better visibility
            statusTxt.setTypeface(getResources().getFont(R.font.poppins_regular));
            
            // Find the student's EDP number and get their status
            String status = "ABSENT";
            String edpNumber = studentNameToEdpMap.get(studentName);
            if (edpNumber != null && attendanceData.containsKey(edpNumber)) {
                Map<String, String> studentAttendance = attendanceData.get(edpNumber);
                if (studentAttendance.containsKey(date)) {
                    status = studentAttendance.get(date);
                }
            }
            
            statusTxt.setText(status);
            android.util.Log.d("ExportingReport", "Setting status text: " + status + " for student: " + studentName + " on date: " + date);
            
            // Set color based on status
            switch (status) {
                case "PRESENT":
                    statusTxt.setTextColor(getResources().getColor(R.color.green));
                    break;
                case "ABSENT":
                    statusTxt.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                    break;
                case "EXCUSED":
                    statusTxt.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                    break;
                case "LATE":
                    statusTxt.setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                    break;
                default:
                    statusTxt.setTextColor(getResources().getColor(android.R.color.black));
                    break;
            }
            
            // Set background to white for better visibility
            statusTxt.setBackgroundColor(getResources().getColor(R.color.white));
            
            attendanceContainer.addView(statusTxt);
        }
        
        return row;
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Export Progress",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Shows progress of attendance report export");
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                    != PackageManager.PERMISSION_GRANTED) {
                // Request notification permission
                ActivityResultLauncher<String> notificationPermissionLauncher = 
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                        if (isGranted) {
                            android.util.Log.d("ExportingReport", "Notification permission granted");
                        } else {
                            android.util.Log.d("ExportingReport", "Notification permission denied");
                            Toast.makeText(this, "Notification permission is recommended for export progress updates", Toast.LENGTH_SHORT).show();
                        }
                    });
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
    }
    
    private void showStoragePermissionDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Storage Permission Required")
                .setMessage("This app needs permission to save files to your Downloads folder. Please grant 'All files access' permission in the next screen.")
                .setPositiveButton("Grant Permission", (dialog, which) -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        try {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        } catch (Exception e) {
                            // Fallback to general settings
                            Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                            startActivity(intent);
                        }
                    }
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    Toast.makeText(this, "Export cancelled - storage permission is required", Toast.LENGTH_SHORT).show();
                })
                .show();
    }
    
    private void exportToExcel() {
        // Show progress bar
        runOnUiThread(() -> {
            progressSection.setVisibility(View.VISIBLE);
            exportBtn.setEnabled(false);
            exportBtn.setText("Exporting...");
            progressText.setText("Preparing Excel file...");
            exportProgressBar.setProgress(0);
        });
        
        // Start export in background thread
        new Thread(() -> {
            try {
                // Create Excel file - always use public Downloads directory
                File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                File excelFile = new File(downloadsDir, fileName);
                
                android.util.Log.d("ExportingReport", "Attempting to create file at: " + excelFile.getAbsolutePath());
                android.util.Log.d("ExportingReport", "Downloads directory exists: " + downloadsDir.exists());
                android.util.Log.d("ExportingReport", "Downloads directory writable: " + downloadsDir.canWrite());
                
                updateProgress(10, "Creating Excel file...");
                
                // Create Excel-compatible HTML file that can be opened in Excel
                StringBuilder htmlContent = new StringBuilder();
                htmlContent.append("<!DOCTYPE html>\n");
                htmlContent.append("<html>\n");
                htmlContent.append("<head>\n");
                htmlContent.append("<meta charset=\"UTF-8\">\n");
                htmlContent.append("<style>\n");
                htmlContent.append("table { border-collapse: collapse; width: 100%; }\n");
                htmlContent.append("th, td { border: 1px solid #ddd; padding: 8px; text-align: center; }\n");
                htmlContent.append("th { background-color: #90EE90; font-weight: bold; }\n");
                htmlContent.append(".present { background-color: #90EE90; }\n");
                htmlContent.append(".absent { background-color: #FFB6C1; }\n");
                htmlContent.append(".excused { background-color: #87CEEB; }\n");
                htmlContent.append(".late { background-color: #FFA500; }\n");
                htmlContent.append("</style>\n");
                htmlContent.append("</head>\n");
                htmlContent.append("<body>\n");
                htmlContent.append("<h2>Attendance Report - ").append(courseName).append("</h2>\n");
                htmlContent.append("<p>Date Range: ").append(dateRangeTxt.getText().toString()).append("</p>\n");
                htmlContent.append("<table>\n");
                
                updateProgress(20, "Setting up headers...");
                
                // Create header row
                htmlContent.append("<tr>\n");
                htmlContent.append("<th>Student</th>\n");
                for (String date : sessionDates) {
                    htmlContent.append("<th>").append(formatDateForDisplay(date)).append("</th>\n");
                }
                htmlContent.append("</tr>\n");
                
                updateProgress(30, "Writing student data...");
                
                // Write student data
                Collections.sort(studentNames);
                int totalStudents = studentNames.size();
                
                for (int i = 0; i < totalStudents; i++) {
                    String studentName = studentNames.get(i);
                    htmlContent.append("<tr>\n");
                    htmlContent.append("<td>").append(studentName).append("</td>\n");
                    
                    // Attendance data
                    for (String date : sessionDates) {
                        String status = "Absent";
                        String edpNumber = studentNameToEdpMap.get(studentName);
                        if (edpNumber != null && attendanceData.containsKey(edpNumber)) {
                            Map<String, String> studentAttendance = attendanceData.get(edpNumber);
                            if (studentAttendance.containsKey(date)) {
                                status = studentAttendance.get(date);
                            }
                        }
                        
                        String cssClass = "";
                        switch (status) {
                            case "PRESENT":
                                cssClass = "present";
                                break;
                            case "ABSENT":
                                cssClass = "absent";
                                break;
                            case "EXCUSED":
                                cssClass = "excused";
                                break;
                            case "LATE":
                                cssClass = "late";
                                break;
                        }
                        
                        htmlContent.append("<td class=\"").append(cssClass).append("\">").append(status).append("</td>\n");
                    }
                    htmlContent.append("</tr>\n");
                    
                    // Update progress
                    int progress = 30 + (int) ((i + 1) * 50.0 / totalStudents);
                    updateProgress(progress, "Writing student " + (i + 1) + " of " + totalStudents + "...");
                }
                
                htmlContent.append("</table>\n");
                htmlContent.append("</body>\n");
                htmlContent.append("</html>");
                
                updateProgress(80, "Finalizing Excel file...");
                
                // Test if we can write to the file
                try {
                    FileOutputStream fileOut = new FileOutputStream(excelFile);
                    fileOut.write(htmlContent.toString().getBytes("UTF-8"));
                    fileOut.close();
                    android.util.Log.d("ExportingReport", "File written successfully");
                } catch (IOException e) {
                    android.util.Log.e("ExportingReport", "Failed to write file: " + e.getMessage());
                    throw e;
                }
                
                updateProgress(100, "Export completed!");
                
                // Save export to database
                saveExportToDatabase(excelFile.getAbsolutePath());
                
                // Show success message
                runOnUiThread(() -> {
                    String message = "Excel file exported successfully to Downloads folder\nPath: " + excelFile.getAbsolutePath();
                    
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                    android.util.Log.d("ExportingReport", "Excel file exported: " + excelFile.getAbsolutePath());
                    
                    // Hide progress bar and reset button
                    progressSection.setVisibility(View.GONE);
                    exportBtn.setEnabled(true);
                    exportBtn.setText("Export");
                });
                
            } catch (IOException e) {
                android.util.Log.e("ExportingReport", "Error exporting Excel: " + e.getMessage());
                runOnUiThread(() -> {
                    Toast.makeText(this, "Error exporting file: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    progressSection.setVisibility(View.GONE);
                    exportBtn.setEnabled(true);
                    exportBtn.setText("Export");
                });
            }
        }).start();
    }
    
    private void updateProgress(int progress, String message) {
        runOnUiThread(() -> {
            exportProgressBar.setProgress(progress);
            progressText.setText(message);
        });
    }
    
    private void saveExportToDatabase(String filePath) {
        String currentUserId = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            android.util.Log.e("ExportingReport", "User not logged in, cannot save export");
            return;
        }
        
        // Create export ID
        String exportId = mDatabase.child("users").child("teachers").child(currentUserId).child("savedExports").push().getKey();
        
        // Create date range string
        String dateRange = startDate + " - " + endDate;
        
        // Log the file path for debugging
        android.util.Log.d("ExportingReport", "Saving export with file path: " + filePath);
        
        // Create SavedExport object
        com.llavore.hereoattendance.models.SavedExport savedExport = new com.llavore.hereoattendance.models.SavedExport(
                exportId,
                fileName,
                courseCode,
                courseName,
                startDate,
                endDate,
                dateRange,
                filePath,
                System.currentTimeMillis(),
                currentUserId
        );
        
        // Save to Firebase
        mDatabase.child("users").child("teachers").child(currentUserId).child("savedExports")
                .child(exportId).setValue(savedExport)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("ExportingReport", "Export saved to database successfully with ID: " + exportId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("ExportingReport", "Failed to save export to database: " + e.getMessage());
                });
    }
}
