package com.llavore.hereoattendance.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

/**
 * Utility class to manage login security features including failed attempt tracking
 * for SMS security alerts.
 */
public class LoginSecurityManager {
    
    private static final String TAG = "LoginSecurityManager";
    private static final String PREF_NAME = "LoginSecurity";
    private static final String KEY_FAILED_ATTEMPTS = "failed_attempts_";
    private static final String KEY_LAST_ATTEMPT_TIME = "last_attempt_time_";
    
    private static final int DEFAULT_MAX_FAILED_ATTEMPTS = 3;
    
    private final Context context;
    private final SharedPreferences prefs;
    private final String userIdentifier;
    private final int maxFailedAttempts;
    
    /**
     * Constructor for LoginSecurityManager
     * @param context Application context
     * @param userIdentifier Unique identifier for the user (email, username, etc.)
     * @param maxFailedAttempts Maximum failed attempts before SMS alert (default: 3)
     */
    public LoginSecurityManager(Context context, String userIdentifier, int maxFailedAttempts) {
        this.context = context;
        this.userIdentifier = userIdentifier;
        this.maxFailedAttempts = maxFailedAttempts > 0 ? maxFailedAttempts : DEFAULT_MAX_FAILED_ATTEMPTS;
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Constructor with default security settings
     * @param context Application context
     * @param userIdentifier Unique identifier for the user
     */
    public LoginSecurityManager(Context context, String userIdentifier) {
        this(context, userIdentifier, DEFAULT_MAX_FAILED_ATTEMPTS);
    }
    
    /**
     * Check if SMS alert should be sent (after max failed attempts)
     * @return true if SMS alert should be sent, false otherwise
     */
    public boolean shouldSendSmsAlert() {
        return getFailedAttempts() >= maxFailedAttempts;
    }
    
    /**
     * Record a failed login attempt
     */
    public void recordFailedAttempt() {
        int currentAttempts = getFailedAttempts();
        currentAttempts++;
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(KEY_FAILED_ATTEMPTS + userIdentifier, currentAttempts);
        editor.putLong(KEY_LAST_ATTEMPT_TIME + userIdentifier, System.currentTimeMillis());
        editor.apply();
        
        Log.d(TAG, "Failed attempt recorded for user: " + userIdentifier + ". Total: " + currentAttempts);
    }
    
    /**
     * Record a successful login attempt and reset counters
     */
    public void recordSuccessfulAttempt() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_FAILED_ATTEMPTS + userIdentifier);
        editor.remove(KEY_LAST_ATTEMPT_TIME + userIdentifier);
        editor.apply();
        
        Log.d(TAG, "Login successful for user: " + userIdentifier + ", counters reset");
    }
    
    /**
     * Get the current number of failed attempts
     * @return Number of failed attempts
     */
    public int getFailedAttempts() {
        return prefs.getInt(KEY_FAILED_ATTEMPTS + userIdentifier, 0);
    }
    
    /**
     * Get the number of remaining attempts before SMS alert
     * @return Remaining attempts
     */
    public int getRemainingAttempts() {
        return Math.max(0, maxFailedAttempts - getFailedAttempts());
    }
    
    /**
     * Manually reset failed attempts (for admin purposes)
     */
    public void resetFailedAttempts() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_FAILED_ATTEMPTS + userIdentifier);
        editor.remove(KEY_LAST_ATTEMPT_TIME + userIdentifier);
        editor.apply();
        
        Log.i(TAG, "Failed attempts manually reset for user: " + userIdentifier);
    }
    
    /**
     * Reset all security data for the user
     */
    public void resetAllData() {
        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(KEY_FAILED_ATTEMPTS + userIdentifier);
        editor.remove(KEY_LAST_ATTEMPT_TIME + userIdentifier);
        editor.apply();
        
        Log.i(TAG, "All security data reset for user: " + userIdentifier);
    }
    
    /**
     * Check if the user should be notified about remaining attempts
     * @return true if notification should be shown
     */
    public boolean shouldShowAttemptWarning() {
        int attempts = getFailedAttempts();
        return attempts > 0 && attempts < maxFailedAttempts;
    }
    
    /**
     * Get the last attempt timestamp
     * @return Last attempt timestamp in milliseconds
     */
    public long getLastAttemptTime() {
        return prefs.getLong(KEY_LAST_ATTEMPT_TIME + userIdentifier, 0);
    }
}
