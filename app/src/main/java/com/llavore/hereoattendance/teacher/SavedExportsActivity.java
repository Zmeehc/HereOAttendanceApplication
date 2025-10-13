package com.llavore.hereoattendance.teacher;

import android.content.Intent;
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
import com.llavore.hereoattendance.adapters.SavedExportAdapter;
import com.llavore.hereoattendance.models.SavedExport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SavedExportsActivity extends AppCompatActivity {

    private ImageView backButton;
    private RecyclerView savedExportsRecyclerView;
    private LinearLayout noExportsLayout;
    private SavedExportAdapter adapter;
    private List<SavedExport> savedExports;
    private DatabaseReference mDatabase;
    private String currentTeacherId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_exports);
        
        initializeViews();
        initializeFirebase();
        setupRecyclerView();
        setupClickListeners();
        loadSavedExports();
    }

    private void initializeViews() {
        backButton = findViewById(R.id.backButton);
        savedExportsRecyclerView = findViewById(R.id.savedExportsRecyclerView);
        noExportsLayout = findViewById(R.id.noExportsLayout);
        
        savedExports = new ArrayList<>();
    }

    private void initializeFirebase() {
        mDatabase = FirebaseDatabase.getInstance().getReference();
        currentTeacherId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    private void setupRecyclerView() {
        adapter = new SavedExportAdapter(savedExports, this);
        savedExportsRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        savedExportsRecyclerView.setAdapter(adapter);
    }

    private void setupClickListeners() {
        backButton.setOnClickListener(v -> finish());
    }

    private void loadSavedExports() {
        if (currentTeacherId == null) {
            Toast.makeText(this, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        mDatabase.child("users").child("teachers").child(currentTeacherId).child("savedExports")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        savedExports.clear();
                        
                        if (snapshot.exists()) {
                            for (DataSnapshot exportSnapshot : snapshot.getChildren()) {
                                SavedExport export = exportSnapshot.getValue(SavedExport.class);
                                if (export != null) {
                                    savedExports.add(export);
                                }
                            }
                            
                            // Sort by timestamp (newest first)
                            Collections.sort(savedExports, (e1, e2) -> Long.compare(e2.timestamp, e1.timestamp));
                        }
                        
                        adapter.notifyDataSetChanged();
                        
                        if (savedExports.isEmpty()) {
                            showNoExports();
                        } else {
                            showExports();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(SavedExportsActivity.this, "Failed to load saved exports", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showExports() {
        savedExportsRecyclerView.setVisibility(View.VISIBLE);
        noExportsLayout.setVisibility(View.GONE);
    }

    private void showNoExports() {
        savedExportsRecyclerView.setVisibility(View.GONE);
        noExportsLayout.setVisibility(View.VISIBLE);
    }
    
    public void onExportDeleted() {
        // Refresh the list when an export is deleted
        loadSavedExports();
    }
}
