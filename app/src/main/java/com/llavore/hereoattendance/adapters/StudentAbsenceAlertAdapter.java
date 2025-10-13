package com.llavore.hereoattendance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.material.button.MaterialButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.StudentAbsenceAlert;

import java.util.List;

public class StudentAbsenceAlertAdapter extends RecyclerView.Adapter<StudentAbsenceAlertAdapter.ViewHolder> {

    private List<StudentAbsenceAlert> absenceAlerts;
    private Context context;
    private OnSendSmsClickListener onSendSmsClickListener;
    private OnOverflowMenuClickListener onOverflowMenuClickListener;

    public interface OnSendSmsClickListener {
        void onSendSmsClick(StudentAbsenceAlert alert, ViewHolder holder);
    }

    public interface OnOverflowMenuClickListener {
        void onOverflowMenuClick(StudentAbsenceAlert alert, View view);
    }

    public StudentAbsenceAlertAdapter(List<StudentAbsenceAlert> absenceAlerts, Context context) {
        this.absenceAlerts = absenceAlerts;
        this.context = context;
    }

    public void setOnSendSmsClickListener(OnSendSmsClickListener listener) {
        this.onSendSmsClickListener = listener;
    }

    public void setOnOverflowMenuClickListener(OnOverflowMenuClickListener listener) {
        this.onOverflowMenuClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_student_absence_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentAbsenceAlert alert = absenceAlerts.get(position);
        
        // Set student name
        holder.studentNameText.setText(alert.getFullName());
        
        // Set total absence count
        holder.absenceCountText.setText("No. of absences: " + alert.getTotalAbsences());
        
        // Clear existing course views
        holder.courseContainer.removeAllViews();
        
        // Add course information for each course with violations
        for (StudentAbsenceAlert.CourseAbsence courseAbsence : alert.getCourseAbsences()) {
            View courseView = LayoutInflater.from(context)
                    .inflate(R.layout.item_course_absence, holder.courseContainer, false);
            
            TextView courseNameText = courseView.findViewById(R.id.courseNameText);
            TextView courseScheduleText = courseView.findViewById(R.id.courseScheduleText);
            
            courseNameText.setText(courseAbsence.getCourseName());
            courseScheduleText.setText(courseAbsence.getCourseSchedule());
            
            holder.courseContainer.addView(courseView);
        }
        
        // Set click listeners
        holder.sendSmsButton.setOnClickListener(v -> {
            if (onSendSmsClickListener != null) {
                onSendSmsClickListener.onSendSmsClick(alert, holder);
            }
        });
        
        holder.overflowMenu.setOnClickListener(v -> {
            if (onOverflowMenuClickListener != null) {
                onOverflowMenuClickListener.onOverflowMenuClick(alert, v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return absenceAlerts.size();
    }

    public void updateData(List<StudentAbsenceAlert> newAlerts) {
        this.absenceAlerts = newAlerts;
        notifyDataSetChanged();
    }
    
    public void setButtonSuccessState(ViewHolder holder) {
        holder.sendSmsButton.setText("SMS Successfully Sent");
        holder.sendSmsButton.setBackgroundTintList(context.getResources().getColorStateList(android.R.color.darker_gray));
        holder.sendSmsButton.setEnabled(false);
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameText;
        TextView absenceCountText;
        LinearLayout courseContainer;
        MaterialButton sendSmsButton;
        ImageView overflowMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameText = itemView.findViewById(R.id.studentNameText);
            absenceCountText = itemView.findViewById(R.id.absenceCountText);
            courseContainer = itemView.findViewById(R.id.courseContainer);
            sendSmsButton = itemView.findViewById(R.id.sendSmsButton);
            overflowMenu = itemView.findViewById(R.id.overflowMenu);
        }
    }
}
