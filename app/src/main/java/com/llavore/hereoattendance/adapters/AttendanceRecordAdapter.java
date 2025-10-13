package com.llavore.hereoattendance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.AttendanceRecord;

import java.util.List;

public class AttendanceRecordAdapter extends RecyclerView.Adapter<AttendanceRecordAdapter.AttendanceRecordViewHolder> {
    
    private List<AttendanceRecord> attendanceRecords;
    private Context context;
    private boolean isExcuseMode = false;
    private OnExcuseStatusChangeListener excuseStatusChangeListener;
    
    public interface OnExcuseStatusChangeListener {
        void onExcuseStatusChanged(AttendanceRecord record, boolean isExcused);
    }
    
    public AttendanceRecordAdapter(List<AttendanceRecord> attendanceRecords) {
        this.attendanceRecords = attendanceRecords;
    }
    
    public void setExcuseMode(boolean excuseMode) {
        this.isExcuseMode = excuseMode;
        notifyDataSetChanged();
    }
    
    public void setOnExcuseStatusChangeListener(OnExcuseStatusChangeListener listener) {
        this.excuseStatusChangeListener = listener;
    }
    
    @NonNull
    @Override
    public AttendanceRecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        View view = LayoutInflater.from(context).inflate(R.layout.attendance_record_item, parent, false);
        return new AttendanceRecordViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull AttendanceRecordViewHolder holder, int position) {
        AttendanceRecord record = attendanceRecords.get(position);
        
        // Set EDP number
        holder.edpNumber.setText(record.getEdpNumber());
        
        // Set student name
        holder.studentName.setText(record.getFullName().trim());
        
        // Set status with appropriate color
        String status = record.getStatus();
        holder.status.setText(status);
        
        // Set status color based on type
        int statusColor;
        switch (status) {
            case "PRESENT":
                statusColor = context.getResources().getColor(R.color.green);
                break;
            case "ABSENT":
                statusColor = context.getResources().getColor(android.R.color.holo_red_dark);
                break;
            case "LATE":
                statusColor = context.getResources().getColor(android.R.color.holo_orange_dark);
                break;
            case "EXCUSED":
                statusColor = context.getResources().getColor(android.R.color.holo_blue_dark);
                break;
            default:
                statusColor = context.getResources().getColor(android.R.color.black);
                break;
        }
        holder.status.setTextColor(statusColor);
        
        // Handle checkbox visibility and state
        if (isExcuseMode && (status.equals("ABSENT") || status.equals("EXCUSED"))) {
            holder.excuseCheckbox.setVisibility(View.VISIBLE);
            holder.excuseCheckbox.setChecked(status.equals("EXCUSED"));
            
            holder.excuseCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (excuseStatusChangeListener != null) {
                    excuseStatusChangeListener.onExcuseStatusChanged(record, isChecked);
                }
            });
        } else {
            holder.excuseCheckbox.setVisibility(View.GONE);
            holder.excuseCheckbox.setOnCheckedChangeListener(null);
        }
    }
    
    @Override
    public int getItemCount() {
        return attendanceRecords.size();
    }
    
    static class AttendanceRecordViewHolder extends RecyclerView.ViewHolder {
        CheckBox excuseCheckbox;
        TextView edpNumber, studentName, status;
        
        public AttendanceRecordViewHolder(@NonNull View itemView) {
            super(itemView);
            excuseCheckbox = itemView.findViewById(R.id.excuseCheckbox);
            edpNumber = itemView.findViewById(R.id.edpNumber);
            studentName = itemView.findViewById(R.id.studentName);
            status = itemView.findViewById(R.id.status);
        }
    }
}
