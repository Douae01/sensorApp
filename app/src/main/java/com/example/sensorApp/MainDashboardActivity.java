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
        findViewById(R.id.sound_meter).setOnClickListener(v -> showSoundLevelModal());

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
        ArrayList<Entry> xValues = new ArrayList<>();
        ArrayList<Entry> yValues = new ArrayList<>();
        ArrayList<Entry> zValues = new ArrayList<>();
        LineDataSet xDataSet = new LineDataSet(xValues, "X-Axis");
        LineDataSet yDataSet = new LineDataSet(yValues, "Y-Axis");
        LineDataSet zDataSet = new LineDataSet(zValues, "Z-Axis");

        xDataSet.setColor(getResources().getColor(android.R.color.holo_red_light));
        yDataSet.setColor(getResources().getColor(android.R.color.holo_green_light));
        zDataSet.setColor(getResources().getColor(android.R.color.holo_blue_light));

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
            	lineData.notifyDataChanged();
            	chart.notifyDataSetChanged();
            	chart.invalidate();

            	timeIndex++;
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {
            }
        };

        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        // Update display every 500ms
        accelerometerUpdater = () -> {
            sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
            handler.postDelayed(accelerometerUpdater, 500);
        };
        handler.post(accelerometerUpdater);
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

    // Sound Level Modal (Niveau sonore)
    private void showSoundLevelModal() {
        final TextView soundLevelData = new TextView(this);
        soundLevelData.setTextSize(16);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Niveau sonore")
                .setView(soundLevelData)
                .setPositiveButton("Close", (dialog, which) -> stopSoundLevelUpdates())
                .show();

        startSoundLevelUpdates(soundLevelData);
    }

    private AudioRecord audioRecord;
    private boolean isSoundLevelUpdating = false;
    private static final int SAMPLE_RATE = 44100;

    private void startSoundLevelUpdates(TextView displayView) {
        int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);

        if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            Toast.makeText(this, "Unable to initialize microphone", Toast.LENGTH_SHORT).show();
            return;
        }

        audioRecord.startRecording();
        isSoundLevelUpdating = true;

        new Thread(() -> {
            short[] buffer = new short[bufferSize];
            while (isSoundLevelUpdating) {
                int read = audioRecord.read(buffer, 0, buffer.length);
                if (read > 0) {
                    double sum = 0;
                    for (short sample : buffer) {
                        sum += sample * sample;
                    }
                    double rms = Math.sqrt(sum / read);
                    final double soundLevelDb = 20 * Math.log10(rms);

                    runOnUiThread(() -> displayView.setText(String.format("Niveau sonore: %.2f dB", soundLevelDb)));
                }
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void stopSoundLevelUpdates() {
        isSoundLevelUpdating = false;
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
    }

    private void updateSoundLevel(TextView displayView) {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isRecording && mediaRecorder != null) {
                    int maxAmplitude = mediaRecorder.getMaxAmplitude();
                    double soundLevelDb = 20 * Math.log10((double) Math.max(maxAmplitude, 1));
                    displayView.setText(String.format("Niveau sonore: %.2f dB", soundLevelDb));
                }
                handler.postDelayed(this, 500);
            }
        }, 500);
    }
}
