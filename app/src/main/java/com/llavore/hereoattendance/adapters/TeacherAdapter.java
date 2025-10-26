package com.llavore.hereoattendance.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.Teacher;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class TeacherAdapter extends RecyclerView.Adapter<TeacherAdapter.TeacherViewHolder> {

    private List<Teacher> teacherList;

    public TeacherAdapter(List<Teacher> teacherList) {
        this.teacherList = teacherList;
    }

    @NonNull
    @Override
    public TeacherViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.teacher_card, parent, false);
        return new TeacherViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeacherViewHolder holder, int position) {
        Teacher teacher = teacherList.get(position);
        holder.bind(teacher);
    }

    @Override
    public int getItemCount() {
        return teacherList.size();
    }

    public class TeacherViewHolder extends RecyclerView.ViewHolder {
        private TextView teacherName, employeeId, teacherEmail;
        private CircleImageView teacherProfilePicture;

        public TeacherViewHolder(@NonNull View itemView) {
            super(itemView);
            teacherName = itemView.findViewById(R.id.teacherName);
            employeeId = itemView.findViewById(R.id.employeeId);
            teacherEmail = itemView.findViewById(R.id.teacherEmail);
            teacherProfilePicture = itemView.findViewById(R.id.teacherProfilePicture);
        }

        public void bind(Teacher teacher) {
            // Display only first + last name
            String displayName = "";
            if (teacher.getFirstName() != null && !teacher.getFirstName().isEmpty()) {
                displayName = teacher.getFirstName();
            }
            if (teacher.getLastName() != null && !teacher.getLastName().isEmpty()) {
                if (!displayName.isEmpty()) {
                    displayName += " " + teacher.getLastName();
                } else {
                    displayName = teacher.getLastName();
                }
            }
            teacherName.setText(displayName.isEmpty() ? "Unknown Teacher" : displayName);
            employeeId.setText(teacher.getIdNumber());
            teacherEmail.setText(teacher.getEmail());
            
            // Load profile picture if available
            loadProfilePicture(teacher);
        }
        
        private void loadProfilePicture(Teacher teacher) {
            if (teacher.getProfileImageUrl() != null && !teacher.getProfileImageUrl().isEmpty()) {
                // Load the image using Glide from the stored URL
                Glide.with(teacherProfilePicture.getContext())
                        .load(teacher.getProfileImageUrl())
                        .placeholder(R.drawable.default_profile_2)
                        .error(R.drawable.default_profile_2)
                        .into(teacherProfilePicture);
            } else {
                // Set default image if no profile image URL
                teacherProfilePicture.setImageResource(R.drawable.default_profile_2);
            }
        }
    }
}
