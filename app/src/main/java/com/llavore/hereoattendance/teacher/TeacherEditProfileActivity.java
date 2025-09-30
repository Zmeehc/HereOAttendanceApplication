package com.llavore.hereoattendance.teacher;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.model.User;
import com.llavore.hereoattendance.utils.SessionManager;
import com.bumptech.glide.Glide;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

public class TeacherEditProfileActivity extends AppCompatActivity {

    private ImageView backArrow, profilePicture;
    private TextInputEditText firstNameEdit, middleNameEdit, lastNameEdit, emailEdit, idNumberEdit, contactEdit;
    private TextInputEditText passwordEdit, confirmPasswordEdit;
    private AutoCompleteTextView genderEdit;
    private TextInputLayout birthdateLayout;
    private TextInputEditText birthdateEdit;
    private MaterialButton saveButton;

    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private FirebaseAuth mAuth;

    private String[] genderOptions = {"Male", "Female", "Prefer not to say"};
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;
    private User currentUser;

    // Activity result launchers
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    showImageSourceDialog();
                } else {
                    Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageBitmap = (Bitmap) result.getData().getExtras().get("data");
                    if (selectedImageBitmap != null) {
                        selectedImageBitmap = cropToSquare(selectedImageBitmap);
                        profilePicture.setImageBitmap(selectedImageBitmap);
                        // Clear any existing profile picture URL since we have a new image
                        if (currentUser != null) {
                            currentUser.setProfileImageUrl(null);
                        }
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    try {
                        selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        selectedImageBitmap = cropToSquare(selectedImageBitmap);
                        profilePicture.setImageBitmap(selectedImageBitmap);
                        // Clear any existing profile picture URL since we have a new image
                        if (currentUser != null) {
                            currentUser.setProfileImageUrl(null);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            EdgeToEdge.enable(this);
        } catch (Exception e) {
            // Ignore EdgeToEdge errors
        }
        setContentView(R.layout.activity_teacher_edit_profile);

        try {
            ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, 0, systemBars.right, systemBars.bottom);
                return insets;
            });
        } catch (Exception e) {
            // Ignore ViewCompat errors
        }

        // Initialize Firebase
        try {
            sessionManager = new SessionManager(this);
            mDatabase = FirebaseDatabase.getInstance().getReference();
            mStorage = FirebaseStorage.getInstance().getReference();
            mAuth = FirebaseAuth.getInstance();
        } catch (Exception e) {
            Toast.makeText(this, "Error initializing Firebase: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        // Initialize views
        initializeViews();
        setupClickListeners();
        setupGenderDropdown();
        setupDatePicker();

        // Load current user data
        loadCurrentUserData();
    }

    private void initializeViews() {
        // Fix: Use the correct ID from XML layout
        backArrow = findViewById(R.id.backLoginArrow); // This matches your XML
        profilePicture = findViewById(R.id.profilePicture);
        firstNameEdit = findViewById(R.id.firstNameEdit);
        middleNameEdit = findViewById(R.id.middleNameEdit);
        lastNameEdit = findViewById(R.id.lastNameEdit);
        emailEdit = findViewById(R.id.emailEdit);
        genderEdit = findViewById(R.id.genderEdit);
        birthdateLayout = findViewById(R.id.birthdateLayout);
        birthdateEdit = findViewById(R.id.birthdateEdit);
        idNumberEdit = findViewById(R.id.idNumberEdit);
        contactEdit = findViewById(R.id.contactEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        confirmPasswordEdit = findViewById(R.id.confirmPasswordEdit);
        saveButton = findViewById(R.id.saveButton);

        // Setup contact number formatting
        setupContactNumberFormatting();
    }

    private void setupClickListeners() {
        backArrow.setOnClickListener(v -> {
            finish(); // Go back to previous activity
        });

        profilePicture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                showImageSourceDialog();
            }
        });

        saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveUserData();
            }
        });
    }

    private void setupGenderDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, genderOptions);
        genderEdit.setAdapter(adapter);

        // Set a default value if none is selected
        if (genderEdit.getText().toString().trim().isEmpty()) {
            genderEdit.setText(genderOptions[0], false);
        }

        // Ensure the dropdown opens when clicked
        genderEdit.setOnClickListener(v -> genderEdit.showDropDown());
    }

    private void setupContactNumberFormatting() {
        contactEdit.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(android.text.Editable s) {
                String text = s.toString();

                // Ensure it always starts with +63
                if (!text.startsWith("+63 ")) {
                    // If user deletes the prefix, restore it
                    if (!text.startsWith("+63")) {
                        contactEdit.removeTextChangedListener(this);
                        contactEdit.setText("+63 ");
                        contactEdit.setSelection(4); // Move cursor after "+63 "
                        contactEdit.addTextChangedListener(this);
                    } else if (text.equals("+63")) {
                        contactEdit.removeTextChangedListener(this);
                        contactEdit.setText("+63 ");
                        contactEdit.setSelection(4);
                        contactEdit.addTextChangedListener(this);
                    }
                }

                // Limit the total length (including +63 prefix and space)
                if (text.length() > 15) { // +63 + space + 10 digits max
                    contactEdit.removeTextChangedListener(this);
                    contactEdit.setText(text.substring(0, 15));
                    contactEdit.setSelection(15);
                    contactEdit.addTextChangedListener(this);
                }
            }
        });
    }

    private void setupDatePicker() {
        birthdateEdit.setOnClickListener(v -> showDatePicker());
    }

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        android.app.DatePickerDialog datePickerDialog = new android.app.DatePickerDialog(
                this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String date = String.format(Locale.getDefault(), "%s %d, %d",
                    getMonthName(selectedMonth), selectedDay, selectedYear);
            birthdateEdit.setText(date);
        }, year, month, day);
        datePickerDialog.show();
    }

    private String getMonthName(int month) {
        String[] months = {"January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"};
        return months[month];
    }

    private Bitmap cropToSquare(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int size = Math.min(width, height);

        int x = (width - size) / 2;
        int y = (height - size) / 2;

        Bitmap croppedBitmap = Bitmap.createBitmap(bitmap, x, y, size, size);

        // Resize to a reasonable size for profile picture (e.g., 512x512)
        if (size > 512) {
            croppedBitmap = Bitmap.createScaledBitmap(croppedBitmap, 512, 512, true);
        }

        return croppedBitmap;
    }

    private void showImageSourceDialog() {
        String[] options;
        if (currentUser != null && currentUser.getProfileImageUrl() != null && !currentUser.getProfileImageUrl().isEmpty()) {
            options = new String[]{"Take Photo", "Choose from Gallery", "Remove Current Photo"};
        } else {
            options = new String[]{"Take Photo", "Choose from Gallery"};
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Camera
                        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        cameraLauncher.launch(cameraIntent);
                    } else if (which == 1) {
                        // Gallery
                        galleryLauncher.launch("image/*");
                    } else if (which == 2 && options.length == 3) {
                        // Remove Current Photo
                        removeCurrentProfilePicture();
                    }
                })
                .show();
    }

    private void removeCurrentProfilePicture() {
        selectedImageBitmap = null;
        if (currentUser != null) {
            currentUser.setProfileImageUrl(null);
        }
        profilePicture.setImageResource(R.drawable.baseline_person_24);
        Toast.makeText(this, "Profile picture removed", Toast.LENGTH_SHORT).show();
    }

    private void loadCurrentUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null) return;

        mDatabase.child("users").child("teachers").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        if (dataSnapshot.exists()) {
                            currentUser = dataSnapshot.getValue(User.class);
                            if (currentUser != null) {
                                populateFields(currentUser);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {
                        Toast.makeText(TeacherEditProfileActivity.this,
                                "Error loading user data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void populateFields(User user) {
        firstNameEdit.setText(user.getFirstName());
        middleNameEdit.setText(user.getMiddleName());
        lastNameEdit.setText(user.getLastName());
        emailEdit.setText(user.getEmail());

        // Handle gender field properly
        if (user.getGender() != null && !user.getGender().isEmpty()) {
            genderEdit.setText(user.getGender(), false);
        } else {
            genderEdit.setText(genderOptions[0], false);
        }

        birthdateEdit.setText(user.getBirthdate());
        idNumberEdit.setText(user.getIdNumber());

        // Format contact number for editing (keep it user-friendly)
        String contactNumber = user.getContactNumber();
        if (contactNumber != null && !contactNumber.isEmpty()) {
            String formattedContact = formatContactForEdit(contactNumber);
            contactEdit.setText(formattedContact);
        } else {
            contactEdit.setText("+63 ");
        }

        // Don't populate password fields for security

        // Load existing profile picture if available
        loadExistingProfilePicture(user.getProfileImageUrl());
    }

    private String formatContactForEdit(String contactNumber) {
        if (contactNumber == null || contactNumber.isEmpty()) {
            return "+63 ";
        }

        // Remove all non-digit characters except +
        String cleanNumber = contactNumber.replaceAll("[^+\\d]", "");

        // Remove +63 prefix if present
        if (cleanNumber.startsWith("+63")) {
            cleanNumber = cleanNumber.substring(3);
        } else if (cleanNumber.startsWith("63")) {
            cleanNumber = cleanNumber.substring(2);
        }

        // Remove leading zeros if any
        cleanNumber = cleanNumber.replaceFirst("^0+", "");

        // Return formatted for editing
        if (cleanNumber.length() >= 10) {
            // Take only first 10 digits and format nicely for editing
            cleanNumber = cleanNumber.substring(0, 10);
            return "+63 " + cleanNumber;
        } else if (cleanNumber.length() >= 7) {
            return "+63 " + cleanNumber;
        } else {
            return "+63 " + cleanNumber;
        }
    }

    private void loadExistingProfilePicture(String imageUrl) {
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .circleCrop()
                    .override(200, 200)
                    .into(profilePicture);
        } else {
            profilePicture.setImageResource(R.drawable.baseline_person_24);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh profile picture when returning to edit profile
        // Only load existing picture if no new image is selected
        if (currentUser != null && selectedImageBitmap == null) {
            loadExistingProfilePicture(currentUser.getProfileImageUrl());
        }
    }

    private boolean validateForm() {
        boolean isValid = true;

        if (firstNameEdit.getText().toString().trim().isEmpty()) {
            firstNameEdit.setError("First name is required");
            isValid = false;
        }

        if (lastNameEdit.getText().toString().trim().isEmpty()) {
            lastNameEdit.setError("Last name is required");
            isValid = false;
        }

        if (emailEdit.getText().toString().trim().isEmpty()) {
            emailEdit.setError("Email is required");
            isValid = false;
        }

        if (genderEdit.getText().toString().trim().isEmpty()) {
            genderEdit.setError("Gender is required");
            isValid = false;
        }

        if (birthdateEdit.getText().toString().trim().isEmpty()) {
            birthdateEdit.setError("Birthdate is required");
            isValid = false;
        }

        if (idNumberEdit.getText().toString().trim().isEmpty()) {
            idNumberEdit.setError("ID number is required");
            isValid = false;
        }

        String contactText = contactEdit.getText().toString().trim();
        if (contactText.isEmpty() || contactText.equals("+63 ")) {
            contactEdit.setError("Contact number is required");
            isValid = false;
        } else if (!contactText.startsWith("+63 ")) {
            contactEdit.setError("Contact number must start with +63");
            isValid = false;
        } else if (contactText.length() < 7) { // +63 + at least 1 digit
            contactEdit.setError("Please enter a valid contact number");
            isValid = false;
        }

        // Check if passwords match if both are filled
        String password = passwordEdit.getText().toString().trim();
        String confirmPassword = confirmPasswordEdit.getText().toString().trim();

        if (!password.isEmpty() && !confirmPassword.isEmpty()) {
            if (!password.equals(confirmPassword)) {
                confirmPasswordEdit.setError("Passwords do not match");
                isValid = false;
            }
        }

        return isValid;
    }

    private void saveUserData() {
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User ID not found", Toast.LENGTH_SHORT).show();
            return;
        }

        // Clean and format contact number before saving
        String rawContactNumber = contactEdit.getText().toString().trim();
        String cleanContactNumber = cleanContactNumber(rawContactNumber);

        // Update user data
        User updatedUser = new User();
        updatedUser.setIdNumber(idNumberEdit.getText().toString().trim());
        updatedUser.setFirstName(firstNameEdit.getText().toString().trim());
        updatedUser.setMiddleName(middleNameEdit.getText().toString().trim());
        updatedUser.setLastName(lastNameEdit.getText().toString().trim());
        updatedUser.setEmail(emailEdit.getText().toString().trim());
        updatedUser.setGender(genderEdit.getText().toString().trim());
        updatedUser.setBirthdate(birthdateEdit.getText().toString().trim());
        updatedUser.setContactNumber(cleanContactNumber); // Use cleaned contact number
        updatedUser.setProgram(currentUser != null ? currentUser.getProgram() : "");
        updatedUser.setUserType("teacher");
        updatedUser.setCreatedAt(currentUser != null ? currentUser.getCreatedAt() : System.currentTimeMillis());

        // Preserve existing profile picture URL if no new image is selected
        if (currentUser != null && currentUser.getProfileImageUrl() != null && selectedImageBitmap == null) {
            updatedUser.setProfileImageUrl(currentUser.getProfileImageUrl());
        }

        // Save to database
        mDatabase.child("users").child("teachers").child(userId).setValue(updatedUser)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // If there's a new image, upload it
                        if (selectedImageBitmap != null) {
                            uploadProfileImage(userId);
                        } else {
                            // No image to upload, just update password if needed
                            updatePasswordIfNeeded();
                        }
                    } else {
                        saveButton.setEnabled(true);
                        saveButton.setText("Save");
                        Toast.makeText(this, "Failed to save data", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String cleanContactNumber(String contactNumber) {
        if (contactNumber == null || contactNumber.isEmpty() || contactNumber.equals("+63 ")) {
            return "";
        }

        // Remove all non-digit characters except +
        String cleanNumber = contactNumber.replaceAll("[^+\\d]", "");

        // Ensure it starts with +63
        if (!cleanNumber.startsWith("+63")) {
            if (cleanNumber.startsWith("63")) {
                cleanNumber = "+" + cleanNumber;
            } else if (cleanNumber.startsWith("9") && cleanNumber.length() >= 10) {
                cleanNumber = "+63" + cleanNumber;
            } else {
                cleanNumber = "+63" + cleanNumber;
            }
        }

        return cleanNumber;
    }

    private void uploadProfileImage(String userId) {
        if (selectedImageBitmap == null) return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();

        String imageFileName = "profile_" + userId + "_" + System.currentTimeMillis() + ".jpg";
        StorageReference imageRef = mStorage.child("ProfilePictures").child(imageFileName);

        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Get download URL and save to database
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    // Save image URL to user profile
                    mDatabase.child("users").child("teachers").child(userId)
                            .child("profileImageUrl").setValue(uri.toString())
                            .addOnCompleteListener(dbTask -> {
                                updatePasswordIfNeeded();
                            });
                });
            } else {
                updatePasswordIfNeeded();
            }
        });
    }

    private void updatePasswordIfNeeded() {
        String newPassword = passwordEdit.getText().toString().trim();
        if (!newPassword.isEmpty()) {
            mAuth.getCurrentUser().updatePassword(newPassword)
                    .addOnCompleteListener(task -> {
                        saveButton.setEnabled(true);
                        saveButton.setText("Save");
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK); // Signal that profile was updated
                            finish();
                        } else {
                            Toast.makeText(this, "Profile updated but password change failed", Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK); // Signal that profile was updated
                            finish();
                        }
                    });
        } else {
            saveButton.setEnabled(true);
            saveButton.setText("Save");
            Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK); // Signal that profile was updated
            finish();
        }
    }
}