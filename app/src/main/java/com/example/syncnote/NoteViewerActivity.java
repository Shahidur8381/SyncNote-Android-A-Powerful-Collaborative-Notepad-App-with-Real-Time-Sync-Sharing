package com.example.syncnote;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncnote.firebase.FirebaseManager;
import com.example.syncnote.models.ActivityLogModel;
import com.example.syncnote.models.NoteModel;
import com.example.syncnote.models.SharedNoteModel;
import com.example.syncnote.utils.DateUtils;
import com.example.syncnote.utils.SessionManager;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class NoteViewerActivity extends AppCompatActivity {

    private TextView noteTitle, createdDate, updatedDate, lastUpdatedBy, noteContent;
    private TextView sharedByText;
    private LinearLayout sharedNoteInfo;
    private FloatingActionButton fabEdit;

    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    private String noteId;
    private NoteModel currentNote;
    private boolean isShared = false;
    private boolean canEdit = false;
    private String ownerUsername;
    private String permission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_viewer);

        firebaseManager = FirebaseManager.getInstance();
        sessionManager = SessionManager.getInstance(this);

        // Get intent extras
        noteId = getIntent().getStringExtra("noteId");
        isShared = getIntent().getBooleanExtra("isShared", false);
        canEdit = getIntent().getBooleanExtra("canEdit", false);
        ownerUsername = getIntent().getStringExtra("ownerUsername");
        permission = getIntent().getStringExtra("permission");

        initViews();
        setupToolbar();
        loadNote();
    }

    private void initViews() {
        noteTitle = findViewById(R.id.noteTitle);
        createdDate = findViewById(R.id.createdDate);
        updatedDate = findViewById(R.id.updatedDate);
        lastUpdatedBy = findViewById(R.id.lastUpdatedBy);
        noteContent = findViewById(R.id.noteContent);
        sharedByText = findViewById(R.id.sharedByText);
        sharedNoteInfo = findViewById(R.id.sharedNoteInfo);
        fabEdit = findViewById(R.id.fabEdit);

        fabEdit.setOnClickListener(v -> openEditor());
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("View Note");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void loadNote() {
        if (noteId == null) {
            Toast.makeText(this, "Error: Note ID not provided", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        firebaseManager.getNoteById(noteId, new FirebaseManager.NoteCallback() {
            @Override
            public void onSuccess(NoteModel note) {
                runOnUiThread(() -> {
                    currentNote = note;
                    displayNote(note);
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(NoteViewerActivity.this, "Error loading note: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void displayNote(NoteModel note) {
        String title = note.getTitle();
        if (title == null || title.isEmpty()) {
            title = "Untitled Note";
        }
        noteTitle.setText(title);

        createdDate.setText("Created: " + DateUtils.formatDateTime(note.getCreatedAt()));
        updatedDate.setText("Updated: " + DateUtils.formatDateTime(note.getUpdatedAt()));

        // Show last updated by
        if (note.getLastUpdatedByUsername() != null && !note.getLastUpdatedByUsername().isEmpty()) {
            String currentUserId = sessionManager.getCurrentUserId();
            if (!note.getLastUpdatedBy().equals(currentUserId)) {
                lastUpdatedBy.setVisibility(View.VISIBLE);
                lastUpdatedBy.setText("Last edited by " + note.getLastUpdatedByUsername());
            } else {
                lastUpdatedBy.setVisibility(View.GONE);
            }
        } else {
            lastUpdatedBy.setVisibility(View.GONE);
        }

        String content = note.getContent();
        if (content == null || content.isEmpty()) {
            content = "No content";
        }
        noteContent.setText(content);

        // Handle shared note info
        if (isShared) {
            sharedNoteInfo.setVisibility(View.VISIBLE);
            String permText = permission != null ? permission.toUpperCase() : "VIEW";
            sharedByText.setText("Shared by @" + ownerUsername + " (" + permText + " permission)");
            
            // Show/hide edit FAB based on permission
            if (canEdit) {
                fabEdit.setVisibility(View.VISIBLE);
            } else {
                fabEdit.setVisibility(View.GONE);
            }
        } else {
            sharedNoteInfo.setVisibility(View.GONE);
            
            // Check if current user is the owner
            String currentUserId = sessionManager.getCurrentUserId();
            if (note.getUserId().equals(currentUserId)) {
                fabEdit.setVisibility(View.VISIBLE);
            } else {
                fabEdit.setVisibility(View.GONE);
            }
        }
    }

    private void openEditor() {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("noteId", noteId);
        startActivity(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (currentNote != null) {
            loadNote(); // Reload to get any updates
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Only show menu for owner or users with edit permission
        if (!isShared || canEdit) {
            getMenuInflater().inflate(R.menu.menu_note_viewer, menu);
            
            // Hide certain options for shared notes
            if (isShared) {
                MenuItem shareItem = menu.findItem(R.id.action_share);
                if (shareItem != null) shareItem.setVisible(false);
                
                MenuItem manageItem = menu.findItem(R.id.action_manage_access);
                if (manageItem != null) manageItem.setVisible(false);
                
                MenuItem deleteItem = menu.findItem(R.id.action_delete);
                if (deleteItem != null) deleteItem.setVisible(false);
            }
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_share) {
            showShareDialog();
            return true;
        } else if (id == R.id.action_activity_log) {
            showActivityLog();
            return true;
        } else if (id == R.id.action_manage_access) {
            showManageAccessDialog();
            return true;
        } else if (id == R.id.action_delete) {
            showDeleteConfirmation();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    private void showActivityLog() {
        firebaseManager.getActivityLogsForNote(noteId, new FirebaseManager.ActivityLogsCallback() {
            @Override
            public void onSuccess(List<ActivityLogModel> logs) {
                runOnUiThread(() -> {
                    if (logs.isEmpty()) {
                        Toast.makeText(NoteViewerActivity.this, "No activity recorded for this note", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Build log display
                    StringBuilder logBuilder = new StringBuilder();
                    for (ActivityLogModel log : logs) {
                        logBuilder.append("• ")
                                .append(log.getUsername())
                                .append(" ")
                                .append(log.getActionDisplayText())
                                .append("\n  ")
                                .append(DateUtils.formatDateTime(log.getTimestamp()));
                        
                        if (log.getDetails() != null && !log.getDetails().isEmpty()) {
                            logBuilder.append("\n  ").append(log.getDetails());
                        }
                        logBuilder.append("\n\n");
                    }
                    
                    new AlertDialog.Builder(NoteViewerActivity.this)
                            .setTitle("Activity Log")
                            .setMessage(logBuilder.toString().trim())
                            .setPositiveButton("Close", null)
                            .show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(NoteViewerActivity.this, "Error loading activity log", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showShareDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_share_note, null);
        EditText usernameInput = dialogView.findViewById(R.id.usernameInput);
        android.widget.RadioGroup permissionGroup = dialogView.findViewById(R.id.permissionGroup);

        new AlertDialog.Builder(this)
                .setTitle("Share Note")
                .setMessage("Share \"" + currentNote.getTitle() + "\" with another user")
                .setView(dialogView)
                .setPositiveButton("Share", (dialog, which) -> {
                    String username = usernameInput.getText().toString().trim();
                    if (username.isEmpty()) {
                        Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String perm = permissionGroup.getCheckedRadioButtonId() == R.id.radioEdit ? "edit" : "view";
                    shareNoteWithUser(username, perm);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareNoteWithUser(String username, String permission) {
        if (username.equalsIgnoreCase(sessionManager.getCurrentUsername())) {
            Toast.makeText(this, "You cannot share a note with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.getUserByUsername(username, new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.syncnote.models.UserModel user) {
                firebaseManager.shareNote(currentNote.getId(), sessionManager.getCurrentUserId(),
                        user.getId(), permission, result -> {
                            runOnUiThread(() -> {
                                if (result) {
                                    Toast.makeText(NoteViewerActivity.this,
                                            "Note shared with " + username + " (" + permission + " permission)",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(NoteViewerActivity.this,
                                            "Failed to share note", Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(NoteViewerActivity.this,
                            "User '" + username + "' not found", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showManageAccessDialog() {
        Toast.makeText(this, "Loading shared users...", Toast.LENGTH_SHORT).show();
        
        firebaseManager.getSharedUsersForNote(noteId, new FirebaseManager.SharedNotesCallback() {
            @Override
            public void onSuccess(List<SharedNoteModel> sharedUsers) {
                runOnUiThread(() -> {
                    if (sharedUsers.isEmpty()) {
                        Toast.makeText(NoteViewerActivity.this,
                                "This note is not currently shared with anyone", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] usernames = new String[sharedUsers.size()];
                    for (int i = 0; i < sharedUsers.size(); i++) {
                        SharedNoteModel shared = sharedUsers.get(i);
                        String username = shared.getSharedWithUsername();
                        String permission = shared.getPermission();
                        usernames[i] = (username != null ? username : "Unknown User") + " (" + 
                                       (permission != null ? permission.toUpperCase() : "VIEW") + ")";
                    }

                    new AlertDialog.Builder(NoteViewerActivity.this)
                            .setTitle("Shared with " + sharedUsers.size() + " user(s)")
                            .setItems(usernames, (dialog, which) -> {
                                SharedNoteModel selected = sharedUsers.get(which);
                                showUserAccessOptions(selected);
                            })
                            .setNegativeButton("Close", null)
                            .show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(NoteViewerActivity.this,
                            "Error loading shared users: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showUserAccessOptions(SharedNoteModel sharedNote) {
        String displayName = sharedNote.getSharedWithUsername();
        if (displayName == null) displayName = "this user";
        final String finalDisplayName = displayName;
        
        String currentPermission = sharedNote.getPermission();
        boolean isCurrentlyEdit = "edit".equalsIgnoreCase(currentPermission);
        
        String[] options = {
            isCurrentlyEdit ? "Change to View Only" : "Change to Edit Permission",
            "Remove Access"
        };
        
        new AlertDialog.Builder(this)
                .setTitle("Manage " + finalDisplayName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Change permission
                        String newPermission = isCurrentlyEdit ? "view" : "edit";
                        changeUserPermission(sharedNote, newPermission, finalDisplayName);
                    } else {
                        // Remove access
                        showRemoveAccessConfirmation(sharedNote);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void changeUserPermission(SharedNoteModel sharedNote, String newPermission, String displayName) {
        firebaseManager.updateSharePermission(noteId, sharedNote.getSharedWithUserId(), newPermission, success -> {
            runOnUiThread(() -> {
                if (success) {
                    // Log activity
                    firebaseManager.addActivityLog(noteId, sessionManager.getCurrentUserId(),
                            sessionManager.getCurrentUsername(), ActivityLogModel.ACTION_PERMISSION_CHANGED, 
                            "Changed " + displayName + "'s permission to " + newPermission.toUpperCase());
                    
                    Toast.makeText(this, displayName + "'s permission changed to " + newPermission.toUpperCase(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to change permission", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showRemoveAccessConfirmation(SharedNoteModel sharedNote) {
        String displayName = sharedNote.getSharedWithUsername();
        if (displayName == null) displayName = "this user";
        final String finalDisplayName = displayName;
        
        new AlertDialog.Builder(this)
                .setTitle("Remove Access")
                .setMessage("Remove access for " + finalDisplayName + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    firebaseManager.unshareNote(noteId, sharedNote.getSharedWithUserId(), result -> {
                        runOnUiThread(() -> {
                            if (result) {
                                // Log activity
                                firebaseManager.addActivityLog(noteId, sessionManager.getCurrentUserId(),
                                        sessionManager.getCurrentUsername(), ActivityLogModel.ACTION_UNSHARED, 
                                        "Removed access for " + finalDisplayName);
                                
                                Toast.makeText(this, "Access removed for " + finalDisplayName, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to remove access", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteConfirmation() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete this note? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firebaseManager.deleteNote(noteId, result -> {
                        runOnUiThread(() -> {
                            if (result) {
                                Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
