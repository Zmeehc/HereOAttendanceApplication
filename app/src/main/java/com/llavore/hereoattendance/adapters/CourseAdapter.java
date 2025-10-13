package com.llavore.hereoattendance.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.Course;
import com.llavore.hereoattendance.teacher.CourseDetails;

import java.util.List;

public class CourseAdapter extends RecyclerView.Adapter<CourseAdapter.CourseViewHolder> {
    private final Context context;
    private final List<Course> courses;
    private final DatabaseReference mDatabase;

    public CourseAdapter(Context context, List<Course> courses) {
        this.context = context;
        this.courses = courses;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
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
        holder.sessions.setText("Sessions: " + c.sessionCount);
        
        // Set up real-time student count listener
        setupStudentCountListener(holder, c);
        setupSessionCountListener(holder, c);
        
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
        
        // Set click listener for 3-dots menu
        holder.menu.setOnClickListener(v -> showCourseOptionsMenu(v, c, position));
    }
    
    private void setupStudentCountListener(CourseViewHolder holder, Course course) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null || course.id == null || course.code == null) return;
        
        // Listen for real-time updates to student count from global courses registry
        mDatabase.child("courses").child(course.code).child("studentCount").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                Integer studentCount = snapshot.getValue(Integer.class);
                if (studentCount != null) {
                    holder.students.setText("Students: " + studentCount);
                    android.util.Log.d("CourseAdapter", "Updated student count for course " + course.name + " to: " + studentCount);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("CourseAdapter", "Failed to listen to student count updates: " + error.getMessage());
            }
        });
    }
    
    private void setupSessionCountListener(CourseViewHolder holder, Course course) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null || course.id == null || course.code == null) return;
        
        // Listen for real-time updates to the actual sessions node to count sessions dynamically
        mDatabase.child("courses").child(course.code).child("sessions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Count the actual number of sessions
                int actualSessionCount = (int) snapshot.getChildrenCount();
                holder.sessions.setText("Sessions: " + actualSessionCount);
                android.util.Log.d("CourseAdapter", "Updated session count for course " + course.name + " to: " + actualSessionCount + " (from actual sessions)");
                
                // Also update the sessionCount field in the global registry to keep it in sync
                mDatabase.child("courses").child(course.code).child("sessionCount").setValue(actualSessionCount)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("CourseAdapter", "Updated global sessionCount field for course " + course.name + " to: " + actualSessionCount);
                            } else {
                                android.util.Log.e("CourseAdapter", "Failed to update global sessionCount field for course " + course.name + ": " + task.getException());
                            }
                        });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("CourseAdapter", "Failed to listen to sessions updates for course " + course.name + ": " + error.getMessage());
            }
        });
    }
    
    private void showCourseOptionsMenu(View anchorView, Course course, int position) {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.course_options_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_archive) {
                showArchiveConfirmation(course, position);
                return true;
            } else if (itemId == R.id.menu_delete) {
                showDeleteConfirmation(course, position);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    private void showArchiveConfirmation(Course course, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Archive Course")
                .setMessage("Are you sure you want to archive this course? Archived courses can be viewed but attendance features will be disabled.")
                .setPositiveButton("Archive", (dialog, which) -> archiveCourse(course, position))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void archiveCourse(Course course, int position) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Move course from active to archived
        mDatabase.child("users").child("teachers").child(currentUserId).child("courses")
                .child(course.id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Add to archived courses
                    mDatabase.child("users").child("teachers").child(currentUserId).child("archivedCourses")
                            .child(course.id).setValue(course)
                            .addOnSuccessListener(aVoid1 -> {
                                // Update course status in global courses
                                mDatabase.child("courses").child(course.code).child("isArchived").setValue(true);
                                
                                // Remove from local list and notify adapter
                                courses.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, courses.size());
                                
                                Toast.makeText(context, "Course archived successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to archive course", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to archive course", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void showDeleteConfirmation(Course course, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to permanently delete this course? This action cannot be undone and will remove all course data including attendance records.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse(course, position))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteCourse(Course course, int position) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Remove course from teacher's active courses
        mDatabase.child("users").child("teachers").child(currentUserId).child("courses")
                .child(course.id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Remove course from global courses registry
                    mDatabase.child("courses").child(course.code).removeValue()
                            .addOnSuccessListener(aVoid1 -> {
                                // Remove from local list and notify adapter
                                courses.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, courses.size());
                                
                                Toast.makeText(context, "Course deleted successfully", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to delete course from registry", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete course", Toast.LENGTH_SHORT).show();
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

