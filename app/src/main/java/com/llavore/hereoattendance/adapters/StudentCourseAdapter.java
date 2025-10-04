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
        
        // Set course title (Name | Room)
        holder.courseTitle.setText(String.format("%s | %s", 
            nullSafe(course.name), nullSafe(course.room)));
        
        // Set schedule (Start - End | Days)
        holder.courseSchedule.setText(String.format("%s - %s | %s", 
            nullSafe(course.startTime), nullSafe(course.endTime), nullSafe(course.scheduleDays)));
        
        // Set sessions count
        holder.sessionsText.setText(String.format("Sessions: %d", course.sessionCount));
        
        // Fetch teacher profile info
        fetchTeacherProfile(holder, course);
        
        // Set click listener for 3-dots menu
        holder.menuIcon.setOnClickListener(v -> showUnenrollMenu(v, course, position));
    }
    
    private void fetchTeacherProfile(StudentCourseViewHolder holder, Course course) {
        android.util.Log.d("TeacherProfile", "Loading teacher profile for course: " + course.name);
        android.util.Log.d("TeacherProfile", "Course data check:");
        android.util.Log.d("TeacherProfile", "  - Teacher First Name: '" + course.teacherFirstName + "'");
        android.util.Log.d("TeacherProfile", "  - Teacher Last Name: '" + course.teacherLastName + "'");
        android.util.Log.d("TeacherProfile", "  - Teacher Profile URL: '" + course.teacherProfileImageUrl + "'");
        android.util.Log.d("TeacherProfile", "  - Teacher ID: '" + course.teacherId + "'");
        
        // First, try to use cached teacher data (faster)
        if (course.teacherFirstName != null && course.teacherLastName != null) {
            String fullName = (course.teacherFirstName + " " + course.teacherLastName).trim();
            android.util.Log.d("TeacherProfile", "Using cached teacher data: " + fullName);
            
            holder.teacherName.setText(fullName.isEmpty() ? "Teacher" : fullName);
            
            // Load cached profile image
            if (course.teacherProfileImageUrl != null && !course.teacherProfileImageUrl.isEmpty()) {
                android.util.Log.d("TeacherProfile", "Loading cached profile image from URL: " + course.teacherProfileImageUrl);
                Glide.with(context)
                        .load(course.teacherProfileImageUrl)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(R.drawable.baseline_person_24)
                        .error(R.drawable.baseline_person_24)
                        .into(holder.teacherProfilePicture);
            } else {
                android.util.Log.d("TeacherProfile", "No cached profile image URL, using placeholder");
                holder.teacherProfilePicture.setImageResource(R.drawable.baseline_person_24);
            }
            return;
        }
        
        android.util.Log.d("TeacherProfile", "No cached teacher data found, will fetch from database");
        
        // If no cached data, fetch from database
        if (course.teacherId == null || course.teacherId.isEmpty()) {
            android.util.Log.e("TeacherProfile", "TeacherId is null or empty for course: " + course.name);
            holder.teacherName.setText("Teacher Unknown");
            holder.teacherProfilePicture.setImageResource(R.drawable.baseline_person_24);
            return;
        }
        
        android.util.Log.d("TeacherProfile", "Fetching teacher data from database for teacherId: " + course.teacherId);
        
        mDatabase.child("users").child("teachers").child(course.teacherId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                android.util.Log.d("TeacherProfile", "Teacher data exists: " + snapshot.exists() + " for teacherId: " + course.teacherId);
                
                if (snapshot.exists()) {
                    User teacher = snapshot.getValue(User.class);
                    if (teacher != null) {
                        android.util.Log.d("TeacherProfile", "Teacher object created successfully");
                        
                        // Set teacher name
                        String firstName = teacher.getFirstName() != null ? teacher.getFirstName() : "";
                        String lastName = teacher.getLastName() != null ? teacher.getLastName() : "";
                        String fullName = (firstName + " " + lastName).trim();
                        
                        android.util.Log.d("TeacherProfile", "Teacher name: '" + fullName + "' (firstName: '" + firstName + "', lastName: '" + lastName + "')");
                        
                        holder.teacherName.setText(fullName.isEmpty() ? "Teacher" : fullName);
                        
                        // Load teacher profile picture
                        String profileImageUrl = teacher.getProfileImageUrl();
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            android.util.Log.d("TeacherProfile", "Loading profile image from URL: " + profileImageUrl);
                            Glide.with(context)
                                    .load(profileImageUrl)
                                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                                    .placeholder(R.drawable.baseline_person_24)
                                    .error(R.drawable.baseline_person_24)
                                    .into(holder.teacherProfilePicture);
                        } else {
                            android.util.Log.d("TeacherProfile", "No profile image URL found, using placeholder");
                            holder.teacherProfilePicture.setImageResource(R.drawable.baseline_person_24);
                        }
                    } else {
                        android.util.Log.e("TeacherProfile", "Teacher object is null");
                        holder.teacherName.setText("Teacher");
                        holder.teacherProfilePicture.setImageResource(R.drawable.baseline_person_24);
                    }
                } else {
                    android.util.Log.e("TeacherProfile", "Teacher data does not exist in database");
                    holder.teacherName.setText("Teacher");
                    holder.teacherProfilePicture.setImageResource(R.drawable.baseline_person_24);
                }
            }
            
            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                android.util.Log.e("TeacherProfile", "Failed to fetch teacher data: " + error.getMessage());
                holder.teacherName.setText("Teacher");
                holder.teacherProfilePicture.setImageResource(R.drawable.baseline_person_24);
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
