package com.llavore.hereoattendance.student;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.StudentCourseAdapter;
import com.llavore.hereoattendance.models.Course;
import com.llavore.hereoattendance.model.User;
import com.llavore.hereoattendance.teacher.MainActivity;
import com.llavore.hereoattendance.utils.SessionManager;
import com.llavore.hereoattendance.utils.NavigationHeaderManager;
import com.llavore.hereoattendance.utils.StudentNavigationManager;
import com.llavore.hereoattendance.adapters.StudentCourseAdapter;

import java.util.ArrayList;
import java.util.List;

public class StudentHome extends AppCompatActivity implements StudentCourseAdapter.UnenrollCallback {

    private ImageView burgerIcon, addIcon;
    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;
    private NavigationHeaderManager headerManager;
    private StudentNavigationManager navigationManager;
    private DatabaseReference mDatabase;
    private RecyclerView coursesRecyclerView;
    private StudentCourseAdapter courseAdapter;
    private List<Course> enrolledCourses;
    private TextView emptyStateText;
    
    // Loading state management
    private View loadingLayout;
    private int loadingTasksCompleted = 0;
    private final int totalLoadingTasks = 2; // Enrolled courses and teacher profile data refresh

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize components
        sessionManager = new SessionManager(this);
        headerManager = new NavigationHeaderManager(sessionManager);
        navigationManager = new StudentNavigationManager(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        enrolledCourses = new ArrayList<>();
        
        // Initialize views
        drawerLayout = findViewById(R.id.main);
        burgerIcon = findViewById(R.id.burgerIcon);
        addIcon = findViewById(R.id.addIcon);
        coursesRecyclerView = findViewById(R.id.coursesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        loadingLayout = findViewById(R.id.loadingLayout);
        
        // Show loading initially
        showLoading();
        
        // Setup click listeners
        addIcon.setOnClickListener(v -> showEnrollmentDialog());
        
        // Setup RecyclerView
        setupRecyclerView();

        // Setup navigation using the common manager
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationManager.setupNavigationDrawer(drawerLayout, burgerIcon, navigationView, "home");
        
        // Load enrolled courses
        loadEnrolledCourses();
        
        // Only refresh teacher profile data if there are enrolled courses
        // This prevents infinite loading when no courses exist
        String currentUserId = sessionManager.getUserId();
        if (currentUserId != null) {
            mDatabase.child("users").child("students").child(currentUserId).child("enrolledCourses")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                        // Only refresh teacher data if there are courses
                        refreshTeacherProfileData();
                        // Debug: List all teachers in database to verify they exist
                        debugListAllTeachers();
                    } else {
                        // No courses, so we only have 1 loading task (loadEnrolledCourses)
                        // Mark the second task as completed to prevent infinite loading
                        onLoadingTaskCompleted();
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // On error, mark the second task as completed to prevent infinite loading
                    onLoadingTaskCompleted();
                }
            });
        } else {
            // No user ID, mark the second task as completed
            onLoadingTaskCompleted();
        }
    }
    
    private void refreshTeacherProfileData() {
        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) return;
        
        android.util.Log.d("ProfileRefresh", "Refreshing ALL teacher profile data for existing courses");
        
        mDatabase.child("users").child("students").child(currentUserId).child("enrolledCourses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int totalCourses = (int) snapshot.getChildrenCount();
                android.util.Log.d("ProfileRefresh", "Found " + totalCourses + " enrolled courses to refresh");
                
                if (totalCourses == 0) {
                    android.util.Log.d("ProfileRefresh", "No courses to refresh");
                    onLoadingTaskCompleted();
                    return;
                }
                
                final int[] refreshedCount = {0};
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    Course course = courseSnapshot.getValue(Course.class);
                    if (course != null && course.teacherId != null) {
                        // Always refresh teacher data (regardless of whether it exists or not)
                        refreshSingleTeacherData(course, courseSnapshot.getKey(), currentUserId, () -> {
                            refreshedCount[0]++;
                            android.util.Log.d("ProfileRefresh", "Refreshed " + refreshedCount[0] + "/" + totalCourses + " courses");
                            
                            // If this was the last course to refresh, notify adapter
                            if (refreshedCount[0] == totalCourses) {
                                android.util.Log.d("ProfileRefresh", "All teacher data refreshed, updating UI");
                                runOnUiThread(() -> {
                                    if (courseAdapter != null) {
                                        courseAdapter.notifyDataSetChanged();
                                    }
                                    onLoadingTaskCompleted();
                                });
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ProfileRefresh", "Failed to load enrolled courses: " + error.getMessage());
                onLoadingTaskCompleted();
            }
        });
    }
    
    private void refreshSingleTeacherData(Course course, String courseKey, String studentId, Runnable onComplete) {
        android.util.Log.d("ProfileRefresh", "Refreshing teacher data for course: " + course.name + " (Teacher ID: " + course.teacherId + ")");
        
        mDatabase.child("users").child("teachers").child(course.teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot teacherSnapshot) {
                if (teacherSnapshot.exists()) {
                    User teacher = teacherSnapshot.getValue(User.class);
                    if (teacher != null) {
                        // Store old values for comparison
                        String oldFirstName = course.teacherFirstName;
                        String oldLastName = course.teacherLastName;
                        String oldProfileUrl = course.teacherProfileImageUrl;
                        
                        // Update course with latest teacher profile data
                        course.teacherFirstName = teacher.getFirstName();
                        course.teacherLastName = teacher.getLastName();
                        course.teacherProfileImageUrl = teacher.getProfileImageUrl();
                        
                        // Check if data actually changed
                        boolean dataChanged = !java.util.Objects.equals(oldFirstName, course.teacherFirstName) ||
                                            !java.util.Objects.equals(oldLastName, course.teacherLastName) ||
                                            !java.util.Objects.equals(oldProfileUrl, course.teacherProfileImageUrl);
                        
                        if (dataChanged) {
                            android.util.Log.d("ProfileRefresh", "Teacher data changed for course: " + course.name);
                            android.util.Log.d("ProfileRefresh", "  - Old name: '" + oldFirstName + " " + oldLastName + "'");
                            android.util.Log.d("ProfileRefresh", "  - New name: '" + course.teacherFirstName + " " + course.teacherLastName + "'");
                            android.util.Log.d("ProfileRefresh", "  - Profile URL changed: " + !java.util.Objects.equals(oldProfileUrl, course.teacherProfileImageUrl));
                        } else {
                            android.util.Log.d("ProfileRefresh", "Teacher data unchanged for course: " + course.name);
                        }
                        
                        // Save updated course back to database
                        mDatabase.child("users").child("students").child(studentId)
                                .child("enrolledCourses").child(courseKey).setValue(course)
                                .addOnCompleteListener(task -> {
                                    if (task.isSuccessful()) {
                                        android.util.Log.d("ProfileRefresh", "Successfully updated teacher data for course: " + course.name);
                                    } else {
                                        android.util.Log.e("ProfileRefresh", "Failed to save updated teacher data: " + task.getException());
                                    }
                                    onComplete.run();
                                });
                    } else {
                        android.util.Log.e("ProfileRefresh", "Teacher object is null for course: " + course.name);
                        onComplete.run();
                    }
                } else {
                    android.util.Log.e("ProfileRefresh", "Teacher not found for course: " + course.name + " (ID: " + course.teacherId + ")");
                    onComplete.run();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ProfileRefresh", "Failed to fetch teacher data for course " + course.name + ": " + error.getMessage());
                onComplete.run();
            }
        });
    }
    
    private void debugListAllTeachers() {
        android.util.Log.d("DebugTeachers", "Listing all teachers in database...");
        mDatabase.child("users").child("teachers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("DebugTeachers", "Found " + snapshot.getChildrenCount() + " teachers in database");
                for (DataSnapshot teacherSnapshot : snapshot.getChildren()) {
                    String teacherId = teacherSnapshot.getKey();
                    User teacher = teacherSnapshot.getValue(User.class);
                    if (teacher != null) {
                        android.util.Log.d("DebugTeachers", "Teacher ID: " + teacherId);
                        android.util.Log.d("DebugTeachers", "  - Name: " + teacher.getFirstName() + " " + teacher.getLastName());
                        android.util.Log.d("DebugTeachers", "  - Email: " + teacher.getEmail());
                        android.util.Log.d("DebugTeachers", "  - User Type: " + teacher.getUserType());
                    } else {
                        android.util.Log.e("DebugTeachers", "Teacher ID: " + teacherId + " - Failed to parse User object");
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("DebugTeachers", "Failed to list teachers: " + error.getMessage());
            }
        });
    }
    
    private void setupRecyclerView() {
        courseAdapter = new StudentCourseAdapter(this, enrolledCourses, this);
        coursesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        coursesRecyclerView.setAdapter(courseAdapter);
    }
    
    // Method to handle unenrollment from adapter (implements UnenrollCallback)
    @Override
    public void onUnenroll(Course course, int position) {
        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (course.teacherId == null || course.teacherId.isEmpty()) {
            Toast.makeText(this, "Cannot unenroll: Teacher information missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Remove course from student's enrolled courses
        mDatabase.child("users").child("students").child(currentUserId).child("enrolledCourses")
                .child(course.id).removeValue().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.util.Log.d("Unenrollment", "Course removed from student's enrolled courses");
            } else {
                android.util.Log.e("Unenrollment", "Failed to remove course from student: " + task.getException().getMessage());
            }
        });
        
        // Remove student from course's students list (new structure: /courses/{courseCode}/students/)
        mDatabase.child("courses").child(course.code).child("students").child(currentUserId).removeValue()
                .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.util.Log.d("Unenrollment", "Student removed from course's students list");
            } else {
                android.util.Log.e("Unenrollment", "Failed to remove student from course: " + task.getException().getMessage());
            }
        });
        
        // Update student count using the existing method (decrement)
        android.util.Log.d("Unenrollment", "Updating student count for course: " + course.name + " (decrement)");
        updateStudentCount(course.teacherId, course.id, course.code, false);
        
        // Remove from local list and notify adapter
        enrolledCourses.remove(position);
        courseAdapter.notifyItemRemoved(position);
        courseAdapter.notifyItemRangeChanged(position, enrolledCourses.size());
        updateEmptyState();
        
        Toast.makeText(this, "Successfully unenrolled from " + course.name, Toast.LENGTH_SHORT).show();
    }
    
    private void showEnrollmentDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_enroll_course, null);
        builder.setView(dialogView);
        
        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        
        TextInputEditText courseCodeInput = dialogView.findViewById(R.id.courseCodeInput);
        MaterialButton enrollButton = dialogView.findViewById(R.id.enrollButton);
        
        enrollButton.setOnClickListener(v -> {
            String courseCode = courseCodeInput.getText().toString().trim();
            if (TextUtils.isEmpty(courseCode)) {
                Toast.makeText(this, "Please enter a course code", Toast.LENGTH_SHORT).show();
                return;
            }
            
            enrollInCourse(courseCode);
            dialog.dismiss();
        });
        
        dialog.show();
    }
    
    private void enrollInCourse(String courseCode) {
        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Validate course code
        if (courseCode == null || courseCode.trim().isEmpty()) {
            Toast.makeText(this, "Please enter a valid course code", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show loading message
        Toast.makeText(this, "Searching for course...", Toast.LENGTH_SHORT).show();
        
        android.util.Log.d("Enrollment", "Starting enrollment process for course code: " + courseCode);
        android.util.Log.d("Enrollment", "Current user ID: " + currentUserId);
        
        // Try global registry first, but immediately fallback to teacher search
        mDatabase.child("courses").child(courseCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("Enrollment", "Global registry search for code: " + courseCode + ", exists: " + snapshot.exists());
                
                if (snapshot.exists()) {
                    Course course = snapshot.getValue(Course.class);
                    if (course != null) {
                        android.util.Log.d("Enrollment", "Found course in global registry: " + course.name);
                        // Find the teacher who owns this course
                        findTeacherForCourse(course, currentUserId);
                        return; // Exit early if found in global registry
                    }
                }
                
                // Always try the fallback search as well (in case global registry is incomplete)
                android.util.Log.d("Enrollment", "Trying fallback search through teachers");
                searchCourseInTeachers(courseCode, currentUserId);
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("Enrollment", "Global registry search cancelled: " + error.getMessage());
                // Fallback: search through all teachers if global registry fails
                searchCourseInTeachers(courseCode, currentUserId);
            }
        });
    }
    
    private void searchCourseInTeachers(String courseCode, String studentId) {
        android.util.Log.d("Enrollment", "Starting fallback search through teachers for code: " + courseCode);
        
        // Fallback method: search through all teachers' courses
        mDatabase.child("users").child("teachers").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot teachersSnapshot) {
                android.util.Log.d("Enrollment", "Found " + teachersSnapshot.getChildrenCount() + " teachers to search");
                boolean courseFound = false;
                int totalCourses = 0;
                
                for (DataSnapshot teacherSnapshot : teachersSnapshot.getChildren()) {
                    String teacherId = teacherSnapshot.getKey();
                    DataSnapshot coursesSnapshot = teacherSnapshot.child("courses");
                    int teacherCourseCount = (int) coursesSnapshot.getChildrenCount();
                    totalCourses += teacherCourseCount;
                    
                    android.util.Log.d("Enrollment", "Teacher " + teacherId + " has " + teacherCourseCount + " courses");
                    
                    for (DataSnapshot courseSnapshot : coursesSnapshot.getChildren()) {
                        Course course = courseSnapshot.getValue(Course.class);
                        if (course != null) {
                            android.util.Log.d("Enrollment", "Checking course: " + course.name + " with code: " + course.code);
                            if (courseCode.equals(course.code)) {
                                android.util.Log.d("Enrollment", "Found matching course: " + course.name);
                                courseFound = true;
                                enrollStudentInCourse(studentId, teacherSnapshot.getKey(), courseSnapshot.getKey(), course);
                                return;
                            }
                        }
                    }
                }
                
                android.util.Log.d("Enrollment", "Searched " + totalCourses + " total courses, course not found");
                if (!courseFound) {
                    Toast.makeText(StudentHome.this, "Course code '" + courseCode + "' not found in any teacher's courses", Toast.LENGTH_LONG).show();
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("Enrollment", "Fallback search cancelled: " + error.getMessage());
                Toast.makeText(StudentHome.this, "Error searching for course: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    private void findTeacherForCourse(Course course, String studentId) {
        // Use the teacherId from the course to directly enroll
        if (course.teacherId != null && !course.teacherId.isEmpty()) {
            enrollStudentInCourse(studentId, course.teacherId, course.id, course);
        } else {
            Toast.makeText(StudentHome.this, "Course owner not found", Toast.LENGTH_SHORT).show();
        }
    }

    private void enrollStudentInCourse(String studentId, String teacherId, String courseId, Course course) {
        android.util.Log.d("Enrollment", "Attempting to enroll student " + studentId + " in course " + course.name + " (ID: " + courseId + ") owned by teacher " + teacherId);

        // Check if student is already enrolled
        mDatabase.child("users").child("students").child(studentId).child("enrolledCourses")
                .child(courseId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            android.util.Log.d("Enrollment", "Student already enrolled in this course");
                            Toast.makeText(StudentHome.this, "You are already enrolled in this course", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        android.util.Log.d("Enrollment", "Student not enrolled, proceeding with enrollment");

                        // Fetch teacher profile data first
                        android.util.Log.d("Enrollment", "Fetching teacher data for teacherId: " + teacherId);
                        android.util.Log.d("Enrollment", "Database path: /users/teachers/" + teacherId);
                        
                        mDatabase.child("users").child("teachers").child(teacherId).addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot teacherSnapshot) {
                                android.util.Log.d("Enrollment", "Teacher snapshot exists: " + teacherSnapshot.exists());
                                android.util.Log.d("Enrollment", "Teacher snapshot has children: " + teacherSnapshot.hasChildren());
                                
                                if (teacherSnapshot.exists()) {
                                    android.util.Log.d("Enrollment", "Teacher snapshot keys: " + teacherSnapshot.getKey());
                                    
                                    User teacher = teacherSnapshot.getValue(User.class);
                                    if (teacher != null) {
                                        android.util.Log.d("Enrollment", "Teacher object created successfully");
                                        
                                        // Add teacher profile info to course data
                                        course.teacherFirstName = teacher.getFirstName();
                                        course.teacherLastName = teacher.getLastName();
                                        course.teacherProfileImageUrl = teacher.getProfileImageUrl();
                                        
                                        android.util.Log.d("Enrollment", "Teacher data added to course:");
                                        android.util.Log.d("Enrollment", "  - First Name: '" + course.teacherFirstName + "'");
                                        android.util.Log.d("Enrollment", "  - Last Name: '" + course.teacherLastName + "'");
                                        android.util.Log.d("Enrollment", "  - Profile URL: '" + course.teacherProfileImageUrl + "'");
                                    } else {
                                        android.util.Log.e("Enrollment", "Teacher object is null - failed to parse User from snapshot");
                                        android.util.Log.e("Enrollment", "Snapshot value: " + teacherSnapshot.getValue());
                                    }
                                } else {
                                    android.util.Log.e("Enrollment", "Teacher snapshot doesn't exist for teacherId: " + teacherId);
                                    android.util.Log.e("Enrollment", "This means the teacher user doesn't exist in the database");
                                }

                                // Save course to student's enrolled courses
                                android.util.Log.d("Enrollment", "Saving course to database...");
                                mDatabase.child("users").child("students").child(studentId).child("enrolledCourses")
                                        .child(courseId).setValue(course).addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                android.util.Log.d("Enrollment", "Course successfully saved to student's enrolled courses");
                                                Toast.makeText(StudentHome.this, "Successfully enrolled in course!", Toast.LENGTH_SHORT).show();
                                            } else {
                                                android.util.Log.e("Enrollment", "Failed to save course to student: " + task.getException());
                                                if (task.getException() != null) {
                                                    android.util.Log.e("Enrollment", "Error details: " + task.getException().getMessage());
                                                }
                                                Toast.makeText(StudentHome.this, "Failed to enroll in course: " + (task.getException() != null ? task.getException().getMessage() : "Unknown error"), Toast.LENGTH_LONG).show();
                                            }
                                        });

                                // Add student to course's students list (new structure: /courses/{courseCode}/students/)
                                mDatabase.child("courses").child(course.code).child("students").child(studentId).setValue(true)
                                        .addOnCompleteListener(task -> {
                                            if (task.isSuccessful()) {
                                                android.util.Log.d("Enrollment", "Student successfully added to course's students list");
                                            } else {
                                                android.util.Log.e("Enrollment", "Failed to add student to course's students list: " + task.getException());
                                            }
                                        });

                                // Update student count
                                updateStudentCount(teacherId, courseId, course.code, true);
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                android.util.Log.e("Enrollment", "Failed to fetch teacher data:");
                                android.util.Log.e("Enrollment", "  - Error code: " + error.getCode());
                                android.util.Log.e("Enrollment", "  - Error message: " + error.getMessage());
                                android.util.Log.e("Enrollment", "  - Error details: " + error.getDetails());
                                android.util.Log.e("Enrollment", "  - Teacher ID: " + teacherId);
                                
                                String errorMessage = "Error fetching teacher data";
                                if (error.getCode() == DatabaseError.PERMISSION_DENIED) {
                                    errorMessage = "Permission denied - check database rules";
                                } else if (error.getCode() == DatabaseError.NETWORK_ERROR) {
                                    errorMessage = "Network error - check internet connection";
                                } else if (error.getCode() == DatabaseError.DISCONNECTED) {
                                    errorMessage = "Database disconnected";
                                }
                                
                                Toast.makeText(StudentHome.this, errorMessage, Toast.LENGTH_LONG).show();
                            }
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("Enrollment", "Error checking enrollment: " + error.getMessage());
                        Toast.makeText(StudentHome.this, "Error checking enrollment", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadEnrolledCourses() {
        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) return;

        android.util.Log.d("LoadCourses", "Loading enrolled courses for student: " + currentUserId);

        mDatabase.child("users").child("students").child(currentUserId).child("enrolledCourses")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        android.util.Log.d("LoadCourses", "Found " + snapshot.getChildrenCount() + " enrolled courses");
                        enrolledCourses.clear();

                        if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                            courseAdapter.notifyDataSetChanged();
                            updateEmptyState();
                            onLoadingTaskCompleted();
                            return;
                        }

                        for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                            Course course = courseSnapshot.getValue(Course.class);
                            if (course != null) {
                                course.id = courseSnapshot.getKey();

                                android.util.Log.d("LoadCourses", "Loaded course: " + course.name);
                                android.util.Log.d("LoadCourses", "  - Course ID: " + course.id);
                                android.util.Log.d("LoadCourses", "  - Teacher ID: " + course.teacherId);
                                android.util.Log.d("LoadCourses", "  - Teacher First Name: '" + course.teacherFirstName + "'");
                                android.util.Log.d("LoadCourses", "  - Teacher Last Name: '" + course.teacherLastName + "'");
                                android.util.Log.d("LoadCourses", "  - Teacher Profile URL: '" + course.teacherProfileImageUrl + "'");

                                // Check if teacher data is missing
                                boolean missingTeacherData = (course.teacherFirstName == null || course.teacherFirstName.trim().isEmpty()) &&
                                        (course.teacherLastName == null || course.teacherLastName.trim().isEmpty());

                                if (missingTeacherData && course.teacherId != null && !course.teacherId.isEmpty()) {
                                    android.util.Log.d("LoadCourses", "Teacher data missing for course " + course.name + ", fetching now...");
                                    fetchAndUpdateTeacherData(course, courseSnapshot.getKey(), currentUserId);
                                }

                                enrolledCourses.add(course);
                            }
                        }

                        courseAdapter.notifyDataSetChanged();
                        updateEmptyState();
                        onLoadingTaskCompleted();

                        android.util.Log.d("LoadCourses", "Total courses loaded: " + enrolledCourses.size());
                        
                        // After loading courses, refresh teacher data to get latest information
                        android.util.Log.d("LoadCourses", "Starting teacher data refresh after loading courses");
                        refreshTeacherProfileData();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("LoadCourses", "Failed to load enrolled courses: " + error.getMessage());
                        onLoadingTaskCompleted();
                    }
                });
    }

    private void fetchAndUpdateTeacherData(Course course, String courseKey, String studentId) {
        mDatabase.child("users").child("teachers").child(course.teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot teacherSnapshot) {
                        if (teacherSnapshot.exists()) {
                            User teacher = teacherSnapshot.getValue(User.class);
                            if (teacher != null) {
                                android.util.Log.d("LoadCourses", "Fetched teacher data for " + course.name);
                                android.util.Log.d("LoadCourses", "  - First Name: '" + teacher.getFirstName() + "'");
                                android.util.Log.d("LoadCourses", "  - Last Name: '" + teacher.getLastName() + "'");
                                android.util.Log.d("LoadCourses", "  - Profile URL: '" + teacher.getProfileImageUrl() + "'");

                                // Update course object
                                course.teacherFirstName = teacher.getFirstName();
                                course.teacherLastName = teacher.getLastName();
                                course.teacherProfileImageUrl = teacher.getProfileImageUrl();

                                // Save updated course back to database
                                mDatabase.child("users").child("students").child(studentId)
                                        .child("enrolledCourses").child(courseKey).setValue(course)
                                        .addOnSuccessListener(aVoid -> {
                                            android.util.Log.d("LoadCourses", "Successfully saved teacher data for " + course.name);
                                            // Notify adapter to refresh this specific course
                                            int position = enrolledCourses.indexOf(course);
                                            if (position >= 0) {
                                                courseAdapter.notifyItemChanged(position);
                                            }
                                        })
                                        .addOnFailureListener(e -> {
                                            android.util.Log.e("LoadCourses", "Failed to save teacher data: " + e.getMessage());
                                        });
                            } else {
                                android.util.Log.e("LoadCourses", "Teacher object is null");
                            }
                        } else {
                            android.util.Log.e("LoadCourses", "Teacher snapshot doesn't exist for ID: " + course.teacherId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("LoadCourses", "Failed to fetch teacher data: " + error.getMessage());
                    }
                });
    }
    
    private void updateEmptyState() {
        if (emptyStateText != null) {
            emptyStateText.setVisibility(enrolledCourses.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();
        // Navigation header is now handled by the navigation manager
        
        // Reset loading state for onResume
        loadingTasksCompleted = 0;
        showLoading();
        
        // Refresh enrolled courses
        loadEnrolledCourses();
        
        // Only refresh teacher profile data if there are enrolled courses
        // This prevents infinite loading when no courses exist
        String currentUserId = sessionManager.getUserId();
        if (currentUserId != null) {
            mDatabase.child("users").child("students").child(currentUserId).child("enrolledCourses")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && snapshot.getChildrenCount() > 0) {
                        // Only refresh teacher data if there are courses
                        refreshTeacherProfileData();
                    } else {
                        // No courses, so we only have 1 loading task (loadEnrolledCourses)
                        // Mark the second task as completed to prevent infinite loading
                        onLoadingTaskCompleted();
                    }
                }
                
                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    // On error, mark the second task as completed to prevent infinite loading
                    onLoadingTaskCompleted();
                }
            });
        } else {
            // No user ID, mark the second task as completed
            onLoadingTaskCompleted();
        }
    }
    
    private void showLoading() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.VISIBLE);
        }
        if (coursesRecyclerView != null) {
            coursesRecyclerView.setVisibility(View.GONE);
        }
        if (emptyStateText != null) {
            emptyStateText.setVisibility(View.GONE);
        }
    }
    
    private void hideLoading() {
        if (loadingLayout != null) {
            loadingLayout.setVisibility(View.GONE);
        }
        if (coursesRecyclerView != null) {
            coursesRecyclerView.setVisibility(View.VISIBLE);
        }
        updateEmptyState();
    }
    
    private void onLoadingTaskCompleted() {
        loadingTasksCompleted++;
        if (loadingTasksCompleted >= totalLoadingTasks) {
            hideLoading();
        }
    }

    private void updateStudentCount(String teacherId, String courseId, String courseCode, boolean increment) {
        android.util.Log.d("StudentCount", "Updating student count - Teacher: " + teacherId + ", Course: " + courseId + ", Code: " + courseCode);
        
        // Count students from the new structure: /courses/{courseCode}/students/
        mDatabase.child("courses").child(courseCode).child("students").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                int studentCount = (int) snapshot.getChildrenCount();
                android.util.Log.d("StudentCount", "Current student count from /courses/{courseCode}/students/: " + studentCount);

                // Update in teacher's course
                mDatabase.child("users").child("teachers").child(teacherId).child("courses")
                        .child(courseId).child("studentCount").setValue(studentCount)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("StudentCount", "Successfully updated student count in teacher's course to: " + studentCount);
                            } else {
                                android.util.Log.e("StudentCount", "Failed to update student count in teacher's course: " + task.getException());
                            }
                        });

                // Update in global courses
                mDatabase.child("courses").child(courseCode).child("studentCount").setValue(studentCount)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("StudentCount", "Successfully updated student count in global courses to: " + studentCount);
                            } else {
                                android.util.Log.e("StudentCount", "Failed to update student count in global courses: " + task.getException());
                            }
                        });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("StudentCount", "Failed to read students from course:");
                android.util.Log.e("StudentCount", "  - Error code: " + error.getCode());
                android.util.Log.e("StudentCount", "  - Error message: " + error.getMessage());
            }
        });
    }
    
    @Override
    public void onBackPressed() {
        // Move app to background instead of going back to MainActivity
        moveTaskToBack(true);
    }
}