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
import com.llavore.hereoattendance.models.SmsNotificationHistory;
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
    private List<SmsNotificationHistory> notificationHistory;
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
        
        notificationHistory = new ArrayList<>();
        adapter = new StudentAbsenceAlertAdapter(notificationHistory, this);
        
        absenceAlertsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        absenceAlertsRecyclerView.setAdapter(adapter);
        
        // Set up adapter click listeners
        adapter.setOnOverflowMenuClickListener((notification, view) -> {
            showOverflowMenu(notification, view);
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
            showNoAlertsMessage("Not logged in", "Please log in to view SMS notifications");
            return;
        }

        android.util.Log.d("SmsAlertsActivity", "Loading SMS notifications for teacher: " + currentTeacherId);

        // Load SMS notification history for this teacher
        mDatabase.child("smsNotifications").child(currentTeacherId)
                .orderByChild("timestamp")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        android.util.Log.d("SmsAlertsActivity", "DataSnapshot exists: " + snapshot.exists());
                        android.util.Log.d("SmsAlertsActivity", "DataSnapshot children count: " + snapshot.getChildrenCount());
                        
                        notificationHistory.clear();
                        
                        if (snapshot.exists()) {
                            for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                                android.util.Log.d("SmsAlertsActivity", "Processing notification: " + notificationSnapshot.getKey());
                                SmsNotificationHistory notification = notificationSnapshot.getValue(SmsNotificationHistory.class);
                                if (notification != null) {
                                    android.util.Log.d("SmsAlertsActivity", "Added notification for student: " + notification.getStudentFullName());
                                    notificationHistory.add(notification);
                                } else {
                                    android.util.Log.e("SmsAlertsActivity", "Failed to parse notification from snapshot");
                                }
                            }
                        }
                        
                        android.util.Log.d("SmsAlertsActivity", "Total notifications loaded: " + notificationHistory.size());
                        
                        // Sort by timestamp (newest first)
                        notificationHistory.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                        
                        adapter.notifyDataSetChanged();
                        
                        if (notificationHistory.isEmpty()) {
                            showNoAlertsMessage("No SMS notifications yet", "SMS notifications will appear here when students reach 3 absences");
                        } else {
                            showAbsenceAlerts();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        android.util.Log.e("SmsAlertsActivity", "Database error loading notifications: " + databaseError.getMessage());
                        android.util.Log.e("SmsAlertsActivity", "Error code: " + databaseError.getCode());
                        android.util.Log.e("SmsAlertsActivity", "Error details: " + databaseError.getDetails());
                        
                        // Handle specific error types
                        if (databaseError.getCode() == DatabaseError.PERMISSION_DENIED) {
                            showNoAlertsMessage("Permission denied", "Please contact administrator to configure SMS notification permissions");
                        } else {
                            showNoAlertsMessage("Error loading notifications", "Please try again later");
                        }
                    }
                });
    }

    // This method will be called from the attendance system when a student reaches exactly 3 absences
    public static void checkAndSendSmsIfNeeded(String teacherId, String studentEdpNumber, String courseCode, String courseName, android.content.Context context) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        
        android.util.Log.d("SmsAlertsActivity", "checkAndSendSmsIfNeeded called for student: " + studentEdpNumber + " in course: " + courseCode);
        
        // Load student information
        mDatabase.child("users").child("students").orderByChild("edpNumber").equalTo(studentEdpNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        android.util.Log.d("SmsAlertsActivity", "Student data snapshot exists: " + snapshot.exists());
                        
                        if (snapshot.exists()) {
                            for (DataSnapshot studentSnapshot : snapshot.getChildren()) {
                                String guardianContactNumber = studentSnapshot.child("guardianContactNumber").getValue(String.class);
                                String studentFirstName = studentSnapshot.child("firstName").getValue(String.class);
                                String studentLastName = studentSnapshot.child("lastName").getValue(String.class);
                                String studentProfileImageUrl = studentSnapshot.child("profileImageUrl").getValue(String.class);
                                
                                android.util.Log.d("SmsAlertsActivity", "Student: " + studentFirstName + " " + studentLastName);
                                android.util.Log.d("SmsAlertsActivity", "Guardian contact: " + guardianContactNumber);
                                
                                if (guardianContactNumber != null && !guardianContactNumber.trim().isEmpty()) {
                                    // Create SMS message
                                    String message = createSmsMessage(studentFirstName, studentLastName, courseName);
                                    
                                    android.util.Log.d("SmsAlertsActivity", "Sending SMS to: " + guardianContactNumber);
                                    android.util.Log.d("SmsAlertsActivity", "SMS message: " + message);
                                    
                                    // Send SMS (asynchronous - no return value)
                                    SemaphoreSmsSender.sendSMS(guardianContactNumber, message);
                                    
                                    // Show toast that SMS is being sent
                                    if (context != null) {
                                        android.widget.Toast.makeText(context, "SMS being sent to parent", android.widget.Toast.LENGTH_LONG).show();
                                    }
                                    
                                    // Save notification to history
                                    saveSmsNotification(teacherId, studentEdpNumber, studentFirstName, studentLastName, 
                                                      studentProfileImageUrl, guardianContactNumber, message, courseCode, courseName);
                                    
                                    // Reset absence counter for this student in this course
                                    resetAbsenceCounter(studentEdpNumber, courseCode);
                                    
                                    android.util.Log.d("SmsAlertsActivity", "SMS notification saved and counter reset for student: " + studentEdpNumber);
                                } else {
                                    android.util.Log.e("SmsAlertsActivity", "Guardian contact number is null or empty for student: " + studentEdpNumber);
                                    if (context != null) {
                                        android.widget.Toast.makeText(context, "Parent contact number not found for student", android.widget.Toast.LENGTH_LONG).show();
                                    }
                                }
                                break; // Only process the first match
                            }
                        } else {
                            android.util.Log.e("SmsAlertsActivity", "Student not found with EDP number: " + studentEdpNumber);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        android.util.Log.e("SmsAlertsActivity", "Database error loading student data: " + databaseError.getMessage());
                    }
                });
    }
    
    // Helper method to create SMS message
    private static String createSmsMessage(String firstName, String lastName, String courseName) {
        StringBuilder message = new StringBuilder();
        message.append("Good day, Here-O Attendance has detected that your son/daughter ");
        message.append(firstName != null ? firstName : "").append(" ");
        message.append(lastName != null ? lastName : "");
        message.append(" has been absent 3 times in ").append(courseName);
        message.append(". Please be informed.");
        return message.toString();
    }
    
    // Helper method to save SMS notification to history
    private static void saveSmsNotification(String teacherId, String studentEdpNumber, String studentFirstName, 
                                          String studentLastName, String studentProfileImageUrl, String guardianContactNumber, 
                                          String message, String courseCode, String courseName) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        
        android.util.Log.d("SmsAlertsActivity", "Saving SMS notification for teacher: " + teacherId);
        
        SmsNotificationHistory notification = new SmsNotificationHistory(
                studentEdpNumber, studentFirstName, studentLastName, studentProfileImageUrl,
                guardianContactNumber, message, System.currentTimeMillis(), courseCode, courseName
        );
        
        // Save to teacher's SMS notification history
        mDatabase.child("smsNotifications").child(teacherId).push().setValue(notification)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("SmsAlertsActivity", "SMS notification saved successfully to database");
                    } else {
                        android.util.Log.e("SmsAlertsActivity", "Failed to save SMS notification: " + task.getException());
                        if (task.getException() != null) {
                            android.util.Log.e("SmsAlertsActivity", "Exception details: " + task.getException().getMessage());
                        }
                    }
                });
    }
    
    // Helper method to reset absence counter after sending SMS
    private static void resetAbsenceCounter(String studentEdpNumber, String courseCode) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Reset the absence counter for this student in this course
        // We'll store this in a separate node to track absence cycles
        mDatabase.child("absenceCounters").child(courseCode).child(studentEdpNumber).setValue(0);
    }
    
    // Method to check and increment absence count, send SMS if needed
    public static void checkAndIncrementAbsenceCount(String teacherId, String studentEdpNumber, String courseCode, String courseName, android.content.Context context) {
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        
        android.util.Log.d("SmsAlertsActivity", "checkAndIncrementAbsenceCount called for student: " + studentEdpNumber + " in course: " + courseCode);
        
        // Get current absence count for this student in this course
        mDatabase.child("absenceCounters").child(courseCode).child(studentEdpNumber)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        int currentCount = 0;
                        if (snapshot.exists()) {
                            Integer count = snapshot.getValue(Integer.class);
                            if (count != null) {
                                currentCount = count;
                            }
                        }
                        
                        android.util.Log.d("SmsAlertsActivity", "Current absence count: " + currentCount);
                        
                        // Increment the count
                        int newCount = currentCount + 1;
                        
                        android.util.Log.d("SmsAlertsActivity", "New absence count: " + newCount);
                        
                        // Update the counter
                        mDatabase.child("absenceCounters").child(courseCode).child(studentEdpNumber).setValue(newCount);
                        
                        // Check if this is exactly the 3rd absence
                        if (newCount == 3) {
                            android.util.Log.d("SmsAlertsActivity", "3rd absence detected! Sending SMS notification");
                            
                            // Show toast notification that 3 absences detected
                            if (context != null) {
                                android.widget.Toast.makeText(context, "3 absences detected. Sending SMS to Parent", android.widget.Toast.LENGTH_LONG).show();
                            }
                            
                            // Send SMS notification
                            checkAndSendSmsIfNeeded(teacherId, studentEdpNumber, courseCode, courseName, context);
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        android.util.Log.e("SmsAlertsActivity", "Database error checking absence count: " + databaseError.getMessage());
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
    
    
    private void showOverflowMenu(SmsNotificationHistory notification, View view) {
        PopupMenu popupMenu = new PopupMenu(this, view);
        popupMenu.getMenu().add("Delete notification");
        
        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().toString().equals("Delete notification")) {
                showDeleteConfirmation(notification);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    private void showDeleteConfirmation(SmsNotificationHistory notification) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete Notification");
        builder.setMessage("Are you sure you want to delete this SMS notification record? This action cannot be undone.");
        
        builder.setPositiveButton("Yes, Delete", (dialog, which) -> {
            deleteNotification(notification);
        });
        
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });
        
        AlertDialog dialog = builder.create();
        dialog.show();
    }
    
    private void deleteNotification(SmsNotificationHistory notification) {
        // Remove from Firebase database first
        mDatabase.child("smsNotifications").child(currentTeacherId)
                .orderByChild("timestamp").equalTo(notification.getTimestamp())
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            for (DataSnapshot notificationSnapshot : snapshot.getChildren()) {
                                // Delete from Firebase
                                notificationSnapshot.getRef().removeValue()
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                android.util.Log.d("SmsAlertsActivity", "Notification deleted from database");
                                                
                                                // Remove from local list
                                                notificationHistory.remove(notification);
                                                adapter.notifyDataSetChanged();
                                                
                                                // Show success message
                                                Toast.makeText(SmsAlertsActivity.this, "Notification deleted", Toast.LENGTH_SHORT).show();
                                                
                                                // Check if no more notifications
                                                if (notificationHistory.isEmpty()) {
                                                    showNoAlertsMessage("No SMS notifications yet", "SMS notifications will appear here when students reach 3 absences");
                                                }
                                            } else {
                                                android.util.Log.e("SmsAlertsActivity", "Failed to delete notification from database: " + task.getException());
                                                Toast.makeText(SmsAlertsActivity.this, "Failed to delete notification", Toast.LENGTH_SHORT).show();
                                            }
                                        });
                                break; // Only delete the first match
                            }
                        } else {
                            android.util.Log.e("SmsAlertsActivity", "Notification not found in database");
                            Toast.makeText(SmsAlertsActivity.this, "Notification not found", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        android.util.Log.e("SmsAlertsActivity", "Database error deleting notification: " + databaseError.getMessage());
                        Toast.makeText(SmsAlertsActivity.this, "Error deleting notification", Toast.LENGTH_SHORT).show();
                    }
                });
    }
    
}
