package com.example.sensorApp;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.LinearLayout;
import java.util.ArrayList;

import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainDashboardActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener accelerometerListener;
    private int stepCount = 0;
    private long lastStepTime = 0;
    private static final float STEP_THRESHOLD = 1.5f; // Step detection threshold
    private static final long STEP_TIME_GAP = 200;    // Minimum time between steps in ms
    private Handler handler = new Handler();
    private Runnable accelerometerUpdater;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        // Load username from SharedPreferences
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String email = prefs.getString(KEY_USER_EMAIL, "Username");
        TextView usernameDisplay = findViewById(R.id.username_display);
        if (email.contains("@")) {
            String username = email.split("@")[0];
            usernameDisplay.setText(username);
        }

        // Profile icon click listener
        ImageView profileIcon = findViewById(R.id.profile_icon);
        profileIcon.setOnClickListener(v -> startActivity(new Intent(MainDashboardActivity.this, EditProfileActivity.class)));

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        // Set up button listeners
        findViewById(R.id.step_counter).setOnClickListener(v -> showAccelerometerModal());
        findViewById(R.id.camera).setOnClickListener(v -> openCamera());
        findViewById(R.id.audio_button).setOnClickListener(v -> showAudioTranscription());

        // Request camera and microphone permissions if not already granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 101);
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 102);
        }

    }

    // Accelerometer Modal
    private void showAccelerometerModal() {
        LinearLayout modalLayout = new LinearLayout(this);
        modalLayout.setOrientation(LinearLayout.VERTICAL);
        modalLayout.setPadding(16, 16, 16, 16);

        final TextView accelerometerData = new TextView(this);
        accelerometerData.setTextSize(16);
        accelerometerData.setPadding(0, 0, 0, 16);
        modalLayout.addView(accelerometerData);

        final TextView stepCountView = new TextView(this);
        stepCountView.setTextSize(16);
        stepCountView.setText("Steps: 0");
        modalLayout.addView(stepCountView);

        // Display step Counter in a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Step Counter")
                .setView(modalLayout)
                .setPositiveButton("Close", (dialog, which) -> stopAccelerometerUpdates())
                .show();

        startAccelerometerUpdates(accelerometerData, stepCountView);
    }

    private void startAccelerometerUpdates(TextView displayView, TextView chart) {
        accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];

                // Calculate magnitude of acceleration
                float magnitude = (float) Math.sqrt(x * x + y * y + z * z);
                float adjustedMagnitude = magnitude - SensorManager.GRAVITY_EARTH;

                // Display raw accelerometer data
                displayView.setText(String.format("X: %.2f, Y: %.2f, Z: %.2f", x, y, z));

                // Step detection logic
                if (adjustedMagnitude > STEP_THRESHOLD) {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastStepTime > STEP_TIME_GAP) {
                        stepCount++;
                        lastStepTime = currentTime;

                        // Update step count display
                        displayView.setText("Steps: " + stepCount);
                    }
                }
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }


    private void stopAccelerometerUpdates() {
        if (accelerometerListener != null) {
            sensorManager.unregisterListener(accelerometerListener);
            handler.removeCallbacks(accelerometerUpdater);
        }
    }

    // Camera Modal (Caméra)
    private void openCamera() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        } else {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK && data != null) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");

            // Show the captured image in a modal
            showCapturedImageModal(imageBitmap);
        }
    }

    private void showCapturedImageModal(Bitmap imageBitmap) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Image Captured");

        ImageView imageView = new ImageView(this);
        imageView.setImageBitmap(imageBitmap);
        imageView.setPadding(16, 16, 16, 16);

        builder.setView(imageView);
        builder.setPositiveButton("Close", (dialog, which) -> dialog.dismiss());
        builder.show();
    }

    // Audio Transcription
    private void showAudioTranscription() {
        final TextView transcriptionData = new TextView(this);
        transcriptionData.setTextSize(16);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Audio Transcription")
                .setView(transcriptionData)
                .setPositiveButton("Close", (dialog, which) -> stopTranscription())
                .show();

        startTranscription(transcriptionData);
    }

    private SpeechRecognizer speechRecognizer;

    private void startTranscription(TextView displayView) {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            Intent recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr");

            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    displayView.setText("Ready to listen (French speakers)...");
                }

                @Override
                public void onBeginningOfSpeech() {
                    displayView.setText("Listening in progress...");
                }

                @Override
                public void onRmsChanged(float rmsdB) {
                    // Vous pouvez afficher un indicateur de niveau sonore si nécessaire
                }

                @Override
                public void onBufferReceived(byte[] buffer) {
                }

                @Override
                public void onEndOfSpeech() {
                    displayView.setText("Transcription processing...");
                }

                @Override
                public void onError(int error) {
                    displayView.setText("Error during voice recognition.");
                }

                @Override
                public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        displayView.setText(matches.get(0)); // Afficher la transcription
                    } else {
                        displayView.setText("No result detected.");
                    }
                }

                @Override
                public void onPartialResults(Bundle partialResults) {
                }

                @Override
                public void onEvent(int eventType, Bundle params) {
                }
            });
        }
        speechRecognizer.startListening(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH));
    }

    private void stopTranscription() {
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }
}
