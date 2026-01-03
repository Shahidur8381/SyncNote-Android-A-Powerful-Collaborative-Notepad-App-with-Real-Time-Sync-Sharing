package com.example.syncnote.firebase;

import androidx.annotation.NonNull;

import com.example.syncnote.models.ActivityLogModel;
import com.example.syncnote.models.CategoryModel;
import com.example.syncnote.models.NoteModel;
import com.example.syncnote.models.SharedNoteModel;
import com.example.syncnote.models.UserModel;
import com.example.syncnote.utils.PasswordUtils;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class FirebaseManager {
    private static FirebaseManager instance;
    private DatabaseReference database;
    private static boolean offlineEnabled = false;

    private static final String USERS_REF = "users";
    private static final String NOTES_REF = "notes";
    private static final String SHARED_NOTES_REF = "shared_notes";
    private static final String USERNAMES_REF = "usernames";
    private static final String EMAILS_REF = "emails";
    private static final String CATEGORIES_REF = "categories";
    private static final String ACTIVITY_LOGS_REF = "activity_logs";
    private static final String SHARE_LINKS_REF = "share_links";

    private FirebaseManager() {
        FirebaseDatabase firebaseDatabase = FirebaseDatabase.getInstance();
        
        // Enable offline persistence (only once)
        if (!offlineEnabled) {
            firebaseDatabase.setPersistenceEnabled(true);
            offlineEnabled = true;
        }
        
        database = firebaseDatabase.getReference();
        
        // Keep important data synced
        database.child(NOTES_REF).keepSynced(true);
        database.child(CATEGORIES_REF).keepSynced(true);
    }

    public static synchronized FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    // ===================== USER OPERATIONS =====================

    public interface AuthCallback {
        void onSuccess(UserModel user);
        void onError(String error);
    }

    public interface RegisterCallback {
        void onSuccess(String userId);
        void onError(String error);
    }

    public interface BooleanCallback {
        void onResult(boolean result);
    }
    
    public interface SaveCallback {
        void onSuccess(String id);
        void onError(String error);
    }
    
    public interface OperationCallback {
        void onResult(boolean success);
    }

    public interface UserCallback {
        void onSuccess(UserModel user);
        void onError(String error);
    }

    public void registerUser(String username, String email, String password,
                            String securityQuestion, String securityAnswer,
                            RegisterCallback callback) {
        // First check if username exists
        checkUsernameExists(username, usernameExists -> {
            if (usernameExists) {
                callback.onError("Username already exists");
                return;
            }

            // Check if email exists
            checkEmailExists(email, emailExists -> {
                if (emailExists) {
                    callback.onError("Email already registered");
                    return;
                }

                // Create the user
                String userId = database.child(USERS_REF).push().getKey();
                if (userId == null) {
                    callback.onError("Failed to create user");
                    return;
                }

                UserModel user = new UserModel();
                user.setId(userId);
                user.setUsername(username.trim().toLowerCase());
                user.setEmail(email.trim().toLowerCase());
                user.setPasswordHash(PasswordUtils.hashPassword(password));
                user.setSecurityQuestion(securityQuestion);
                user.setSecurityAnswerHash(PasswordUtils.hashPassword(securityAnswer.toLowerCase().trim()));
                user.setCreatedAt(System.currentTimeMillis());

                // Create atomic update
                Map<String, Object> updates = new HashMap<>();
                updates.put("/" + USERS_REF + "/" + userId, user.toMap());
                updates.put("/" + USERNAMES_REF + "/" + username.trim().toLowerCase(), userId);
                updates.put("/" + EMAILS_REF + "/" + email.trim().toLowerCase().replace(".", ","), userId);

                database.updateChildren(updates)
                        .addOnSuccessListener(aVoid -> callback.onSuccess(userId))
                        .addOnFailureListener(e -> callback.onError(e.getMessage()));
            });
        });
    }

    public void authenticateUser(String username, String password, AuthCallback callback) {
        String normalizedUsername = username.trim().toLowerCase();
        
        database.child(USERNAMES_REF).child(normalizedUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onError("Invalid username or password");
                            return;
                        }

                        String userId = snapshot.getValue(String.class);
                        if (userId == null) {
                            callback.onError("Invalid username or password");
                            return;
                        }

                        // Get user data
                        database.child(USERS_REF).child(userId)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot userSnapshot) {
                                        UserModel user = userSnapshot.getValue(UserModel.class);
                                        if (user == null) {
                                            callback.onError("User not found");
                                            return;
                                        }

                                        // Verify password
                                        if (PasswordUtils.verifyPassword(password, user.getPasswordHash())) {
                                            // Update last login
                                            user.setLastLogin(System.currentTimeMillis());
                                            database.child(USERS_REF).child(userId).child("lastLogin")
                                                    .setValue(user.getLastLogin());
                                            callback.onSuccess(user);
                                        } else {
                                            callback.onError("Invalid username or password");
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {
                                        callback.onError(error.getMessage());
                                    }
                                });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void checkUsernameExists(String username, BooleanCallback callback) {
        String normalizedUsername = username.trim().toLowerCase();
        database.child(USERNAMES_REF).child(normalizedUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        callback.onResult(snapshot.exists());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(false);
                    }
                });
    }

    public void checkEmailExists(String email, BooleanCallback callback) {
        String normalizedEmail = email.trim().toLowerCase().replace(".", ",");
        database.child(EMAILS_REF).child(normalizedEmail)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        callback.onResult(snapshot.exists());
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(false);
                    }
                });
    }

    public void getUserByUsername(String username, UserCallback callback) {
        String normalizedUsername = username.trim().toLowerCase();
        database.child(USERNAMES_REF).child(normalizedUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onError("User not found");
                            return;
                        }

                        String userId = snapshot.getValue(String.class);
                        getUserById(userId, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void getUserById(String userId, UserCallback callback) {
        database.child(USERS_REF).child(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserModel user = snapshot.getValue(UserModel.class);
                        if (user != null) {
                            callback.onSuccess(user);
                        } else {
                            callback.onError("User not found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void verifySecurityAnswer(String username, String answer, BooleanCallback callback) {
        getUserByUsername(username, new UserCallback() {
            @Override
            public void onSuccess(UserModel user) {
                if (user.getSecurityAnswerHash() != null) {
                    boolean matches = PasswordUtils.verifyPassword(
                            answer.toLowerCase().trim(),
                            user.getSecurityAnswerHash()
                    );
                    callback.onResult(matches);
                } else {
                    callback.onResult(false);
                }
            }

            @Override
            public void onError(String error) {
                callback.onResult(false);
            }
        });
    }

    public void updatePassword(String username, String newPassword, BooleanCallback callback) {
        String normalizedUsername = username.trim().toLowerCase();
        database.child(USERNAMES_REF).child(normalizedUsername)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onResult(false);
                            return;
                        }

                        String userId = snapshot.getValue(String.class);
                        String newHash = PasswordUtils.hashPassword(newPassword);

                        database.child(USERS_REF).child(userId).child("passwordHash")
                                .setValue(newHash)
                                .addOnSuccessListener(aVoid -> callback.onResult(true))
                                .addOnFailureListener(e -> callback.onResult(false));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(false);
                    }
                });
    }

    // ===================== NOTE OPERATIONS =====================

    public interface NoteCallback {
        void onSuccess(NoteModel note);
        void onError(String error);
    }

    public interface NotesCallback {
        void onSuccess(List<NoteModel> notes);
        void onError(String error);
    }

    public interface SaveNoteCallback {
        void onSuccess(String noteId);
        void onError(String error);
    }

    public void saveNote(NoteModel note, SaveNoteCallback callback) {
        String noteId;
        if (note.getId() == null || note.getId().isEmpty()) {
            noteId = database.child(NOTES_REF).push().getKey();
            if (noteId == null) {
                callback.onError("Failed to create note");
                return;
            }
            note.setId(noteId);
        } else {
            noteId = note.getId();
        }

        note.setUpdatedAt(System.currentTimeMillis());

        database.child(NOTES_REF).child(noteId).setValue(note.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(noteId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getNotesForUser(String userId, NotesCallback callback) {
        database.child(NOTES_REF).orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<NoteModel> notes = new ArrayList<>();
                        for (DataSnapshot noteSnapshot : snapshot.getChildren()) {
                            NoteModel note = noteSnapshot.getValue(NoteModel.class);
                            if (note != null) {
                                // IMPORTANT: Set the ID from the snapshot key
                                note.setId(noteSnapshot.getKey());
                                notes.add(note);
                            }
                        }
                        // Sort by updatedAt descending
                        notes.sort((n1, n2) -> Long.compare(n2.getUpdatedAt(), n1.getUpdatedAt()));
                        callback.onSuccess(notes);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void getNoteById(String noteId, NoteCallback callback) {
        database.child(NOTES_REF).child(noteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        NoteModel note = snapshot.getValue(NoteModel.class);
                        if (note != null) {
                            // IMPORTANT: Set the ID from the snapshot key
                            note.setId(snapshot.getKey());
                            callback.onSuccess(note);
                        } else {
                            callback.onError("Note not found");
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void updateNote(NoteModel note, String updatedByUserId, String updatedByUsername, BooleanCallback callback) {
        note.setUpdatedAt(System.currentTimeMillis());
        note.setLastUpdatedBy(updatedByUserId);
        note.setLastUpdatedByUsername(updatedByUsername);

        database.child(NOTES_REF).child(note.getId()).setValue(note.toMap())
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public void deleteNote(String noteId, BooleanCallback callback) {
        // Delete the note and all its shares
        Map<String, Object> updates = new HashMap<>();
        updates.put("/" + NOTES_REF + "/" + noteId, null);

        // Delete shared notes entries
        database.child(SHARED_NOTES_REF).orderByChild("noteId").equalTo(noteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot shareSnapshot : snapshot.getChildren()) {
                            updates.put("/" + SHARED_NOTES_REF + "/" + shareSnapshot.getKey(), null);
                        }

                        database.updateChildren(updates)
                                .addOnSuccessListener(aVoid -> callback.onResult(true))
                                .addOnFailureListener(e -> callback.onResult(false));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(false);
                    }
                });
    }

    // ===================== SHARING OPERATIONS =====================

    public interface SharedNotesCallback {
        void onSuccess(List<SharedNoteModel> sharedNotes);
        void onError(String error);
    }

    public void shareNote(String noteId, String ownerId, String sharedWithUserId, 
                         String permission, BooleanCallback callback) {
        // Check if already shared
        database.child(SHARED_NOTES_REF).orderByChild("noteId").equalTo(noteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        String existingShareId = null;
                        for (DataSnapshot shareSnapshot : snapshot.getChildren()) {
                            SharedNoteModel existing = shareSnapshot.getValue(SharedNoteModel.class);
                            if (existing != null && sharedWithUserId.equals(existing.getSharedWithUserId())) {
                                existingShareId = shareSnapshot.getKey();
                                break;
                            }
                        }

                        String shareId = existingShareId != null ? existingShareId : 
                                database.child(SHARED_NOTES_REF).push().getKey();
                        
                        if (shareId == null) {
                            callback.onResult(false);
                            return;
                        }

                        SharedNoteModel sharedNote = new SharedNoteModel();
                        sharedNote.setId(shareId);
                        sharedNote.setNoteId(noteId);
                        sharedNote.setOwnerId(ownerId);
                        sharedNote.setSharedWithUserId(sharedWithUserId);
                        sharedNote.setPermission(permission);
                        sharedNote.setSharedAt(System.currentTimeMillis());

                        database.child(SHARED_NOTES_REF).child(shareId).setValue(sharedNote.toMap())
                                .addOnSuccessListener(aVoid -> callback.onResult(true))
                                .addOnFailureListener(e -> callback.onResult(false));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(false);
                    }
                });
    }

    public void getSharedNotesForUser(String userId, SharedNotesCallback callback) {
        database.child(SHARED_NOTES_REF).orderByChild("sharedWithUserId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<SharedNoteModel> sharedNotes = new ArrayList<>();
                        int[] pending = {(int) snapshot.getChildrenCount()};
                        
                        if (pending[0] == 0) {
                            callback.onSuccess(sharedNotes);
                            return;
                        }

                        for (DataSnapshot shareSnapshot : snapshot.getChildren()) {
                            SharedNoteModel sharedNote = shareSnapshot.getValue(SharedNoteModel.class);
                            if (sharedNote != null) {
                                // Get note details
                                getNoteById(sharedNote.getNoteId(), new NoteCallback() {
                                    @Override
                                    public void onSuccess(NoteModel note) {
                                        sharedNote.setNoteTitle(note.getTitle());
                                        sharedNote.setNoteContent(note.getContent());
                                        
                                        // Get owner username
                                        getUserById(sharedNote.getOwnerId(), new UserCallback() {
                                            @Override
                                            public void onSuccess(UserModel user) {
                                                sharedNote.setOwnerUsername(user.getUsername());
                                                sharedNotes.add(sharedNote);
                                                pending[0]--;
                                                if (pending[0] == 0) {
                                                    sharedNotes.sort((s1, s2) -> 
                                                            Long.compare(s2.getSharedAt(), s1.getSharedAt()));
                                                    callback.onSuccess(sharedNotes);
                                                }
                                            }

                                            @Override
                                            public void onError(String error) {
                                                pending[0]--;
                                                if (pending[0] == 0) {
                                                    callback.onSuccess(sharedNotes);
                                                }
                                            }
                                        });
                                    }

                                    @Override
                                    public void onError(String error) {
                                        pending[0]--;
                                        if (pending[0] == 0) {
                                            callback.onSuccess(sharedNotes);
                                        }
                                    }
                                });
                            } else {
                                pending[0]--;
                                if (pending[0] == 0) {
                                    callback.onSuccess(sharedNotes);
                                }
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void getSharedUsersForNote(String noteId, SharedNotesCallback callback) {
        // Fetch all shared_notes and filter by noteId in code (avoids index requirement)
        database.child(SHARED_NOTES_REF).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<SharedNoteModel> matchingShares = new ArrayList<>();
                
                // First pass: find all shares for this noteId
                for (DataSnapshot shareSnapshot : snapshot.getChildren()) {
                    SharedNoteModel sharedNote = shareSnapshot.getValue(SharedNoteModel.class);
                    if (sharedNote != null && noteId.equals(sharedNote.getNoteId())) {
                        sharedNote.setId(shareSnapshot.getKey());
                        matchingShares.add(sharedNote);
                    }
                }
                
                if (matchingShares.isEmpty()) {
                    callback.onSuccess(new ArrayList<>());
                    return;
                }
                
                // Second pass: fetch usernames for each share
                List<SharedNoteModel> sharedUsers = new ArrayList<>();
                int[] pending = {matchingShares.size()};
                
                for (SharedNoteModel sharedNote : matchingShares) {
                    String sharedWithUserId = sharedNote.getSharedWithUserId();
                    if (sharedWithUserId == null || sharedWithUserId.isEmpty()) {
                        sharedNote.setSharedWithUsername("Unknown User");
                        sharedUsers.add(sharedNote);
                        pending[0]--;
                        if (pending[0] == 0) callback.onSuccess(sharedUsers);
                        continue;
                    }
                    
                    getUserById(sharedWithUserId, new UserCallback() {
                        @Override
                        public void onSuccess(UserModel user) {
                            sharedNote.setSharedWithUsername(user.getUsername());
                            sharedUsers.add(sharedNote);
                            pending[0]--;
                            if (pending[0] == 0) callback.onSuccess(sharedUsers);
                        }

                        @Override
                        public void onError(String error) {
                            sharedNote.setSharedWithUsername("Unknown User");
                            sharedUsers.add(sharedNote);
                            pending[0]--;
                            if (pending[0] == 0) callback.onSuccess(sharedUsers);
                        }
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
                    }
                });
    }

    public void getSharePermission(String noteId, String userId, 
                                   com.google.android.gms.tasks.OnSuccessListener<SharedNoteModel> onSuccess,
                                   com.google.android.gms.tasks.OnFailureListener onFailure) {
        database.child(SHARED_NOTES_REF).orderByChild("noteId").equalTo(noteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot shareSnapshot : snapshot.getChildren()) {
                            SharedNoteModel sharedNote = shareSnapshot.getValue(SharedNoteModel.class);
                            if (sharedNote != null && userId.equals(sharedNote.getSharedWithUserId())) {
                                onSuccess.onSuccess(sharedNote);
                                return;
                            }
                        }
                        onSuccess.onSuccess(null);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        onFailure.onFailure(error.toException());
                    }
                });
    }

    public void unshareNote(String noteId, String sharedWithUserId, BooleanCallback callback) {
        database.child(SHARED_NOTES_REF).orderByChild("noteId").equalTo(noteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot shareSnapshot : snapshot.getChildren()) {
                            SharedNoteModel sharedNote = shareSnapshot.getValue(SharedNoteModel.class);
                            if (sharedNote != null && sharedWithUserId.equals(sharedNote.getSharedWithUserId())) {
                                shareSnapshot.getRef().removeValue()
                                        .addOnSuccessListener(aVoid -> callback.onResult(true))
                                        .addOnFailureListener(e -> callback.onResult(false));
                                return;
                            }
                        }
                        callback.onResult(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(false);
                    }
                });
    }
    
    public void updateSharePermission(String noteId, String sharedWithUserId, String newPermission, BooleanCallback callback) {
        database.child(SHARED_NOTES_REF).orderByChild("noteId").equalTo(noteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot shareSnapshot : snapshot.getChildren()) {
                            SharedNoteModel sharedNote = shareSnapshot.getValue(SharedNoteModel.class);
                            if (sharedNote != null && sharedWithUserId.equals(sharedNote.getSharedWithUserId())) {
                                shareSnapshot.getRef().child("permission").setValue(newPermission)
                                        .addOnSuccessListener(aVoid -> callback.onResult(true))
                                        .addOnFailureListener(e -> callback.onResult(false));
                                return;
                            }
                        }
                        callback.onResult(false);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onResult(false);
                    }
                });
    }

    // ===================== CATEGORY OPERATIONS =====================

    public interface CategoriesCallback {
        void onSuccess(List<CategoryModel> categories);
        void onError(String error);
    }

    public interface CategoryCallback {
        void onSuccess(CategoryModel category);
        void onError(String error);
    }

    public void createCategory(String userId, String name, String color, CategoryCallback callback) {
        String categoryId = database.child(CATEGORIES_REF).push().getKey();
        if (categoryId == null) {
            callback.onError("Failed to create category");
            return;
        }

        CategoryModel category = new CategoryModel(userId, name, color);
        category.setId(categoryId);

        database.child(CATEGORIES_REF).child(categoryId).setValue(category.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(category))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }
    
    public void createCategory(CategoryModel category, SaveCallback callback) {
        String categoryId = database.child(CATEGORIES_REF).push().getKey();
        if (categoryId == null) {
            callback.onError("Failed to create category");
            return;
        }

        category.setId(categoryId);

        database.child(CATEGORIES_REF).child(categoryId).setValue(category.toMap())
                .addOnSuccessListener(aVoid -> callback.onSuccess(categoryId))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getCategoriesForUser(String userId, CategoriesCallback callback) {
        database.child(CATEGORIES_REF).orderByChild("userId").equalTo(userId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<CategoryModel> categories = new ArrayList<>();
                        for (DataSnapshot catSnapshot : snapshot.getChildren()) {
                            CategoryModel category = catSnapshot.getValue(CategoryModel.class);
                            if (category != null) {
                                categories.add(category);
                            }
                        }
                        callback.onSuccess(categories);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void updateCategory(CategoryModel category, BooleanCallback callback) {
        database.child(CATEGORIES_REF).child(category.getId()).setValue(category.toMap())
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public void deleteCategory(String categoryId, BooleanCallback callback) {
        database.child(CATEGORIES_REF).child(categoryId).removeValue()
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // ===================== ACTIVITY LOG OPERATIONS =====================

    public interface ActivityLogsCallback {
        void onSuccess(List<ActivityLogModel> logs);
        void onError(String error);
    }

    public void addActivityLog(String noteId, String userId, String username, 
                               String action, String details) {
        String logId = database.child(ACTIVITY_LOGS_REF).push().getKey();
        if (logId == null) return;

        ActivityLogModel log = new ActivityLogModel(noteId, userId, username, action, details);
        log.setId(logId);

        database.child(ACTIVITY_LOGS_REF).child(logId).setValue(log.toMap());
    }

    public void getActivityLogsForNote(String noteId, ActivityLogsCallback callback) {
        database.child(ACTIVITY_LOGS_REF).orderByChild("noteId").equalTo(noteId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        List<ActivityLogModel> logs = new ArrayList<>();
                        for (DataSnapshot logSnapshot : snapshot.getChildren()) {
                            ActivityLogModel log = logSnapshot.getValue(ActivityLogModel.class);
                            if (log != null) {
                                logs.add(log);
                            }
                        }
                        // Sort by timestamp descending (newest first)
                        logs.sort((l1, l2) -> Long.compare(l2.getTimestamp(), l1.getTimestamp()));
                        callback.onSuccess(logs);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    // ===================== SHARE LINK OPERATIONS =====================

    public interface ShareLinkCallback {
        void onSuccess(String shareLink);
        void onError(String error);
    }

    public void generateShareLink(String noteId, String permission, ShareLinkCallback callback) {
        // Generate a unique share link code
        String linkCode = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        
        Map<String, Object> linkData = new HashMap<>();
        linkData.put("noteId", noteId);
        linkData.put("permission", permission);
        linkData.put("createdAt", System.currentTimeMillis());
        linkData.put("active", true);

        database.child(SHARE_LINKS_REF).child(linkCode).setValue(linkData)
                .addOnSuccessListener(aVoid -> {
                    // Also update the note with the share link
                    database.child(NOTES_REF).child(noteId).child("shareLink").setValue(linkCode);
                    callback.onSuccess(linkCode);
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getNoteByShareLink(String linkCode, NoteCallback callback) {
        database.child(SHARE_LINKS_REF).child(linkCode)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!snapshot.exists()) {
                            callback.onError("Invalid share link");
                            return;
                        }

                        Boolean active = snapshot.child("active").getValue(Boolean.class);
                        if (active == null || !active) {
                            callback.onError("Share link has expired");
                            return;
                        }

                        String noteId = snapshot.child("noteId").getValue(String.class);
                        if (noteId == null) {
                            callback.onError("Invalid share link");
                            return;
                        }

                        getNoteById(noteId, callback);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        callback.onError(error.getMessage());
                    }
                });
    }

    public void deactivateShareLink(String linkCode, BooleanCallback callback) {
        database.child(SHARE_LINKS_REF).child(linkCode).child("active").setValue(false)
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // ===================== PIN AND COLOR OPERATIONS =====================

    public void toggleNotePin(String noteId, boolean isPinned, BooleanCallback callback) {
        database.child(NOTES_REF).child(noteId).child("isPinned").setValue(isPinned)
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public void updateNoteColor(String noteId, String color, BooleanCallback callback) {
        database.child(NOTES_REF).child(noteId).child("color").setValue(color)
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    public void updateNoteCategory(String noteId, String category, BooleanCallback callback) {
        database.child(NOTES_REF).child(noteId).child("category").setValue(category)
                .addOnSuccessListener(aVoid -> callback.onResult(true))
                .addOnFailureListener(e -> callback.onResult(false));
    }

    // ===================== PASSWORD CHANGE =====================

    public void changePassword(String userId, String currentPassword, String newPassword, BooleanCallback callback) {
        getUserById(userId, new UserCallback() {
            @Override
            public void onSuccess(UserModel user) {
                // Verify current password
                if (!PasswordUtils.verifyPassword(currentPassword, user.getPasswordHash())) {
                    callback.onResult(false);
                    return;
                }

                // Update to new password
                String newHash = PasswordUtils.hashPassword(newPassword);
                database.child(USERS_REF).child(userId).child("passwordHash")
                        .setValue(newHash)
                        .addOnSuccessListener(aVoid -> callback.onResult(true))
                        .addOnFailureListener(e -> callback.onResult(false));
            }

            @Override
            public void onError(String error) {
                callback.onResult(false);
            }
        });
    }

    // ===================== REAL-TIME LISTENERS =====================

    public void addNotesListener(String userId, ValueEventListener listener) {
        database.child(NOTES_REF).orderByChild("userId").equalTo(userId)
                .addValueEventListener(listener);
    }

    public void removeNotesListener(String userId, ValueEventListener listener) {
        database.child(NOTES_REF).orderByChild("userId").equalTo(userId)
                .removeEventListener(listener);
    }
}
