package com.example.syncnote;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.syncnote.firebase.FirebaseManager;
import com.example.syncnote.models.CategoryModel;
import com.example.syncnote.utils.SessionManager;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private FirebaseManager firebaseManager;
    private SessionManager sessionManager;
    
    // Change Password Views
    private TextInputLayout currentPasswordLayout, newPasswordLayout, confirmPasswordLayout;
    private TextInputEditText currentPasswordInput, newPasswordInput, confirmPasswordInput;
    private Button changePasswordButton;
    private ProgressBar progressBar;
    
    // User Info Views
    private MaterialCardView userInfoSection;
    
    // Categories Views
    private RecyclerView categoriesRecyclerView;
    private TextView noCategoriesText;
    private Button addCategoryButton;
    private CategoriesAdapter categoriesAdapter;
    private List<CategoryModel> categories = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        firebaseManager = FirebaseManager.getInstance();
        sessionManager = SessionManager.getInstance(this);
        
        setupToolbar();
        initViews();
        setupClickListeners();
        loadCategories();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("Settings");
        }
    }

    private void initViews() {
        currentPasswordLayout = findViewById(R.id.currentPasswordLayout);
        newPasswordLayout = findViewById(R.id.newPasswordLayout);
        confirmPasswordLayout = findViewById(R.id.confirmPasswordLayout);
        
        currentPasswordInput = findViewById(R.id.currentPasswordInput);
        newPasswordInput = findViewById(R.id.newPasswordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        
        changePasswordButton = findViewById(R.id.changePasswordButton);
        progressBar = findViewById(R.id.progressBar);
        userInfoSection = (MaterialCardView) findViewById(R.id.userInfoSection);
        
        // Categories
        categoriesRecyclerView = findViewById(R.id.categoriesRecyclerView);
        noCategoriesText = findViewById(R.id.noCategoriesText);
        addCategoryButton = findViewById(R.id.addCategoryButton);
        
        // Setup categories RecyclerView
        categoriesAdapter = new CategoriesAdapter();
        categoriesRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        categoriesRecyclerView.setAdapter(categoriesAdapter);
        
        // Display user info
        TextView usernameText = findViewById(R.id.usernameText);
        TextView emailText = findViewById(R.id.emailText);
        
        if (usernameText != null) {
            usernameText.setText(sessionManager.getCurrentUsername());
        }
        if (emailText != null) {
            String email = sessionManager.getCurrentEmail();
            emailText.setText(email != null ? email : "No email set");
        }
    }

    private void setupClickListeners() {
        changePasswordButton.setOnClickListener(v -> validateAndChangePassword());
        addCategoryButton.setOnClickListener(v -> showAddCategoryDialog());
        
        // Clear errors on text change
        currentPasswordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentPasswordLayout.setError(null);
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        newPasswordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                newPasswordLayout.setError(null);
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        confirmPasswordInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                confirmPasswordLayout.setError(null);
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }
    
    private void loadCategories() {
        firebaseManager.getCategoriesForUser(sessionManager.getCurrentUserId(),
            new FirebaseManager.CategoriesCallback() {
                @Override
                public void onSuccess(List<CategoryModel> categoryList) {
                    runOnUiThread(() -> {
                        categories.clear();
                        categories.addAll(categoryList);
                        categoriesAdapter.notifyDataSetChanged();
                        updateCategoriesUI();
                    });
                }

                @Override
                public void onError(String error) {
                    runOnUiThread(() -> updateCategoriesUI());
                }
            });
    }
    
    private void updateCategoriesUI() {
        if (categories.isEmpty()) {
            noCategoriesText.setVisibility(View.VISIBLE);
            categoriesRecyclerView.setVisibility(View.GONE);
        } else {
            noCategoriesText.setVisibility(View.GONE);
            categoriesRecyclerView.setVisibility(View.VISIBLE);
        }
    }
    
    private void showAddCategoryDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_add_category, null);
        EditText categoryNameInput = dialogView.findViewById(R.id.categoryNameInput);
        
        new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String name = categoryNameInput.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "Category name is required", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    
                    // Check if category already exists
                    for (CategoryModel cat : categories) {
                        if (cat.getName().equalsIgnoreCase(name)) {
                            Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                    
                    addCategory(name);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    
    private void addCategory(String name) {
        CategoryModel category = new CategoryModel();
        category.setUserId(sessionManager.getCurrentUserId());
        category.setName(name);
        category.setCreatedAt(System.currentTimeMillis());
        
        firebaseManager.createCategory(category, new FirebaseManager.SaveCallback() {
            @Override
            public void onSuccess(String id) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Category added", Toast.LENGTH_SHORT).show();
                    loadCategories();
                });
            }

            @Override
            public void onError(String error) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Failed to add category", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    private void deleteCategory(CategoryModel category) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete \"" + category.getName() + "\"?\n\nNotes in this category will be moved to 'Uncategorized'.")
                .setPositiveButton("Delete", (dialog, which) -> {
                    firebaseManager.deleteCategory(category.getId(), success -> {
                        runOnUiThread(() -> {
                            if (success) {
                                Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show();
                                loadCategories();
                            } else {
                                Toast.makeText(this, "Failed to delete category", Toast.LENGTH_SHORT).show();
                            }
                        });
                    });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void validateAndChangePassword() {
        String currentPassword = currentPasswordInput.getText().toString().trim();
        String newPassword = newPasswordInput.getText().toString().trim();
        String confirmPassword = confirmPasswordInput.getText().toString().trim();
        
        // Clear previous errors
        currentPasswordLayout.setError(null);
        newPasswordLayout.setError(null);
        confirmPasswordLayout.setError(null);
        
        // Validate inputs
        boolean isValid = true;
        
        if (TextUtils.isEmpty(currentPassword)) {
            currentPasswordLayout.setError("Current password is required");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(newPassword)) {
            newPasswordLayout.setError("New password is required");
            isValid = false;
        } else if (newPassword.length() < 6) {
            newPasswordLayout.setError("Password must be at least 6 characters");
            isValid = false;
        }
        
        if (TextUtils.isEmpty(confirmPassword)) {
            confirmPasswordLayout.setError("Please confirm your new password");
            isValid = false;
        } else if (!newPassword.equals(confirmPassword)) {
            confirmPasswordLayout.setError("Passwords do not match");
            isValid = false;
        }
        
        if (currentPassword.equals(newPassword)) {
            newPasswordLayout.setError("New password must be different from current password");
            isValid = false;
        }
        
        if (!isValid) {
            return;
        }
        
        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Change Password")
                .setMessage("Are you sure you want to change your password?")
                .setPositiveButton("Change", (dialog, which) -> {
                    changePassword(currentPassword, newPassword);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void changePassword(String currentPassword, String newPassword) {
        showLoading(true);
        
        String userId = sessionManager.getCurrentUserId();
        
        firebaseManager.changePassword(userId, currentPassword, newPassword, success -> {
            runOnUiThread(() -> {
                showLoading(false);
                
                if (success) {
                    // Clear input fields
                    currentPasswordInput.setText("");
                    newPasswordInput.setText("");
                    confirmPasswordInput.setText("");
                    
                    Toast.makeText(SettingsActivity.this, 
                            "Password changed successfully", Toast.LENGTH_LONG).show();
                } else {
                    currentPasswordLayout.setError("Current password is incorrect");
                }
            });
        });
    }

    private void showLoading(boolean show) {
        progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        changePasswordButton.setEnabled(!show);
        currentPasswordInput.setEnabled(!show);
        newPasswordInput.setEnabled(!show);
        confirmPasswordInput.setEnabled(!show);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    // Categories Adapter
    private class CategoriesAdapter extends RecyclerView.Adapter<CategoriesAdapter.ViewHolder> {
        
        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_category, parent, false);
            return new ViewHolder(view);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            CategoryModel category = categories.get(position);
            holder.categoryName.setText(category.getName());
            holder.deleteButton.setOnClickListener(v -> deleteCategory(category));
        }
        
        @Override
        public int getItemCount() {
            return categories.size();
        }
        
        class ViewHolder extends RecyclerView.ViewHolder {
            TextView categoryName;
            ImageButton deleteButton;
            
            ViewHolder(View itemView) {
                super(itemView);
                categoryName = itemView.findViewById(R.id.categoryName);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}
