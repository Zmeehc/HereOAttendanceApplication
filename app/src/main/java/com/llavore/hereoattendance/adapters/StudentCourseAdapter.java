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

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.Course;
import com.llavore.hereoattendance.model.User;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class StudentCourseAdapter extends RecyclerView.Adapter<StudentCourseAdapter.StudentCourseViewHolder> {

    private Context context;
    private List<Course> courses;
    private DatabaseReference mDatabase;
    private UnenrollCallback unenrollCallback;

    public interface UnenrollCallback {
        void onUnenroll(Course course, int position);
    }

    public StudentCourseAdapter(Context context, List<Course> courses, UnenrollCallback callback) {
        this.context = context;
        this.courses = courses;
        this.mDatabase = FirebaseDatabase.getInstance().getReference();
        this.unenrollCallback = callback;
    }

    @NonNull
    @Override
    public StudentCourseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.course_for_student_card, parent, false);
        return new StudentCourseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull StudentCourseViewHolder holder, int position) {
        Course course = courses.get(position);

        android.util.Log.d("StudentCourseAdapter", "Binding course: " + course.name);
        android.util.Log.d("StudentCourseAdapter", "  - Teacher ID: " + course.teacherId);
        android.util.Log.d("StudentCourseAdapter", "  - Teacher First Name: '" + course.teacherFirstName + "'");
        android.util.Log.d("StudentCourseAdapter", "  - Teacher Last Name: '" + course.teacherLastName + "'");
        android.util.Log.d("StudentCourseAdapter", "  - Teacher Profile URL: '" + course.teacherProfileImageUrl + "'");

        // Set course title (Name | Room)
        holder.courseTitle.setText(String.format("%s | %s",
                nullSafe(course.name), nullSafe(course.room)));

        // Set schedule (Start - End | Days)
        holder.courseSchedule.setText(String.format("%s - %s | %s",
                nullSafe(course.startTime), nullSafe(course.endTime), nullSafe(course.scheduleDays)));

        // Set sessions count
        holder.sessionsText.setText(String.format("Sessions: %d", course.sessionCount));
        
        // Set up real-time session count listener
        setupSessionCountListener(holder, course);

        // Load teacher profile info - prioritize cached data
        loadTeacherProfile(holder, course, position);

        // Set click listener for 3-dots menu
        holder.menuIcon.setOnClickListener(v -> showUnenrollMenu(v, course, position));
        
        // Set click listener for the entire card to navigate to course details
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.llavore.hereoattendance.student.StudentCourseDetails.class);
            intent.putExtra("courseId", course.id);
            intent.putExtra("courseName", course.name);
            intent.putExtra("courseRoom", course.room);
            intent.putExtra("courseSchedule", course.scheduleDays);
            intent.putExtra("courseStartTime", course.startTime);
            intent.putExtra("courseEndTime", course.endTime);
            intent.putExtra("courseLateAfter", course.lateAfter);
            intent.putExtra("courseCode", course.code);
            intent.putExtra("courseStudentCount", course.studentCount);
            intent.putExtra("courseSessionCount", course.sessionCount);
            context.startActivity(intent);
        });
    }

    private void loadTeacherProfile(StudentCourseViewHolder holder, Course course, int position) {
        // Always use cached data first (it should be refreshed on login)
        String firstName = nullSafe(course.teacherFirstName);
        String lastName = nullSafe(course.teacherLastName);
        String fullName = (firstName + " " + lastName).trim();

        android.util.Log.d("StudentCourseAdapter", "Loading teacher profile for course: " + course.name);
        android.util.Log.d("StudentCourseAdapter", "  - Cached name: '" + fullName + "'");
        android.util.Log.d("StudentCourseAdapter", "  - Cached profile URL: '" + course.teacherProfileImageUrl + "'");

        // Set teacher name (use cached data)
        holder.teacherName.setText(fullName.isEmpty() ? "Teacher" : fullName);

        // Load profile image (use cached data)
        if (course.teacherProfileImageUrl != null && !course.teacherProfileImageUrl.isEmpty()) {
            android.util.Log.d("StudentCourseAdapter", "Loading cached profile image from URL");
            Glide.with(context)
                    .load(course.teacherProfileImageUrl)
                    .diskCacheStrategy(DiskCacheStrategy.NONE) // Don't cache to ensure fresh images
                    .skipMemoryCache(true) // Skip memory cache for fresh images
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .into(holder.teacherProfilePicture);
        } else {
            android.util.Log.d("StudentCourseAdapter", "No cached profile image, using placeholder");
            holder.teacherProfilePicture.setImageResource(R.drawable.baseline_person_24);
        }

        // If we don't have cached data, fetch from database as fallback
        boolean hasCachedData = (course.teacherFirstName != null && !course.teacherFirstName.trim().isEmpty()) ||
                (course.teacherLastName != null && !course.teacherLastName.trim().isEmpty());

        if (!hasCachedData && course.teacherId != null && !course.teacherId.isEmpty()) {
            android.util.Log.d("StudentCourseAdapter", "No cached data available, fetching from database as fallback");
            holder.teacherName.setText("Loading...");
            fetchTeacherFromDatabase(holder, course, position);
        }
    }

    private void fetchTeacherFromDatabase(StudentCourseViewHolder holder, Course course, int position) {
        android.util.Log.d("StudentCourseAdapter", "Fetching teacher from database for ID: " + course.teacherId);

        mDatabase.child("users").child("teachers").child(course.teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            User teacher = snapshot.getValue(User.class);
                            if (teacher != null) {
                                // Update course object with teacher data
                                course.teacherFirstName = teacher.getFirstName();
                                course.teacherLastName = teacher.getLastName();
                                course.teacherProfileImageUrl = teacher.getProfileImageUrl();

                                android.util.Log.d("StudentCourseAdapter", "Fetched teacher data:");
                                android.util.Log.d("StudentCourseAdapter", "  - First Name: '" + teacher.getFirstName() + "'");
                                android.util.Log.d("StudentCourseAdapter", "  - Last Name: '" + teacher.getLastName() + "'");
                                android.util.Log.d("StudentCourseAdapter", "  - Profile URL: '" + teacher.getProfileImageUrl() + "'");

                                // Update UI
                                String firstName = nullSafe(teacher.getFirstName());
                                String lastName = nullSafe(teacher.getLastName());
                                String fullName = (firstName + " " + lastName).trim();

                                holder.teacherName.setText(fullName.isEmpty() ? "Teacher" : fullName);

                                if (teacher.getProfileImageUrl() != null && !teacher.getProfileImageUrl().isEmpty()) {
                                    Glide.with(context)
                                            .load(teacher.getProfileImageUrl())
                                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                                            .placeholder(R.drawable.baseline_person_24)
                                            .error(R.drawable.baseline_person_24)
                                            .into(holder.teacherProfilePicture);
                                }

                                // Notify adapter that this item changed
                                notifyItemChanged(position);
                            } else {
                                android.util.Log.e("StudentCourseAdapter", "Teacher object is null");
                                holder.teacherName.setText("Teacher");
                            }
                        } else {
                            android.util.Log.e("StudentCourseAdapter", "Teacher snapshot doesn't exist");
                            holder.teacherName.setText("Teacher");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        android.util.Log.e("StudentCourseAdapter", "Failed to fetch teacher: " + error.getMessage());
                        holder.teacherName.setText("Teacher");
                    }
                });
    }

    private void showUnenrollMenu(View anchorView, Course course, int position) {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.student_course_options_menu, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.menu_unenroll) {
                showUnenrollConfirmation(course, position);
                return true;
            }
            return false;
        });

        popupMenu.show();
    }

    private void showUnenrollConfirmation(Course course, int position) {
        new AlertDialog.Builder(context)
                .setTitle("Unenroll from Course")
                .setMessage("Are you sure you want to unenroll from " + course.name + "?")
                .setPositiveButton("Unenroll", (dialog, which) -> unenrollFromCourse(course, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void unenrollFromCourse(Course course, int position) {
        if (unenrollCallback != null) {
            unenrollCallback.onUnenroll(course, position);
        }
    }

    @Override
    public int getItemCount() {
        return courses.size();
    }

    private void setupSessionCountListener(StudentCourseViewHolder holder, Course course) {
        if (course.code == null) return;
        
        // Listen for real-time updates to the actual sessions node to count sessions dynamically
        mDatabase.child("courses").child(course.code).child("sessions").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Count the actual number of sessions
                int actualSessionCount = (int) snapshot.getChildrenCount();
                holder.sessionsText.setText("Sessions: " + actualSessionCount);
                android.util.Log.d("StudentCourseAdapter", "Updated session count for course " + course.name + " to: " + actualSessionCount + " (from actual sessions)");
                
                // Also update the sessionCount field in the global registry to keep it in sync
                mDatabase.child("courses").child(course.code).child("sessionCount").setValue(actualSessionCount)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                android.util.Log.d("StudentCourseAdapter", "Updated global sessionCount field for course " + course.name + " to: " + actualSessionCount);
                            } else {
                                android.util.Log.e("StudentCourseAdapter", "Failed to update global sessionCount field for course " + course.name + ": " + task.getException());
                            }
                        });
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("StudentCourseAdapter", "Failed to listen to sessions updates for course " + course.name + ": " + error.getMessage());
            }
        });
    }

    private String nullSafe(String str) {
        return str != null ? str : "";
    }

    static class StudentCourseViewHolder extends RecyclerView.ViewHolder {
        TextView courseTitle, courseSchedule, sessionsText, teacherName;
        ImageView teacherProfilePicture, menuIcon;

        public StudentCourseViewHolder(@NonNull View itemView) {
            super(itemView);
            courseTitle = itemView.findViewById(R.id.courseTitle);
            courseSchedule = itemView.findViewById(R.id.courseSchedule);
            sessionsText = itemView.findViewById(R.id.studentsText);
            teacherName = itemView.findViewById(R.id.teacherName);
            teacherProfilePicture = itemView.findViewById(R.id.teacherProfilePicture);
            menuIcon = itemView.findViewById(R.id.menuIcon);
        }
    }
}