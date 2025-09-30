package com.llavore.hereoattendance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.Course;
import com.llavore.hereoattendance.teacher.CourseDetails;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private final Context context;
    private final List<Course> courses;

    public CourseAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses;
    }

    @NonNull
    @Override
    public CourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.course_card, parent, false);
        return new CourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CourseViewHolder holder, int position) {
        Course c = courses.get(position);
        holder.title.setText(String.format("%s | %s", nullSafe(c.name), nullSafe(c.room)));
        holder.schedule.setText(String.format("%s - %s | %s", nullSafe(c.startTime), nullSafe(c.endTime), nullSafe(c.scheduleDays)));
        holder.students.setText("Students: " + c.studentCount);
        holder.sessions.setText("Sessions: " + c.sessionCount);
        
        // Set click listener to navigate to course details
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, CourseDetails.class);
            intent.putExtra("courseId", c.id);
            intent.putExtra("courseName", c.name);
            intent.putExtra("courseRoom", c.room);
            intent.putExtra("courseSchedule", c.scheduleDays);
            intent.putExtra("courseStartTime", c.startTime);
            intent.putExtra("courseEndTime", c.endTime);
            intent.putExtra("courseLateAfter", c.lateAfter);
            intent.putExtra("courseCode", c.code);
            intent.putExtra("courseStudentCount", c.studentCount);
            intent.putExtra("courseSessionCount", c.sessionCount);
            context.startActivity(intent);
        });


    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView title, schedule, students, sessions;
        ImageView menu;
        CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.courseTitle);
            schedule = itemView.findViewById(R.id.courseSchedule);
            students = itemView.findViewById(R.id.studentsText);
            sessions = itemView.findViewById(R.id.sessionsText);
            menu = itemView.findViewById(R.id.menuIcon);
        }
    }

    private static String nullSafe(String s) { return s == null ? "" : s; }


}

