package com.example.syncnote.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncnote.R;
import com.example.syncnote.models.SharedNoteModel;
import com.example.syncnote.utils.DateUtils;

import java.util.ArrayList;
import java.util.List;

public class SharedNotesAdapter extends RecyclerView.Adapter<SharedNotesAdapter.SharedNoteViewHolder> {

    private List<SharedNoteModel> sharedNotes = new ArrayList<>();
    private final Context context;
    private final OnSharedNoteClickListener listener;

    public interface OnSharedNoteClickListener {
        void onSharedNoteClick(SharedNoteModel sharedNote);
    }

    public SharedNotesAdapter(Context context, OnSharedNoteClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setSharedNotes(List<SharedNoteModel> sharedNotes) {
        this.sharedNotes = sharedNotes != null ? sharedNotes : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public SharedNoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_shared_note, parent, false);
        return new SharedNoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SharedNoteViewHolder holder, int position) {
        SharedNoteModel sharedNote = sharedNotes.get(position);
        holder.bind(sharedNote);
    }

    @Override
    public int getItemCount() {
        return sharedNotes.size();
    }

    class SharedNoteViewHolder extends RecyclerView.ViewHolder {
        private final TextView ownerUsername;
        private final TextView permissionBadge;
        private final TextView noteTitle;
        private final TextView noteContent;
        private final TextView sharedDate;

        SharedNoteViewHolder(@NonNull View itemView) {
            super(itemView);
            ownerUsername = itemView.findViewById(R.id.ownerUsername);
            permissionBadge = itemView.findViewById(R.id.permissionBadge);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteContent = itemView.findViewById(R.id.noteContent);
            sharedDate = itemView.findViewById(R.id.sharedDate);
        }

        void bind(SharedNoteModel sharedNote) {
            ownerUsername.setText("Shared by @" + sharedNote.getOwnerUsername());
            
            String permission = sharedNote.getPermission().toUpperCase();
            permissionBadge.setText(permission);
            
            String title = sharedNote.getNoteTitle();
            if (title == null || title.isEmpty()) {
                title = "Untitled Note";
            }
            noteTitle.setText(title);

            String content = sharedNote.getNoteContent();
            if (content == null || content.isEmpty()) {
                content = "No content";
            }
            noteContent.setText(content);

            sharedDate.setText("Shared on " + DateUtils.formatDate(sharedNote.getSharedAt()));

            itemView.setOnClickListener(v -> listener.onSharedNoteClick(sharedNote));
        }
    }
}
