package com.example.syncnote;

import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.syncnote.firebase.FirebaseManager;
import com.example.syncnote.models.ActivityLogModel;
import com.example.syncnote.models.CategoryModel;
import com.example.syncnote.models.NoteModel;
import com.example.syncnote.utils.DateUtils;
import com.example.syncnote.utils.SessionManager;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import jp.wasabeef.richeditor.RichEditor;

public class NoteEditorActivity extends AppCompatActivity {

    private EditText titleInput;
    private RichEditor richEditor;
    private TextView statusLabel, lastSavedLabel;
    private MaterialButton saveButton, cancelButton;
    private FrameLayout progressOverlay;
    private HorizontalScrollView formattingToolbar;
    private Spinner categorySpinner;
    private View colorIndicator;

    // Formatting buttons
    private ImageButton btnBold, btnItalic, btnUnderline, btnStrikethrough;
    private ImageButton btnBulletList, btnNumberedList;
    private ImageButton btnAlignLeft, btnAlignCenter, btnAlignRight;
    private ImageButton btnUndo, btnRedo;
    private ImageButton btnColor, btnCategory, btnPin;

    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;

    private NoteModel currentNote;
    private String noteId;
    private boolean isModified = false;
    private List<CategoryModel> categories = new ArrayList<>();
    private String selectedCategory = "Uncategorized";
    private String selectedColor = "#FFFFFF";
    private boolean isPinned = false;

    // Predefined colors for notes
    private final String[] NOTE_COLORS = {
            "#FFFFFF", "#FFCDD2", "#F8BBD9", "#E1BEE7", "#D1C4E9",
            "#C5CAE9", "#BBDEFB", "#B3E5FC", "#B2EBF2", "#B2DFDB",
            "#C8E6C9", "#DCEDC8", "#F0F4C3", "#FFF9C4", "#FFECB3",
            "#FFE0B2", "#FFCCBC", "#D7CCC8", "#CFD8DC"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note_editor);

        firebaseManager = FirebaseManager.getInstance();
        sessionManager = SessionManager.getInstance(this);

        initViews();
        setupToolbar();
        setupRichEditor();
        setupFormattingButtons();
        setupListeners();
        loadCategories();

