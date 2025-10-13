package com.llavore.hereoattendance.student;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.StudentNotificationAdapter;
import com.llavore.hereoattendance.models.Course;
import com.llavore.hereoattendance.models.StudentNotification;
import com.llavore.hereoattendance.utils.SessionManager;
import com.llavore.hereoattendance.utils.StudentNavigationManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class StudentNotificationsActivity extends AppCompatActivity implements StudentNotificationAdapter.OnNotificationClickListener {

    private ImageView menuButton;
    private LinearLayout filterContainer;
    private TextView filterText;
    private ImageView filterDropdownIcon;
    private TextView sectionTitleText;
    private RecyclerView notificationsRecyclerView;
    private LinearLayout noNotificationsLayout;
    private StudentNotificationAdapter adapter;
    private List<StudentNotification> notifications;
    private List<StudentNotification> allNotifications; // Store all notifications for filtering
    
    private DatabaseReference mDatabase;
    private String currentStudentId;
    private SessionManager sessionManager;
    private StudentNavigationManager navigationManager;
    private String currentFilter = "All"; // Current filter state

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_notifications);
        
        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        setupClickListeners();
        setupNavigation();
        loadNotifications();
    }

    private void initializeViews() {
        menuButton = findViewById(R.id.menuButton);
        filterContainer = findViewById(R.id.filterContainer);
        filterText = findViewById(R.id.filterText);
        filterDropdownIcon = findViewById(R.id.filterDropdownIcon);
        sectionTitleText = findViewById(R.id.sectionTitleText);
        notificationsRecyclerView = findViewById(R.id.notificationsRecyclerView);
        noNotificationsLayout = findViewById(R.id.noNotificationsLayout);
        
        notifications = new ArrayList<>();
        allNotifications = new ArrayList<>();
    }

    private void initializeFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        sessionManager = new SessionManager(this);
        currentStudentId = sessionManager.getUserId();
        
        if (currentStudentId == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void setupRecyclerView() {
        adapter = new StudentNotificationAdapter(notifications, this);
        adapter.setOnNotificationClickListener(this);
        notificationsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        filterContainer.setOnClickListener(v -> showFilterDropdown());
    }
    
    private void setupNavigation() {
        navigationManager = new StudentNavigationManager(this);
        androidx.drawerlayout.widget.DrawerLayout drawerLayout = findViewById(R.id.main);
        ImageView burgerIcon = findViewById(R.id.menuButton);
        com.google.android.material.navigation.NavigationView navigationView = findViewById(R.id.navigationView);
        navigationManager.setupNavigationDrawer(drawerLayout, burgerIcon, navigationView, "notifications");
    }
    
    private void showFilterDropdown() {
        String[] filterOptions = {"All", "Read", "Unread"};
        
        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this, R.style.CustomAlertDialog);
        builder.setTitle("Filter Notifications");
        builder.setItems(filterOptions, (dialog, which) -> {
            String selectedFilter = filterOptions[which];
            currentFilter = selectedFilter;
            filterText.setText(selectedFilter);
            updateSectionTitle();
            applyFilter();
        });
        
        // Create and show the dialog
        androidx.appcompat.app.AlertDialog dialog = builder.create();
        dialog.show();
        
        // Customize the dialog appearance for better visibility
        dialog.getListView().setDividerHeight(1);
        dialog.getListView().setPadding(16, 16, 16, 16);
        
        // Ensure text is visible
        for (int i = 0; i < dialog.getListView().getChildCount(); i++) {
            android.view.View child = dialog.getListView().getChildAt(i);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(getResources().getColor(android.R.color.black));
            }
        }
    }
    
    private void updateSectionTitle() {
        switch (currentFilter) {
            case "Read":
                sectionTitleText.setText("Read Notifications");
                break;
            case "Unread":
                sectionTitleText.setText("Unread Notifications");
                break;
            default:
                sectionTitleText.setText("All Notifications");
                break;
        }
    }
    
    private void applyFilter() {
        List<StudentNotification> filteredNotifications = new ArrayList<>();
        
        switch (currentFilter) {
            case "Read":
                for (StudentNotification notification : allNotifications) {
                    if (notification.isRead()) {
                        filteredNotifications.add(notification);
                    }
                }
                break;
            case "Unread":
                for (StudentNotification notification : allNotifications) {
                    if (!notification.isRead()) {
                        filteredNotifications.add(notification);
                    }
                }
                break;
            default: // "All"
                filteredNotifications.addAll(allNotifications);
                break;
        }
        
        notifications.clear();
        notifications.addAll(filteredNotifications);
        adapter.notifyDataSetChanged();
        
        if (notifications.isEmpty()) {
            showNoNotifications();
        } else {
            showNotifications();
        }
    }

    private void loadNotifications() {
        if (currentStudentId == null) {
            android.util.Log.e("StudentNotifications", "Current student ID is null");
            return;
        }

        android.util.Log.d("StudentNotifications", "Loading notifications for student: " + currentStudentId);

        // Get all courses the student is enrolled in
        mDatabase.child("users").child("students").child(currentStudentId).child("enrolledCourses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot coursesSnapshot) {
                        android.util.Log.d("StudentNotifications", "Courses snapshot exists: " + coursesSnapshot.exists());
                        
                        if (!coursesSnapshot.exists()) {
                            android.util.Log.d("StudentNotifications", "No courses found for student");
                            showNoNotifications();
                            return;
                        }

                        final List<StudentNotification> notifications = Collections.synchronizedList(new ArrayList<>());
                        final AtomicInteger coursesProcessed = new AtomicInteger(0);
                        final int totalCourses = (int) coursesSnapshot.getChildrenCount();
                        
                        // Add timeout mechanism to ensure notifications are processed even if some courses fail
                        android.os.Handler timeoutHandler = new android.os.Handler();
                        timeoutHandler.postDelayed(() -> {
                            int processed = coursesProcessed.get();
                            if (processed < totalCourses) {
                                android.util.Log.w("StudentNotifications", "Timeout reached. Processed " + processed + "/" + totalCourses + " courses. Processing notifications anyway.");
                                processNotifications(notifications);
                            }
                        }, 10000); // 10 second timeout

                        for (DataSnapshot courseSnapshot : coursesSnapshot.getChildren()) {
                            Course course = courseSnapshot.getValue(Course.class);
                            if (course != null && course.code != null) {
                                android.util.Log.d("StudentNotifications", "Checking course: " + course.code + " (" + course.name + ")");
                                
                                // Check attendance for this course using the same logic as StudentCourseDetails
                                checkCourseAttendanceSimple(course, notifications, coursesProcessed, totalCourses, timeoutHandler);
                            } else {
                                int processed = coursesProcessed.incrementAndGet();
                                if (processed == totalCourses) {
                                    timeoutHandler.removeCallbacksAndMessages(null);
                                    processNotifications(notifications);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("StudentNotifications", "Failed to load student courses: " + databaseError.getMessage());
                        Toast.makeText(StudentNotificationsActivity.this, "Failed to load notifications", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void checkConsecutiveAbsencesSequentially(Course course, List<String> sessionDates, String studentEdpNumber,
                                                     List<StudentNotification> notifications, AtomicInteger coursesProcessed, int totalCourses, android.os.Handler timeoutHandler) {
        
        final AtomicInteger maxConsecutiveAbsences = new AtomicInteger(0);
        final AtomicInteger currentConsecutiveAbsences = new AtomicInteger(0);
        final AtomicInteger sessionsProcessed = new AtomicInteger(0);
        final AtomicBoolean courseProcessed = new AtomicBoolean(false);
        
        // Helper method to mark course as processed
        Runnable markCourseProcessed = () -> {
            if (courseProcessed.compareAndSet(false, true)) {
                // Check if we need to create notification based on max consecutive absences
                int maxAbsences = maxConsecutiveAbsences.get();
                if (maxAbsences >= 2) {
                    if (maxAbsences >= 3) {
                        android.util.Log.d("StudentNotifications", "Creating ALERT notification for " + maxAbsences + " max consecutive absences");
                        createSimpleNotification(course, StudentNotification.NotificationType.ALERT, 
                                              maxAbsences, notifications, coursesProcessed, totalCourses, timeoutHandler);
                    } else {
                        android.util.Log.d("StudentNotifications", "Creating WARNING notification for " + maxAbsences + " max consecutive absences");
                        createSimpleNotification(course, StudentNotification.NotificationType.WARNING, 
                                              maxAbsences, notifications, coursesProcessed, totalCourses, timeoutHandler);
                    }
                } else {
                    // No notification needed
                    android.util.Log.d("StudentNotifications", "No notification needed - max consecutive absences: " + maxAbsences);
                    int processed = coursesProcessed.incrementAndGet();
                    android.util.Log.d("StudentNotifications", "Course processed: " + processed + "/" + totalCourses);
                    if (processed == totalCourses) {
                        timeoutHandler.removeCallbacksAndMessages(null);
                        android.util.Log.d("StudentNotifications", "All courses processed, calling processNotifications with " + notifications.size() + " notifications");
                        processNotifications(notifications);
                    }
                }
            }
        };
        
        for (String sessionDate : sessionDates) {
            mDatabase.child("courses").child(course.code).child("sessions").child(sessionDate)
                    .child("attendance").child(studentEdpNumber)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                            boolean isAbsent = false;
                            
                            if (attendanceSnapshot.exists()) {
                                String status = attendanceSnapshot.child("status").getValue(String.class);
                                isAbsent = "ABSENT".equals(status);
                            } else {
                                // No attendance record means absent
                                isAbsent = true;
                            }

                            if (isAbsent) {
                                int currentAbsences = currentConsecutiveAbsences.incrementAndGet();
                                android.util.Log.d("StudentNotifications", "Found absence in session " + sessionDate + ", current consecutive: " + currentAbsences);
                            } else {
                                android.util.Log.d("StudentNotifications", "Student was not absent in session " + sessionDate);
                                // Update max consecutive absences if current streak is higher
                                int currentAbsences = currentConsecutiveAbsences.get();
                                if (currentAbsences > maxConsecutiveAbsences.get()) {
                                    maxConsecutiveAbsences.set(currentAbsences);
                                    android.util.Log.d("StudentNotifications", "Updated max consecutive absences to: " + currentAbsences);
                                }
                                // Reset current consecutive count
                                currentConsecutiveAbsences.set(0);
                            }
                            
                            // Check if this is the last session
                            int sessionsProcessedCount = sessionsProcessed.incrementAndGet();
                            if (sessionsProcessedCount == sessionDates.size()) {
                                // This was the last session, check if we need to update max consecutive absences
                                int currentAbsences = currentConsecutiveAbsences.get();
                                if (currentAbsences > maxConsecutiveAbsences.get()) {
                                    maxConsecutiveAbsences.set(currentAbsences);
                                    android.util.Log.d("StudentNotifications", "Final max consecutive absences: " + currentAbsences);
                                }
                                
                                // Mark course as processed
                                markCourseProcessed.run();
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            android.util.Log.e("StudentNotifications", "Failed to check attendance: " + databaseError.getMessage());
                            markCourseProcessed.run();
                        }
                    });
        }
    }

    private void checkCourseAttendanceSimple(Course course, List<StudentNotification> notifications, 
                                           AtomicInteger coursesProcessed, int totalCourses, android.os.Handler timeoutHandler) {
        
        // Get student's EDP number
        mDatabase.child("users").child("students").child(currentStudentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                        if (studentSnapshot.exists()) {
                            String studentEdpNumber = studentSnapshot.child("edpNumber").getValue(String.class);
                            android.util.Log.d("StudentNotifications", "Student EDP number: " + studentEdpNumber);
                            
                            if (studentEdpNumber != null) {
                                // Get all sessions for this course and check attendance
                                mDatabase.child("courses").child(course.code).child("sessions")
                                        .addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot sessionsSnapshot) {
                                                if (sessionsSnapshot.exists()) {
                                                    List<String> sessionDates = new ArrayList<>();
                                                    for (DataSnapshot sessionSnapshot : sessionsSnapshot.getChildren()) {
                                                        String sessionDate = sessionSnapshot.getKey();
                                                        sessionDates.add(sessionDate);
                                                    }
                                                    
                                                    // Sort dates (newest first) to check consecutive absences
                                                    Collections.sort(sessionDates, Collections.reverseOrder());
                                                    
                                                    // Process sessions sequentially to count consecutive absences properly
                                                    checkConsecutiveAbsencesSequentially(course, sessionDates, studentEdpNumber, notifications, coursesProcessed, totalCourses, timeoutHandler);
                                                } else {
                                                    android.util.Log.d("StudentNotifications", "No sessions found for course: " + course.code);
                                                    int processed = coursesProcessed.incrementAndGet();
                                                    if (processed == totalCourses) {
                                                        processNotifications(notifications);
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError databaseError) {
                                                android.util.Log.e("StudentNotifications", "Failed to load sessions: " + databaseError.getMessage());
                                                int processed = coursesProcessed.incrementAndGet();
                                                if (processed == totalCourses) {
                                                    processNotifications(notifications);
                                                }
                                            }
                                        });
                            } else {
                                android.util.Log.d("StudentNotifications", "Student EDP number is null");
                                int processed = coursesProcessed.incrementAndGet();
                                if (processed == totalCourses) {
                                    processNotifications(notifications);
                                }
                            }
                        } else {
                            android.util.Log.d("StudentNotifications", "Student data not found");
                            int processed = coursesProcessed.incrementAndGet();
                            if (processed == totalCourses) {
                                processNotifications(notifications);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("StudentNotifications", "Failed to load student data: " + databaseError.getMessage());
                        int processed = coursesProcessed.incrementAndGet();
                        if (processed == totalCourses) {
                            processNotifications(notifications);
                        }
                    }
                });
    }

    private void checkAttendanceForCourses(List<String> courseCodes) {
        if (courseCodes.isEmpty()) {
            android.util.Log.d("StudentNotifications", "No course codes to check");
            showNoNotifications();
            return;
        }

        android.util.Log.d("StudentNotifications", "Checking attendance for " + courseCodes.size() + " courses");

        final AtomicInteger coursesProcessed = new AtomicInteger(0);
        final List<StudentNotification> newNotifications = Collections.synchronizedList(new ArrayList<>());

        for (String courseCode : courseCodes) {
            android.util.Log.d("StudentNotifications", "Checking course: " + courseCode);
            
            // Get course details and check attendance
            mDatabase.child("courses").child(courseCode)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot courseSnapshot) {
                            android.util.Log.d("StudentNotifications", "Course snapshot exists for " + courseCode + ": " + courseSnapshot.exists());
                            
                            if (courseSnapshot.exists()) {
                                String courseName = courseSnapshot.child("courseName").getValue(String.class);
                                String courseSchedule = courseSnapshot.child("schedule").getValue(String.class);
                                String teacherId = courseSnapshot.child("teacherId").getValue(String.class);

                                android.util.Log.d("StudentNotifications", "Course details - Name: " + courseName + ", Schedule: " + courseSchedule + ", Teacher: " + teacherId);

                                // Check attendance for this course
                                checkCourseAttendance(courseCode, courseName, courseSchedule, teacherId, newNotifications, coursesProcessed, courseCodes.size());
                            } else {
                                android.util.Log.d("StudentNotifications", "Course not found: " + courseCode);
                                int processed = coursesProcessed.incrementAndGet();
                                if (processed == courseCodes.size()) {
                                    processNotifications(newNotifications);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError databaseError) {
                            android.util.Log.e("StudentNotifications", "Failed to load course " + courseCode + ": " + databaseError.getMessage());
                            int processed = coursesProcessed.incrementAndGet();
                            if (processed == courseCodes.size()) {
                                processNotifications(newNotifications);
                            }
                        }
                    });
        }
    }

    private void checkCourseAttendance(String courseCode, String courseName, String courseSchedule, 
                                     String teacherId, List<StudentNotification> newNotifications, 
                                     AtomicInteger coursesProcessed, int totalCourses) {
        
        android.util.Log.d("StudentNotifications", "Checking attendance for course: " + courseCode);
        
        // Get student's EDP number
        mDatabase.child("users").child("students").child(currentStudentId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot studentSnapshot) {
                        if (studentSnapshot.exists()) {
                            String studentEdpNumber = studentSnapshot.child("edpNumber").getValue(String.class);
                            android.util.Log.d("StudentNotifications", "Student EDP number: " + studentEdpNumber);
                            
                            if (studentEdpNumber != null) {
                                // Count consecutive absences for this course
                                countConsecutiveAbsences(courseCode, courseName, courseSchedule, teacherId, 
                                                       studentEdpNumber, newNotifications, coursesProcessed, totalCourses);
                            } else {
                                android.util.Log.d("StudentNotifications", "Student EDP number is null");
                                int processed = coursesProcessed.incrementAndGet();
                                if (processed == totalCourses) {
                                    processNotifications(newNotifications);
                                }
                            }
                        } else {
                            android.util.Log.d("StudentNotifications", "Student data not found");
                            int processed = coursesProcessed.incrementAndGet();
                            if (processed == totalCourses) {
                                processNotifications(newNotifications);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("StudentNotifications", "Failed to load student data: " + databaseError.getMessage());
                        int processed = coursesProcessed.incrementAndGet();
                        if (processed == totalCourses) {
                            processNotifications(newNotifications);
                        }
                    }
                });
    }

    private void countConsecutiveAbsences(String courseCode, String courseName, String courseSchedule, 
                                        String teacherId, String studentEdpNumber, 
                                        List<StudentNotification> newNotifications, 
                                        AtomicInteger coursesProcessed, int totalCourses) {
        
        // Get all sessions for this course
        mDatabase.child("courses").child(courseCode).child("sessions")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot sessionsSnapshot) {
                        if (!sessionsSnapshot.exists()) {
                            int processed = coursesProcessed.incrementAndGet();
                            if (processed == totalCourses) {
                                processNotifications(newNotifications);
                            }
                            return;
                        }

                        List<String> sessionDates = new ArrayList<>();
                        for (DataSnapshot sessionSnapshot : sessionsSnapshot.getChildren()) {
                            String sessionDate = sessionSnapshot.getKey();
                            sessionDates.add(sessionDate);
                        }

                        // Sort dates to check consecutive absences (newest first)
                        Collections.sort(sessionDates, Collections.reverseOrder());

                        // Use AtomicInteger for thread-safe access
                        final AtomicInteger consecutiveAbsences = new AtomicInteger(0);
                        final AtomicInteger sessionsProcessed = new AtomicInteger(0);
                        final int totalSessions = sessionDates.size();

                        for (String sessionDate : sessionDates) {
                            // Check if student was absent in this session
                            mDatabase.child("courses").child(courseCode).child("sessions").child(sessionDate)
                                    .child("attendance").child(studentEdpNumber)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot attendanceSnapshot) {
                                            boolean isAbsent = false;
                                            
                                            if (attendanceSnapshot.exists()) {
                                                String status = attendanceSnapshot.child("status").getValue(String.class);
                                                isAbsent = "ABSENT".equals(status);
                                            } else {
                                                // No attendance record means absent
                                                isAbsent = true;
                                            }

                                            if (isAbsent) {
                                                int currentAbsences = consecutiveAbsences.incrementAndGet();
                                                android.util.Log.d("StudentNotifications", "Found absence in session " + sessionDate + ", total consecutive: " + currentAbsences);
                                                
                                                // Check if we need to create a notification
                                                if (currentAbsences == 2) {
                                                    android.util.Log.d("StudentNotifications", "Creating WARNING notification for 2 absences");
                                                    createNotification(courseCode, courseName, courseSchedule, 
                                                                     teacherId, StudentNotification.NotificationType.WARNING, 
                                                                     currentAbsences, newNotifications);
                                                } else if (currentAbsences >= 3) {
                                                    android.util.Log.d("StudentNotifications", "Creating ALERT notification for 3+ absences");
                                                    createNotification(courseCode, courseName, courseSchedule, 
                                                                     teacherId, StudentNotification.NotificationType.ALERT, 
                                                                     currentAbsences, newNotifications);
                                                }
                                            } else {
                                                android.util.Log.d("StudentNotifications", "Student was not absent in session " + sessionDate);
                                            }

                                            // Check if we've processed all sessions
                                            int processed = sessionsProcessed.incrementAndGet();
                                            if (processed == totalSessions) {
                                                int coursesProcessedCount = coursesProcessed.incrementAndGet();
                                                if (coursesProcessedCount == totalCourses) {
                                                    processNotifications(newNotifications);
                                                }
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError databaseError) {
                                            android.util.Log.e("StudentNotifications", "Failed to check attendance: " + databaseError.getMessage());
                                            
                                            // Still count as processed even if failed
                                            int processed = sessionsProcessed.incrementAndGet();
                                            if (processed == totalSessions) {
                                                int coursesProcessedCount = coursesProcessed.incrementAndGet();
                                                if (coursesProcessedCount == totalCourses) {
                                                    processNotifications(newNotifications);
                                                }
                                            }
                                        }
                                    });
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("StudentNotifications", "Failed to load sessions: " + databaseError.getMessage());
                        int processed = coursesProcessed.incrementAndGet();
                        if (processed == totalCourses) {
                            processNotifications(newNotifications);
                        }
                    }
                });
    }

    private void createSimpleNotification(Course course, StudentNotification.NotificationType type, 
                                        int absenceCount, List<StudentNotification> notifications, 
                                        AtomicInteger coursesProcessed, int totalCourses, android.os.Handler timeoutHandler) {
        
        android.util.Log.d("StudentNotifications", "Creating simple notification for course: " + course.code + ", type: " + type + ", absences: " + absenceCount);
        
        // Create notification using course data
        String notificationId = mDatabase.child("notifications").push().getKey();
        String title = (type == StudentNotification.NotificationType.WARNING) ? 
                     StudentNotification.getWarningTitle(absenceCount) : 
                     StudentNotification.getAlertTitle(absenceCount);
        String message = (type == StudentNotification.NotificationType.WARNING) ? 
                       StudentNotification.getWarningMessage() : 
                       StudentNotification.getAlertMessage();

        // Create schedule string from course data
        String courseSchedule = course.startTime + " - " + course.endTime + " | " + course.scheduleDays;
        
        // Create notification immediately without checking read status to avoid race condition
        StudentNotification notification = new StudentNotification(
                notificationId, currentStudentId, course.id, course.code, 
                course.name, courseSchedule, course.teacherId, 
                course.teacherFirstName + " " + course.teacherLastName, 
                course.teacherProfileImageUrl, type, absenceCount, title, message, false);

        notifications.add(notification);
        android.util.Log.d("StudentNotifications", "Simple notification created and added to list. Total notifications: " + notifications.size());
        
        // Check if all courses are processed after adding this notification
        int processed = coursesProcessed.incrementAndGet();
        android.util.Log.d("StudentNotifications", "Course processed: " + processed + "/" + totalCourses);
        if (processed == totalCourses) {
            timeoutHandler.removeCallbacksAndMessages(null);
            android.util.Log.d("StudentNotifications", "All courses processed, calling processNotifications with " + notifications.size() + " notifications");
            processNotifications(notifications);
        }
    }

    private void createNotification(String courseCode, String courseName, String courseSchedule, 
                                  String teacherId, StudentNotification.NotificationType type, 
                                  int absenceCount, List<StudentNotification> newNotifications) {
        
        android.util.Log.d("StudentNotifications", "Creating notification for course: " + courseCode + ", type: " + type + ", absences: " + absenceCount);
        
        // Get teacher information
        mDatabase.child("users").child("teachers").child(teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot teacherSnapshot) {
                        if (teacherSnapshot.exists()) {
                            String teacherName = teacherSnapshot.child("firstName").getValue(String.class) + " " +
                                               teacherSnapshot.child("lastName").getValue(String.class);
                            String teacherProfileImageUrl = teacherSnapshot.child("profileImageUrl").getValue(String.class);

                            android.util.Log.d("StudentNotifications", "Teacher info - Name: " + teacherName + ", Image: " + teacherProfileImageUrl);

                            // Create notification
                            String notificationId = mDatabase.child("notifications").push().getKey();
                            String title = (type == StudentNotification.NotificationType.WARNING) ? 
                                         StudentNotification.getWarningTitle(absenceCount) : 
                                         StudentNotification.getAlertTitle(absenceCount);
                            String message = (type == StudentNotification.NotificationType.WARNING) ? 
                                           StudentNotification.getWarningMessage() : 
                                           StudentNotification.getAlertMessage();

                            StudentNotification notification = new StudentNotification(
                                    notificationId, currentStudentId, courseCode, courseCode, 
                                    courseName, courseSchedule, teacherId, teacherName, 
                                    teacherProfileImageUrl, type, absenceCount, title, message, false);

                            newNotifications.add(notification);
                            android.util.Log.d("StudentNotifications", "Notification created and added to list. Total notifications: " + newNotifications.size());
                        } else {
                            android.util.Log.d("StudentNotifications", "Teacher data not found for ID: " + teacherId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("StudentNotifications", "Failed to load teacher data: " + databaseError.getMessage());
                    }
                });
    }

    private void processNotifications(List<StudentNotification> newNotifications) {
        android.util.Log.d("StudentNotifications", "Processing " + newNotifications.size() + " notifications");
        
        if (newNotifications.isEmpty()) {
            android.util.Log.w("StudentNotifications", "WARNING: processNotifications called with empty list!");
            runOnUiThread(() -> {
                showNoNotifications();
            });
            return;
        }
        
        // Sort notifications by date (newest first)
        Collections.sort(newNotifications, new Comparator<StudentNotification>() {
            @Override
            public int compare(StudentNotification n1, StudentNotification n2) {
                return Long.compare(n2.getTimestamp(), n1.getTimestamp());
            }
        });

        runOnUiThread(() -> {
            // Store all notifications for filtering
            allNotifications.clear();
            allNotifications.addAll(newNotifications);
            android.util.Log.d("StudentNotifications", "Stored " + allNotifications.size() + " notifications in allNotifications");
            
            // Apply current filter
            applyFilter();
        });
    }

    private void showNoNotifications() {
        notificationsRecyclerView.setVisibility(View.GONE);
        noNotificationsLayout.setVisibility(View.VISIBLE);
    }

    private void showNotifications() {
        notificationsRecyclerView.setVisibility(View.VISIBLE);
        noNotificationsLayout.setVisibility(View.GONE);
    }

    @Override
    public void onNotificationClick(StudentNotification notification) {
        // This method is called when the card is clicked
        // The adapter will handle the expand/collapse logic
        android.util.Log.d("StudentNotifications", "Notification clicked: " + notification.getId());
    }

    @Override
    public void onNotificationExpand(StudentNotification notification, boolean isExpanded) {
        android.util.Log.d("StudentNotifications", "Notification expanded: " + isExpanded);
        
        // Only mark as read when collapsing (isExpanded = false)
        if (!isExpanded && !notification.isRead()) {
            markNotificationAsRead(notification);
        }
    }
    
    private void markNotificationAsRead(StudentNotification notification) {
        android.util.Log.d("StudentNotifications", "Marking notification as read: " + notification.getId());
        
        // Update notification status
        notification.setRead(true);
        
        // Update in allNotifications list as well
        for (StudentNotification n : allNotifications) {
            if (n.getId().equals(notification.getId())) {
                n.setRead(true);
                break;
            }
        }
        
        // Save to database for persistence
        saveNotificationReadStatus(notification.getId());
        
        // Update UI
        adapter.notifyDataSetChanged();
    }
    
    private void checkNotificationReadStatus(String notificationId, ReadStatusCallback callback) {
        // Check if this notification has been read before
        mDatabase.child("users").child("students").child(currentStudentId)
                .child("readNotifications").child(notificationId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        boolean isRead = snapshot.exists() && snapshot.getValue(Boolean.class) != null && snapshot.getValue(Boolean.class);
                        callback.onReadStatusChecked(isRead);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        android.util.Log.e("StudentNotifications", "Failed to check read status: " + databaseError.getMessage());
                        callback.onReadStatusChecked(false); // Default to unread if error
                    }
                });
    }
    
    private void saveNotificationReadStatus(String notificationId) {
        // Save read status to database under the student's read notifications
        mDatabase.child("users").child("students").child(currentStudentId)
                .child("readNotifications").child(notificationId).setValue(true)
                .addOnSuccessListener(aVoid -> {
                    android.util.Log.d("StudentNotifications", "Read status saved to database: " + notificationId);
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("StudentNotifications", "Failed to save read status: " + e.getMessage());
                });
    }
    
    // Interface for callback
    private interface ReadStatusCallback {
        void onReadStatusChecked(boolean isRead);
    }
}
