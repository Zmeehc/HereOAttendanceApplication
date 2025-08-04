package com.llavore.hereoattendance.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SessionManager {
    private static final String PREF_NAME = "HereOAttendanceSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_USER_TYPE = "userType";
    
    private SharedPreferences pref;
    private SharedPreferences.Editor editor;
    private Context context;
    private FirebaseAuth mAuth;
    
    public SessionManager(Context context) {
        this.context = context;
        pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = pref.edit();
        mAuth = FirebaseAuth.getInstance();
    }
    
    public void setLogin(boolean isLoggedIn, String userId, String userType) {
        editor.putBoolean(KEY_IS_LOGGED_IN, isLoggedIn);
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_TYPE, userType);
        editor.commit();
    }
    
    public boolean isLoggedIn() {
        // Check both SharedPreferences and Firebase Auth
        boolean prefLoggedIn = pref.getBoolean(KEY_IS_LOGGED_IN, false);
        FirebaseUser currentUser = mAuth.getCurrentUser();
        return prefLoggedIn && currentUser != null;
    }
    
    public String getUserId() {
        return pref.getString(KEY_USER_ID, null);
    }
    
    public String getUserType() {
        return pref.getString(KEY_USER_TYPE, null);
    }
    
    public void logout() {
        editor.clear();
        editor.commit();
        mAuth.signOut();
    }
    
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }
} 