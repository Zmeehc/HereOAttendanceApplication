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
import com.llavore.hereoattendance.models.SmsNotificationHistory;

import java.util.List;

public class StudentAbsenceAlertAdapter extends RecyclerView.Adapter<StudentAbsenceAlertAdapter.ViewHolder> {

    private List<SmsNotificationHistory> notificationHistory;
    private Context context;
    private OnOverflowMenuClickListener onOverflowMenuClickListener;

    public interface OnOverflowMenuClickListener {
        void onOverflowMenuClick(SmsNotificationHistory notification, View view);
    }

    public StudentAbsenceAlertAdapter(List<SmsNotificationHistory> notificationHistory, Context context) {
        this.notificationHistory = notificationHistory;
        this.context = context;
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
        SmsNotificationHistory notification = notificationHistory.get(position);
        
        // Set student name (first + last name only)
        holder.studentNameText.setText(notification.getStudentFullName());
        
        // Set absence count to show "3 absents detected"
        holder.absenceCountText.setText("3 absents detected");
        
        // Clear existing course views
        holder.courseContainer.removeAllViews();
        
        // Add course information
        View courseView = LayoutInflater.from(context)
                .inflate(R.layout.item_course_absence, holder.courseContainer, false);
        
        TextView courseNameText = courseView.findViewById(R.id.courseNameText);
        TextView courseScheduleText = courseView.findViewById(R.id.courseScheduleText);
        
        courseNameText.setText(notification.getCourseName());
        courseScheduleText.setText("Course: " + notification.getCourseCode());
        
        holder.courseContainer.addView(courseView);
        
        // Set SMS status message
        holder.smsStatusText.setText("SMS Alert has been sent to parent");
        
        // Set overflow menu click listener
        holder.overflowMenu.setOnClickListener(v -> {
            if (onOverflowMenuClickListener != null) {
                onOverflowMenuClickListener.onOverflowMenuClick(notification, v);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notificationHistory.size();
    }

    public void updateData(List<SmsNotificationHistory> newNotifications) {
        this.notificationHistory = newNotifications;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView studentNameText;
        TextView absenceCountText;
        LinearLayout courseContainer;
        LinearLayout smsStatusContainer;
        TextView smsStatusText;
        ImageView overflowMenu;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            studentNameText = itemView.findViewById(R.id.studentNameText);
            absenceCountText = itemView.findViewById(R.id.absenceCountText);
            courseContainer = itemView.findViewById(R.id.courseContainer);
            smsStatusContainer = itemView.findViewById(R.id.smsStatusContainer);
            smsStatusText = itemView.findViewById(R.id.smsStatusText);
            overflowMenu = itemView.findViewById(R.id.overflowMenu);
        }
    }
}
