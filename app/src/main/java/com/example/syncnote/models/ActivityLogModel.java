package com.example.syncnote.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class ActivityLogModel {
    
    public static final String ACTION_CREATED = "created";
    public static final String ACTION_EDITED = "edited";
    public static final String ACTION_SHARED = "shared";
    public static final String ACTION_UNSHARED = "unshared";
    public static final String ACTION_PERMISSION_CHANGED = "permission_changed";
    public static final String ACTION_PINNED = "pinned";
    public static final String ACTION_UNPINNED = "unpinned";
    public static final String ACTION_COLOR_CHANGED = "color_changed";
    public static final String ACTION_CATEGORY_CHANGED = "category_changed";
    
    private String id;
    private String noteId;
    private String userId;
    private String username;
    private String action;
    private String details;  // Additional details like "Shared with @john (edit permission)"
    private long timestamp;
    
    public ActivityLogModel() {
        this.timestamp = System.currentTimeMillis();
    }
    
    public ActivityLogModel(String noteId, String userId, String username, String action, String details) {
        this.noteId = noteId;
        this.userId = userId;
        this.username = username;
        this.action = action;
        this.details = details;
        this.timestamp = System.currentTimeMillis();
    }
    
    // Getters and Setters
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getNoteId() {
        return noteId;
    }
    
    public void setNoteId(String noteId) {
        this.noteId = noteId;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public void setUserId(String userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("noteId", noteId);
        result.put("userId", userId);
        result.put("username", username);
        result.put("action", action);
        result.put("details", details);
        result.put("timestamp", timestamp);
        return result;
    }
    
    @Exclude
    public String getActionDisplayText() {
        switch (action) {
            case ACTION_CREATED:
                return "created this note";
            case ACTION_EDITED:
                return "edited this note";
            case ACTION_SHARED:
                return details != null ? details : "shared this note";
            case ACTION_UNSHARED:
                return details != null ? details : "removed access";
            case ACTION_PERMISSION_CHANGED:
                return details != null ? details : "changed permissions";
            case ACTION_PINNED:
                return "pinned this note";
            case ACTION_UNPINNED:
                return "unpinned this note";
            case ACTION_COLOR_CHANGED:
                return "changed note color";
            case ACTION_CATEGORY_CHANGED:
                return details != null ? details : "changed category";
            default:
                return action;
        }
    }
}
