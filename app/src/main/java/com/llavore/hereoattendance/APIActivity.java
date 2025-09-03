package com.llavore.hereoattendance;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

public class APIActivity extends AppCompatActivity {

    private TextInputEditText enterText;
    private MaterialButton sendButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_apiactivity);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        enterText = findViewById(R.id.textSample);
        sendButton = findViewById(R.id.btnSend);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String message = enterText.getText().toString().trim();
                
                // Validate input
                if (message.isEmpty()) {
                    Toast.makeText(APIActivity.this, "Please enter a message", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // Show sending state
                sendButton.setEnabled(false);
                sendButton.setText("Sending...");
                
                String mobileNumber = "+639663996287";
                sendMessage(message, mobileNumber);
            }
        });
    }

    private void sendMessage(String message, String mobileNumber) {
        // Send SMS using SemaphoreSmsSender
        try {
            SemaphoreSmsSender.sendSMS(mobileNumber, message);
            
            // Show success message
            Toast.makeText(this, "SMS request sent to " + mobileNumber, Toast.LENGTH_SHORT).show();
            
            // Reset button state
            sendButton.setEnabled(true);
            sendButton.setText("Send");
            
            // Clear the input
            enterText.setText("");
            
        } catch (Exception e) {
            // Show error message
            Toast.makeText(this, "Error sending SMS: " + e.getMessage(), Toast.LENGTH_LONG).show();
            
            // Reset button state
            sendButton.setEnabled(true);
            sendButton.setText("Send");
            
            // Log the error
            android.util.Log.e("APIActivity", "Error sending SMS", e);
        }
    }
}