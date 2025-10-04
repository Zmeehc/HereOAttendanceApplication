package com.llavore.hereoattendance.student;

import android.Manifest;
import android.app.DatePickerDialog;
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

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.bumptech.glide.Glide;
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;

public class StudentEditProfileActivity extends AppCompatActivity {

    private ImageView backLoginArrow, profilePicture;
    private TextInputEditText edpEdit, firstNameEdit, middleNameEdit, lastNameEdit, emailEdit,
            birthdateEdit, programEdit, yearLevelEdit, contactEdit, guardianNameEdit, guardianContactEdit,
            passwordEdit, confirmPasswordEdit;
    private AutoCompleteTextView genderEdit;
    private TextInputLayout birthdateLayout;
    private MaterialButton saveButton;

    private SessionManager sessionManager;
    private DatabaseReference mDatabase;
    private StorageReference mStorage;
    private FirebaseAuth mAuth;

    private final String[] genderOptions = {"Male", "Female", "Prefer not to say"};
    private Uri selectedImageUri;
    private Bitmap selectedImageBitmap;
    private User currentUser;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) showImageSourceDialog();
                else Toast.makeText(this, "Camera permission is required", Toast.LENGTH_SHORT).show();
            });

    private final ActivityResultLauncher<Intent> cameraLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageBitmap = (Bitmap) result.getData().getExtras().get("data");
                    if (selectedImageBitmap != null) {
                        profilePicture.setImageBitmap(selectedImageBitmap);
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    try {
                        selectedImageBitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                        profilePicture.setImageBitmap(selectedImageBitmap);
                    } catch (IOException e) {
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_edit_profile);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        mDatabase = FirebaseDatabase.getInstance().getReference();
        mStorage = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        bindViews();
        setupGenderDropdown();
        setupClickListeners();
        loadCurrentUserData();
    }

    private void bindViews() {
        backLoginArrow = findViewById(R.id.backLoginArrow);
        profilePicture = findViewById(R.id.profilePicture);
        edpEdit = findViewById(R.id.edpEdit);
        firstNameEdit = findViewById(R.id.firstNameEdit);
        middleNameEdit = findViewById(R.id.middleNameEdit);
        lastNameEdit = findViewById(R.id.lastNameEdit);
        emailEdit = findViewById(R.id.emailEdit);
        genderEdit = findViewById(R.id.genderEdit);
        birthdateLayout = findViewById(R.id.birthdateLayout);
        birthdateEdit = findViewById(R.id.birthdateEdit);
        programEdit = findViewById(R.id.programEdit);
        yearLevelEdit = findViewById(R.id.yearLevelEdit);
        contactEdit = findViewById(R.id.contactEdit);
        guardianNameEdit = findViewById(R.id.guardianNameEdit);
        guardianContactEdit = findViewById(R.id.guardianContactEdit);
        passwordEdit = findViewById(R.id.passwordEdit);
        confirmPasswordEdit = findViewById(R.id.confirmPasswordEdit);
        saveButton = findViewById(R.id.saveButton);
    }

    private void setupGenderDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, genderOptions);
        genderEdit.setAdapter(adapter);
        genderEdit.setOnClickListener(v -> genderEdit.showDropDown());
    }

    private void setupClickListeners() {
        backLoginArrow.setOnClickListener(v -> finish());
        profilePicture.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            } else {
                showImageSourceDialog();
            }
        });

        birthdateLayout.setEndIconOnClickListener(v -> showDatePicker());
        birthdateEdit.setOnClickListener(v -> showDatePicker());

        saveButton.setOnClickListener(v -> {
            if (validateForm()) {
                saveUserData();
            }
        });
    }

    private void showDatePicker() {
        Calendar c = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this, (view, y, m, d) -> {
            String formatted = String.format("%04d-%02d-%02d", y, m + 1, d);
            birthdateEdit.setText(formatted);
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void showImageSourceDialog() {
        String[] options = new String[]{"Take Photo", "Choose from Gallery"};
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Select Image Source")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        cameraLauncher.launch(new Intent(MediaStore.ACTION_IMAGE_CAPTURE));
                    } else {
                        galleryLauncher.launch("image/*");
                    }
                })
                .show();
    }

    private void loadCurrentUserData() {
        String userId = sessionManager.getUserId();
        if (userId == null) {
            Toast.makeText(this, "User ID not found. Please login again.", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // Loading student data for editing...

        mDatabase.child("users").child("students").child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            Toast.makeText(StudentEditProfileActivity.this, "Student data not found in database", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }
                        
                        User user = snapshot.getValue(User.class);
                        if (user == null) {
                            Toast.makeText(StudentEditProfileActivity.this, "Error parsing user data", Toast.LENGTH_LONG).show();
                            finish();
                            return;
                        }

                        // Load student-specific fields
                        edpEdit.setText(snapshot.child("edpNumber").getValue(String.class));
                        firstNameEdit.setText(user.getFirstName());
                        middleNameEdit.setText(user.getMiddleName());
                        lastNameEdit.setText(user.getLastName());
                        emailEdit.setText(user.getEmail());
                        genderEdit.setText(user.getGender(), false);
                        birthdateEdit.setText(user.getBirthdate());
                        programEdit.setText(user.getProgram());
                        yearLevelEdit.setText(snapshot.child("yearLevel").getValue(String.class));
                        
                        // Format contact numbers for editing
                        String contactNumber = user.getContactNumber();
                        if (contactNumber != null && !contactNumber.isEmpty()) {
                            contactEdit.setText(formatContactForEdit(contactNumber));
                        } else {
                            contactEdit.setText("+63 ");
                        }
                        
                        String guardianContact = snapshot.child("guardianContactNumber").getValue(String.class);
                        if (guardianContact != null && !guardianContact.isEmpty()) {
                            guardianContactEdit.setText(formatContactForEdit(guardianContact));
                        } else {
                            guardianContactEdit.setText("+63 ");
                        }
                        
                        guardianNameEdit.setText(snapshot.child("guardianName").getValue(String.class));

                        // Load existing profile picture
                        String imageUrl = user.getProfileImageUrl();
                        if (imageUrl != null && !imageUrl.isEmpty()) {
                            Glide.with(StudentEditProfileActivity.this)
                                    .load(imageUrl)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .circleCrop()
                                    .override(200, 200)
                                    .into(profilePicture);
                        } else {
                            profilePicture.setImageResource(R.drawable.baseline_person_24);
                        }
                        
                        // Student data loaded successfully for editing
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(StudentEditProfileActivity.this, "Error loading data: " + error.getMessage(), Toast.LENGTH_LONG).show();
                        finish();
                    }
                });
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

    private boolean validateForm() {
        if (firstNameEdit.getText().toString().trim().isEmpty()) { firstNameEdit.setError("Required"); return false; }
        if (lastNameEdit.getText().toString().trim().isEmpty()) { lastNameEdit.setError("Required"); return false; }
        if (emailEdit.getText().toString().trim().isEmpty()) { emailEdit.setError("Required"); return false; }
        if (genderEdit.getText().toString().trim().isEmpty()) { genderEdit.setError("Required"); return false; }
        if (birthdateEdit.getText().toString().trim().isEmpty()) { birthdateEdit.setError("Required"); return false; }
        if (edpEdit.getText().toString().trim().isEmpty()) { edpEdit.setError("Required"); return false; }
        return true;
    }

    private void saveUserData() {
        saveButton.setEnabled(false);
        saveButton.setText("Saving...");

        String userId = sessionManager.getUserId();
        if (userId == null) { finish(); return; }

        // Update base user data
        User updated = new User();
        updated.setFirstName(firstNameEdit.getText().toString().trim());
        updated.setMiddleName(middleNameEdit.getText().toString().trim());
        updated.setLastName(lastNameEdit.getText().toString().trim());
        updated.setEmail(emailEdit.getText().toString().trim());
        updated.setGender(genderEdit.getText().toString().trim());
        updated.setBirthdate(birthdateEdit.getText().toString().trim());
        updated.setContactNumber(contactEdit.getText().toString().trim());
        updated.setProgram(programEdit.getText().toString().trim());
        updated.setUserType("student");

        DatabaseReference ref = mDatabase.child("users").child("students").child(userId);
        ref.setValue(updated).addOnCompleteListener(t -> {
            if (t.isSuccessful()) {
                // Save student-specific fields
                ref.child("edpNumber").setValue(edpEdit.getText().toString().trim());
                ref.child("yearLevel").setValue(yearLevelEdit.getText().toString().trim());
                ref.child("guardianName").setValue(guardianNameEdit.getText().toString().trim());
                ref.child("guardianContactNumber").setValue(guardianContactEdit.getText().toString().trim());

                if (selectedImageBitmap != null) {
                    uploadProfileImage(userId, ref);
                } else {
                    updatePasswordIfNeeded();
                }
            } else {
                saveButton.setEnabled(true);
                saveButton.setText("Save");
                Toast.makeText(this, "Failed to save", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadProfileImage(String userId, DatabaseReference ref) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        selectedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
        byte[] data = baos.toByteArray();
        StorageReference imageRef = mStorage.child("ProfilePictures").child("student_" + userId + ".jpg");
        UploadTask uploadTask = imageRef.putBytes(data);
        uploadTask.addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    ref.child("profileImageUrl").setValue(uri.toString()).addOnCompleteListener(v -> updatePasswordIfNeeded());
                });
            } else {
                updatePasswordIfNeeded();
            }
        });
    }

    private void updatePasswordIfNeeded() {
        String newPass = passwordEdit.getText().toString().trim();
        String confirm = confirmPasswordEdit.getText().toString().trim();
        if (!newPass.isEmpty()) {
            if (!newPass.equals(confirm)) {
                confirmPasswordEdit.setError("Passwords do not match");
                saveButton.setEnabled(true);
                saveButton.setText("Save");
                return;
            }
            mAuth.getCurrentUser().updatePassword(newPass).addOnCompleteListener(done -> {
                saveButton.setEnabled(true);
                saveButton.setText("Save");
                Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
                setResult(RESULT_OK); // Signal that profile was updated
                finish();
            });
        } else {
            saveButton.setEnabled(true);
            saveButton.setText("Save");
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK); // Signal that profile was updated
            finish();
        }
    }
}


