package com.llavore.hereoattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class ArchivesActivity extends AppCompatActivity {

    private ImageView backToDashboardButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_archives);

        backToDashboardButton = findViewById(R.id.backDashArrow);
        backToDashboardButton.setOnClickListener(v -> {
            Intent intent = new Intent(ArchivesActivity.this, TeacherDashboard.class);
            startActivity(intent);
            finish();
        });
    }
}