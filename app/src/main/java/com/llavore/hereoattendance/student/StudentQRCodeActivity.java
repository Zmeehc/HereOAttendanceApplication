package com.llavore.hereoattendance.student;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.model.User;
import com.llavore.hereoattendance.utils.SessionManager;
import com.bumptech.glide.Glide;

import org.json.JSONException;
import org.json.JSONObject;

public class StudentQRCodeActivity extends AppCompatActivity {

    private ImageView backArrow, studentProfilePicture, qrCodeImage;
    private TextView studentName;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_qr_code);
        
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize components
        initializeViews();
        initializeFirebase();
        
        // Setup click listeners
        setupClickListeners();
        
        // Load student data and generate QR code
        loadStudentData();
    }

    private void initializeViews() {
        backArrow = findViewById(R.id.backArrow);
        studentProfilePicture = findViewById(R.id.studentProfilePicture);
        qrCodeImage = findViewById(R.id.qrCodeImage);
        studentName = findViewById(R.id.studentName);
    }

    private void initializeFirebase() {
        sessionManager = new SessionManager(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
    }

    private void setupClickListeners() {
        backArrow.setOnClickListener(v -> finish());
    }

    private void loadStudentData() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        android.util.Log.d("StudentQRCode", "Loading student data for user: " + userId);

        mDatabase.child("users").child("students").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                displayStudentData(user, snapshot);
                                generateQRCode(user, snapshot);
                            } else {
                                android.util.Log.e("StudentQRCode", "Failed to parse user data");
                                Toast.makeText(StudentQRCodeActivity.this, "Error loading student data", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            android.util.Log.e("StudentQRCode", "Student data not found");
                            Toast.makeText(StudentQRCodeActivity.this, "Student data not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError error) {
                        android.util.Log.e("StudentQRCode", "Failed to load student data: " + error.getMessage());
                        Toast.makeText(StudentQRCodeActivity.this, "Error loading student data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void displayStudentData(User user, DataSnapshot snapshot) {
        // Display student name
        String fullName = (user.getFirstName() + " " + user.getLastName()).trim();
        studentName.setText(fullName);

        // Load profile picture
        if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(user.getProfileImageUrl())
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .circleCrop()
                    .into(studentProfilePicture);
        } else {
            studentProfilePicture.setImageResource(R.drawable.baseline_person_24);
        }

        android.util.Log.d("StudentQRCode", "Displayed student data for: " + fullName);
    }

    private void generateQRCode(User user, DataSnapshot snapshot) {
        try {
            // Get all student data from snapshot
            String edpNumber = snapshot.child("edpNumber").getValue(String.class);
            String guardianName = snapshot.child("guardianName").getValue(String.class);
            String guardianContact = snapshot.child("guardianContactNumber").getValue(String.class);
            String yearLevel = snapshot.child("yearLevel").getValue(String.class);
            String middleName = user.getMiddleName();
            String profileImageUrl = user.getProfileImageUrl();

            // Create comprehensive JSON object with all student information
            JSONObject studentInfo = new JSONObject();
            
            // Basic Information
            studentInfo.put("edpNumber", edpNumber != null ? edpNumber : "");
            studentInfo.put("firstName", user.getFirstName() != null ? user.getFirstName() : "");
            studentInfo.put("middleName", middleName != null ? middleName : "");
            studentInfo.put("lastName", user.getLastName() != null ? user.getLastName() : "");
            studentInfo.put("email", user.getEmail() != null ? user.getEmail() : "");
            studentInfo.put("gender", user.getGender() != null ? user.getGender() : "");
            studentInfo.put("birthdate", user.getBirthdate() != null ? user.getBirthdate() : "");
            studentInfo.put("contactNumber", user.getContactNumber() != null ? user.getContactNumber() : "");
            studentInfo.put("program", user.getProgram() != null ? user.getProgram() : "");
            studentInfo.put("yearLevel", yearLevel != null ? yearLevel : "");
            studentInfo.put("userType", user.getUserType() != null ? user.getUserType() : "");
            
            // Guardian Information
            studentInfo.put("guardianName", guardianName != null ? guardianName : "");
            studentInfo.put("guardianContact", guardianContact != null ? guardianContact : "");
            
            // Profile Information
            studentInfo.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
            
            // Additional metadata
            studentInfo.put("createdAt", user.getCreatedAt() > 0 ? String.valueOf(user.getCreatedAt()) : "");
            studentInfo.put("idNumber", user.getIdNumber() != null ? user.getIdNumber() : "");

            String qrData = studentInfo.toString();
            android.util.Log.d("StudentQRCode", "QR Code data: " + qrData);

            // Generate QR code bitmap (increased size from 512x512 to 800x800)
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(qrData, BarcodeFormat.QR_CODE, 800, 800);
            
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    // Use get method with proper error handling
                    boolean isBlack = false;
                    try {
                        isBlack = bitMatrix.get(x, y);
                    } catch (Exception e) {
                        android.util.Log.e("StudentQRCode", "Error getting bit at (" + x + "," + y + "): " + e.getMessage());
                        isBlack = false;
                    }
                    bitmap.setPixel(x, y, isBlack ? 0xFF000000 : 0xFFFFFFFF);
                }
            }

            // Display QR code
            qrCodeImage.setImageBitmap(bitmap);
            
            android.util.Log.d("StudentQRCode", "QR Code generated successfully");

        } catch (WriterException e) {
            android.util.Log.e("StudentQRCode", "Failed to generate QR code: " + e.getMessage());
            Toast.makeText(this, "Failed to generate QR code", Toast.LENGTH_SHORT).show();
        } catch (JSONException e) {
            android.util.Log.e("StudentQRCode", "Failed to create JSON: " + e.getMessage());
            Toast.makeText(this, "Failed to create QR code data", Toast.LENGTH_SHORT).show();
        }
    }
}
