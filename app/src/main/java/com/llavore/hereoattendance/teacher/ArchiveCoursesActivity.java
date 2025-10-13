package com.llavore.hereoattendance.teacher;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
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
import com.llavore.hereoattendance.adapters.ArchivedCourseAdapter;
import com.llavore.hereoattendance.models.Course;

import java.util.ArrayList;
import java.util.List;

public class ArchiveCoursesActivity extends AppCompatActivity {

    private ImageView backButton;
    private RecyclerView archivedCoursesRecyclerView;
    private LinearLayout noArchivedCoursesLayout;
    private ArchivedCourseAdapter adapter;
    private List<Course> archivedCourses;
    private DatabaseReference mDatabase;
    private String currentTeacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archive_courses);
        
        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        setupClickListeners();
        loadArchivedCourses();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        archivedCoursesRecyclerView = findViewById(R.id.archivedCoursesRecyclerView);
        noArchivedCoursesLayout = findViewById(R.id.noArchivedCoursesLayout);
        
        archivedCourses = new ArrayList<>();
    }

    private void initializeFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentTeacherId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private void setupRecyclerView() {
        adapter = new ArchivedCourseAdapter(archivedCourses, this);
        archivedCoursesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        archivedCoursesRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadArchivedCourses() {
        if (currentTeacherId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users").child("teachers").child(currentTeacherId).child("archivedCourses")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        archivedCourses.clear();
                        
                        if (snapshot.exists()) {
                            for (DataSnapshot courseSnapshot : snapshot.getChildren()) {
                                Course course = courseSnapshot.getValue(Course.class);
                                if (course != null) {
                                    archivedCourses.add(course);
                                }
                            }
                        }
                        
                        adapter.notifyDataSetChanged();
                        
                        if (archivedCourses.isEmpty()) {
                            showNoArchivedCourses();
                        } else {
                            showArchivedCourses();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(ArchiveCoursesActivity.this, "Failed to load archived courses", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showArchivedCourses() {
        archivedCoursesRecyclerView.setVisibility(View.VISIBLE);
        noArchivedCoursesLayout.setVisibility(View.GONE);
    }

    private void showNoArchivedCourses() {
        archivedCoursesRecyclerView.setVisibility(View.GONE);
        noArchivedCoursesLayout.setVisibility(View.VISIBLE);
    }
    
    public void onCourseUnarchived() {
        // Refresh the archive count in the dashboard
        // This will be handled by the dashboard's onResume when user returns
        android.util.Log.d("ArchiveCoursesActivity", "Course unarchived, archive count will be updated when returning to dashboard");
        
        // Check if we need to show the no archived courses message
        if (archivedCourses.isEmpty()) {
            showNoArchivedCourses();
        }
    }
}
