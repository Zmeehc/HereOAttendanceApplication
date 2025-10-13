package com.llavore.hereoattendance.teacher;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.AboutActivity;
import com.llavore.hereoattendance.PrivacyPolicyActivity;
import com.llavore.hereoattendance.TermsConditionsActivity;
import com.llavore.hereoattendance.HelpSupportActivity;
import com.llavore.hereoattendance.utils.SessionManager;
import com.llavore.hereoattendance.utils.TeacherNavigationManager;
import com.llavore.hereoattendance.utils.TransitionManager;

public class SettingsActivity extends AppCompatActivity {

    private ImageView burgerIcon;
    private DrawerLayout drawerLayout;
    private SessionManager sessionManager;
    private TeacherNavigationManager navigationManager;
    
    // Toggle switches
    private SwitchMaterial notificationSwitch;
    private SwitchMaterial darkModeSwitch;
    
    // Clickable layouts
    private LinearLayout aboutLayout;
    private LinearLayout privacyLayout;
    private LinearLayout termsLayout;
    private LinearLayout helpLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        // Initialize views and setup
        initializeViews();
        
        // Setup navigation using the common manager
        navigationManager = new TeacherNavigationManager(this);
        navigationManager.setupNavigationDrawer(drawerLayout, burgerIcon, findViewById(R.id.navigationView), "settings");
        
        // Setup click listeners
        setupClickListeners();

    }

    private void initializeViews() {
        burgerIcon = findViewById(R.id.burgerIcon);
        drawerLayout = findViewById(R.id.main);
        
        // Initialize toggle switches
        notificationSwitch = findViewById(R.id.notificationSwitch);
        darkModeSwitch = findViewById(R.id.darkModeSwitch);
        
        // Initialize clickable layouts
        aboutLayout = findViewById(R.id.aboutLayout);
        privacyLayout = findViewById(R.id.privacyLayout);
        termsLayout = findViewById(R.id.termsLayout);
        helpLayout = findViewById(R.id.helpLayout);
        
        // Initialize session manager
        sessionManager = new SessionManager(this);
    }

    private void setupClickListeners() {
        // About click listener
        aboutLayout.setOnClickListener(v -> {
            TransitionManager.startActivityForward(this, AboutActivity.class);
        });

        // Privacy Policy click listener
        privacyLayout.setOnClickListener(v -> {
            TransitionManager.startActivityForward(this, PrivacyPolicyActivity.class);
        });

        // Terms and Conditions click listener
        termsLayout.setOnClickListener(v -> {
            TransitionManager.startActivityForward(this, TermsConditionsActivity.class);
        });

        // Help and Support click listener
        helpLayout.setOnClickListener(v -> {
            TransitionManager.startActivityForward(this, HelpSupportActivity.class);
        });
    }





    @Override
    protected void onResume() {
        super.onResume();
        // Update navigation drawer to highlight settings
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationManager.setCurrentActivity(navigationView, "settings");
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}