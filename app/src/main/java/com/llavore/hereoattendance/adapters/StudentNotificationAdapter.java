package com.llavore.hereoattendance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.StudentNotification;

import java.util.List;

public class StudentNotificationAdapter extends RecyclerView.Adapter<StudentNotificationAdapter.ViewHolder> {

    private List<StudentNotification> notifications;
    private Context context;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(StudentNotification notification);
        void onNotificationExpand(StudentNotification notification, boolean isExpanded);
    }

    public StudentNotificationAdapter(List<StudentNotification> notifications, Context context) {
        this.notifications = notifications;
        this.context = context;
    }

    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_student_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        StudentNotification notification = notifications.get(position);
        
        // Set course information
        holder.courseInfoText.setText(notification.getCourseInfo());
        holder.notificationDateText.setText(notification.getFormattedDate());
        
        // Set alert title and message
        holder.alertTitleText.setText(notification.getTitle());
        holder.messageText.setText(notification.getMessage());
        holder.truncatedMessageText.setText(notification.getMessage().substring(0, 
                Math.min(notification.getMessage().length(), 50)) + "...");
        
        // Set alert icon based on type
        if (notification.getType() == StudentNotification.NotificationType.WARNING) {
            holder.alertIcon.setImageResource(R.drawable.alert_yellow);
        } else {
            holder.alertIcon.setImageResource(R.drawable.alert_red);
        }
        
        // Set read/unread state
        if (notification.isRead()) {
            // Read state - grayed out appearance
            holder.itemView.setAlpha(0.6f);
            holder.accentBar.setBackgroundColor(context.getResources().getColor(R.color.gray));
            holder.courseInfoText.setTextColor(context.getResources().getColor(R.color.gray));
            holder.alertTitleText.setTextColor(context.getResources().getColor(R.color.gray));
            holder.messageText.setTextColor(context.getResources().getColor(R.color.gray));
            holder.truncatedMessageText.setTextColor(context.getResources().getColor(R.color.gray));
        } else {
            // Unread state - full opacity and colors
            holder.itemView.setAlpha(1.0f);
            holder.accentBar.setBackgroundColor(context.getResources().getColor(R.color.green));
            holder.courseInfoText.setTextColor(context.getResources().getColor(R.color.green));
            holder.alertTitleText.setTextColor(context.getResources().getColor(R.color.black));
            holder.messageText.setTextColor(context.getResources().getColor(R.color.black));
            holder.truncatedMessageText.setTextColor(context.getResources().getColor(R.color.black));
        }
        
        // Load teacher profile image
        if (notification.getTeacherProfileImageUrl() != null && !notification.getTeacherProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(notification.getTeacherProfileImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .placeholder(R.drawable.default_profile)
                    .error(R.drawable.default_profile)
                    .into(holder.teacherProfileImage);
        } else {
            holder.teacherProfileImage.setImageResource(R.drawable.default_profile);
        }
        
        // Set click listener for expand/collapse
        holder.itemView.setOnClickListener(v -> {
            boolean isExpanded = holder.messageContainer.getVisibility() == View.VISIBLE;
            
            // Toggle expanded state
            holder.messageContainer.setVisibility(isExpanded ? View.GONE : View.VISIBLE);
            holder.truncatedMessageText.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
            
            // Notify listener about the state change
            if (listener != null) {
                listener.onNotificationClick(notification);
                listener.onNotificationExpand(notification, !isExpanded);
            }
        });
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    public void updateNotifications(List<StudentNotification> newNotifications) {
        this.notifications = newNotifications;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View accentBar;
        ImageView teacherProfileImage;
        TextView courseInfoText;
        TextView notificationDateText;
        ImageView alertIcon;
        TextView alertTitleText;
        LinearLayout messageContainer;
        TextView messageText;
        TextView truncatedMessageText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            accentBar = itemView.findViewById(R.id.accentBar);
            teacherProfileImage = itemView.findViewById(R.id.teacherProfileImage);
            courseInfoText = itemView.findViewById(R.id.courseInfoText);
            notificationDateText = itemView.findViewById(R.id.notificationDateText);
            alertIcon = itemView.findViewById(R.id.alertIcon);
            alertTitleText = itemView.findViewById(R.id.alertTitleText);
            messageContainer = itemView.findViewById(R.id.messageContainer);
            messageText = itemView.findViewById(R.id.messageText);
            truncatedMessageText = itemView.findViewById(R.id.truncatedMessageText);
        }
    }
}
