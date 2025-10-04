package com.llavore.hereoattendance.utils;

import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.model.User;

public class NavigationHeaderManager {
    
    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    
    public NavigationHeaderManager() {
        this.sessionManager = null;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }
    
    public NavigationHeaderManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
    }
    
    public void loadUserData(ImageView profilePicture, TextView userName, TextView userEmail) {
        if (sessionManager == null) {
            setDefaultValues(profilePicture, userName, userEmail);
            return;
        }
        
        String userId = sessionManager.getUserId();
        String userType = sessionManager.getUserType();
        
        if (userId == null || userType == null) {
            setDefaultValues(profilePicture, userName, userEmail);
            return;
        }
        
        // Determine the correct database path based on user type
        String dbPath = userType.equals("teacher") ? "teachers" : "students";
        
        mDatabase.child("users").child(dbPath).child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User user = snapshot.getValue(User.class);
                            if (user != null) {
                                // Set user name (prefer first + last name, fallback to fullName)
                                String firstLast = buildFirstLastName(user);
                                if (firstLast != null && !firstLast.isEmpty()) {
                                    userName.setText(firstLast);
                                } else {
                                    String fullName = user.getFullName();
                                    userName.setText(fullName != null && !fullName.isEmpty() ? fullName : "User");
                                }
                                
                                // Set email
                                String email = user.getEmail();
                                if (email != null && !email.isEmpty()) {
                                    userEmail.setText(email);
                                } else {
                                    userEmail.setText("No email");
                                }
                                
                                // Load profile picture
                                String imageUrl = user.getProfileImageUrl();
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    Glide.with(profilePicture.getContext())
                                            .load(imageUrl)
                                            .placeholder(R.drawable.baseline_person_24)
                                            .error(R.drawable.baseline_person_24)
                                            .circleCrop()
                                            .override(120, 120)
                                            .into(profilePicture);
                                } else {
                                    profilePicture.setImageResource(R.drawable.baseline_person_24);
                                }
                            } else {
                                setDefaultValues(profilePicture, userName, userEmail);
                            }
                        } else {
                            setDefaultValues(profilePicture, userName, userEmail);
                        }
                    }
                    
                    @Override
                    public void onCancelled(DatabaseError error) {
                        setDefaultValues(profilePicture, userName, userEmail);
                    }
                });
    }
    
    private String buildFirstLastName(User user) {
        if (user == null) return null;
        String first = user.getFirstName();
        String last = user.getLastName();
        boolean hasFirst = first != null && !first.isEmpty();
        boolean hasLast = last != null && !last.isEmpty();
        if (!hasFirst && !hasLast) return null;
        if (hasFirst && hasLast) return first + " " + last;
        return hasFirst ? first : last;
    }
    
    private void setDefaultValues(ImageView profilePicture, TextView userName, TextView userEmail) {
        if (profilePicture != null) {
            profilePicture.setImageResource(R.drawable.baseline_person_24);
        }
        if (userName != null) {
            userName.setText("User");
        }
        if (userEmail != null) {
            userEmail.setText("No email");
        }
    }
}
