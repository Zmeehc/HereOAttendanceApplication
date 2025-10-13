package com.llavore.hereoattendance;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.llavore.hereoattendance.utils.TransitionManager;

public class HelpSupportActivity extends AppCompatActivity {

    private ImageView backArrow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_help_support);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.toolbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize views
        backArrow = findViewById(R.id.backArrow);

        // Setup back button
        setupBackButton();
    }

    private void setupBackButton() {
        backArrow.setOnClickListener(v -> {
            // Go back with backward transition
            TransitionManager.finishActivityBackward(this);
        });
    }

    @Override
    public void onBackPressed() {
        // Override back button to use transition
        TransitionManager.finishActivityBackward(this);
    }
}
