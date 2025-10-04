package com.llavore.hereoattendance.student;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.GravityCompat;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.teacher.MainActivity;
import com.llavore.hereoattendance.utils.SessionManager;

public class StudentDashboard extends AppCompatActivity {

    private ImageView burgerIcon;
    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_home);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        drawerLayout = findViewById(R.id.main);
        burgerIcon = findViewById(R.id.burgerIcon);
        burgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

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
                            Intent intent = new Intent(StudentDashboard.this, MainActivity.class);
                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        })
                        .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                        .show();
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_home) {
                // Stay on Home
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_account) {
                // Placeholder for future student account screen
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_notifications) {
                // Placeholder for future student notifications screen
                drawerLayout.closeDrawers();
                return true;
            } else if (id == R.id.nav_settings) {
                // Placeholder for future student settings
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });
    }
}


