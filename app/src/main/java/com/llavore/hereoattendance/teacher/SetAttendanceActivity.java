package com.llavore.hereoattendance.teacher;

import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.button.MaterialButton;
import com.llavore.hereoattendance.R;

import java.util.Calendar;
import java.util.Locale;

public class SetAttendanceActivity extends AppCompatActivity {
    private ImageView btnBackToCourseDetails;

    private TextInputEditText classStartTxt;
    private TextInputEditText classEndTxt;
    private TextInputEditText lateAttTxt;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_set_attendance);

        btnBackToCourseDetails = findViewById(R.id.backToCourseDetails);
        btnBackToCourseDetails.setOnClickListener(v -> {
           Intent intent = new Intent(SetAttendanceActivity.this, CourseDetails.class);
              startActivity(intent);
        });
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Bind fields
        classStartTxt = findViewById(R.id.classStartTxt);
        classEndTxt = findViewById(R.id.classEndTxt);
        lateAttTxt = findViewById(R.id.lateAttTxt);

        // Disable keyboard for picker-based fields
        disableKeyboard(classStartTxt);
        disableKeyboard(classEndTxt);
        disableKeyboard(lateAttTxt);

        // Time pickers for start, end, and late attendance
        classStartTxt.setOnClickListener(v -> showTimePicker(classStartTxt));
        classEndTxt.setOnClickListener(v -> showTimePicker(classEndTxt));
        lateAttTxt.setOnClickListener(v -> showTimePicker(lateAttTxt));
    }

    private void disableKeyboard(TextInputEditText editText) {
        if (editText == null) return;
        editText.setInputType(InputType.TYPE_NULL);
        editText.setFocusable(false);
        editText.setClickable(true);
    }

    private void showTimePicker(TextInputEditText targetField) {
        if (targetField == null) return;
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
}