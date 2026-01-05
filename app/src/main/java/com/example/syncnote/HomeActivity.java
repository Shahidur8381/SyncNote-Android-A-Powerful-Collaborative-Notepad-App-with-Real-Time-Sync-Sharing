package com.example.syncnote;

import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncnote.adapters.NotesAdapter;
import com.example.syncnote.adapters.SharedNotesAdapter;
import com.example.syncnote.firebase.FirebaseManager;
import com.example.syncnote.models.ActivityLogModel;
import com.example.syncnote.models.NoteModel;
import com.example.syncnote.models.SharedNoteModel;
import com.example.syncnote.utils.SessionManager;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class HomeActivity extends AppCompatActivity implements
        NotesAdapter.OnNoteClickListener, SharedNotesAdapter.OnSharedNoteClickListener {
    
    // Sort options
    public static final int SORT_DATE_MODIFIED = 0;
    public static final int SORT_DATE_CREATED = 1;
    public static final int SORT_TITLE_AZ = 2;
    public static final int SORT_TITLE_ZA = 3;
    
    private int currentSortOption = SORT_DATE_MODIFIED;
    
    // Predefined colors for notes
    private final String[] NOTE_COLORS = {
            "#FFFFFF", "#FFCDD2", "#F8BBD9", "#E1BEE7", "#D1C4E9",
            "#C5CAE9", "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB",
            "#C8E6C9", "#DCEDC8", "#F0F4C3", "#FFF9C4", "#FFECB3",
            "#FFE0B2", "#FFCCBC", "#D7CCC8", "#CFD8DC"
    };

    private RecyclerView notesRecyclerView;
    private LinearLayout emptyState;
    private TextView emptyStateTitle, emptyStateSubtitle;
    private ProgressBar progressBar;
    private EditText searchInput;
    private ExtendedFloatingActionButton fabNewNote;
    private TabLayout tabLayout;

    private NotesAdapter notesAdapter;
    private SharedNotesAdapter sharedNotesAdapter;
    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    private List<NoteModel> allNotes = new ArrayList<>();
    private List<SharedNoteModel> allSharedNotes = new ArrayList<>();
    private boolean isShowingMyNotes = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        firebaseManager = FirebaseManager.getInstance();
        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupTabs();
        setupFab();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void initViews() {
        notesRecyclerView = findViewById(R.id.notesRecyclerView);
        emptyState = findViewById(R.id.emptyState);
        emptyStateTitle = findViewById(R.id.emptyStateTitle);
        emptyStateSubtitle = findViewById(R.id.emptyStateSubtitle);
        progressBar = findViewById(R.id.progressBar);
        searchInput = findViewById(R.id.searchInput);
        fabNewNote = findViewById(R.id.fabNewNote);
        tabLayout = findViewById(R.id.tabLayout);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Notes");
        }
    }

    private void setupRecyclerView() {
        notesAdapter = new NotesAdapter(this, this);
        sharedNotesAdapter = new SharedNotesAdapter(this, this);
        notesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notesRecyclerView.setAdapter(notesAdapter);
        
        // Setup swipe actions
        setupSwipeActions();
    }
    
    private void setupSwipeActions() {
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, 
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, 
                    @NonNull RecyclerView.ViewHolder viewHolder, 
                    @NonNull RecyclerView.ViewHolder target) {
                return false;
            }
            
            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                
                if (isShowingMyNotes && position < notesAdapter.getItemCount()) {
                    NoteModel note = notesAdapter.getNoteAtPosition(position);
                    if (note != null) {
                        if (direction == ItemTouchHelper.LEFT) {
                            // Swipe left - Delete
                            showDeleteConfirmation(note);
                            notesAdapter.notifyItemChanged(position);
                        } else if (direction == ItemTouchHelper.RIGHT) {
                            // Swipe right - Share
                            showShareDialog(note);
                            notesAdapter.notifyItemChanged(position);
                        }
                    }
                }
            }
            
            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, 
                    @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, 
                    int actionState, boolean isCurrentlyActive) {
                
                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, 
                        actionState, isCurrentlyActive)
                        .addSwipeLeftBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.error))
                        .addSwipeLeftActionIcon(R.drawable.ic_delete)
                        .addSwipeRightBackgroundColor(ContextCompat.getColor(HomeActivity.this, R.color.primary))
                        .addSwipeRightActionIcon(R.drawable.ic_share)
                        .setActionIconTint(Color.WHITE)
                        .create()
                        .decorate();
                
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
            
            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, 
                    @NonNull RecyclerView.ViewHolder viewHolder) {
                // Only allow swiping on My Notes tab
                if (!isShowingMyNotes) {
                    return 0;
                }
                return super.getSwipeDirs(recyclerView, viewHolder);
            }
        };
        
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(notesRecyclerView);
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterNotes(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                isShowingMyNotes = tab.getPosition() == 0;
                updateUI();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupFab() {
        fabNewNote.setOnClickListener(v -> {
            Intent intent = new Intent(this, NoteEditorActivity.class);
            startActivity(intent);
        });
    }

    private void loadData() {
        showLoading(true);

        String userId = sessionManager.getCurrentUserId();

        // Load user's own notes
        firebaseManager.getNotesForUser(userId, new FirebaseManager.NotesCallback() {
            @Override
            public void onSuccess(List<NoteModel> notes) {
                runOnUiThread(() -> {
                    allNotes = notes;
                    if (isShowingMyNotes) {
                        updateUI();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(HomeActivity.this, "Error loading notes: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });

        // Load shared notes
        firebaseManager.getSharedNotesForUser(userId, new FirebaseManager.SharedNotesCallback() {
            @Override
            public void onSuccess(List<SharedNoteModel> sharedNotes) {
                runOnUiThread(() -> {
                    showLoading(false);
                    allSharedNotes = sharedNotes;
                    if (!isShowingMyNotes) {
                        updateUI();
                    }
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                });
            }
        });
    }

    private void updateUI() {
        if (isShowingMyNotes) {
            fabNewNote.setVisibility(View.VISIBLE);
            notesRecyclerView.setAdapter(notesAdapter);
            
            // Sort and display notes (pinned first)
            List<NoteModel> sortedNotes = sortNotes(allNotes);
            notesAdapter.setNotes(sortedNotes);

            if (allNotes.isEmpty()) {
                showEmptyState("No notes yet", "Tap the + button to create your first note");
            } else {
                emptyState.setVisibility(View.GONE);
                notesRecyclerView.setVisibility(View.VISIBLE);
            }
        } else {
            fabNewNote.setVisibility(View.GONE);
            notesRecyclerView.setAdapter(sharedNotesAdapter);
            sharedNotesAdapter.setSharedNotes(allSharedNotes);

            if (allSharedNotes.isEmpty()) {
                showEmptyState("No shared notes", "Notes shared with you will appear here");
            } else {
                emptyState.setVisibility(View.GONE);
                notesRecyclerView.setVisibility(View.VISIBLE);
            }
        }
    }
    
    private List<NoteModel> sortNotes(List<NoteModel> notes) {
        List<NoteModel> sortedNotes = new ArrayList<>(notes);
        
        // First separate pinned and unpinned notes
        List<NoteModel> pinnedNotes = new ArrayList<>();
        List<NoteModel> unpinnedNotes = new ArrayList<>();
        
        for (NoteModel note : sortedNotes) {
            if (note.isPinned()) {
                pinnedNotes.add(note);
            } else {
                unpinnedNotes.add(note);
            }
        }
        
        // Sort each list based on current sort option
        Comparator<NoteModel> comparator = getComparator();
        Collections.sort(pinnedNotes, comparator);
        Collections.sort(unpinnedNotes, comparator);
        
        // Combine: pinned first, then unpinned
        sortedNotes.clear();
        sortedNotes.addAll(pinnedNotes);
        sortedNotes.addAll(unpinnedNotes);
        
        return sortedNotes;
    }
    
    private Comparator<NoteModel> getComparator() {
        switch (currentSortOption) {
            case SORT_DATE_CREATED:
                return (n1, n2) -> Long.compare(n2.getCreatedAt(), n1.getCreatedAt());
            case SORT_TITLE_AZ:
                return (n1, n2) -> {
                    String t1 = n1.getTitle() != null ? n1.getTitle() : "";
                    String t2 = n2.getTitle() != null ? n2.getTitle() : "";
                    return t1.compareToIgnoreCase(t2);
                };
            case SORT_TITLE_ZA:
                return (n1, n2) -> {
                    String t1 = n1.getTitle() != null ? n1.getTitle() : "";
                    String t2 = n2.getTitle() != null ? n2.getTitle() : "";
                    return t2.compareToIgnoreCase(t1);
                };
            case SORT_DATE_MODIFIED:
            default:
                return (n1, n2) -> Long.compare(n2.getUpdatedAt(), n1.getUpdatedAt());
        }
    }
    
    private void showSortDialog() {
        String[] sortOptions = {
                "Date Modified (Newest First)",
                "Date Created (Newest First)",
                "Title (A-Z)",
                "Title (Z-A)"
        };
        
        new AlertDialog.Builder(this)
                .setTitle("Sort Notes")
                .setSingleChoiceItems(sortOptions, currentSortOption, (dialog, which) -> {
                    currentSortOption = which;
                    updateUI();
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterNotes(String query) {
        if (isShowingMyNotes) {
            List<NoteModel> filtered = new ArrayList<>();
            for (NoteModel note : allNotes) {
                if (note.getTitle().toLowerCase().contains(query.toLowerCase()) ||
                        note.getContent().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(note);
                }
            }
            notesAdapter.setNotes(filtered);
        } else {
            List<SharedNoteModel> filtered = new ArrayList<>();
            for (SharedNoteModel note : allSharedNotes) {
                if (note.getNoteTitle().toLowerCase().contains(query.toLowerCase()) ||
                        note.getNoteContent().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(note);
                }
            }
            sharedNotesAdapter.setSharedNotes(filtered);
        }
    }

    private void showEmptyState(String title, String subtitle) {
        emptyStateTitle.setText(title);
        emptyStateSubtitle.setText(subtitle);
        emptyState.setVisibility(View.VISIBLE);
        notesRecyclerView.setVisibility(View.GONE);
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    // NotesAdapter callbacks
    @Override
    public void onNoteClick(NoteModel note) {
        Intent intent = new Intent(this, NoteViewerActivity.class);
        intent.putExtra("noteId", note.getId());
        startActivity(intent);
    }

    @Override
    public void onEditClick(NoteModel note) {
        Intent intent = new Intent(this, NoteEditorActivity.class);
        intent.putExtra("noteId", note.getId());
        startActivity(intent);
    }

    @Override
    public void onShareClick(NoteModel note) {
        showShareDialog(note);
    }

    @Override
    public void onDeleteClick(NoteModel note) {
        showDeleteConfirmation(note);
    }
    
    @Override
    public void onManageAccessClick(NoteModel note) {
        showManageAccessDialog(note);
    }
    
    @Override
    public void onPinClick(NoteModel note) {
        togglePinNote(note);
    }
    
    @Override
    public void onColorClick(NoteModel note) {
        showColorPicker(note);
    }
    
    private void showManageAccessDialog(NoteModel note) {
        String noteId = note.getId();
        
        if (noteId == null || noteId.isEmpty()) {
            Toast.makeText(this, "Error: Note ID is missing", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Show progress dialog
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Loading shared users...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        firebaseManager.getSharedUsersForNote(noteId, new FirebaseManager.SharedNotesCallback() {
            @Override
            public void onSuccess(List<SharedNoteModel> sharedUsers) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    
                    if (sharedUsers.isEmpty()) {
                        Toast.makeText(HomeActivity.this,
                                "This note is not currently shared with anyone", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Build list items with usernames and permissions
                    CharSequence[] items = new CharSequence[sharedUsers.size()];
                    for (int i = 0; i < sharedUsers.size(); i++) {
                        SharedNoteModel shared = sharedUsers.get(i);
                        String username = shared.getSharedWithUsername();
                        String permission = shared.getPermission();
                        
                        if (username == null || username.isEmpty()) {
                            username = "User #" + (i + 1);
                        }
                        if (permission == null || permission.isEmpty()) {
                            permission = "view";
                        }
                        
                        items[i] = username + " (" + permission.toUpperCase() + ")";
                    }

                    new AlertDialog.Builder(HomeActivity.this)
                            .setTitle("Shared with " + sharedUsers.size() + " user(s)")
                            .setItems(items, (dialog, which) -> {
                                SharedNoteModel selected = sharedUsers.get(which);
                                showUserAccessOptions(note, selected);
                            })
                            .setNegativeButton("Close", null)
                            .show();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    progressDialog.dismiss();
                    Toast.makeText(HomeActivity.this,
                            "Error loading shared users: " + error, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void showUserAccessOptions(NoteModel note, SharedNoteModel sharedNote) {
        String username = sharedNote.getSharedWithUsername();
        if (username == null) username = "this user";
        final String displayName = username;
        
        String currentPermission = sharedNote.getPermission();
        boolean isCurrentlyEdit = "edit".equalsIgnoreCase(currentPermission);
        
        String[] options = {
            isCurrentlyEdit ? "Change to View Only" : "Change to Edit Permission",
            "Remove Access"
        };
        
        new AlertDialog.Builder(this)
                .setTitle("Manage " + displayName)
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        // Change permission
                        String newPermission = isCurrentlyEdit ? "view" : "edit";
                        changeUserPermission(note, sharedNote, newPermission, displayName);
                    } else {
                        // Remove access
                        showRemoveAccessConfirmation(note, sharedNote);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void changeUserPermission(NoteModel note, SharedNoteModel sharedNote, String newPermission, String displayName) {
        firebaseManager.updateSharePermission(note.getId(), sharedNote.getSharedWithUserId(), newPermission, success -> {
            runOnUiThread(() -> {
                if (success) {
                    // Log activity
                    firebaseManager.addActivityLog(note.getId(), sessionManager.getCurrentUserId(),
                            sessionManager.getCurrentUsername(), ActivityLogModel.ACTION_PERMISSION_CHANGED, 
                            "Changed " + displayName + "'s permission to " + newPermission.toUpperCase());
                    
                    Toast.makeText(this, displayName + "'s permission changed to " + newPermission.toUpperCase(), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Failed to change permission", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void showRemoveAccessConfirmation(NoteModel note, SharedNoteModel sharedNote) {
        String username = sharedNote.getSharedWithUsername();
        final String displayName = (username != null) ? username : "this user";
        
        new AlertDialog.Builder(this)
                .setTitle("Remove Access")
                .setMessage("Remove access for " + displayName + "?")
                .setPositiveButton("Remove", (dialog, which) -> {
                    String sharedUserId = sharedNote.getSharedWithUserId();
                    if (sharedUserId == null) {
                        Toast.makeText(this, "Error: User ID not found", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    firebaseManager.unshareNote(note.getId(), sharedUserId, result -> {
                        runOnUiThread(() -> {
                            if (result) {
                                // Log activity
                                firebaseManager.addActivityLog(note.getId(), sessionManager.getCurrentUserId(),
                                        sessionManager.getCurrentUsername(), ActivityLogModel.ACTION_UNSHARED, 
                                        "Removed access for " + displayName);
                                
                                Toast.makeText(this, "Access removed for " + displayName, Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "Failed to remove access", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void togglePinNote(NoteModel note) {
        boolean newPinState = !note.isPinned();
        firebaseManager.toggleNotePin(note.getId(), newPinState, success -> {
            runOnUiThread(() -> {
                if (success) {
                    note.setPinned(newPinState);
                    
                    // Log activity
                    firebaseManager.addActivityLog(note.getId(), sessionManager.getCurrentUserId(),
                            sessionManager.getCurrentUsername(), 
                            newPinState ? ActivityLogModel.ACTION_PINNED : ActivityLogModel.ACTION_UNPINNED, 
                            null);
                    
                    Toast.makeText(this, newPinState ? "Note pinned" : "Note unpinned", Toast.LENGTH_SHORT).show();
                    updateUI();
                } else {
                    Toast.makeText(this, "Failed to update pin status", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    private void showColorPicker(NoteModel note) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Note Color");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        // Create color grid
        LinearLayout row = null;
        for (int i = 0; i < NOTE_COLORS.length; i++) {
            if (i % 5 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setGravity(android.view.Gravity.CENTER);
                layout.addView(row);
            }

            View colorView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(Color.parseColor(NOTE_COLORS[i]));

            final String color = NOTE_COLORS[i];
            final AlertDialog[] dialog = new AlertDialog[1];
            colorView.setOnClickListener(v -> {
                firebaseManager.updateNoteColor(note.getId(), color, success -> {
                    runOnUiThread(() -> {
                        if (success) {
                            note.setColor(color);
                            
                            // Log activity
                            firebaseManager.addActivityLog(note.getId(), sessionManager.getCurrentUserId(),
                                    sessionManager.getCurrentUsername(), ActivityLogModel.ACTION_COLOR_CHANGED, 
                                    "Changed color to " + color);
                            
                            Toast.makeText(this, "Color updated", Toast.LENGTH_SHORT).show();
                            notesAdapter.notifyDataSetChanged();
                        } else {
                            Toast.makeText(this, "Failed to update color", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
                if (dialog[0] != null) {
                    dialog[0].dismiss();
                }
            });

            if (row != null) {
                row.addView(colorView);
            }
        }

        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    // SharedNotesAdapter callbacks
    @Override
    public void onSharedNoteClick(SharedNoteModel sharedNote) {
        Intent intent = new Intent(this, NoteViewerActivity.class);
        intent.putExtra("noteId", sharedNote.getNoteId());
        intent.putExtra("isShared", true);
        intent.putExtra("canEdit", sharedNote.canEdit());
        intent.putExtra("ownerUsername", sharedNote.getOwnerUsername());
        intent.putExtra("permission", sharedNote.getPermission());
        startActivity(intent);
    }

    private void showShareDialog(NoteModel note) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_share_note, null);
        EditText usernameInput = dialogView.findViewById(R.id.usernameInput);
        
        // Get RadioGroup for permission
        android.widget.RadioGroup permissionGroup = dialogView.findViewById(R.id.permissionGroup);

        new AlertDialog.Builder(this)
                .setTitle("Share Note")
                .setMessage("Share \"" + note.getTitle() + "\" with another user")
                .setView(dialogView)
                .setPositiveButton("Share", (dialog, which) -> {
                    String username = usernameInput.getText().toString().trim();
                    if (username.isEmpty()) {
                        Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String permission = permissionGroup.getCheckedRadioButtonId() == R.id.radioEdit ? "edit" : "view";
                    shareNoteWithUser(note, username, permission);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void shareNoteWithUser(NoteModel note, String username, String permission) {
        // First check if trying to share with self
        if (username.equalsIgnoreCase(sessionManager.getCurrentUsername())) {
            Toast.makeText(this, "You cannot share a note with yourself", Toast.LENGTH_SHORT).show();
            return;
        }

        firebaseManager.getUserByUsername(username, new FirebaseManager.UserCallback() {
            @Override
            public void onSuccess(com.example.syncnote.models.UserModel user) {
                firebaseManager.shareNote(note.getId(), sessionManager.getCurrentUserId(),
                        user.getId(), permission, result -> {
                            runOnUiThread(() -> {
                                if (result) {
                                    Toast.makeText(HomeActivity.this,
                                            "Note shared with " + username + " (" + permission + " permission)",
                                            Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(HomeActivity.this,
                                            "Failed to share note", Toast.LENGTH_SHORT).show();
                                }
                            });
                        });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(HomeActivity.this,
                            "User '" + username + "' not found", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showDeleteConfirmation(NoteModel note) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Note")
                .setMessage("Are you sure you want to delete \"" + note.getTitle() + "\"? This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteNote(note))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteNote(NoteModel note) {
        firebaseManager.deleteNote(note.getId(), result -> {
            runOnUiThread(() -> {
                if (result) {
                    Toast.makeText(this, "Note deleted", Toast.LENGTH_SHORT).show();
                    loadData();
                } else {
                    Toast.makeText(this, "Failed to delete note", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_logout) {
            logout();
            return true;
        } else if (itemId == R.id.action_sort) {
            showSortDialog();
            return true;
        } else if (itemId == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle("Logout")
                .setMessage("Are you sure you want to logout?")
                .setPositiveButton("Logout", (dialog, which) -> {
                    sessionManager.logout();
                    Intent intent = new Intent(this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
