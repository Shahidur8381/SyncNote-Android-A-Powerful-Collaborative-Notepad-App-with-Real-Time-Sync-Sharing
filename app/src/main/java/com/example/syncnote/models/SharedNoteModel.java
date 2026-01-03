package com.example.syncnote.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class SharedNoteModel {
    private String id;
    private String noteId;
    private String ownerId;
    private String sharedWithUserId;
    private String permission; // "view" or "edit"
    private long sharedAt;

    // Additional fields for display purposes
    private String noteTitle;
    private String noteContent;
    private String ownerUsername;
    private String sharedWithUsername;

    public SharedNoteModel() {
        this.sharedAt = System.currentTimeMillis();
    }

    public SharedNoteModel(String noteId, String ownerId, String sharedWithUserId, String permission) {
        this.noteId = noteId;
        this.ownerId = ownerId;
        this.sharedWithUserId = sharedWithUserId;
        this.permission = permission;
        this.sharedAt = System.currentTimeMillis();
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

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public String getSharedWithUserId() {
        return sharedWithUserId;
    }

    public void setSharedWithUserId(String sharedWithUserId) {
        this.sharedWithUserId = sharedWithUserId;
    }

    public String getPermission() {
        return permission;
    }

    public void setPermission(String permission) {
        this.permission = permission;
    }

    public long getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(long sharedAt) {
        this.sharedAt = sharedAt;
    }

    public String getNoteTitle() {
        return noteTitle;
    }

    public void setNoteTitle(String noteTitle) {
        this.noteTitle = noteTitle;
    }

    public String getNoteContent() {
        return noteContent;
    }

    public void setNoteContent(String noteContent) {
        this.noteContent = noteContent;
    }

    public String getOwnerUsername() {
        return ownerUsername;
    }

    public void setOwnerUsername(String ownerUsername) {
        this.ownerUsername = ownerUsername;
    }

    public String getSharedWithUsername() {
        return sharedWithUsername;
    }

    public void setSharedWithUsername(String sharedWithUsername) {
        this.sharedWithUsername = sharedWithUsername;
    }

    @Exclude
    public boolean canEdit() {
        return "edit".equalsIgnoreCase(permission);
    }

    @Exclude
    public boolean canView() {
        return "view".equalsIgnoreCase(permission) || "edit".equalsIgnoreCase(permission);
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("noteId", noteId);
        result.put("ownerId", ownerId);
        result.put("sharedWithUserId", sharedWithUserId);
        result.put("permission", permission);
        result.put("sharedAt", sharedAt);
        return result;
    }
}
