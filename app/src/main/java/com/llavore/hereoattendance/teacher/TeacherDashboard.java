package com.llavore.hereoattendance.teacher;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.model.User;
import com.llavore.hereoattendance.utils.SessionManager;
import com.llavore.hereoattendance.utils.NavigationHeaderManager;
import com.llavore.hereoattendance.utils.TeacherNavigationManager;
import com.llavore.hereoattendance.utils.TransitionManager;
import com.llavore.hereoattendance.adapters.TodaySessionAdapter;
import com.llavore.hereoattendance.models.TodaySession;
import com.bumptech.glide.Glide;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;
import java.util.ArrayList;

public class TeacherDashboard extends AppCompatActivity {

    private ImageView burgerIcon;
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    private NavigationHeaderManager headerManager;
    private TeacherNavigationManager navigationManager;
    
    private RecyclerView todaysSessionsRecyclerView;
    private TextView todaysSessionsTitle;
    private TodaySessionAdapter todaySessionAdapter;
    private List<TodaySession> todaySessions;

    private CardView coursesBtn;
    
    // Loading state management
    private View loadingLayout;
    private View mainContentScrollView;
    private int loadingTasksCompleted = 0;
    private final int totalLoadingTasks = 7; // Profile picture, user data, course count, SMS count, archive count, saved exports count, today's sessions
    private android.os.Handler loadingTimeoutHandler;
    private static final int LOADING_TIMEOUT_MS = 10000; // 10 seconds timeout
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_teacher_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize loading views
        loadingLayout = findViewById(R.id.loadingLayout);
        mainContentScrollView = findViewById(R.id.mainContentScrollView);
        
        // Show loading initially
        showLoading();
        
        coursesBtn = findViewById(R.id.courseCardView);
        coursesBtn.setOnClickListener(v -> {
            TransitionManager.startActivityForward(TeacherDashboard.this, ActiveCoursesActivity.class);
            finish();
        });

        sessionManager = new SessionManager(this);
        headerManager = new NavigationHeaderManager(sessionManager);
        navigationManager = new TeacherNavigationManager(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();

        DrawerLayout drawerLayout = findViewById(R.id.main);
        burgerIcon = findViewById(R.id.burgerIcon);
        NavigationView navigationView = findViewById(R.id.navigationView);
        
        // Setup navigation using the common manager
        navigationManager.setupNavigationDrawer(drawerLayout, burgerIcon, navigationView, "dashboard");

        // Setup dashboard card click listeners
        setupDashboardCards();
        
        // Set current date and day
        setCurrentDateAndDay();
        
        // Start timer to update date at midnight
        startDateUpdateTimer();
        
        // Load user profile picture and data
        loadUserProfilePicture();
        loadUserData();
        loadCourseCount();
        loadSmsAlertCount();
        loadArchiveCount();
        loadSavedExportsCount();
        initializeTodaysSessions();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile picture when returning to dashboard
        loadUserProfilePicture();
        
        // Refresh course count and SMS alert count when returning to dashboard
        loadCourseCount();
        loadSmsAlertCount();
        loadArchiveCount();
        loadSavedExportsCount();
        loadTodaysSessions();
        
        // Refresh current date and day when returning to dashboard
        setCurrentDateAndDay();
        
        // Update navigation drawer to highlight dashboard
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationManager.setCurrentActivity(navigationView, "dashboard");
        
        // Reset loading state for onResume
        loadingTasksCompleted = 0;
        showLoading();
    }
    
