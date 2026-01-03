package com.example.syncnote.models;

import com.google.firebase.database.Exclude;
import com.google.firebase.database.IgnoreExtraProperties;

import java.util.HashMap;
import java.util.Map;

@IgnoreExtraProperties
public class CategoryModel {
    
    private String id;
    private String userId;
    private String name;
    private String color;
    private int noteCount;
    private long createdAt;
    
    public CategoryModel() {
        this.createdAt = System.currentTimeMillis();
        this.noteCount = 0;
    }
    
    public CategoryModel(String userId, String name, String color) {
        this.userId = userId;
        this.name = name;
        this.color = color;
        this.createdAt = System.currentTimeMillis();
        this.noteCount = 0;
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
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getColor() {
        return color;
    }
    
    public void setColor(String color) {
        this.color = color;
    }
    
    public int getNoteCount() {
        return noteCount;
    }
    
    public void setNoteCount(int noteCount) {
        this.noteCount = noteCount;
    }
    
    public long getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
    
    @Exclude
    public Map<String, Object> toMap() {
        HashMap<String, Object> result = new HashMap<>();
        result.put("id", id);
        result.put("userId", userId);
        result.put("name", name);
        result.put("color", color);
        result.put("noteCount", noteCount);
        result.put("createdAt", createdAt);
        return result;
    }
    
    @Override
    public String toString() {
        return name;
    }
}
