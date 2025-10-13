package com.llavore.hereoattendance.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.TodaySession;

import java.util.List;

public class TodaySessionAdapter extends RecyclerView.Adapter<TodaySessionAdapter.ViewHolder> {

    private List<TodaySession> todaySessions;
    private Context context;
    private OnSessionClickListener onSessionClickListener;

    public interface OnSessionClickListener {
        void onSessionClick(TodaySession session);
    }

    public TodaySessionAdapter(List<TodaySession> todaySessions, Context context) {
        this.todaySessions = todaySessions;
        this.context = context;
    }

    public void setOnSessionClickListener(OnSessionClickListener listener) {
        this.onSessionClickListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.card_today_session, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        TodaySession session = todaySessions.get(position);
        
        // Set course information
        holder.courseInfoText.setText(session.getCourseInfo());
        
        // Set time information
        holder.startTimeText.setText(session.getFormattedStartTime());
        holder.lateTimeText.setText(session.getFormattedLateTime());
        holder.endTimeText.setText(session.getFormattedEndTime());
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (onSessionClickListener != null) {
                onSessionClickListener.onSessionClick(session);
            }
        });
    }

    @Override
    public int getItemCount() {
        return todaySessions.size();
    }

    public void updateData(List<TodaySession> newSessions) {
        this.todaySessions = newSessions;
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView courseInfoText;
        TextView startTimeText;
        TextView lateTimeText;
        TextView endTimeText;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            courseInfoText = itemView.findViewById(R.id.courseInfoText);
            startTimeText = itemView.findViewById(R.id.startTimeText);
            lateTimeText = itemView.findViewById(R.id.lateTimeText);
            endTimeText = itemView.findViewById(R.id.endTimeText);
        }
    }
}
