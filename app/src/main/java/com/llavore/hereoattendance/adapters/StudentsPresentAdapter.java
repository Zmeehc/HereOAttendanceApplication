package com.llavore.hereoattendance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.AttendanceRecord;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class StudentsPresentAdapter extends RecyclerView.Adapter<StudentsPresentAdapter.StudentPresentViewHolder> {
    
    private List<AttendanceRecord> students;
    private Context context;
    
    public StudentsPresentAdapter(List<AttendanceRecord> students) {
        this.students = students;
    }
    
    @NonNull
    @Override
    public StudentPresentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.students_present_card, parent, false);
        return new StudentPresentViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull StudentPresentViewHolder holder, int position) {
        AttendanceRecord student = students.get(position);
        
        // Set student name
        holder.studentName.setText(student.getFullName().trim());
        
        // Set student ID/EDP number
        holder.studentId.setText(student.getEdpNumber());
        
        // Set time in
        holder.timeIn.setText("Time in: " + student.getTimeIn());
        
        // Load profile image
        if (student.getProfileImageUrl() != null && !student.getProfileImageUrl().isEmpty()) {
            Glide.with(context)
                    .load(student.getProfileImageUrl())
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .placeholder(R.drawable.baseline_person_24)
                    .error(R.drawable.baseline_person_24)
                    .into(holder.studentProfilePicture);
        } else {
            holder.studentProfilePicture.setImageResource(R.drawable.baseline_person_24);
        }
    }
    
    @Override
    public int getItemCount() {
        return students.size();
    }
    
    public static class StudentPresentViewHolder extends RecyclerView.ViewHolder {
        CircleImageView studentProfilePicture;
        TextView studentName, studentId, timeIn;
        
        public StudentPresentViewHolder(@NonNull View itemView) {
            super(itemView);
            studentProfilePicture = itemView.findViewById(R.id.studentProfilePicture);
            studentName = itemView.findViewById(R.id.studentName);
            studentId = itemView.findViewById(R.id.studentId);
            timeIn = itemView.findViewById(R.id.timeIn);
        }
    }
}
