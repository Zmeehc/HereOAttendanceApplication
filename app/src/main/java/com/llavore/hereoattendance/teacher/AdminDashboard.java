package com.llavore.hereoattendance.teacher;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.adapters.TeacherAdapter;
import com.llavore.hereoattendance.models.Teacher;

import java.util.ArrayList;
import java.util.List;

public class AdminDashboard extends AppCompatActivity {

    private ImageView backArrow, addTeacherButton;
    private RecyclerView teachersRecyclerView;
    private LinearLayout emptyState, loadingState;
    private TeacherAdapter teacherAdapter;
    private List<Teacher> teacherList;
    private DatabaseReference mDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_admin_dashboard);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize Firebase
        mDatabase = FirebaseDatabase.getInstance().getReference();

        initializeViews();
        setupClickListeners();
        setupRecyclerView();
        loadTeachers();
    }

    private void initializeViews() {
        backArrow = findViewById(R.id.backArrow);
        addTeacherButton = findViewById(R.id.addTeacherButton);
        teachersRecyclerView = findViewById(R.id.teachersRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        loadingState = findViewById(R.id.loadingState);
    }

    private void setupClickListeners() {
        backArrow.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboard.this, TeacherLoginActivity.class);
            startActivity(intent);
            finish();
        });

        addTeacherButton.setOnClickListener(v -> {
            Intent intent = new Intent(AdminDashboard.this, TeacherSignUpActivity.class);
            startActivity(intent);
        });
    }

    private void setupRecyclerView() {
        teacherList = new ArrayList<>();
        teacherAdapter = new TeacherAdapter(teacherList);
        teachersRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        teachersRecyclerView.setAdapter(teacherAdapter);
    }

    private void loadTeachers() {
        // Show loading state
        loadingState.setVisibility(View.VISIBLE);
        teachersRecyclerView.setVisibility(View.GONE);
        emptyState.setVisibility(View.GONE);
        
        mDatabase.child("users").child("teachers")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        teacherList.clear();
                        
                        for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                            Teacher teacher = snapshot.getValue(Teacher.class);
                            if (teacher != null) {
                                teacher.setUid(snapshot.getKey());
                                teacherList.add(teacher);
                            }
                        }
                        
                        teacherAdapter.notifyDataSetChanged();
                        updateEmptyState();
                        
                        // Hide loading state
                        loadingState.setVisibility(View.GONE);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                        loadingState.setVisibility(View.GONE);
                        Toast.makeText(AdminDashboard.this, "Failed to load teachers: " + databaseError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateEmptyState() {
        if (teacherList.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            teachersRecyclerView.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            teachersRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh the list when returning from teacher signup
        loadTeachers();
    }
}