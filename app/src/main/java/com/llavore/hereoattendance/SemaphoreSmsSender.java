package com.llavore.hereoattendance;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SemaphoreSmsSender {

    private static final String TAG = "SemaphoreSmsSender";
    private static final String API_KEY = "8a34c80dad25abdd76c4c7bcc1bf6e98";
    private static final String SENDER_NAME = "FarmBite";
    private static final String ENDPOINT = "https://api.semaphore.co/api/v4/messages";
    
    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public static void sendSMS(String recipientNumber, String message) {
        Log.i(TAG, "Starting SMS send process to: " + recipientNumber);
        Log.i(TAG, "Message: " + message);
        
        executor.execute(() -> {
            try {
                // Validate phone number format
                if (recipientNumber == null || recipientNumber.trim().isEmpty()) {
                    Log.e(TAG, "Invalid phone number: " + recipientNumber);
                    return;
                }
                
                // Ensure phone number starts with +63 for Philippines
                String formattedNumber = recipientNumber;
                if (!formattedNumber.startsWith("+")) {
                    if (formattedNumber.startsWith("09")) {
                        formattedNumber = "+63" + formattedNumber.substring(1);
                    } else if (formattedNumber.startsWith("9")) {
                        formattedNumber = "+63" + formattedNumber;
                    } else {
                        formattedNumber = "+" + formattedNumber;
                    }
                }
                
                Log.i(TAG, "Formatted phone number: " + formattedNumber);
                
                String jsonData = "{"
                        + "\"apikey\": \"" + API_KEY + "\","
                        + "\"number\": \"" + formattedNumber + "\","
                        + "\"message\": \"" + message + "\","
                        + "\"sendername\":\"" + SENDER_NAME + "\""
                        + "}";
                
                Log.d(TAG, "Sending SMS with JSON: " + jsonData);
                Log.i(TAG, "API Key: " + API_KEY);
                Log.i(TAG, "Endpoint: " + ENDPOINT);
                
                URL url = new URL(ENDPOINT);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setRequestProperty("User-Agent", "HereOAttendance/1.0");
                connection.setDoOutput(true);
                connection.setConnectTimeout(30000); // Increased timeout
                connection.setReadTimeout(30000);   // Increased timeout
                
                // Enable logging for debugging
                connection.setUseCaches(false);
                connection.setAllowUserInteraction(false);

                try (OutputStreamWriter writer = new OutputStreamWriter(connection.getOutputStream(), "UTF-8")) {
                    writer.write(jsonData);
                    writer.flush();
                    Log.d(TAG, "Data written to connection");
                }

                int responseCode = connection.getResponseCode();
                Log.i(TAG, "HTTP Response Code: " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read success response
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getInputStream()))) {
                        String line;
                        StringBuilder response = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        Log.i(TAG, "SMS sent successfully! Response: " + response.toString());
                    }
                } else {
                    Log.e(TAG, "Failed to send SMS. Response code: " + responseCode);
                    
                    // Read error response
                    try (java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(connection.getErrorStream()))) {
                        String line;
                        StringBuilder response = new StringBuilder();
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        Log.e(TAG, "Error response: " + response.toString());
                    }
                    
                    // Log connection details for debugging
                    Log.e(TAG, "Connection URL: " + url.toString());
                    Log.e(TAG, "Request Method: " + connection.getRequestMethod());
                    Log.e(TAG, "Content-Type: " + connection.getRequestProperty("Content-Type"));
                }
                
                connection.disconnect();
                
            } catch (Exception e) {
                Log.e(TAG, "Error sending SMS", e);
                Log.e(TAG, "Error details: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

}

