package com.llavore.hereoattendance.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.CourseAdapter;
import com.llavore.hereoattendance.models.Course;
import com.llavore.hereoattendance.utils.TransitionManager;

import java.util.ArrayList;
import java.util.List;

public class ActiveCoursesActivity extends AppCompatActivity {

    private ImageView backToDashboardButton, addCourseButton;
    private RecyclerView recyclerCourses;
    private View emptyStateText;
    private CourseAdapter adapter;
    private final List<Course> courses = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_active_courses);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        backToDashboardButton = findViewById(R.id.backDashArrow);
        addCourseButton = findViewById(R.id.addCoursebtn);
        recyclerCourses = findViewById(R.id.recyclerCourses);
        emptyStateText = findViewById(R.id.emptyStateText);

        recyclerCourses.setLayoutManager(new LinearLayoutManager(this));
        adapter = new CourseAdapter(this, courses);
        recyclerCourses.setAdapter(adapter);

        backToDashboardButton.setOnClickListener(v -> {
            TransitionManager.startActivityBackward(ActiveCoursesActivity.this, TeacherDashboard.class);
            finish();
        });

        addCourseButton.setOnClickListener(v -> {
            TransitionManager.startActivityForward(ActiveCoursesActivity.this, CreateCourseActivity.class);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the course list when returning to this activity
        android.util.Log.d("ActiveCourses", "onResume called, refreshing courses");
        loadCoursesForCurrentUser();
        
        // Also test database access
        testDatabaseAccess();
    }
    
    private void testDatabaseAccess() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;
        
        android.util.Log.d("ActiveCourses", "Testing database access for teacher: " + uid);
        android.util.Log.d("ActiveCourses", "Database path: /users/teachers/" + uid + "/courses");
        
        // Test reading teacher profile
        DatabaseReference teacherRef = FirebaseDatabase.getInstance().getReference("users").child("teachers").child(uid);
        teacherRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("ActiveCourses", "Teacher profile test - snapshot exists: " + snapshot.exists());
                if (snapshot.exists()) {
                    android.util.Log.d("ActiveCourses", "Teacher profile test - has children: " + snapshot.hasChildren());
                    android.util.Log.d("ActiveCourses", "Teacher profile test - keys: " + snapshot.getKey());
                    
                    // List all children of teacher profile
                    for (DataSnapshot child : snapshot.getChildren()) {
                        android.util.Log.d("ActiveCourses", "Teacher profile child: " + child.getKey() + " = " + child.getValue());
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ActiveCourses", "Teacher profile test failed:");
                android.util.Log.e("ActiveCourses", "  - Error code: " + error.getCode());
                android.util.Log.e("ActiveCourses", "  - Error message: " + error.getMessage());
            }
        });
        
        // Test reading courses
        DatabaseReference coursesRef = FirebaseDatabase.getInstance().getReference("users").child("teachers").child(uid).child("courses");
        coursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("ActiveCourses", "Courses test - snapshot exists: " + snapshot.exists());
                android.util.Log.d("ActiveCourses", "Courses test - children count: " + snapshot.getChildrenCount());
                
                if (snapshot.exists()) {
                    // List all course children
                    for (DataSnapshot child : snapshot.getChildren()) {
                        android.util.Log.d("ActiveCourses", "Course child: " + child.getKey() + " = " + child.getValue());
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ActiveCourses", "Courses test failed:");
                android.util.Log.e("ActiveCourses", "  - Error code: " + error.getCode());
                android.util.Log.e("ActiveCourses", "  - Error message: " + error.getMessage());
            }
        });
        
        // Also test the global courses registry
        DatabaseReference globalCoursesRef = FirebaseDatabase.getInstance().getReference("courses");
        globalCoursesRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("ActiveCourses", "Global courses test - snapshot exists: " + snapshot.exists());
                android.util.Log.d("ActiveCourses", "Global courses test - children count: " + snapshot.getChildrenCount());
                
                if (snapshot.exists()) {
                    // List all global course children
                    for (DataSnapshot child : snapshot.getChildren()) {
                        android.util.Log.d("ActiveCourses", "Global course: " + child.getKey() + " = " + child.getValue());
                    }
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("ActiveCourses", "Global courses test failed:");
                android.util.Log.e("ActiveCourses", "  - Error code: " + error.getCode());
                android.util.Log.e("ActiveCourses", "  - Error message: " + error.getMessage());
            }
        });
    }

    private void loadCoursesForCurrentUser() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            android.util.Log.e("ActiveCourses", "User ID is null, cannot load courses");
            return;
        }

        android.util.Log.d("ActiveCourses", "Loading courses for teacher: " + uid);
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child("teachers").child(uid).child("courses");
        
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                android.util.Log.d("ActiveCourses", "Data changed, snapshot exists: " + snapshot.exists());
                android.util.Log.d("ActiveCourses", "Snapshot children count: " + snapshot.getChildrenCount());
                
                courses.clear();
                
                if (!snapshot.exists() || snapshot.getChildrenCount() == 0) {
                    android.util.Log.d("ActiveCourses", "No courses found or snapshot doesn't exist");
                    adapter.notifyDataSetChanged();
                    emptyStateText.setVisibility(View.VISIBLE);
                    return;
                }
                
                for (DataSnapshot child : snapshot.getChildren()) {
                    Course course = child.getValue(Course.class);
                    if (course != null) {
                        course.id = child.getKey();
                        courses.add(course);
                        android.util.Log.d("ActiveCourses", "Loaded course: " + course.name + " (ID: " + course.id + ")");
                    } else {
                        android.util.Log.e("ActiveCourses", "Failed to parse course from snapshot: " + child.getKey());
                    }
                }
                
                android.util.Log.d("ActiveCourses", "Total courses loaded: " + courses.size());
                adapter.notifyDataSetChanged();
                emptyStateText.setVisibility(courses.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                android.util.Log.e("ActiveCourses", "Failed to load courses:");
                android.util.Log.e("ActiveCourses", "  - Error code: " + error.getCode());
                android.util.Log.e("ActiveCourses", "  - Error message: " + error.getMessage());
                android.util.Log.e("ActiveCourses", "  - Error details: " + error.getDetails());
                
                // Show empty state on error
                courses.clear();
                adapter.notifyDataSetChanged();
                emptyStateText.setVisibility(View.VISIBLE);
            }
        });
    }
}

