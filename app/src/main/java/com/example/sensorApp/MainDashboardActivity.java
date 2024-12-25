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
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import android.widget.LinearLayout;
import java.util.ArrayList;
import android.app.Activity;
import android.content.Intent;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import android.widget.Button;
import android.widget.TextView;


import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainDashboardActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "LoginPrefs";
    private static final String KEY_USER_EMAIL = "userEmail";
    private static final int REQUEST_IMAGE_CAPTURE = 1;

    private int timeIndex = 0; // Declare at the class level

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener accelerometerListener;
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

    	LineChart accelerometerChart = new LineChart(this);
    	modalLayout.addView(accelerometerChart);
    	setupChart(accelerometerChart); // Configure the chart

        // Display accelerometer data in a dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Accelerometer data")
                .setView(modalLayout)
                .setPositiveButton("Close", (dialog, which) -> stopAccelerometerUpdates())
                .show();

        startAccelerometerUpdates(accelerometerData, accelerometerChart);
    }

    private void setupChart(LineChart chart) {
    	chart.getDescription().setEnabled(false); // Remove default description
    	XAxis xAxis = chart.getXAxis();
    	xAxis.setPosition(XAxis.XAxisPosition.BOTTOM); // Place X-axis at the bottom
    	xAxis.setGranularity(1f); // Set minimum interval
    	YAxis leftAxis = chart.getAxisLeft();
    	leftAxis.setAxisMinimum(-20f); // Set Y-axis range
    	leftAxis.setAxisMaximum(20f);
    	chart.getAxisRight().setEnabled(false); // Disable right Y-axis
    }

    private void startAccelerometerUpdates(TextView displayView, LineChart chart) {
        // Declare data sets at class level if needed for persistence
        ArrayList<Entry> xValues = new ArrayList<>();
        ArrayList<Entry> yValues = new ArrayList<>();
        ArrayList<Entry> zValues = new ArrayList<>();

        LineDataSet xDataSet = new LineDataSet(xValues, "X-Axis");
        LineDataSet yDataSet = new LineDataSet(yValues, "Y-Axis");
        LineDataSet zDataSet = new LineDataSet(zValues, "Z-Axis");

        xDataSet.setColor(ContextCompat.getColor(this, android.R.color.holo_red_light));
        yDataSet.setColor(ContextCompat.getColor(this, android.R.color.holo_green_light));
        zDataSet.setColor(ContextCompat.getColor(this, android.R.color.holo_blue_light));

        LineData lineData = new LineData(xDataSet, yDataSet, zDataSet);
        chart.setData(lineData);

        accelerometerListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                displayView.setText(String.format("X: %.2f, Y: %.2f, Z: %.2f", x, y, z));

                // Add data points to the graph
                xValues.add(new Entry(timeIndex, x));
                yValues.add(new Entry(timeIndex, y));
                zValues.add(new Entry(timeIndex, z));

                // Notify datasets and chart of changes
                xDataSet.notifyDataSetChanged();
                yDataSet.notifyDataSetChanged();
                zDataSet.notifyDataSetChanged();
                lineData.notifyDataChanged();
                chart.notifyDataSetChanged();
                chart.invalidate();

                timeIndex++;
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
