package com.llavore.hereoattendance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.SessionStatus;

import java.util.List;

public class SessionStatusAdapter extends RecyclerView.Adapter<SessionStatusAdapter.SessionStatusViewHolder> {

    private Context context;
    private List<SessionStatus> sessionStatuses;

    public SessionStatusAdapter(Context context, List<SessionStatus> sessionStatuses) {
        this.context = context;
        this.sessionStatuses = sessionStatuses;
    }

    @NonNull
    @Override
    public SessionStatusViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_session_status, parent, false);
        return new SessionStatusViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionStatusViewHolder holder, int position) {
        SessionStatus sessionStatus = sessionStatuses.get(position);
        
        // Set date
        holder.dateText.setText(sessionStatus.getDate());
        
        // Set status with appropriate color
        holder.statusText.setText(sessionStatus.getStatus());
        
        // Set status color based on attendance status
        switch (sessionStatus.getStatus().toUpperCase()) {
            case "PRESENT":
                holder.statusText.setTextColor(context.getResources().getColor(R.color.green));
                break;
            case "LATE":
                holder.statusText.setTextColor(context.getResources().getColor(android.R.color.holo_orange_dark));
                break;
            case "ABSENT":
                holder.statusText.setTextColor(context.getResources().getColor(android.R.color.holo_red_dark));
                break;
            case "EXCUSED":
                holder.statusText.setTextColor(context.getResources().getColor(android.R.color.holo_blue_dark));
                break;
            case "NOT AVAILABLE":
                holder.statusText.setTextColor(context.getResources().getColor(R.color.gray));
                break;
            default:
                holder.statusText.setTextColor(context.getResources().getColor(R.color.black));
                break;
        }
    }

    @Override
    public int getItemCount() {
        return sessionStatuses.size();
    }

    static class SessionStatusViewHolder extends RecyclerView.ViewHolder {
        TextView dateText, statusText;

        public SessionStatusViewHolder(@NonNull View itemView) {
            super(itemView);
            dateText = itemView.findViewById(R.id.dateText);
            statusText = itemView.findViewById(R.id.statusText);
        }
    }
}
