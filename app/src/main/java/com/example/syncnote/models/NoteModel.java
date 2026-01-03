package com.example.syncnote.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@IgnoreExtraProperties
public class NoteModel {
    private String id;
    private String userId;
    private String title;
    private String content;
    private String htmlContent;
    private long createdAt;
    private long updatedAt;
    private String lastUpdatedBy;
    private String lastUpdatedByUsername;
    
    // New fields for enhanced features
    private boolean isPinned;
    private String color;  // Hex color code like "#FF5722"
    private String category;
    private String shareLink;  // Unique share link code
    private List<String> tags;

    public NoteModel() {
        this.title = "";
        this.content = "";
        this.htmlContent = "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isPinned = false;
        this.color = "#FFFFFF";  // Default white
        this.category = "Uncategorized";
        this.tags = new ArrayList<>();
    }

    public NoteModel(String title, String content) {
        this.title = title;
        this.content = content;
        this.htmlContent = "";
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
        this.isPinned = false;
        this.color = "#FFFFFF";
        this.category = "Uncategorized";
        this.tags = new ArrayList<>();
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getLastUpdatedBy() {
        return lastUpdatedBy;
    }

    public void setLastUpdatedBy(String lastUpdatedBy) {
        this.lastUpdatedBy = lastUpdatedBy;
    }

    public String getLastUpdatedByUsername() {
        return lastUpdatedByUsername;
    }

    public void setLastUpdatedByUsername(String lastUpdatedByUsername) {
        this.lastUpdatedByUsername = lastUpdatedByUsername;
    }

    // New getters and setters for enhanced features
    public boolean isPinned() {
        return isPinned;
    }

    public void setPinned(boolean pinned) {
        isPinned = pinned;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getShareLink() {
        return shareLink;
    }

    public void setShareLink(String shareLink) {
        this.shareLink = shareLink;
    }

    public List<String> getTags() {
        return tags != null ? tags : new ArrayList<>();
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTag(String tag) {
        if (this.tags == null) {
            this.tags = new ArrayList<>();
        }
        if (!this.tags.contains(tag)) {
            this.tags.add(tag);
        }
    }

    public void removeTag(String tag) {
        if (this.tags != null) {
            this.tags.remove(tag);
        }
    }

    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("userId", userId);
        result.put("title", title);
        result.put("content", content);
        result.put("htmlContent", htmlContent);
        result.put("createdAt", createdAt);
        result.put("updatedAt", updatedAt);
        result.put("lastUpdatedBy", lastUpdatedBy);
        result.put("lastUpdatedByUsername", lastUpdatedByUsername);
        result.put("isPinned", isPinned);
        result.put("color", color);
        result.put("category", category);
        result.put("shareLink", shareLink);
        result.put("tags", tags);
        return result;
    }

    @Override
    public String toString() {
        return title != null && !title.isEmpty() ? title : "Untitled Note";
    }
}
