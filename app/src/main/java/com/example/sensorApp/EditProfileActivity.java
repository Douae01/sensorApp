package com.example.sensorApp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.imageview.ShapeableImageView;

public class EditProfileActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_PHONE = "phone";
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private EditText usernameInput, phoneInput;
    private ShapeableImageView profileImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        usernameInput = findViewById(R.id.username_input);
        phoneInput = findViewById(R.id.phone_input);
        profileImageView = findViewById(R.id.profile_image);

        // Load saved profile details
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String email = prefs.getString(KEY_EMAIL, "");
        String phone = prefs.getString(KEY_PHONE, "");

        // Pre-fill username and phone
        usernameInput.setText(email.split("@")[0]);
        phoneInput.setText(phone);

        // Save Profile Button
        Button saveButton = findViewById(R.id.save_button);
        saveButton.setOnClickListener(v -> {
            // Save username and phone
            SharedPreferences.Editor editor = prefs.edit();
            editor.putString(KEY_EMAIL, email);
            editor.putString(KEY_PHONE, phoneInput.getText().toString().trim());
            editor.apply();
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show();
        });

        // Logout Button
        Button logoutButton = findViewById(R.id.logout_button);
        logoutButton.setOnClickListener(v -> {
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Intent intent = new Intent(EditProfileActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
        });

        // Profile Image Click to Change Image
        profileImageView.setOnClickListener(v -> {
            // Check camera permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                // Request camera permission
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, REQUEST_IMAGE_CAPTURE);
            }
        });
    }

    // Method to open the camera and capture an image
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            // Get the photo as a Bitmap from the intent data
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            profileImageView.setImageBitmap(imageBitmap); // Set the captured image as the profile picture
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_CAPTURE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera(); // Permission granted, open the camera
            } else {
                Toast.makeText(this, "Camera permission is required to take profile picture", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
