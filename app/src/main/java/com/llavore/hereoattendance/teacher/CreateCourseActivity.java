package com.llavore.hereoattendance.teacher;

import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.Course;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CreateCourseActivity extends AppCompatActivity {

    private ImageView backToActiveCoursesButton;

    private TextInputEditText scheduleTxt;
    private TextInputEditText classStartTxt;
    private TextInputEditText classEndTxt;
    private TextInputEditText lateAttTxt;
    private TextInputEditText courseCodeTxt;
    private MaterialButton btnCreateCourse;

    // Persist multi-select state
    private final String[] fullDayNames = new String[]{
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
    };
    private final boolean[] selectedDays = new boolean[7];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_create_course);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        backToActiveCoursesButton = findViewById(R.id.backCourseArrow);
        backToActiveCoursesButton.setOnClickListener(v -> {
            Intent intent = new Intent(CreateCourseActivity.this, ActiveCoursesActivity.class);
            startActivity(intent);
            finish();
        });

        // Bind fields
        scheduleTxt = findViewById(R.id.scheduleTxt);
        classStartTxt = findViewById(R.id.classStartTxt);
        classEndTxt = findViewById(R.id.classEndTxt);
        lateAttTxt = findViewById(R.id.lateAttTxt);
        courseCodeTxt = findViewById(R.id.courseCodeTxt);
        btnCreateCourse = findViewById(R.id.btnCreateCourse);

        // Disable keyboard for picker-based fields
        disableKeyboard(scheduleTxt);
        disableKeyboard(classStartTxt);
        disableKeyboard(classEndTxt);
        disableKeyboard(lateAttTxt);

        // Auto-generate course code on open
        courseCodeTxt.setText(generateCourseCode());

        // Time pickers for start, end, and late attendance
        classStartTxt.setOnClickListener(v -> showTimePicker(classStartTxt));
        classEndTxt.setOnClickListener(v -> showTimePicker(classEndTxt));
        lateAttTxt.setOnClickListener(v -> showTimePicker(lateAttTxt));

        // Multi-select schedule picker
        scheduleTxt.setOnClickListener(v -> showSchedulePicker());

        btnCreateCourse.setOnClickListener(v -> saveCourse());
    }

    private void disableKeyboard(TextInputEditText editText) {
        editText.setInputType(InputType.TYPE_NULL);
        editText.setFocusable(false);
        editText.setClickable(true);
    }

    private void showTimePicker(TextInputEditText targetField) {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);
        TimePickerDialog dialog = new TimePickerDialog(this, (view, hourOfDay, minuteOfHour) -> {
            String formatted = formatTime(hourOfDay, minuteOfHour);
            targetField.setText(formatted);
        }, hour, minute, false);
        dialog.show();
    }

    private String formatTime(int hourOfDay, int minute) {
        int hour12 = hourOfDay % 12;
        if (hour12 == 0) {
            hour12 = 12;
        }
        String ampm = hourOfDay < 12 ? "AM" : "PM";
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minute, ampm);
    }

    private void showSchedulePicker() {
        List<Integer> initialChecked = new ArrayList<>();
        for (int i = 0; i < selectedDays.length; i++) {
            if (selectedDays[i]) initialChecked.add(i);
        }

        new AlertDialog.Builder(this)
                .setTitle("Select Days")
                .setMultiChoiceItems(fullDayNames, selectedDays, (dialog, which, isChecked) -> selectedDays[which] = isChecked)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("OK", (dialog, which) -> scheduleTxt.setText(formatSelectedDays()))
                .show();
    }

    private String formatSelectedDays() {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < selectedDays.length; i++) {
            if (!selectedDays[i]) continue;
            switch (i) {
                case 0: tokens.add("M"); break;        // Monday
                case 1: tokens.add("T"); break;        // Tuesday
                case 2: tokens.add("W"); break;        // Wednesday
                case 3: tokens.add("Th"); break;       // Thursday
                case 4: tokens.add("F"); break;        // Friday
                case 5: tokens.add("S"); break;        // Saturday
                case 6: tokens.add("SD"); break;       // Sunday
            }
        }
        if (tokens.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) builder.append('-');
            builder.append(tokens.get(i));
        }
        return builder.toString();
    }

    private String generateCourseCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnopqrstuvwxyz23456789";
        SecureRandom random = new SecureRandom();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int idx = random.nextInt(alphabet.length());
            builder.append(alphabet.charAt(idx));
        }
        return builder.toString();
    }

    private void saveCourse() {
        // Validate all required fields
        if (!validateFields()) {
            return;
        }

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (uid == null) {
            Toast.makeText(this, "Not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        Course c = new Course();
        c.name = textOf(findViewById(R.id.txtCourseName));
        c.room = textOf(findViewById(R.id.roomTxt));
        c.scheduleDays = textOf(scheduleTxt);
        c.startTime = textOf(classStartTxt);
        c.endTime = textOf(classEndTxt);
        c.lateAfter = textOf(lateAttTxt);
        c.code = textOf(courseCodeTxt);
        c.studentCount = 0;
        c.sessionCount = 0;

        // Save to the current logged-in user's courses
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users").child("teachers").child(uid).child("courses");
        String key = ref.push().getKey();
        if (key == null) {
            Toast.makeText(this, "Failed to create key", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Set the course ID for reference
        c.id = key;
        
        ref.child(key).setValue(c).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Toast.makeText(this, "Course created successfully!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, ActiveCoursesActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Failed to create course: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private boolean validateFields() {
        String courseName = textOf(findViewById(R.id.txtCourseName));
        String room = textOf(findViewById(R.id.roomTxt));
        String schedule = textOf(scheduleTxt);
        String startTime = textOf(classStartTxt);
        String endTime = textOf(classEndTxt);
        String lateAfter = textOf(lateAttTxt);
        String courseCode = textOf(courseCodeTxt);

        if (courseName.isEmpty()) {
            findViewById(R.id.txtCourseName).requestFocus();
            Toast.makeText(this, "Please enter course name", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (room.isEmpty()) {
            findViewById(R.id.roomTxt).requestFocus();
            Toast.makeText(this, "Please enter room number", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (schedule.isEmpty()) {
            scheduleTxt.requestFocus();
            Toast.makeText(this, "Please select schedule days", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (startTime.isEmpty()) {
            classStartTxt.requestFocus();
            Toast.makeText(this, "Please select class start time", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (endTime.isEmpty()) {
            classEndTxt.requestFocus();
            Toast.makeText(this, "Please select class end time", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (lateAfter.isEmpty()) {
            lateAttTxt.requestFocus();
            Toast.makeText(this, "Please select late attendance time", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (courseCode.isEmpty()) {
            courseCodeTxt.requestFocus();
            Toast.makeText(this, "Please ensure course code is generated", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private String textOf(TextInputEditText e) {
        return e.getText() == null ? "" : e.getText().toString().trim();
    }
}