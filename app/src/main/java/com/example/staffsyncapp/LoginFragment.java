package com.example.staffsyncapp; // Main package for the fragment

// Android libraries for UI, logging, and data handling
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.util.Patterns;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.text.method.PasswordTransformationMethod;
import android.content.ContentValues;
import android.widget.EditText;
import android.widget.TextView;

// additional AndroidX imports for fragment navigation and annotations
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

// data-binding and utility classes specific to the project
import com.example.staffsyncapp.databinding.LoginFragmentBinding;
import com.example.staffsyncapp.utils.LocalDataService;
import com.google.android.material.button.MaterialButton;

// project-specific utility class for location tracking [ ]

// LoginFragment: extend/inherit general Android Fragment functionality
public class LoginFragment extends Fragment { // core tracking variables for security measures
    private LoginFragmentBinding binding;
    private static final String TAG = "LoginFragment";
    private boolean isPasswordVisible = false; // initialise password visibility state as non visible

    // security constants
    private static final int MAX_LOGIN_ATTEMPTS = 3;
    private int loginAttempts = 0;
    private long lastLoginAttempt = 0;
    private static final long LOCKOUT_DURATION = 900000; // 15-minutes lockout duration in milliseconds


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) { // 1- create view
        binding = LoginFragmentBinding.inflate(inflater, container, false); // 2- inflate fragment's layout
        return binding.getRoot(); // 3- return the root view of the inflated layout
    }


    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1- create the admin account
        LocalDataService dbHelper = new LocalDataService(requireContext());
        ContentValues adminValues = new ContentValues();
        adminValues.put("email", "alfie@staffsync.com");
        adminValues.put("password", dbHelper.hashPassword("alfie123"));
        adminValues.put("is_admin", 1);

        try {
            dbHelper.getWritableDatabase().insertOrThrow("employees", null, adminValues);
            Log.d(TAG, "Created new admin account successfully");
        } catch (Exception e) {
            Log.d(TAG, "Admin creation failed");
        }

        // 2a - create a test user account
        ContentValues userValues = new ContentValues();
        userValues.put("email", "user@staffsync.com");
        userValues.put("password", dbHelper.hashPassword("user123"));
        userValues.put("is_admin", 0); // not admin

        long userId = -1;
        try {
            userId = dbHelper.getWritableDatabase().insertOrThrow("users", null, userValues);
            Log.d(TAG, "Created new user account successfully");
        } catch (Exception e) {
            Log.d(TAG, "User creation failed");
        }

        // 2b- create a test employee account if user creation succeeded
        if (userId != -1) {
            ContentValues employeeValues = new ContentValues();
            Log.d(TAG, "Attempting to create employee details for user ID: " + userId);
            employeeValues.put("user_id", userId);
            employeeValues.put("full_name", "Test User");
            employeeValues.put("department", "IT");
            employeeValues.put("salary", 50000);
            employeeValues.put("hire_date", "2024-01-01");

            try {
                dbHelper.getWritableDatabase().insertOrThrow("employee_details", null, employeeValues);
                Log.d(TAG, "Created employee details successfully");
            } catch (Exception e) {
                Log.d(TAG, "Employee details creation failed");
            }
        }

        Log.d(TAG, "Created test user with ID: " + userId);

        setupClickListeners();

        hideAllErrorMessages();
    }

    private void setupClickListeners() {
        binding.backArrow.setOnClickListener(v -> {
            Log.d(TAG, "Back arrow clicked");
            try {
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_SecondFragment_to_FirstFragment);
                Log.d(TAG, "Navigation executed");
            } catch (Exception e) {
                Log.e(TAG, "Navigation failed", e);
            }
        });

        // password visibility toggle
        binding.passwordVisibilityToggle.setOnClickListener(v -> {
            Log.d(TAG, "Password visibility toggle clicked");
            try {
                togglePasswordVisibility();
            } catch (Exception e) {
                Log.e(TAG, "Password visibility toggle failed", e);
            }
        });

        // email input focus listener for validation; validates email if focus is lost
        binding.emailInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) {
                validateEmail();
            }
        });

        //  if password container is clicked, focus on password input
        binding.passwordContainer.setOnClickListener(v -> {
            binding.passwordInput.requestFocus();
        });

        // login button click listener; log if clicked
        binding.loginButton.setOnClickListener(v -> {
            Log.d(TAG, "Login button clicked");
            attemptLogin();
        });

        // login attempt on password input done
        binding.passwordInput.setOnEditorActionListener((v, actionId, event) -> {
            attemptLogin();
            return true;
        });
    }

    private void loadUserDarkModePreference(String email) { // load preference for dark mode
        SharedPreferences prefs = requireContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode_" + email, false);

        AppCompatDelegate.setDefaultNightMode(
                isDarkMode ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }

    private void togglePasswordVisibility() { // function to toggle password visibility
        isPasswordVisible = !isPasswordVisible; // toggle password visibility state as the opposite state
        if (isPasswordVisible) {
            binding.passwordInput.setTransformationMethod(null);
            binding.passwordVisibilityToggle.setAlpha(1.0f); // fully opaque
        } else {
            binding.passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
            binding.passwordVisibilityToggle.setAlpha(0.5f);
        }
        // maintain cursor position
        binding.passwordInput.setSelection(binding.passwordInput.getText().length());
        Log.d(TAG, "Password visibility: " + isPasswordVisible);
    }

    private void hideAllErrorMessages() { // function to hide all error messages
        binding.emailFormatError.setVisibility(View.GONE);
        binding.incorrectLoginAlert.setVisibility(View.GONE);
        binding.accountLockedAlert.setVisibility(View.GONE);
        binding.anomalyAlert.setVisibility(View.GONE);
        binding.verifyIdentityButton.setVisibility(View.GONE);
    }

    private boolean validateEmail() { // function to check if email is valid/not-empty and matches email pattern
        String email = binding.emailInput.getText().toString().trim();
        if (email.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.emailFormatError.setVisibility(View.VISIBLE);
            return false;
        }
        binding.emailFormatError.setVisibility(View.GONE);
        return true;
    }

    private boolean validatePassword() { // function to validate password; return true if password is NOT empty
        String password = binding.passwordInput.getText().toString().trim();
        return !password.isEmpty();
    }

    private void attemptLogin() {
        hideAllErrorMessages();

        if (!validateEmail() || !validatePassword()) {
            return;
        }

        String email = binding.emailInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();

        LocalDataService dbHelper = new LocalDataService(requireContext());

        // 1- check if it's an admin login
        if (dbHelper.verifyAdminLogin(email, password)) {
            Log.d(TAG, "Admin login successful");
            loadUserDarkModePreference(email);
            try {
                NavHostFragment.findNavController(LoginFragment.this)
                        .navigate(R.id.action_LoginFragment_to_AdminDashboardFragment);
            } catch (Exception e) {
                Log.e(TAG, "Navigation to admin dashboard failed", e);
            }
        }
        // 2- not admin, try user login
        else {
            int loginStatus = dbHelper.verifyEmployeeLogin(email, password);
            if (loginStatus == 2) { // normal login
                Log.d(TAG, "User login successful");
                loadUserDarkModePreference(email);
                try {
                    NavHostFragment.findNavController(LoginFragment.this)
                            .navigate(R.id.action_LoginFragment_to_EmployeeMainFragment);
                } catch (Exception e) {
                    Log.e(TAG, "Navigation to user dashboard failed", e);
                }
            }
            else if (loginStatus == 1) { // first-time login
                Log.d(TAG, "First time login - showing password change dialog");
                showPasswordChangeDialog(email);
            }
            else {
                Log.d(TAG, "Login failed");
                binding.incorrectLoginAlert.setVisibility(View.VISIBLE);
            }
        }
    }
    /**
     * Displays a password change dialog for first-time login users;
     * this dialog prompts the user to enter and confirm new password.
     *
     * @param email The email address of the user, used to update the password.
     */
    private void showPasswordChangeDialog(String email) { // function to show password change dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.employee_change_password_dialog, null);
        builder.setView(dialogView);
        AlertDialog dialog = builder.create();
        dialog.setCancelable(false);

        EditText newPassword = dialogView.findViewById(R.id.new_password);
        EditText confirmPassword = dialogView.findViewById(R.id.confirm_password);
        TextView errorText = dialogView.findViewById(R.id.password_error);

        MaterialButton saveButton = dialogView.findViewById(R.id.save_password_btn);
        saveButton.setOnClickListener(v -> { // save button click listener, if pass validation, update password and proceed with login
            if (validateNewPassword(newPassword, confirmPassword, errorText)) {
                updatePassword(email, newPassword.getText().toString().trim());
                dialog.dismiss();
                proceedWithLogin();
            }
        });

        dialog.show();
    }

    private boolean validateNewPassword(EditText newPassword, EditText confirmPassword, TextView errorText) { // function to validate new password
        String password = newPassword.getText().toString().trim();
        String confirm = confirmPassword.getText().toString().trim();

        if (password.isEmpty() || confirm.isEmpty()) {
            errorText.setText("Please fill in both fields");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }

        if (password.length() < 6) {
            errorText.setText("Password must be at least 6 characters");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }

        if (!password.equals(confirm)) {
            errorText.setText("Passwords do not match");
            errorText.setVisibility(View.VISIBLE);
            return false;
        }

        return true;
    }

    private void updatePassword(String email, String newPassword) { // function to update password
        LocalDataService dbHelper = new LocalDataService(requireContext());
        String hashedPassword = dbHelper.hashPassword(newPassword);

        ContentValues values = new ContentValues();
        values.put("password", hashedPassword);
        values.put("first_login", 0);

        dbHelper.getWritableDatabase().update(
                "employees",
                values,
                "email = ?",
                new String[]{email}
        );
    }

    private void proceedWithLogin() { // function to proceed with login after password update
        Log.d(TAG, "Password updated, proceeding with login");
        loadUserDarkModePreference(binding.emailInput.getText().toString().trim());
        try {
            NavHostFragment.findNavController(LoginFragment.this)
                    .navigate(R.id.action_LoginFragment_to_EmployeeMainFragment);
        } catch (Exception e) {
            Log.e(TAG, "Navigation to user dashboard failed", e);
        }
    }

    private boolean shouldShowAnomalyAlert() {
        /*  TODO [ ]: anomaly detection logic/algorithm
            determine if anomaly alert should be shown
        */
        return false;
    }
}
