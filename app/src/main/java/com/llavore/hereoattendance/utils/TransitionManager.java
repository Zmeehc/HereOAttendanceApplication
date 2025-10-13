package com.llavore.hereoattendance.utils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import com.llavore.hereoattendance.R;

/**
 * Utility class to manage activity transitions with directional animations
 */
public class TransitionManager {
    
    public static final String EXTRA_TRANSITION_DIRECTION = "transition_direction";
    public static final String DIRECTION_FORWARD = "forward";
    public static final String DIRECTION_BACKWARD = "backward";
    
    /**
     * Start activity with forward transition (slide in from right)
     */
    public static void startActivityForward(Activity currentActivity, Class<?> targetActivity) {
        Intent intent = new Intent(currentActivity, targetActivity);
        intent.putExtra(EXTRA_TRANSITION_DIRECTION, DIRECTION_FORWARD);
        currentActivity.startActivity(intent);
        currentActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    /**
     * Start activity with forward transition (slide in from right) with bundle
     */
    public static void startActivityForward(Activity currentActivity, Class<?> targetActivity, Bundle bundle) {
        Intent intent = new Intent(currentActivity, targetActivity);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.putExtra(EXTRA_TRANSITION_DIRECTION, DIRECTION_FORWARD);
        currentActivity.startActivity(intent);
        currentActivity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    /**
     * Start activity with backward transition (slide in from left)
     */
    public static void startActivityBackward(Activity currentActivity, Class<?> targetActivity) {
        Intent intent = new Intent(currentActivity, targetActivity);
        intent.putExtra(EXTRA_TRANSITION_DIRECTION, DIRECTION_BACKWARD);
        currentActivity.startActivity(intent);
        currentActivity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    
    /**
     * Start activity with backward transition (slide in from left) with bundle
     */
    public static void startActivityBackward(Activity currentActivity, Class<?> targetActivity, Bundle bundle) {
        Intent intent = new Intent(currentActivity, targetActivity);
        if (bundle != null) {
            intent.putExtras(bundle);
        }
        intent.putExtra(EXTRA_TRANSITION_DIRECTION, DIRECTION_BACKWARD);
        currentActivity.startActivity(intent);
        currentActivity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    
    /**
     * Finish activity with backward transition (slide out to right)
     */
    public static void finishActivityBackward(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
    }
    
    /**
     * Finish activity with forward transition (slide out to left)
     */
    public static void finishActivityForward(Activity activity) {
        activity.finish();
        activity.overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
    
    /**
     * Check if the current activity was opened with backward transition
     */
    public static boolean wasOpenedBackward(Activity activity) {
        return DIRECTION_BACKWARD.equals(activity.getIntent().getStringExtra(EXTRA_TRANSITION_DIRECTION));
    }
}
