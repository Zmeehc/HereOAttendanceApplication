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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.llavore.hereoattendance.R;
import com.llavore.hereoattendance.models.SavedExport;
import com.llavore.hereoattendance.teacher.SavedExportsActivity;

import java.util.List;

public class SavedExportAdapter extends RecyclerView.Adapter<SavedExportAdapter.ExportViewHolder> {

    private List<SavedExport> exports;
    private Context context;

    public SavedExportAdapter(List<SavedExport> exports, Context context) {
        this.exports = exports;
        this.context = context;
    }

    @NonNull
    @Override
    public ExportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.card_saved_export, parent, false);
        return new ExportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExportViewHolder holder, int position) {
        SavedExport export = exports.get(position);
        
        holder.fileNameText.setText(export.fileName);
        holder.dateRangeText.setText(export.dateRange);
        
        // Set click listener to open export activity
        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.llavore.hereoattendance.teacher.ExportingReportActivity.class);
            intent.putExtra("courseCode", export.courseCode);
            intent.putExtra("courseName", export.courseName);
            intent.putExtra("startDate", export.startDate);
            intent.putExtra("endDate", export.endDate);
            intent.putExtra("fileName", export.fileName);
            context.startActivity(intent);
        });
        
        // Set click listener for 3-dot menu
        holder.menuIcon.setOnClickListener(v -> showExportOptionsMenu(v, export, position));
    }
    
    private void showExportOptionsMenu(View anchorView, SavedExport export, int position) {
        PopupMenu popupMenu = new PopupMenu(context, anchorView);
        popupMenu.getMenuInflater().inflate(R.menu.saved_export_options_menu, popupMenu.getMenu());
        
        popupMenu.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_delete) {
                showDeleteConfirmation(export, position);
                return true;
            }
            return false;
        });
        
        popupMenu.show();
    }
    
    
    private void showDeleteConfirmation(SavedExport export, int position) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Delete Export")
                .setMessage("Are you sure you want to delete this export? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteExport(export, position))
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void deleteExport(SavedExport export, int position) {
        String currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ?
                FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Toast.makeText(context, "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        DatabaseReference mDatabase = FirebaseDatabase.getInstance().getReference();
        
        // Remove from Firebase
        mDatabase.child("users").child("teachers").child(currentUserId).child("savedExports")
                .child(export.id).removeValue()
                .addOnSuccessListener(aVoid -> {
                    // Remove from local list and notify adapter
                    exports.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, exports.size());
                    
                    Toast.makeText(context, "Export deleted successfully", Toast.LENGTH_SHORT).show();
                    
                    // Notify activity to refresh if no exports left
                    if (exports.isEmpty() && context instanceof SavedExportsActivity) {
                        ((SavedExportsActivity) context).onExportDeleted();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(context, "Failed to delete export", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public int getItemCount() {
        return exports.size();
    }

    static class ExportViewHolder extends RecyclerView.ViewHolder {
        TextView fileNameText, dateRangeText;
        ImageView menuIcon;

        public ExportViewHolder(@NonNull View itemView) {
            super(itemView);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            dateRangeText = itemView.findViewById(R.id.dateRangeText);
            menuIcon = itemView.findViewById(R.id.menuIcon);
        }
    }
}
