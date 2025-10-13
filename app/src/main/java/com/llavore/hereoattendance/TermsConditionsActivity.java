package com.llavore.hereoattendance;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.llavore.hereoattendance.utils.TransitionManager;
import com.llavore.hereoattendance.utils.TermsScrollView;

public class TermsConditionsActivity extends AppCompatActivity {

    private ImageView backArrow;
    private MaterialButton agreeButton;
    private TermsScrollView termsScrollView;
    private boolean hasScrolledToBottom = false;
    private boolean hasReadMinimumTime = false;
    private long startTime;
    private static final long MINIMUM_READ_TIME = 30000; // 30 seconds minimum reading time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_terms_conditions);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        backArrow = findViewById(R.id.backArrow);
        agreeButton = findViewById(R.id.agreeButton);
        termsScrollView = findViewById(R.id.termsScrollView);

        // Record start time for minimum reading requirement
        startTime = System.currentTimeMillis();

        // Setup back button
        setupBackButton();

        // Setup agree button
        setupAgreeButton();

        // Setup scroll listener
        setupScrollListener();

        // Start minimum reading time timer
        startMinimumReadingTimer();
    }

    private void setupBackButton() {
        backArrow.setOnClickListener(v -> {
            // Go back with backward transition
            TransitionManager.finishActivityBackward(this);
        });
    }

    private void setupAgreeButton() {
        agreeButton.setOnClickListener(v -> {
            if (hasScrolledToBottom && hasReadMinimumTime) {
                // User has met all requirements
                Intent resultIntent = new Intent();
                resultIntent.putExtra("terms_accepted", true);
                setResult(RESULT_OK, resultIntent);
                TransitionManager.finishActivityBackward(this);
            } else {
                // Show message about requirements
                String message = "Please ";
                if (!hasScrolledToBottom && !hasReadMinimumTime) {
                    message += "scroll to the bottom and read for at least 30 seconds";
                } else if (!hasScrolledToBottom) {
                    message += "scroll to the bottom";
                } else {
                    message += "read for at least 30 seconds";
                }
                message += " before agreeing to the terms.";
                
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupScrollListener() {
        termsScrollView.setOnScrollToBottomListener(() -> {
            onScrolledToBottom();
        });
    }

    private void startMinimumReadingTimer() {
        Handler handler = new Handler();
        handler.postDelayed(() -> {
            hasReadMinimumTime = true;
            checkIfCanEnableButton();
        }, MINIMUM_READ_TIME);
    }

    @Override
    public void onBackPressed() {
        // Override back button to use transition
        TransitionManager.finishActivityBackward(this);
    }

    // This method will be called when the user scrolls to the bottom
    public void onScrolledToBottom() {
        hasScrolledToBottom = true;
        checkIfCanEnableButton();
    }

    private void checkIfCanEnableButton() {
        if (hasScrolledToBottom && hasReadMinimumTime) {
            agreeButton.setEnabled(true);
            agreeButton.setAlpha(1.0f);
            agreeButton.setText("I Agree to Terms and Conditions");
        } else {
            agreeButton.setEnabled(false);
            agreeButton.setAlpha(0.6f);
            
            // Update button text based on what's missing
            if (!hasScrolledToBottom && !hasReadMinimumTime) {
                agreeButton.setText("Please read and scroll to bottom");
            } else if (!hasScrolledToBottom) {
                agreeButton.setText("Please scroll to the bottom");
            } else {
                long remainingTime = MINIMUM_READ_TIME - (System.currentTimeMillis() - startTime);
                int remainingSeconds = (int) (remainingTime / 1000);
                if (remainingSeconds > 0) {
                    agreeButton.setText("Please read for " + remainingSeconds + " more seconds");
                }
            }
        }
    }

    // Method to be called from the ScrollView when user reaches the bottom
    public void markAsScrolledToBottom() {
        onScrolledToBottom();
    }
}
