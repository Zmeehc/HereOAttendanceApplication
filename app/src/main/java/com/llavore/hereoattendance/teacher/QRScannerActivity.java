package com.llavore.hereoattendance.teacher;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.StudentsPresentAdapter;
import com.llavore.hereoattendance.models.AttendanceRecord;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class QRScannerActivity extends AppCompatActivity {
    
    private ImageView backArrow;
    private PreviewView cameraPreview;
    private TextView scanStatus;
    private RecyclerView studentsRecyclerView;
    
    // Scanned student card views
    private android.view.View scannedStudentCard;
    private de.hdodenhof.circleimageview.CircleImageView scannedStudentImage;
    private TextView scannedStudentName;
    private TextView scannedStudentEdp;
    private TextView scannedStudentStatus;
    
    // Camera and scanning
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private BarcodeScanner barcodeScanner; // Temporarily commented out
    
    // Data
    private String courseId, courseCode, sessionId, selectedDate;
    private List<AttendanceRecord> presentStudents;
    private StudentsPresentAdapter adapter;
    
    // Firebase
    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;
    
    // Permission launcher
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_LONG).show();
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_qr_scanner);
        
        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        
        initializeViews();
        loadCourseData();
        setupRecyclerView();
        setupBarcodeScanner();
        checkCameraPermission();
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
    
    private void initializeViews() {
        backArrow = findViewById(R.id.backArrow);
        cameraPreview = findViewById(R.id.cameraPreview);
        scanStatus = findViewById(R.id.scanStatus);
        studentsRecyclerView = findViewById(R.id.studentsRecyclerView);
        
        // Initialize scanned student card views
        scannedStudentCard = findViewById(R.id.scannedStudentCard);
        scannedStudentImage = findViewById(R.id.scannedStudentImage);
        scannedStudentName = findViewById(R.id.scannedStudentName);
        scannedStudentEdp = findViewById(R.id.scannedStudentEdp);
        scannedStudentStatus = findViewById(R.id.scannedStudentStatus);
        
        backArrow.setOnClickListener(v -> finish());
    }
    
    private void loadCourseData() {
        Intent intent = getIntent();
        if (intent != null) {
            courseId = intent.getStringExtra("courseId");
            courseCode = intent.getStringExtra("courseCode");
            sessionId = intent.getStringExtra("sessionId");
            selectedDate = intent.getStringExtra("selectedDate");
            
            // If no selected date provided, use current date
            if (selectedDate == null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                selectedDate = dateFormat.format(Calendar.getInstance().getTime());
            }
            
            android.util.Log.d("QRScanner", "Course ID: " + courseId + ", Course Code: " + courseCode + ", Session ID: " + sessionId + ", Selected Date: " + selectedDate);
        }
    }
    
    private void setupRecyclerView() {
        // RecyclerView is now handled in CourseDetails, not here
        // This method is kept for compatibility but doesn't do anything
    }
    
    private void setupBarcodeScanner() {
        // Temporarily commented out ML Kit barcode scanner
         BarcodeScannerOptions options = new BarcodeScannerOptions.Builder()
                 .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                 .build();
        barcodeScanner = BarcodeScanning.getClient(options);
    }
    
    private void checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }
    
    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraUseCases();
            } catch (ExecutionException | InterruptedException e) {
                android.util.Log.e("QRScanner", "Camera initialization failed", e);
                Toast.makeText(this, "Camera initialization failed", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }
    
    private void bindCameraUseCases() {
        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(cameraPreview.getSurfaceProvider());
        
        ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build();
        
        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(this), this::analyzeImage);
        
        CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
        
        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);
        } catch (Exception e) {
            android.util.Log.e("QRScanner", "Camera binding failed", e);
        }
    }
    
    private void analyzeImage(ImageProxy image) {
        InputImage inputImage = InputImage.fromMediaImage(image.getImage(), image.getImageInfo().getRotationDegrees());
        
        Task<List<Barcode>> task = barcodeScanner.process(inputImage);
        task.addOnSuccessListener(barcodes -> {
            for (Barcode barcode : barcodes) {
                if (barcode.getRawValue() != null) {
                    processQRCode(barcode.getRawValue());
                }
            }
        }).addOnFailureListener(e -> {
            android.util.Log.e("QRScanner", "Barcode scanning failed", e);
        }).addOnCompleteListener(task1 -> {
            image.close();
        });
    }
    
    private void processQRCode(String qrData) {
        try {
            JSONObject studentInfo = new JSONObject(qrData);
            String edpNumber = studentInfo.optString("edpNumber", "");
            String firstName = studentInfo.optString("firstName", "");
            String lastName = studentInfo.optString("lastName", "");
            String profileImageUrl = studentInfo.optString("profileImageUrl", "");
            
            android.util.Log.d("QRScanner", "Scanned student: " + firstName + " " + lastName + " (EDP: " + edpNumber + ")");
            
            // First, check if student is enrolled in this course
            checkStudentEnrollment(edpNumber, firstName, lastName, profileImageUrl);
            
        } catch (JSONException e) {
            android.util.Log.e("QRScanner", "Failed to parse QR code data", e);
            runOnUiThread(() -> {
                scanStatus.setText("Invalid QR code format");
                Toast.makeText(this, "Invalid QR code format", Toast.LENGTH_SHORT).show();
            });
        }
    }
    
    private void checkStudentEnrollment(String edpNumber, String firstName, String lastName, String profileImageUrl) {
        android.util.Log.d("QRScanner", "Checking enrollment for student: " + edpNumber + " in course: " + courseCode);
        
        // First, find the student ID by EDP number
        mDatabase.child("users").child("students").orderByChild("edpNumber").equalTo(edpNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        android.util.Log.d("QRScanner", "Query result - snapshot exists: " + snapshot.exists());
                        android.util.Log.d("QRScanner", "Query result - children count: " + snapshot.getChildrenCount());
                        
                        if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                            // Get the student ID
                            String studentId = snapshot.getChildren().iterator().next().getKey();
                            android.util.Log.d("QRScanner", "Found student ID: " + studentId + " for EDP: " + edpNumber);
                            
                            // Now check if this student is enrolled in the course
                            mDatabase.child("courses").child(courseCode).child("students").child(studentId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot enrollmentSnapshot) {
                                            android.util.Log.d("QRScanner", "Enrollment check - snapshot exists: " + enrollmentSnapshot.exists());
                                            if (enrollmentSnapshot.exists()) {
                                                android.util.Log.d("QRScanner", "Student is enrolled in course");
                                                checkIfAlreadyPresent(edpNumber, firstName, lastName, profileImageUrl, studentId);
                                            } else {
                                                android.util.Log.d("QRScanner", "Student is not enrolled in course");
                                                runOnUiThread(() -> {
                                                    scanStatus.setText("Student not enrolled in this course");
                                                    Toast.makeText(QRScannerActivity.this, firstName + " " + lastName + " is not enrolled in this course", Toast.LENGTH_LONG).show();
                                                });
                                            }
                                        }
                                        
                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            android.util.Log.e("QRScanner", "Failed to check enrollment: " + error.getMessage());
                                            android.util.Log.e("QRScanner", "Error code: " + error.getCode());
                                            runOnUiThread(() -> {
                                                scanStatus.setText("Error checking enrollment");
                                                Toast.makeText(QRScannerActivity.this, "Error checking student enrollment", Toast.LENGTH_SHORT).show();
                                            });
                                        }
                                    });
                        } else {
                            android.util.Log.d("QRScanner", "Student with EDP number not found in database");
                            android.util.Log.d("QRScanner", "Trying alternative approach - checking all students...");
                            
                            // Try alternative approach - get all students and check manually
                            checkAllStudentsForEDP(edpNumber, firstName, lastName, profileImageUrl);
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("QRScanner", "Failed to find student by EDP number: " + error.getMessage());
                        android.util.Log.e("QRScanner", "Error code: " + error.getCode());
                        android.util.Log.d("QRScanner", "Trying alternative approach - checking all students...");
                        
                        // Try alternative approach if the query fails
                        checkAllStudentsForEDP(edpNumber, firstName, lastName, profileImageUrl);
                    }
                });
    }
    
    private void checkAllStudentsForEDP(String edpNumber, String firstName, String lastName, String profileImageUrl) {
        android.util.Log.d("QRScanner", "Checking all students for EDP: " + edpNumber);
        
        mDatabase.child("users").child("students").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("QRScanner", "All students query - snapshot exists: " + snapshot.exists());
                android.util.Log.d("QRScanner", "All students query - children count: " + snapshot.getChildrenCount());
                
                boolean found = false;
                for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                    String studentId = studentSnapshot.getKey();
                    String studentEdpNumber = studentSnapshot.child("edpNumber").getValue(String.class);
                    
                    android.util.Log.d("QRScanner", "Checking student ID: " + studentId + ", EDP: " + studentEdpNumber);
                    
                    if (edpNumber.equals(studentEdpNumber)) {
                        android.util.Log.d("QRScanner", "Found matching student: " + studentId);
                        found = true;
                        
                        // Check if this student is enrolled in the course
                        mDatabase.child("courses").child(courseCode).child("students").child(studentId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot enrollmentSnapshot) {
                                        if (enrollmentSnapshot.exists()) {
                                            android.util.Log.d("QRScanner", "Student is enrolled in course");
                                            checkIfAlreadyPresent(edpNumber, firstName, lastName, profileImageUrl, studentId);
                                        } else {
                                            android.util.Log.d("QRScanner", "Student is not enrolled in course");
                                            runOnUiThread(() -> {
                                                scanStatus.setText("Student not enrolled in this course");
                                                Toast.makeText(QRScannerActivity.this, firstName + " " + lastName + " is not enrolled in this course", Toast.LENGTH_LONG).show();
                                            });
                                        }
                                    }
                                    
                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        android.util.Log.e("QRScanner", "Failed to check enrollment: " + error.getMessage());
                                    }
                                });
                        break;
                    }
                }
                
                if (!found) {
                    android.util.Log.d("QRScanner", "Student with EDP " + edpNumber + " not found in any students");
                    runOnUiThread(() -> {
                        scanStatus.setText("Student not found");
                        Toast.makeText(QRScannerActivity.this, "Student with EDP " + edpNumber + " not found", Toast.LENGTH_LONG).show();
                    });
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("QRScanner", "Failed to get all students: " + error.getMessage());
                android.util.Log.e("QRScanner", "Error code: " + error.getCode());
                runOnUiThread(() -> {
                    scanStatus.setText("Error finding student");
                    Toast.makeText(QRScannerActivity.this, "Error finding student", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void checkIfAlreadyPresent(String edpNumber, String firstName, String lastName, String profileImageUrl, String studentId) {
        // Check if student is already marked present for the selected date's session
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                .child("attendance").child(edpNumber).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            // Student is already present
                            runOnUiThread(() -> {
                                scanStatus.setText("Student already marked present");
                                Toast.makeText(QRScannerActivity.this, firstName + " " + lastName + " is already present", Toast.LENGTH_SHORT).show();
                            });
                        } else {
                            // Student is not present yet, mark them as present
                            markStudentPresent(edpNumber, firstName, lastName, profileImageUrl, studentId);
                        }
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("QRScanner", "Failed to check if student is already present: " + error.getMessage());
                    }
                });
    }
    
    private void markStudentPresent(String edpNumber, String firstName, String lastName, String profileImageUrl, String studentId) {
        // Get session data to check late attendance time
        getSessionDataAndMarkAttendance(edpNumber, firstName, lastName, profileImageUrl, studentId);
    }
    
    private void getSessionDataAndMarkAttendance(String edpNumber, String firstName, String lastName, String profileImageUrl, String studentId) {
        // Get session data to retrieve late attendance time
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String lateAttendanceTime = snapshot.child("lateAttendanceTime").getValue(String.class);
                            String classEndTime = snapshot.child("classEndTime").getValue(String.class);
                            
                            // Determine attendance status based on current time
                            String attendanceStatus = determineAttendanceStatus(lateAttendanceTime, classEndTime);
                            
                            // Create attendance record
                            String currentTime = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(Calendar.getInstance().getTime());
                            AttendanceRecord attendanceRecord = new AttendanceRecord(edpNumber, firstName, lastName, profileImageUrl, currentTime, attendanceStatus);
                            
                            // Update UI feedback
                            runOnUiThread(() -> {
                                String statusText = attendanceStatus.equals("LATE") ? "late" : "present";
                                scanStatus.setText("Student marked " + statusText + ": " + firstName + " " + lastName);
                                Toast.makeText(QRScannerActivity.this, firstName + " " + lastName + " marked " + statusText, Toast.LENGTH_SHORT).show();
                                
                                // Show scanned student card
                                showScannedStudentCard(firstName, lastName, edpNumber, profileImageUrl, attendanceStatus);
                            });

                            // Save to database
                            saveAttendanceToDatabase(attendanceRecord);
                        } else {
                            android.util.Log.e("QRScanner", "Session not found for date: " + selectedDate);
                            runOnUiThread(() -> {
                                scanStatus.setText("Session not found");
                                Toast.makeText(QRScannerActivity.this, "Session not found for selected date", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("QRScanner", "Failed to get session data: " + error.getMessage());
                        runOnUiThread(() -> {
                            scanStatus.setText("Error getting session data");
                            Toast.makeText(QRScannerActivity.this, "Error getting session data", Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }
    
    private String determineAttendanceStatus(String lateAttendanceTime, String classEndTime) {
        try {
            // Get current time
            Calendar currentCalendar = Calendar.getInstance();
            String currentTimeStr = new SimpleDateFormat("h:mm a", Locale.getDefault()).format(currentCalendar.getTime());
            
            // Parse times
            SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            
            Date currentTime = timeFormat.parse(currentTimeStr);
            Date lateTime = timeFormat.parse(lateAttendanceTime);
            Date endTime = timeFormat.parse(classEndTime);
            
            // Compare times
            if (currentTime.after(endTime)) {
                // If current time is after class end time, mark as ABSENT (shouldn't happen in normal flow)
                return "ABSENT";
            } else if (currentTime.after(lateTime) || currentTime.equals(lateTime)) {
                // If current time is at or after late attendance time, mark as LATE
                return "LATE";
            } else {
                // If current time is before late attendance time, mark as PRESENT
                return "PRESENT";
            }
        } catch (Exception e) {
            android.util.Log.e("QRScanner", "Error parsing time: " + e.getMessage());
            // Default to PRESENT if there's an error
            return "PRESENT";
        }
    }
    
    private void saveAttendanceToDatabase(AttendanceRecord record) {
        String userId = mAuth.getCurrentUser().getUid();
        if (userId == null) return;
        
        // Save to session attendance (new structure: /courses/{courseCode}/sessions/{date}/attendance/)
        mDatabase.child("courses").child(courseCode).child("sessions").child(selectedDate)
                .child("attendance").child(record.getEdpNumber()).setValue(record)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("QRScanner", "Attendance saved to database");
                    } else {
                        android.util.Log.e("QRScanner", "Failed to save attendance: " + task.getException().getMessage());
                    }
                });
    }
    
    private void loadExistingAttendance() {
        // Attendance loading is now handled in CourseDetails
        // This method is kept for compatibility but doesn't do anything
    }
    
    private void showScannedStudentCard(String firstName, String lastName, String edpNumber, String profileImageUrl, String attendanceStatus) {
        // Update the scanned student card with student information
        scannedStudentName.setText(firstName + " " + lastName);
        scannedStudentEdp.setText("EDP: " + edpNumber);
        scannedStudentStatus.setText(attendanceStatus);
        
        // Set status background color based on attendance status
        if ("LATE".equals(attendanceStatus)) {
            scannedStudentStatus.setBackgroundResource(R.drawable.status_late_background);
            scannedStudentStatus.setText("LATE");
        } else if ("PRESENT".equals(attendanceStatus)) {
            scannedStudentStatus.setBackgroundResource(R.drawable.status_present_background);
            scannedStudentStatus.setText("PRESENT");
        }
        
        // Load profile image using Glide
        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(scannedStudentImage);
        } else {
            scannedStudentImage.setImageResource(R.drawable.default_profile);
        }
        
        // Show the card with animation
        scannedStudentCard.setVisibility(android.view.View.VISIBLE);
        scannedStudentCard.setAlpha(0f);
        scannedStudentCard.animate()
                .alpha(1f)
                .setDuration(300)
                .start();
        
        // Hide the card after 5 seconds
        scannedStudentCard.postDelayed(() -> {
            scannedStudentCard.animate()
                    .alpha(0f)
                    .setDuration(500)
                    .withEndAction(() -> scannedStudentCard.setVisibility(android.view.View.GONE))
                    .start();
        }, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (barcodeScanner != null) {
            barcodeScanner.close();
        }
    }
}
