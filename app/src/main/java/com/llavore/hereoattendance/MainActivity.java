package com.llavore.hereoattendance;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.llavore.hereoattendance.utils.SessionManager;

public class MainActivity extends AppCompatActivity {
    private CardView teacherButton;
    private SessionManager sessionManager;


    @SuppressLint("WrongViewCast")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Initialize session manager
        sessionManager = new SessionManager(this);
        
        // Check if user is already logged in
        if (sessionManager.isLoggedIn()) {
            String userType = sessionManager.getUserType();
            if ("teacher".equals(userType)) {
                // User is logged in as teacher, go directly to dashboard
                Intent intent = new Intent(MainActivity.this, TeacherDashboard.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                finish();
                return;
            }
        }
        
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        teacherButton = findViewById(R.id.teacherBtn);
        teacherButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TeacherLoginActivity.class);
            startActivity(intent);
        });
    }
}