package com.example.event_lottery;

import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Spinner;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.UUID;

public class SignupActivity extends AppCompatActivity {
    private EditText emailEditText, usernameEditText, firstNameEditText, lastNameEditText, passwordEditText;
    private Button signupButton;
    private ImageButton backButton;
    private Spinner roleSpinner;
    private String selectedRole;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sign_up_page);

        db = FirebaseFirestore.getInstance();

        emailEditText = findViewById(R.id.emailEditText);
        usernameEditText = findViewById(R.id.usernameEditText);
        firstNameEditText = findViewById(R.id.firstNameEditText);
        lastNameEditText = findViewById(R.id.lastNameEditText);
        passwordEditText = findViewById(R.id.passwordEditText);
        signupButton = findViewById(R.id.save_changes);
        backButton = findViewById(R.id.backButton);
        roleSpinner = findViewById(R.id.roleSpinner);

        // Setting up the spinner with role options
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.role_options, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, android.view.View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                selectedRole = null;
            }
        });

        backButton.setOnClickListener(v -> finish());

        signupButton.setOnClickListener(v -> {
            String email = emailEditText.getText().toString().trim();
            String password = passwordEditText.getText().toString();
            String username = usernameEditText.getText().toString();
            String firstName = firstNameEditText.getText().toString();
            String lastName = lastNameEditText.getText().toString();

            if (!areFieldsFilled(email, password, username, firstName, lastName, selectedRole)) {
                Toast.makeText(SignupActivity.this, "Please fill in all required fields", Toast.LENGTH_SHORT).show();
            } else if (!isEmailValid(email)) {
                Toast.makeText(SignupActivity.this, "Please enter a valid email address", Toast.LENGTH_SHORT).show();
            } else if (!isPasswordValid(password)) {
                Toast.makeText(SignupActivity.this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            } else {
                // Generate a unique ID for the user
                String userId = UUID.randomUUID().toString();

                // Save user information in Firestore
                saveUserInfo(userId, email, username, firstName, lastName, password, selectedRole);
            }
        });
    }

    // Validation Methods
    public boolean areFieldsFilled(String... fields) {
        for (String field : fields) {
            if (field == null || field.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    public boolean isEmailValid(String email) {
        // Simple regex for email validation
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public boolean isPasswordValid(String password) {
        // Password should be at least 6 characters
        return password != null && password.length() >= 6;
    }

    private void saveUserInfo(String id, String email, String username, String firstName, String lastName, String password, String role) {
        // Create a User object with id as the first parameter
        User userInfo = new User(id, email, username, firstName, lastName, password, role);

        // Store the user information in Firestore, using the email as the document ID
        db.collection("users").document(email).set(userInfo)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignupActivity.this, "Signup successful", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(SignupActivity.this, "Failed to save user info: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
