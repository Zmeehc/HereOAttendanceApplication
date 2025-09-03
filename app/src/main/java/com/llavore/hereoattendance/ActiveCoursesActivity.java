package com.llavore.hereoattendance;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
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
import com.llavore.hereoattendance.adapters.CourseAdapter;
import com.llavore.hereoattendance.models.Course;

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
            Intent intent = new Intent(ActiveCoursesActivity.this, TeacherDashboard.class);
            startActivity(intent);
            finish();
        });

        addCourseButton.setOnClickListener(v -> {
            Intent intent = new Intent(ActiveCoursesActivity.this, CreateCourseActivity.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the course list when returning to this activity
        loadCoursesForCurrentUser();

    }

    private void loadCoursesForCurrentUser() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) return;

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child("teachers").child(uid).child("courses");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                courses.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    Course course = child.getValue(Course.class);
                    if (course != null) {
                        course.id = child.getKey();
                        courses.add(course);
                    }
                }
                adapter.notifyDataSetChanged();
                emptyStateText.setVisibility(courses.isEmpty() ? View.VISIBLE : View.GONE);
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // no-op for now
            }
        });
    }
}

