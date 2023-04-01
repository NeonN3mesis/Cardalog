package com.example.cardalog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.tesseract.android.TessBaseAPI;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.ExecutionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



public class ScanActivity extends AppCompatActivity {

    private static final String TAG = "ScanActivity";
    private PreviewView previewView;
    private ImageCapture imageCapture;
    private ImageButton captureImageButton;

    private TessBaseAPI tessBaseAPI;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                    new ActivityResultCallback<Boolean>() {
                        @Override
                        public void onActivityResult(Boolean isGranted) {
                            if (isGranted) {
                                startCamera();
                            } else {
                                finish();
                            }
                        }
                    });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Button takePictureButton = findViewById(R.id.takePictureButton);
        takePictureButton.setOnClickListener(v -> takePictureAndAnalyze());

        String language = "eng";
        tessBaseAPI = new TessBaseAPI();

        copyTrainedData();{
            try {
                File tessdataFolder = new File(getFilesDir(), "tessdata");
                tessdataFolder.mkdirs();
                InputStream trainedDataInputStream = getAssets().open("tessdata/eng.traineddata");
                File trainedDataFile = new File(tessdataFolder, "eng.traineddata");
                OutputStream trainedDataOutputStream = new FileOutputStream(trainedDataFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = trainedDataInputStream.read(buffer)) != -1) {
                    trainedDataOutputStream.write(buffer, 0, read);
                }
                trainedDataInputStream.close();
                trainedDataOutputStream.flush();
                trainedDataOutputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Failed to copy trained data", e);
            }
        }

        initializeTesseract(language);

        previewView = findViewById(R.id.previewView);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        TextView nameTextView = findViewById(R.id.name);
        TextView titleTextView = findViewById(R.id.Title);
        TextView companyTextView = findViewById(R.id.company);
        TextView phoneNumberTextView = findViewById(R.id.phone_number);
        TextView emailTextView = findViewById(R.id.email);
        TextView websiteTextView = findViewById(R.id.website);
        TextView addressTextView = findViewById(R.id.address);

        BusinessCardInfo info = new BusinessCardInfo();
        info.setName("John Doe");
        info.setJobTitle("Software Engineer");
        info.setBusinessName("Example Company");
        info.setPhoneNumber("123-456-7890");
        info.setEmail("john.doe@example.com");
        info.setWebsite("www.example.com");
        info.setAddress("123 Example Street, City, Country");

        nameTextView.setText(info.getName());
        titleTextView.setText(info.getJobTitle());
        companyTextView.setText(info.getBusinessName());
        phoneNumberTextView.setText(info.getPhoneNumber());
        emailTextView.setText(info.getEmail());
        websiteTextView.setText(info.getWebsite());
        addressTextView.setText(info.getAddress());
    }

    private void copyTrainedData() {
        try {
            File tessdataFolder = new File(getFilesDir(), "tessdata");
            tessdataFolder.mkdirs();
            InputStream trainedDataInputStream = getAssets().open("tessdata/eng.traineddata");
            File trainedDataFile = new File(tessdataFolder, "eng.traineddata");
            OutputStream trainedDataOutputStream = new FileOutputStream(trainedDataFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = trainedDataInputStream.read(buffer)) != -1) {
                trainedDataOutputStream.write(buffer, 0, read);
            }
            trainedDataInputStream.close();
            trainedDataOutputStream.flush();
            trainedDataOutputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Failed to copy trained data", e);
        }
    }
    private void initializeTesseract(String language) {
        File tessdataFolder = new File(getFilesDir(), "tessdata");
        if (tessdataFolder.exists()) {
            tessBaseAPI.init(getFilesDir().toString(), language);
        } else {
            Log.e(TAG, "tessdata folder not found");
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                        .build();

                cameraProvider.unbindAll();

                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview);

                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, imageCapture, preview);

            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        cameraProvider.unbindAll();

        try {
            cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void takePictureAndAnalyze() {
        Log.d(TAG, "takePictureAndAnalyze called");
        File outputDirectory = getOutputDirectory();
        File outputFile = new File(outputDirectory, System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d(TAG, "Image saved successfully");
                        Uri savedUri = Uri.fromFile(outputFile);
                        analyzeImage(savedUri);
                    }

                    @Override public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                    }
                });
    }

    private File getOutputDirectory() {
        File[] mediaDirs = getExternalMediaDirs();
        if (mediaDirs.length > 0) {
            File outputDir = new File(mediaDirs[0], getResources().getString(R.string.app_name));
            outputDir.mkdirs();
            return outputDir;
        }
        return getFilesDir();
    }

    private void analyzeImage(Uri imageUri) {
        Bitmap bitmap = BitmapFactory.decodeFile(imageUri.getPath());
        tessBaseAPI.setImage(bitmap);
        String recognizedText = tessBaseAPI.getUTF8Text();
        Log.d(TAG, "Recognized text: " + recognizedText);
        BusinessCardInfo info = parseTesseractOutput(recognizedText);

        // Start ConfirmDetailsActivity
        Intent intent = new Intent(this, ConfirmDetailsActivity.class);
        intent.putExtra("info", info);
        intent.putExtra("imageUri", imageUri.toString());
        startActivity(intent);
    }

    private BusinessCardInfo parseTesseractOutput(String text) {
        BusinessCardInfo info = new BusinessCardInfo();

        // Extract name
        Pattern namePattern = Pattern.compile("Name\\s*:\\s*(\\w+\\s*\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher nameMatcher = namePattern.matcher(text);
        if (nameMatcher.find()) {
            info.name = nameMatcher.group(1).trim();
        }
        Log.d(TAG, "Name: " + info.name);

        // Extract phone number
        Pattern phonePattern = Pattern.compile("(?:\\+\\d{1,2}\\s?)?\\(?\\d{1,4}?\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}");
        Matcher phoneMatcher = phonePattern.matcher(text);
        if (phoneMatcher.find()) {
            info.phoneNumber = phoneMatcher.group().trim();
        }
        Log.d(TAG, "Phone Number: " +info.phoneNumber);

        // Extract email
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)");
        Matcher emailMatcher = emailPattern.matcher(text);
        if (emailMatcher.find()) {
            info.email = emailMatcher.group().trim();
        }
        Log.d(TAG, "Email: " + info.email);

        // Extract mailing address
        Pattern addressPattern = Pattern.compile("Address\\s*:\\s*(.*)(?=\\n)", Pattern.CASE_INSENSITIVE);
        Matcher addressMatcher = addressPattern.matcher(text);
        if (addressMatcher.find()) {
            info.address = addressMatcher.group().trim();
        }

        // Extract business name
        Pattern businessNamePattern = Pattern.compile("Business\\s*:\\s*(.*)(?=\\n)", Pattern.CASE_INSENSITIVE);
        Matcher businessNameMatcher = businessNamePattern.matcher(text);
        if (businessNameMatcher.find()) {
            info.businessName = businessNameMatcher.group(1).trim();
        }

        // Extract job title
        Pattern jobTitlePattern = Pattern.compile("Title\\s*:\\s*(.*)(?=\\n)", Pattern.CASE_INSENSITIVE);
        Matcher jobTitleMatcher = jobTitlePattern.matcher(text);
        if (jobTitleMatcher.find()) {
            info.jobTitle = jobTitleMatcher.group(1).trim();
        }
        return info;
    }
}
