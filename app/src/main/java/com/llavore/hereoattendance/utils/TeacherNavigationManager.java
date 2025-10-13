package com.llavore.hereoattendance.utils;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

import com.google.android.material.navigation.NavigationView;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.teacher.TeacherDashboard;
import com.llavore.hereoattendance.teacher.TeacherProfileActivity;
import com.llavore.hereoattendance.teacher.SettingsActivity;
import com.llavore.hereoattendance.teacher.TeacherLoginActivity;
import com.llavore.hereoattendance.utils.TransitionManager;

public class TeacherNavigationManager {
    
    private Context context;
    private SessionManager sessionManager;
    private NavigationHeaderManager headerManager;
    
    public TeacherNavigationManager(Context context) {
        this.context = context;
        this.sessionManager = new SessionManager(context);
        this.headerManager = new NavigationHeaderManager(sessionManager);
    }
    
    public void setupNavigationDrawer(DrawerLayout drawerLayout, ImageView burgerIcon, 
                                    NavigationView navigationView, String currentActivity) {
        
        // Setup burger icon click listener
        burgerIcon.setOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));
        
        // Set the current item as checked
        setCurrentActivity(navigationView, currentActivity);
        
        // Setup navigation item selection listener
        navigationView.setNavigationItemSelectedListener(item -> {
            if (item.getItemId() == R.id.nav_logout) {
                showLogoutDialog();
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_dashboard) {
                if (!currentActivity.equals("dashboard")) {
                    TransitionManager.startActivityBackward((android.app.Activity) context, TeacherDashboard.class);
                }
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_account) {
                if (!currentActivity.equals("account")) {
                    TransitionManager.startActivityForward((android.app.Activity) context, TeacherProfileActivity.class);
                }
                drawerLayout.closeDrawers();
                return true;
            } else if (item.getItemId() == R.id.nav_settings) {
                if (!currentActivity.equals("settings")) {
                    TransitionManager.startActivityForward((android.app.Activity) context, SettingsActivity.class);
                }
                drawerLayout.closeDrawers();
                return true;
            }
            return false;
        });
        
        // Load user data into navigation header
        loadNavigationHeader(navigationView);
    }
    
    public void setCurrentActivity(NavigationView navigationView, String currentActivity) {
        // Set the current item as checked
        switch (currentActivity) {
            case "dashboard":
                navigationView.setCheckedItem(R.id.nav_dashboard);
                break;
            case "account":
                navigationView.setCheckedItem(R.id.nav_account);
                break;
            case "settings":
                navigationView.setCheckedItem(R.id.nav_settings);
                break;
            case "notifications":
                // Notifications is no longer in the menu, so don't set any item as checked
                break;
        }
    }
    
    private void loadNavigationHeader(NavigationView navigationView) {
        View headerView = navigationView.getHeaderView(0);
        ImageView profilePicture = headerView.findViewById(R.id.navProfilePicture);
        TextView userName = headerView.findViewById(R.id.navUserName);
        TextView userEmail = headerView.findViewById(R.id.navUserEmail);
        
        if (profilePicture != null && userName != null && userEmail != null) {
            headerManager.loadUserData(profilePicture, userName, userEmail);
        }
    }
    
    private void showLogoutDialog() {
        new AlertDialog.Builder(context)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(context, TeacherLoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    context.startActivity(intent);
                    if (context instanceof android.app.Activity) {
                        TransitionManager.finishActivityBackward((android.app.Activity) context);
                    }
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