    private void showLoading() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }
        if (mainContentScrollView != null) {
            mainContentScrollView.setVisibility(View.GONE);
        }
        
        // Start timeout handler
        loadingTimeoutHandler = new android.os.Handler();
        loadingTimeoutHandler.postDelayed(() -> {
            android.util.Log.w("TeacherDashboard", "Loading timeout reached, hiding loading screen");
            hideLoading();
        }, LOADING_TIMEOUT_MS);
    }
    
    private void hideLoading() {
        // Cancel timeout handler
        if (loadingTimeoutHandler != null) {
            loadingTimeoutHandler.removeCallbacksAndMessages(null);
        }
        
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }
        if (mainContentScrollView != null) {
            mainContentScrollView.setVisibility(View.VISIBLE);
        }
    }
    
    private void onLoadingTaskCompleted() {
        loadingTasksCompleted++;
        if (loadingTasksCompleted >= totalLoadingTasks) {
            hideLoading();
        }
    }



    private void loadUserProfilePicture() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        mDatabase.child("users").child("teachers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                ImageView profilePicture = findViewById(R.id.imageView3);
                                if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                                    Glide.with(TeacherDashboard.this)
                                            .load(user.getProfileImageUrl())
                                            .placeholder(R.drawable.default_profile_2)
                                            .error(R.drawable.default_profile_2)
                                            .circleCrop()
                                            .override(200, 200)
                                            .into(profilePicture);
                                } else {
                                    profilePicture.setImageResource(R.drawable.default_profile_2);
                                }
                            }
                        }
                        onLoadingTaskCompleted();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error silently
                        onLoadingTaskCompleted();
                    }
                });
    }

    private void loadUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        mDatabase.child("users").child("teachers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            User user = dataSnapshot.getValue(User.class);
                            if (user != null) {
                                displayUserData(user);
                            }
                        }
                        onLoadingTaskCompleted();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        // Handle error silently
                        onLoadingTaskCompleted();
                    }
                });
    }

    private void displayUserData(User user) {
        TextView welcomeName = findViewById(R.id.welcomeName);
        TextView welcomeEmail = findViewById(R.id.welcomeEmail);

        // Display "Welcome!" in black and first & last name in green
        String firstLastName = getFirstLastName(user);
        if (firstLastName != null && !firstLastName.isEmpty()) {
            // Create spannable string to have different colors
            android.text.SpannableString spannableString = new android.text.SpannableString("Welcome! " + firstLastName);
            // Set black color for "Welcome! "
            spannableString.setSpan(new android.text.style.ForegroundColorSpan(android.graphics.Color.BLACK),
                    0, 9, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            // Set green color for the name
            spannableString.setSpan(new android.text.style.ForegroundColorSpan(
                            androidx.core.content.ContextCompat.getColor(this, R.color.green)),
                    9, spannableString.length(), android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            welcomeName.setText(spannableString);
        } else {
            welcomeName.setText("Welcome! User");
        }

        // Display email
        String email = user.getEmail();
        if (email != null && !email.isEmpty()) {
            welcomeEmail.setText(email);
        } else {
            welcomeEmail.setText("No email available");
        }
    }

    // Helper method to get only first and last name using User model getters
    private String getFirstLastName(User user) {
        StringBuilder name = new StringBuilder();
        if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
            name.append(user.getFirstName());
        }
        if (user.getLastName() != null && !user.getLastName().isEmpty()) {
            if (name.length() > 0) name.append(" ");
            name.append(user.getLastName());
        }
        return name.toString();
    }

    private void setCurrentDateAndDay() {
        // This method is now handled by updateTodaysSessionsTitle()
        // which provides the correct format with green day color
        updateTodaysSessionsTitle();
    }

    private void startDateUpdateTimer() {
        java.util.Timer timer = new java.util.Timer();
        java.util.TimerTask task = new java.util.TimerTask() {
            @Override
            public void run() {
                runOnUiThread(() -> setCurrentDateAndDay());
            }
        };
        
        // Calculate time until next midnight
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_MONTH, 1);
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0);
        calendar.set(java.util.Calendar.MINUTE, 0);
        calendar.set(java.util.Calendar.SECOND, 0);
        calendar.set(java.util.Calendar.MILLISECOND, 0);
        
        long delay = calendar.getTimeInMillis() - System.currentTimeMillis();
        
        // Schedule the task to run at midnight and then every 24 hours
        timer.scheduleAtFixedRate(task, delay, 24 * 60 * 60 * 1000);
    }

    private void setupDashboardCards() {
        // Reports card
        findViewById(R.id.reportCardView).setOnClickListener(v -> {
            TransitionManager.startActivityForward(TeacherDashboard.this, SavedExportsActivity.class);
        });

        // Archives card
        findViewById(R.id.archivesCardView).setOnClickListener(v -> {
            TransitionManager.startActivityForward(TeacherDashboard.this, ArchiveCoursesActivity.class);
        });

        // SMS Alerts card
        findViewById(R.id.smsCardView).setOnClickListener(v -> {
            TransitionManager.startActivityForward(TeacherDashboard.this, SmsAlertsActivity.class);
        });
    }

    private void loadCourseCount() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            android.util.Log.e("TeacherDashboard", "User ID is null, cannot load course count");
            return;
        }

        android.util.Log.d("TeacherDashboard", "Loading course count for teacher: " + userId);
        DatabaseReference coursesRef = mDatabase.child("users").child("teachers").child(userId).child("courses");
        coursesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                android.util.Log.d("TeacherDashboard", "Course count data changed, snapshot exists: " + dataSnapshot.exists());
                android.util.Log.d("TeacherDashboard", "Course count snapshot children count: " + dataSnapshot.getChildrenCount());
                
                int courseCount = (int) dataSnapshot.getChildrenCount();
                TextView courseCountText = findViewById(R.id.courseCountText);
                if (courseCountText != null) {
                    courseCountText.setText(String.valueOf(courseCount));
                    android.util.Log.d("TeacherDashboard", "Updated course count display to: " + courseCount);
                } else {
                    android.util.Log.e("TeacherDashboard", "Course count text view not found");
                }
                onLoadingTaskCompleted();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                android.util.Log.e("TeacherDashboard", "Failed to load course count:");
                android.util.Log.e("TeacherDashboard", "  - Error code: " + databaseError.getCode());
                android.util.Log.e("TeacherDashboard", "  - Error message: " + databaseError.getMessage());
                android.util.Log.e("TeacherDashboard", "  - Error details: " + databaseError.getDetails());
                onLoadingTaskCompleted();
            }
        });
    }
    
    private void loadSmsAlertCount() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            android.util.Log.e("TeacherDashboard", "User ID is null, cannot load SMS alert count");
            onLoadingTaskCompleted();
            return;
        }

        android.util.Log.d("TeacherDashboard", "Loading SMS alert count for teacher: " + userId);
        
        // Set default count immediately to avoid blocking
        updateSmsAlertCount(0);
        
        // Load teacher's courses first
        DatabaseReference coursesRef = mDatabase.child("users").child("teachers").child(userId).child("courses");
        coursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot coursesSnapshot) {
                if (!coursesSnapshot.exists()) {
                    updateSmsAlertCount(0);
                    return;
                }

                int totalCourses = (int) coursesSnapshot.getChildrenCount();
                if (totalCourses == 0) {
                    updateSmsAlertCount(0);
                    return;
                }

                // Count students with 3+ absences across all courses
                countStudentsWithViolations(coursesSnapshot, totalCourses);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                android.util.Log.e("TeacherDashboard", "Failed to load courses for SMS alert count: " + databaseError.getMessage());
                updateSmsAlertCount(0);
            }
        });
    }
    
    private void countStudentsWithViolations(DataSnapshot coursesSnapshot, int totalCourses) {
        java.util.concurrent.atomic.AtomicInteger coursesProcessed = new java.util.concurrent.atomic.AtomicInteger(0);
        java.util.Map<String, java.util.Map<String, Integer>> studentAbsenceCounts = new java.util.HashMap<>();
        
        for (DataSnapshot courseSnapshot : coursesSnapshot.getChildren()) {
            com.llavore.hereoattendance.models.Course course = courseSnapshot.getValue(com.llavore.hereoattendance.models.Course.class);
            if (course == null || course.code == null) continue;

            // Load all sessions for this course
            mDatabase.child("courses").child(course.code).child("sessions")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(DataSnapshot sessionsSnapshot) {
                            int processed = coursesProcessed.incrementAndGet();
                            
                            if (sessionsSnapshot.exists()) {
                                // Process each session
                                for (DataSnapshot sessionSnapshot : sessionsSnapshot.getChildren()) {
                                    DataSnapshot attendanceSnapshot = sessionSnapshot.child("attendance");
                                    if (attendanceSnapshot.exists()) {
                                        processSessionForViolations(attendanceSnapshot, course.code, studentAbsenceCounts);
                                    }
                                }
                            }

                            // Check if we've processed all courses
                            if (processed == totalCourses) {
                                int violationCount = countStudentsWith3PlusAbsences(studentAbsenceCounts);
                                updateSmsAlertCount(violationCount);
                                onLoadingTaskCompleted();
                            }
                        }

                        @Override
                        public void onCancelled(DatabaseError databaseError) {
                            int processed = coursesProcessed.incrementAndGet();
                            if (processed == totalCourses) {
                                int violationCount = countStudentsWith3PlusAbsences(studentAbsenceCounts);
                                updateSmsAlertCount(violationCount);
                                onLoadingTaskCompleted();
                            }
                        }
                    });
        }
    }
    
    private void processSessionForViolations(DataSnapshot attendanceSnapshot, String courseCode, java.util.Map<String, java.util.Map<String, Integer>> studentAbsenceCounts) {
        // Count absences per student for this course
        for (DataSnapshot studentSnapshot : attendanceSnapshot.getChildren()) {
            com.llavore.hereoattendance.models.AttendanceRecord record = studentSnapshot.getValue(com.llavore.hereoattendance.models.AttendanceRecord.class);
            if (record == null || !"ABSENT".equals(record.getStatus())) {
                continue; // Only count ABSENT records
            }

            String edpNumber = record.getEdpNumber();
            
            // Initialize maps if needed
            if (!studentAbsenceCounts.containsKey(edpNumber)) {
                studentAbsenceCounts.put(edpNumber, new java.util.HashMap<>());
            }
            
            java.util.Map<String, Integer> courseCounts = studentAbsenceCounts.get(edpNumber);
            int currentCount = courseCounts.getOrDefault(courseCode, 0);
            courseCounts.put(courseCode, currentCount + 1);
        }
    }
    
    private int countStudentsWith3PlusAbsences(java.util.Map<String, java.util.Map<String, Integer>> studentAbsenceCounts) {
        int violationCount = 0;
        
        for (java.util.Map.Entry<String, java.util.Map<String, Integer>> studentEntry : studentAbsenceCounts.entrySet()) {
            java.util.Map<String, Integer> courseCounts = studentEntry.getValue();
            
            // Check if student has 3+ absences in any course
            boolean hasViolation = false;
            for (java.util.Map.Entry<String, Integer> courseEntry : courseCounts.entrySet()) {
                if (courseEntry.getValue() >= 3) {
                    hasViolation = true;
                    break;
                }
            }
            
            if (hasViolation) {
                violationCount++;
            }
        }
        
        return violationCount;
    }
    
    private void updateSmsAlertCount(int count) {
        TextView smsAlertCountText = findViewById(R.id.smsAlertCountText);
        if (smsAlertCountText != null) {
            smsAlertCountText.setText(String.valueOf(count));
            android.util.Log.d("TeacherDashboard", "Updated SMS alert count display to: " + count);
        } else {
            android.util.Log.e("TeacherDashboard", "SMS alert count text view not found");
        }
    }
    
    private void loadArchiveCount() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            android.util.Log.e("TeacherDashboard", "User ID is null, cannot load archive count");
            return;
        }

        mDatabase.child("users").child("teachers").child(userId).child("archivedCourses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        int archiveCount = 0;
                        if (snapshot.exists()) {
                            archiveCount = (int) snapshot.getChildrenCount();
                        }
                        updateArchiveCount(archiveCount);
                        onLoadingTaskCompleted();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("TeacherDashboard", "Failed to load archive count: " + databaseError.getMessage());
                        onLoadingTaskCompleted();
                    }
                });
    }
    
    private void updateArchiveCount(int count) {
        TextView archiveCountText = findViewById(R.id.archiveCountText);
        if (archiveCountText != null) {
            archiveCountText.setText(String.valueOf(count));
            android.util.Log.d("TeacherDashboard", "Updated archive count display to: " + count);
        } else {
            android.util.Log.e("TeacherDashboard", "Archive count text view not found");
        }
    }
    
    
    private void initializeTodaysSessions() {
        todaysSessionsRecyclerView = findViewById(R.id.todaysSessionsRecyclerView);
        todaysSessionsTitle = findViewById(R.id.todaysSessionsTitle);
        
        // Update the title now that the TextView is initialized
        updateTodaysSessionsTitle();
        
        todaySessions = new ArrayList<>();
        todaySessionAdapter = new TodaySessionAdapter(todaySessions, this);
        
        todaysSessionsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        todaysSessionsRecyclerView.setAdapter(todaySessionAdapter);
        
        // Set up click listener
        todaySessionAdapter.setOnSessionClickListener(session -> {
            navigateToCourseDetails(session);
        });
        
        // Load today's sessions
        loadTodaysSessions();
    }
    
    private void loadTodaysSessions() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            android.util.Log.e("TeacherDashboard", "User ID is null, cannot load today's sessions");
            return;
        }

        // Update title with current date
        updateTodaysSessionsTitle();
        
        // Get today's day of week
        String todayDay = getTodayDayOfWeek();
        android.util.Log.d("TeacherDashboard", "Today is: " + todayDay);
        
        // Load teacher's courses
        mDatabase.child("users").child("teachers").child(userId).child("courses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot coursesSnapshot) {
                        todaySessions.clear();
                        
                        if (coursesSnapshot.exists()) {
                            for (DataSnapshot courseSnapshot : coursesSnapshot.getChildren()) {
                                com.llavore.hereoattendance.models.Course course = courseSnapshot.getValue(com.llavore.hereoattendance.models.Course.class);
                                if (course != null && course.code != null) {
                                    // Check if course has today's schedule
                                    if (hasTodaySchedule(course.scheduleDays, todayDay)) {
                                        // Create today session
                                        TodaySession session = new TodaySession(
                                                course.id,
                                                course.code,
                                                course.name,
                                                course.room,
                                                "Session 1", // Default session name
                                                course.startTime,
                                                course.endTime,
                                                course.lateAfter,
                                                course.scheduleDays
                                        );
                                        todaySessions.add(session);
                                    }
                                }
                            }
                        }
                        
                        // Sort sessions by start time
                        todaySessions.sort((s1, s2) -> {
                            try {
                                java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault());
                                java.util.Date time1 = format.parse(s1.getStartTime());
                                java.util.Date time2 = format.parse(s2.getStartTime());
                                return time1.compareTo(time2);
                            } catch (Exception e) {
                                return 0;
                            }
                        });
                        
                        todaySessionAdapter.notifyDataSetChanged();
                        android.util.Log.d("TeacherDashboard", "Loaded " + todaySessions.size() + " sessions for today");
                        onLoadingTaskCompleted();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("TeacherDashboard", "Failed to load today's sessions: " + databaseError.getMessage());
                        onLoadingTaskCompleted();
                    }
                });
    }
    
    private String getTodayDayOfWeek() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int dayOfWeek = calendar.get(java.util.Calendar.DAY_OF_WEEK);
        
        switch (dayOfWeek) {
            case java.util.Calendar.SUNDAY: return "SD";
            case java.util.Calendar.MONDAY: return "M";
            case java.util.Calendar.TUESDAY: return "T";
            case java.util.Calendar.WEDNESDAY: return "W";
            case java.util.Calendar.THURSDAY: return "Tth";
            case java.util.Calendar.FRIDAY: return "F";
            case java.util.Calendar.SATURDAY: return "ST";
            default: return "M";
        }
    }
    
    private boolean hasTodaySchedule(String scheduleDays, String todayDay) {
        if (scheduleDays == null || scheduleDays.isEmpty()) {
            return false;
        }
        
        // Clean and normalize the schedule string
        String cleanSchedule = scheduleDays.replaceAll("\\s+", "").toUpperCase();
        String cleanTodayDay = todayDay.toUpperCase();
        
        android.util.Log.d("TeacherDashboard", "Checking schedule: '" + cleanSchedule + "' for today: '" + cleanTodayDay + "'");
        
        // Check if today's day is in the schedule
        boolean hasSchedule = cleanSchedule.contains(cleanTodayDay);
        android.util.Log.d("TeacherDashboard", "Has today's schedule: " + hasSchedule);
        
        return hasSchedule;
    }
    
    private void updateTodaysSessionsTitle() {
        if (todaysSessionsTitle == null) {
            return; // TextView not initialized yet
        }
        
        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("MMMM dd, yyyy", java.util.Locale.getDefault());
        String currentDate = dateFormat.format(java.util.Calendar.getInstance().getTime());
        
        // Get full day name for display
        java.text.SimpleDateFormat dayFormat = new java.text.SimpleDateFormat("EEEE", java.util.Locale.getDefault());
        String todayDayDisplay = dayFormat.format(java.util.Calendar.getInstance().getTime());
        
        // Create formatted title with green day of week
        String title = "Today's Sessions - " + currentDate + " | " + todayDayDisplay;
        
        // Create SpannableString to make the day of week green
        android.text.SpannableString spannableTitle = new android.text.SpannableString(title);
        int dayStartIndex = title.indexOf("| " + todayDayDisplay);
        if (dayStartIndex != -1) {
            int dayStart = dayStartIndex + 2; // Skip "| "
            int dayEnd = dayStart + todayDayDisplay.length();
            spannableTitle.setSpan(new android.text.style.ForegroundColorSpan(getResources().getColor(R.color.green)), 
                                 dayStart, dayEnd, android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        
        todaysSessionsTitle.setText(spannableTitle);
    }
    
    private void navigateToCourseDetails(TodaySession session) {
        android.os.Bundle bundle = new android.os.Bundle();
        bundle.putString("courseId", session.getCourseId());
        bundle.putString("courseCode", session.getCourseCode());
        bundle.putString("courseName", session.getCourseName());
        bundle.putString("courseRoom", session.getRoom());
        bundle.putString("courseSchedule", session.getScheduleDays());
        bundle.putString("courseStartTime", session.getStartTime());
        bundle.putString("courseEndTime", session.getEndTime());
        bundle.putInt("courseStudentCount", 0); // Will be loaded in CourseDetails
        bundle.putInt("courseSessionCount", 0); // Will be loaded in CourseDetails
        TransitionManager.startActivityForward(TeacherDashboard.this, CourseDetails.class, bundle);
    }
    
    private void loadSavedExportsCount() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            android.util.Log.e("TeacherDashboard", "User ID is null, cannot load saved exports count");
            return;
        }

        android.util.Log.d("TeacherDashboard", "Loading saved exports count for teacher: " + userId);
        DatabaseReference savedExportsRef = mDatabase.child("users").child("teachers").child(userId).child("savedExports");
        savedExportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int savedExportsCount = 0;
                if (snapshot.exists()) {
                    savedExportsCount = (int) snapshot.getChildrenCount();
                }
                updateSavedExportsCount(savedExportsCount);
                onLoadingTaskCompleted();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                android.util.Log.e("TeacherDashboard", "Failed to load saved exports count: " + databaseError.getMessage());
                onLoadingTaskCompleted();
            }
        });
    }
    
    private void updateSavedExportsCount(int count) {
        TextView reportsCountText = findViewById(R.id.reportsCountText);
        if (reportsCountText != null) {
            reportsCountText.setText(String.valueOf(count));
            android.util.Log.d("TeacherDashboard", "Updated saved exports count display to: " + count);
        } else {
            android.util.Log.e("TeacherDashboard", "reportsCountText TextView not found");
        }
    }
    
    @Override
    public void onBackPressed() {
        // Move app to background instead of going back to MainActivity
        moveTaskToBack(true);
    }
}