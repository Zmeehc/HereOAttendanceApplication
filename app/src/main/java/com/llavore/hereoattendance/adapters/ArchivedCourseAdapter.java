package com.llavore.hereoattendance.adapters;

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

import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.Course;
import com.llavore.hereoattendance.teacher.ArchiveCoursesActivity;
import com.llavore.hereoattendance.teacher.AttendanceRecordActivity;
import com.llavore.hereoattendance.teacher.CourseDetails;

import java.util.List;

public class ArchivedCourseAdapter extends RecyclerView.Adapter<ArchivedCourseAdapter.CourseViewHolder> {

    private List<Course> courses;
    private Context context;

    public ArchivedCourseAdapter(List<Course> courses, Context context) {
        this.courses = courses;
        this.context = context;
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
        
        // Set click listener to navigate to course details (archived version)
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
            intent.putExtra("isArchived", true); // Mark as archived
            context.startActivity(intent);
        });
        
        // Set click listener for 3-dots menu (only export functionality)
        holder.menu.setOnClickListener(v -> showArchivedCourseOptionsMenu(v, c, position));
    }
    
    private void setupStudentCountListener(CourseViewHolder holder, Course course) {
        com.google.firebase.database.DatabaseReference mDatabase = com.google.firebase.database.FirebaseDatabase.getInstance().getReference();
        mDatabase.child("courses").child(course.code).child("students")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            int studentCount = (int) snapshot.getChildrenCount();
                            holder.students.setText("Students: " + studentCount);
                        } else {
                            holder.students.setText("Students: 0");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        android.util.Log.e("ArchivedCourseAdapter", "Failed to listen to student count updates for course " + course.name + ": " + error.getMessage());
                    }
                });
    }
    
    private void setupSessionCountListener(CourseViewHolder holder, Course course) {
        com.google.firebase.database.DatabaseReference mDatabase = com.google.firebase.database.FirebaseDatabase.getInstance().getReference();
        mDatabase.child("courses").child(course.code).child("sessions")
                .addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull com.google.firebase.database.DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            int sessionCount = (int) snapshot.getChildrenCount();
                            holder.sessions.setText("Sessions: " + sessionCount);
                        } else {
                            holder.sessions.setText("Sessions: 0");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull com.google.firebase.database.DatabaseError error) {
                        android.util.Log.e("ArchivedCourseAdapter", "Failed to listen to sessions updates for course " + course.name + ": " + error.getMessage());
                    }
                });
    }
    
    private void showArchivedCourseOptionsMenu(View anchorView, Course course, int position) {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.archived_course_options_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_unarchive) {
                showUnarchiveConfirmation(course, position);
                return true;
            } else if (itemId == R.id.menu_export_record) {
                exportAttendanceRecord(course);
                return true;
            } else if (itemId == R.id.menu_delete) {
                showDeleteConfirmation(course, position);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    private void showUnarchiveConfirmation(Course course, int position) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Unarchive Course")
                .setMessage("Are you sure you want to unarchive this course? It will be moved back to active courses.")
                .setPositiveButton("Unarchive", (dialog, which) -> unarchiveCourse(course, position))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void unarchiveCourse(Course course, int position) {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ?
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.google.firebase.database.DatabaseReference mDatabase = com.google.firebase.database.FirebaseDatabase.getInstance().getReference();
        
        // Move course from archived to active
        mDatabase.child("users").child("teachers").child(currentUserId).child("archivedCourses")
                .child(course.id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Add to active courses
                    mDatabase.child("users").child("teachers").child(currentUserId).child("courses")
                            .child(course.id).setValue(course)
                            .addOnSuccessListener(aVoid1 -> {
                                // Update course status in global courses
                                mDatabase.child("courses").child(course.code).child("isArchived").setValue(false);
                                
                                // Remove from local list and notify adapter
                                courses.remove(position);
                                notifyItemRemoved(position);
                                notifyItemRangeChanged(position, courses.size());
                                
                                Toast.makeText(context, "Course unarchived successfully", Toast.LENGTH_SHORT).show();
                                
                                // Notify the activity to refresh archive count
                                if (context instanceof ArchiveCoursesActivity) {
                                    ((ArchiveCoursesActivity) context).onCourseUnarchived();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to unarchive course", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to unarchive course", Toast.LENGTH_SHORT).show();
                });
    }
    
    private void exportAttendanceRecord(Course course) {
        // Navigate to attendance record with archived flag
        android.content.Intent intent = new android.content.Intent(context, AttendanceRecordActivity.class);
        intent.putExtra("courseId", course.id);
        intent.putExtra("courseName", course.name);
        intent.putExtra("courseCode", course.code);
        intent.putExtra("isArchived", true); // Mark as archived
        context.startActivity(intent);
    }
    
    private void showDeleteConfirmation(Course course, int position) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Delete Course")
                .setMessage("Are you sure you want to permanently delete this archived course? This action cannot be undone and will remove all course data including attendance records.")
                .setPositiveButton("Delete", (dialog, which) -> deleteCourse(course, position))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteCourse(Course course, int position) {
        String currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ?
                com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        com.google.firebase.database.DatabaseReference mDatabase = com.google.firebase.database.FirebaseDatabase.getInstance().getReference();
        
        // Remove course from teacher's archived courses
        mDatabase.child("users").child("teachers").child(currentUserId).child("archivedCourses")
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
                                
                                // Notify the activity to refresh archive count
                                if (context instanceof ArchiveCoursesActivity) {
                                    ((ArchiveCoursesActivity) context).onCourseUnarchived();
                                }
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(context, "Failed to delete course from registry", Toast.LENGTH_SHORT).show();
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete course", Toast.LENGTH_SHORT).show();
                });
    }
    
    private String nullSafe(String str) {
        return str != null ? str : "";
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    static class CourseViewHolder extends RecyclerView.ViewHolder {
        TextView title, schedule, students, sessions;
        ImageView menu;

        public CourseViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.courseTitle);
            schedule = itemView.findViewById(R.id.courseSchedule);
            students = itemView.findViewById(R.id.studentsText);
            sessions = itemView.findViewById(R.id.sessionsText);
            menu = itemView.findViewById(R.id.menuIcon);
        }
    }
}
