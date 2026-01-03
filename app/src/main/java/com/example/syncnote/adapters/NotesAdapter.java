package com.example.syncnote.adapters;

import android.content.Context;
import android.graphics.Color;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncnote.R;
import com.example.syncnote.models.NoteModel;
import com.example.syncnote.utils.DateUtils;

import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<NoteModel> notes = new ArrayList<>();
    private final Context context;
    private final OnNoteClickListener listener;

    public interface OnNoteClickListener {
        void onNoteClick(NoteModel note);
        void onEditClick(NoteModel note);
        void onShareClick(NoteModel note);
        void onDeleteClick(NoteModel note);
        void onManageAccessClick(NoteModel note);
        void onPinClick(NoteModel note);
        void onColorClick(NoteModel note);
    }

    public NotesAdapter(Context context, OnNoteClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setNotes(List<NoteModel> notes) {
        this.notes = notes != null ? notes : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public NoteModel getNoteAtPosition(int position) {
        if (position >= 0 && position < notes.size()) {
            return notes.get(position);
        }
        return null;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteModel note = notes.get(position);
        holder.bind(note);
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final MaterialCardView noteCard;
        private final TextView noteTitle;
        private final TextView noteContent;
        private final TextView noteDate;
        private final TextView lastUpdatedBy;
        private final ImageButton moreButton;
        private final LinearLayout sharedBadge;
        private final TextView sharedBadgeText;
        private final ImageView pinIcon;
        private final TextView categoryBadge;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            noteCard = itemView.findViewById(R.id.noteCard);
            noteTitle = itemView.findViewById(R.id.noteTitle);
            noteContent = itemView.findViewById(R.id.noteContent);
            noteDate = itemView.findViewById(R.id.noteDate);
            lastUpdatedBy = itemView.findViewById(R.id.lastUpdatedBy);
            moreButton = itemView.findViewById(R.id.moreButton);
            sharedBadge = itemView.findViewById(R.id.sharedBadge);
            sharedBadgeText = itemView.findViewById(R.id.sharedBadgeText);
            pinIcon = itemView.findViewById(R.id.pinIcon);
            categoryBadge = itemView.findViewById(R.id.categoryBadge);
        }

        void bind(NoteModel note) {
            String title = note.getTitle();
            if (title == null || title.isEmpty()) {
                title = "Untitled Note";
            }
            noteTitle.setText(title);

            // Handle HTML content from RichEditor
            String content = note.getContent();
            if (content == null || content.isEmpty()) {
                content = "No content";
            } else {
                // Strip HTML tags for preview
                content = Html.fromHtml(content, Html.FROM_HTML_MODE_COMPACT).toString().trim();
                if (content.isEmpty()) {
                    content = "No content";
                }
            }
            noteContent.setText(content);

            noteDate.setText("Updated " + DateUtils.getRelativeTime(note.getUpdatedAt()));

            // Show last updated by if different from owner
            if (note.getLastUpdatedByUsername() != null && !note.getLastUpdatedByUsername().isEmpty()) {
                lastUpdatedBy.setVisibility(View.VISIBLE);
                lastUpdatedBy.setText("by " + note.getLastUpdatedByUsername());
            } else {
                lastUpdatedBy.setVisibility(View.GONE);
            }

            // Note card background color
            if (noteCard != null) {
                String color = note.getColor();
                if (color != null && !color.isEmpty()) {
                    try {
                        noteCard.setCardBackgroundColor(Color.parseColor(color));
                    } catch (IllegalArgumentException e) {
                        noteCard.setCardBackgroundColor(context.getResources().getColor(R.color.card_background));
                    }
                } else {
                    noteCard.setCardBackgroundColor(context.getResources().getColor(R.color.card_background));
                }
            }
            
            // Pin icon
            if (pinIcon != null) {
                pinIcon.setVisibility(note.isPinned() ? View.VISIBLE : View.GONE);
            }
            
            // Category badge
            if (categoryBadge != null) {
                String category = note.getCategory();
                if (category != null && !category.isEmpty()) {
                    categoryBadge.setText(category);
                    categoryBadge.setVisibility(View.VISIBLE);
                } else {
                    categoryBadge.setVisibility(View.GONE);
                }
            }

            // Hide shared badge for own notes (it's used for shared indicator if needed)
            sharedBadge.setVisibility(View.GONE);

            itemView.setOnClickListener(v -> listener.onNoteClick(note));

            moreButton.setOnClickListener(v -> showPopupMenu(v, note));
        }

        private void showPopupMenu(View anchor, NoteModel note) {
            PopupMenu popup = new PopupMenu(context, anchor);
            popup.getMenuInflater().inflate(R.menu.menu_note_item, popup.getMenu());
            
            // Update pin menu item text based on current state
            popup.getMenu().findItem(R.id.action_pin).setTitle(note.isPinned() ? "Unpin Note" : "Pin Note");

            popup.setOnMenuItemClickListener(item -> {
                int id = item.getItemId();
                if (id == R.id.action_edit) {
                    listener.onEditClick(note);
                    return true;
                } else if (id == R.id.action_share) {
                    listener.onShareClick(note);
                    return true;
                } else if (id == R.id.action_manage_access) {
                    listener.onManageAccessClick(note);
                    return true;
                } else if (id == R.id.action_pin) {
                    listener.onPinClick(note);
                    return true;
                } else if (id == R.id.action_color) {
                    listener.onColorClick(note);
                    return true;
                } else if (id == R.id.action_delete) {
                    listener.onDeleteClick(note);
                    return true;
                }
                return false;
            });

            popup.show();
        }
    }
}