        noteId = getIntent().getStringExtra("noteId");
        if (noteId != null) {
            loadNote(noteId);
        } else {
            statusLabel.setText("New");
            currentNote = new NoteModel();
        }
    }

    private void initViews() {
        titleInput = findViewById(R.id.titleInput);
        richEditor = findViewById(R.id.richEditor);
        statusLabel = findViewById(R.id.statusLabel);
        lastSavedLabel = findViewById(R.id.lastSavedLabel);
        saveButton = findViewById(R.id.saveButton);
        cancelButton = findViewById(R.id.cancelButton);
        progressOverlay = findViewById(R.id.progressOverlay);
        formattingToolbar = findViewById(R.id.formattingToolbar);
        categorySpinner = findViewById(R.id.categorySpinner);
        colorIndicator = findViewById(R.id.colorIndicator);

        // Formatting buttons
        btnBold = findViewById(R.id.btnBold);
        btnItalic = findViewById(R.id.btnItalic);
        btnUnderline = findViewById(R.id.btnUnderline);
        btnStrikethrough = findViewById(R.id.btnStrikethrough);
        btnBulletList = findViewById(R.id.btnBulletList);
        btnNumberedList = findViewById(R.id.btnNumberedList);
        btnAlignLeft = findViewById(R.id.btnAlignLeft);
        btnAlignCenter = findViewById(R.id.btnAlignCenter);
        btnAlignRight = findViewById(R.id.btnAlignRight);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        btnColor = findViewById(R.id.btnColor);
        btnPin = findViewById(R.id.btnPin);
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(noteId == null ? "New Note" : "Edit Note");
        }
        toolbar.setNavigationOnClickListener(v -> onBackPressed());
    }

    private void setupRichEditor() {
        richEditor.setEditorHeight(200);
        richEditor.setEditorFontSize(16);
        richEditor.setEditorFontColor(Color.parseColor("#212121"));
        richEditor.setEditorBackgroundColor(Color.WHITE);
        richEditor.setPadding(16, 16, 16, 16);
        richEditor.setPlaceholder("Start writing your note...");

        richEditor.setOnTextChangeListener(text -> {
            if (!isModified && text != null && !text.isEmpty()) {
                isModified = true;
                statusLabel.setText("Modified");
            }
        });
    }

    private void setupFormattingButtons() {
        btnBold.setOnClickListener(v -> richEditor.setBold());
        btnItalic.setOnClickListener(v -> richEditor.setItalic());
        btnUnderline.setOnClickListener(v -> richEditor.setUnderline());
        btnStrikethrough.setOnClickListener(v -> richEditor.setStrikeThrough());
        btnBulletList.setOnClickListener(v -> richEditor.setBullets());
        btnNumberedList.setOnClickListener(v -> richEditor.setNumbers());
        btnAlignLeft.setOnClickListener(v -> richEditor.setAlignLeft());
        btnAlignCenter.setOnClickListener(v -> richEditor.setAlignCenter());
        btnAlignRight.setOnClickListener(v -> richEditor.setAlignRight());
        btnUndo.setOnClickListener(v -> richEditor.undo());
        btnRedo.setOnClickListener(v -> richEditor.redo());

        btnColor.setOnClickListener(v -> showColorPicker());
        btnPin.setOnClickListener(v -> togglePin());

        updatePinButton();
    }

    private void setupListeners() {
        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isModified) {
                    isModified = true;
                    statusLabel.setText("Modified");
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        saveButton.setOnClickListener(v -> saveNote());
        cancelButton.setOnClickListener(v -> onBackPressed());
    }

    private void loadCategories() {
        firebaseManager.getCategoriesForUser(sessionManager.getCurrentUserId(), 
            new FirebaseManager.CategoriesCallback() {
                @Override
                public void onSuccess(List<CategoryModel> categoryList) {
                    runOnUiThread(() -> {
                        categories.clear();
                        categories.addAll(categoryList);
                        setupCategorySpinner();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> setupCategorySpinner());
                }
            });
    }

    private void setupCategorySpinner() {
        List<String> categoryNames = new ArrayList<>();
        categoryNames.add("Uncategorized");
        for (CategoryModel cat : categories) {
            categoryNames.add(cat.getName());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        categorySpinner.setAdapter(adapter);

        // Set selected category
        int position = categoryNames.indexOf(selectedCategory);
        if (position >= 0) {
            categorySpinner.setSelection(position);
        }

        categorySpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
                selectedCategory = categoryNames.get(pos);
                if (!isModified && currentNote != null) {
                    isModified = true;
                    statusLabel.setText("Modified");
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void showColorPicker() {
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
                layout.addView(row);
            }

            View colorView = new View(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(100, 100);
            params.setMargins(8, 8, 8, 8);
            colorView.setLayoutParams(params);
            colorView.setBackgroundColor(Color.parseColor(NOTE_COLORS[i]));

            final String color = NOTE_COLORS[i];
            colorView.setOnClickListener(v -> {
                selectedColor = color;
                colorIndicator.setBackgroundColor(Color.parseColor(color));
                isModified = true;
                statusLabel.setText("Modified");
            });

            if (row != null) {
                row.addView(colorView);
            }
        }

        builder.setView(layout);
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void togglePin() {
        isPinned = !isPinned;
        updatePinButton();
        isModified = true;
        statusLabel.setText("Modified");
        Toast.makeText(this, isPinned ? "Note will be pinned" : "Note unpinned", Toast.LENGTH_SHORT).show();
    }

    private void updatePinButton() {
        btnPin.setAlpha(isPinned ? 1.0f : 0.5f);
    }

    private void loadNote(String noteId) {
        showLoading(true);

        firebaseManager.getNoteById(noteId, new FirebaseManager.NoteCallback() {
            @Override
            public void onSuccess(NoteModel note) {
                runOnUiThread(() -> {
                    showLoading(false);
                    currentNote = note;
                    titleInput.setText(note.getTitle());
                    
                    // Load HTML content if available, otherwise plain text
                    if (note.getHtmlContent() != null && !note.getHtmlContent().isEmpty()) {
                        richEditor.setHtml(note.getHtmlContent());
                    } else {
                        richEditor.setHtml(note.getContent());
                    }
                    
                    // Load other properties
                    selectedCategory = note.getCategory() != null ? note.getCategory() : "Uncategorized";
                    selectedColor = note.getColor() != null ? note.getColor() : "#FFFFFF";
                    isPinned = note.isPinned();
                    
                    colorIndicator.setBackgroundColor(Color.parseColor(selectedColor));
                    updatePinButton();
                    setupCategorySpinner();
                    
                    statusLabel.setText("Loaded");
                    lastSavedLabel.setText("Last saved: " + DateUtils.formatDateTime(note.getUpdatedAt()));
                    isModified = false;
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    showLoading(false);
                    Toast.makeText(NoteEditorActivity.this, "Error loading note: " + error, Toast.LENGTH_SHORT).show();
                    finish();
                });
            }
        });
    }

    private void saveNote() {
        String title = titleInput.getText().toString().trim();
        String htmlContent = richEditor.getHtml();
        String plainContent = android.text.Html.fromHtml(htmlContent != null ? htmlContent : "").toString().trim();

        if (title.isEmpty()) {
            title = "Untitled Note";
        }

        if (plainContent.isEmpty()) {
            Toast.makeText(this, "Note content is empty!", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        boolean isNewNote = (currentNote.getId() == null || currentNote.getId().isEmpty());

        currentNote.setTitle(title);
        currentNote.setContent(plainContent);
        currentNote.setHtmlContent(htmlContent);
        currentNote.setCategory(selectedCategory);
        currentNote.setColor(selectedColor);
        currentNote.setPinned(isPinned);
        currentNote.setLastUpdatedBy(sessionManager.getCurrentUserId());
        currentNote.setLastUpdatedByUsername(sessionManager.getCurrentUsername());

        if (isNewNote) {
            currentNote.setUserId(sessionManager.getCurrentUserId());
            
            firebaseManager.saveNote(currentNote, new FirebaseManager.SaveNoteCallback() {
                @Override
                public void onSuccess(String noteId) {
                    // Add activity log
                    firebaseManager.addActivityLog(noteId, sessionManager.getCurrentUserId(),
                            sessionManager.getCurrentUsername(), ActivityLogModel.ACTION_CREATED, null);
                    
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(NoteEditorActivity.this, "Note saved successfully!", Toast.LENGTH_SHORT).show();
                        finish();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> {
                        showLoading(false);
                        Toast.makeText(NoteEditorActivity.this, "Failed to save note: " + error, Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } else {
            firebaseManager.updateNote(currentNote, sessionManager.getCurrentUserId(),
                    sessionManager.getCurrentUsername(), result -> {
                        if (result) {
                            // Add activity log
                            firebaseManager.addActivityLog(currentNote.getId(), sessionManager.getCurrentUserId(),
                                    sessionManager.getCurrentUsername(), ActivityLogModel.ACTION_EDITED, null);
                        }
                        
                        runOnUiThread(() -> {
                            showLoading(false);
                            if (result) {
                                Toast.makeText(this, "Note updated successfully!", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Failed to update note!", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
        }
    }

    private void showLoading(boolean show) {
        progressOverlay.setVisibility(show ? View.VISIBLE : View.GONE);
        saveButton.setEnabled(!show);
    }

    @Override
    public void onBackPressed() {
        if (isModified) {
            new AlertDialog.Builder(this)
                    .setTitle("Unsaved Changes")
                    .setMessage("You have unsaved changes. Do you want to save before leaving?")
                    .setPositiveButton("Save", (dialog, which) -> saveNote())
                    .setNegativeButton("Discard", (dialog, which) -> super.onBackPressed())
                    .setNeutralButton("Cancel", null)
                    .show();
        } else {
            super.onBackPressed();
        }
    }
}
