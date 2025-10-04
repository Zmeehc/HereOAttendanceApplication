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
import com.llavore.hereoattendance.adapters.StudentCourseAdapter;

import java.util.ArrayList;
import java.util.List;

public class StudentHome extends AppCompatActivity implements StudentCourseAdapter.UnenrollCallback {

    private ImageView burgerIcon, addIcon;
    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;
    private NavigationHeaderManager headerManager;
    private DatabaseReference mDatabase;
    private RecyclerView coursesRecyclerView;
    private StudentCourseAdapter courseAdapter;
    private List<Course> enrolledCourses;
    private TextView emptyStateText;

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
        mDatabase = FirebaseDatabase.getInstance().getReference();
        enrolledCourses = new ArrayList<>();
        
        // Initialize views
        drawerLayout = findViewById(R.id.main);
        burgerIcon = findViewById(R.id.burgerIcon);
        addIcon = findViewById(R.id.addIcon);
        coursesRecyclerView = findViewById(R.id.coursesRecyclerView);
        emptyStateText = findViewById(R.id.emptyStateText);
        
        // Setup click listeners
        burgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        addIcon.setOnClickListener(v -> showEnrollmentDialog());
        
        // Setup RecyclerView
        setupRecyclerView();

        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setCheckedItem(R.id.nav_home);
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_logout) {
                new AlertDialog.Builder(this)
                        .setTitle("Logout")
                        .setMessage("Are you sure you want to logout?")
                        .setPositiveButton("Yes", (dialog, which) -> {
                            sessionManager.logout();
                            Intent intent = new Intent(StudentHome.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_home) {
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_account) {
                startActivity(new Intent(StudentHome.this, StudentProfileActivity.class));
                drawerLayout.closeDrawers();
                return true;
            } else {
                drawerLayout.closeDrawers();
                return true;
            }
        });
        
        // Load user data into navigation header
        loadNavigationHeader(navigationView);
        
        // Load enrolled courses
        loadEnrolledCourses();
        
        // Refresh teacher profile data for existing courses (for courses enrolled before this update)
        refreshTeacherProfileData();
    }
    
    private void refreshTeacherProfileData() {
        String currentUserId = sessionManager.getUserId();
        if (currentUserId == null) return;
        
        android.util.Log.d("ProfileRefresh", "Refreshing teacher profile data for existing courses");
        
        mDatabase.child("users").child("students").child(currentUserId).child("enrolledCourses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    Course course = courseSnapshot.getValue(Course.class);
                    if (course != null && course.teacherId != null && 
                        (course.teacherFirstName == null || course.teacherLastName == null)) {
                        
                        // Fetch teacher data and update course
                        mDatabase.child("users").child("teachers").child(course.teacherId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot teacherSnapshot) {
                                if (teacherSnapshot.exists()) {
                                    User teacher = teacherSnapshot.getValue(User.class);
                                    if (teacher != null) {
                                        // Update course with teacher profile data
                                        course.teacherFirstName = teacher.getFirstName();
                                        course.teacherLastName = teacher.getLastName();
                                        course.teacherProfileImageUrl = teacher.getProfileImageUrl();
                                        
                                        // Save updated course back to database
                                        mDatabase.child("users").child("students").child(currentUserId)
                                                .child("enrolledCourses").child(courseSnapshot.getKey()).setValue(course);
                                        
                                        android.util.Log.d("ProfileRefresh", "Updated teacher profile data for course: " + course.name);
                                    }
                                }
                            }
                            
                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {
                                android.util.Log.e("ProfileRefresh", "Failed to fetch teacher data: " + error.getMessage());
                            }
                        });
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ProfileRefresh", "Failed to load enrolled courses: " + error.getMessage());
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
        
        // Remove student from course's enrolled students
        mDatabase.child("users").child("teachers").child(course.teacherId).child("courses")
                .child(course.id).child("enrolledStudents").child(currentUserId).removeValue()
                .addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                android.util.Log.d("Unenrollment", "Student removed from course's enrolled students");
            } else {
                android.util.Log.e("Unenrollment", "Failed to remove student from course: " + task.getException().getMessage());
            }
        });
        
        // Update student count in both teacher's course and global courses
        mDatabase.child("users").child("teachers").child(course.teacherId).child("courses")
                .child(course.id).child("studentCount").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer currentCount = snapshot.getValue(Integer.class);
                if (currentCount != null && currentCount > 0) {
                    int newCount = currentCount - 1;
                    android.util.Log.d("Unenrollment", "Updating student count from " + currentCount + " to " + newCount);
                    
                    // Update student count in teacher's course
                    mDatabase.child("users").child("teachers").child(course.teacherId).child("courses")
                            .child(course.id).child("studentCount").setValue(newCount).addOnCompleteListener(updateTask -> {
                        if (updateTask.isSuccessful()) {
                            android.util.Log.d("Unenrollment", "Student count successfully updated in teacher's course to: " + newCount);
                        } else {
                            android.util.Log.e("Unenrollment", "Failed to update student count in teacher's course: " + updateTask.getException().getMessage());
                        }
                    });
                    
                    // Also update student count in global courses registry
                    mDatabase.child("courses").child(course.code).child("studentCount").setValue(newCount).addOnCompleteListener(globalUpdateTask -> {
                        if (globalUpdateTask.isSuccessful()) {
                            android.util.Log.d("Unenrollment", "Student count successfully updated in global courses to: " + newCount);
                        } else {
                            android.util.Log.e("Unenrollment", "Failed to update student count in global courses: " + globalUpdateTask.getException().getMessage());
                        }
                    });
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("Unenrollment", "Failed to get current student count: " + error.getMessage());
            }
        });
        
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
        
        // Show loading message
        Toast.makeText(this, "Searching for course...", Toast.LENGTH_SHORT).show();
        
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
                
                // Fetch teacher profile data and add course to student's enrolled courses
                android.util.Log.d("Enrollment", "Fetching teacher data for teacherId: " + teacherId);
                mDatabase.child("users").child("teachers").child(teacherId).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot teacherSnapshot) {
                        android.util.Log.d("Enrollment", "Teacher snapshot exists: " + teacherSnapshot.exists());
                        
                        if (teacherSnapshot.exists()) {
                            User teacher = teacherSnapshot.getValue(User.class);
                            if (teacher != null) {
                                // Add teacher profile info to course data for faster loading
                                course.teacherFirstName = teacher.getFirstName();
                                course.teacherLastName = teacher.getLastName();
                                course.teacherProfileImageUrl = teacher.getProfileImageUrl();
                                
                                android.util.Log.d("Enrollment", "Teacher data found:");
                                android.util.Log.d("Enrollment", "  - First Name: '" + teacher.getFirstName() + "'");
                                android.util.Log.d("Enrollment", "  - Last Name: '" + teacher.getLastName() + "'");
                                android.util.Log.d("Enrollment", "  - Profile Image URL: '" + teacher.getProfileImageUrl() + "'");
                                android.util.Log.d("Enrollment", "  - Full Name: '" + (teacher.getFirstName() + " " + teacher.getLastName()).trim() + "'");
                                
                                // Log the course data before saving
                                android.util.Log.d("Enrollment", "Course data before saving:");
                                android.util.Log.d("Enrollment", "  - Course Name: '" + course.name + "'");
                                android.util.Log.d("Enrollment", "  - Teacher First Name: '" + course.teacherFirstName + "'");
                                android.util.Log.d("Enrollment", "  - Teacher Last Name: '" + course.teacherLastName + "'");
                                android.util.Log.d("Enrollment", "  - Teacher Profile URL: '" + course.teacherProfileImageUrl + "'");
                            } else {
                                android.util.Log.e("Enrollment", "Teacher object is null");
                            }
                        } else {
                            android.util.Log.e("Enrollment", "Teacher snapshot does not exist for teacherId: " + teacherId);
                        }
                        
                        // Add course to student's enrolled courses (with or without teacher data)
                        android.util.Log.d("Enrollment", "Saving course to student's enrolled courses...");
                        mDatabase.child("users").child("students").child(studentId).child("enrolledCourses")
                                .child(courseId).setValue(course).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("Enrollment", "Course successfully added to student's enrolled courses");
                            } else {
                                android.util.Log.e("Enrollment", "Failed to add course to student: " + task.getException().getMessage());
                            }
                        });
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("Enrollment", "Failed to fetch teacher data: " + error.getMessage());
                        // Still add course to student's enrolled courses even if teacher data fetch fails
                        mDatabase.child("users").child("students").child(studentId).child("enrolledCourses")
                                .child(courseId).setValue(course).addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("Enrollment", "Course added to student's enrolled courses (without teacher data)");
                            } else {
                                android.util.Log.e("Enrollment", "Failed to add course to student: " + task.getException().getMessage());
                            }
                        });
                    }
                });
                
                // Add student to course's enrolled students
                mDatabase.child("users").child("teachers").child(teacherId).child("courses")
                        .child(courseId).child("enrolledStudents").child(studentId).setValue(true).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        android.util.Log.d("Enrollment", "Student added to course's enrolled students");
                    } else {
                        android.util.Log.e("Enrollment", "Failed to add student to course: " + task.getException().getMessage());
                    }
                });
                
                // Update student count in both teacher's course and global courses
                mDatabase.child("users").child("teachers").child(teacherId).child("courses")
                        .child(courseId).child("studentCount").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Integer currentCount = snapshot.getValue(Integer.class);
                        if (currentCount == null) currentCount = 0;
                        int newCount = currentCount + 1;
                        android.util.Log.d("Enrollment", "Current student count: " + currentCount + ", updating to: " + newCount);
                        
                        // Update student count in teacher's course
                        mDatabase.child("users").child("teachers").child(teacherId).child("courses")
                                .child(courseId).child("studentCount").setValue(newCount).addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                android.util.Log.d("Enrollment", "Student count successfully updated in teacher's course to: " + newCount);
                            } else {
                                android.util.Log.e("Enrollment", "Failed to update student count in teacher's course: " + updateTask.getException().getMessage());
                            }
                        });
                        
                        // Also update student count in global courses registry
                        mDatabase.child("courses").child(course.code).child("studentCount").setValue(newCount).addOnCompleteListener(globalUpdateTask -> {
                            if (globalUpdateTask.isSuccessful()) {
                                android.util.Log.d("Enrollment", "Student count successfully updated in global courses to: " + newCount);
                            } else {
                                android.util.Log.e("Enrollment", "Failed to update student count in global courses: " + globalUpdateTask.getException().getMessage());
                            }
                        });
                    }
                    
                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("Enrollment", "Failed to get current student count: " + error.getMessage());
                    }
                });
                
                Toast.makeText(StudentHome.this, "Successfully enrolled in course!", Toast.LENGTH_SHORT).show();
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
                for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                    Course course = courseSnapshot.getValue(Course.class);
                    if (course != null) {
                        course.id = courseSnapshot.getKey();
                        android.util.Log.d("LoadCourses", "Loaded course: " + course.name + " | TeacherId: " + course.teacherId + " | ID: " + course.id);
                        android.util.Log.d("LoadCourses", "  - Teacher First Name: '" + course.teacherFirstName + "'");
                        android.util.Log.d("LoadCourses", "  - Teacher Last Name: '" + course.teacherLastName + "'");
                        android.util.Log.d("LoadCourses", "  - Teacher Profile URL: '" + course.teacherProfileImageUrl + "'");
                        enrolledCourses.add(course);
                    }
                }
                courseAdapter.notifyDataSetChanged();
                updateEmptyState();
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("LoadCourses", "Failed to load enrolled courses: " + error.getMessage());
            }
        });
    }
    
    private void updateEmptyState() {
        if (emptyStateText != null) {
            emptyStateText.setVisibility(enrolledCourses.isEmpty() ? View.VISIBLE : View.GONE);
        }
    }
    
    private void loadNavigationHeader(NavigationView navigationView) {
        android.view.View headerView = navigationView.getHeaderView(0);
        ImageView profilePicture = headerView.findViewById(R.id.navProfilePicture);
        TextView userName = headerView.findViewById(R.id.navUserName);
        TextView userEmail = headerView.findViewById(R.id.navUserEmail);
        
        if (profilePicture != null && userName != null && userEmail != null) {
            headerManager.loadUserData(profilePicture, userName, userEmail);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // Refresh navigation header when returning to activity
        NavigationView navigationView = findViewById(R.id.navigationView);
        loadNavigationHeader(navigationView);
        
        // Refresh enrolled courses
        loadEnrolledCourses();
    }
}