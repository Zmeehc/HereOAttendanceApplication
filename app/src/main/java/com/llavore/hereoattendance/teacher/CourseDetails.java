package com.llavore.hereoattendance.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class CourseDetails extends AppCompatActivity {

    private TextView courseTitleDetail, courseScheduleDetail, classCodeDetail;
    private TextView studentsTextDetail, sessionsTextDetail;
    private TextView dateTextDetail, dayTextDetail;
    private ImageView backArrow;
    private Timer dateUpdateTimer;
    private MaterialButton btnSetAttendance;
    private DatabaseReference mDatabase;
    private String courseId;
    private String courseCode;

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
        setupBackButton();
        startDateUpdateTimer();
        setupRealTimeStudentCountListener();

        btnSetAttendance.setOnClickListener(v -> {
            Intent intent = new Intent(CourseDetails.this, SetAttendanceActivity.class);
            startActivity(intent);
        });
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
        btnSetAttendance = findViewById(R.id.setAttendanceBtnDetail);
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

    private void setCurrentDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        
        String currentDate = dateFormat.format(calendar.getTime());
        String currentDay = dayFormat.format(calendar.getTime());
        
        dateTextDetail.setText(currentDate);
        dayTextDetail.setText("- " + currentDay);
    }

    private void setupBackButton() {
        backArrow.setOnClickListener(v -> {
            Intent intent = new Intent(CourseDetails.this, ActiveCoursesActivity.class);
            startActivity(intent);
            finish();
        });
    }

    private void startDateUpdateTimer() {
        // Check every minute if the date has changed
        dateUpdateTimer = new Timer();
        dateUpdateTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> setCurrentDate());
            }
        }, 60000, 60000); // Check every minute
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dateUpdateTimer != null) {
            dateUpdateTimer.cancel();
        }
    }
}