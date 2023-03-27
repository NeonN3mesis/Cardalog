package com.example.cardalog;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.widget.ImageButton;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

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
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    startCamera();
                } else {
                    finish();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        String language = "eng";
        tessBaseAPI = new TessBaseAPI();
        tessBaseAPI.init(getFilesDir().getAbsolutePath(), language);

        copyTrainedData();

        previewView = findViewById(R.id.previewView);
        captureImageButton = findViewById(R.id.captureImageButton);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }

        captureImageButton.setOnClickListener(v -> takePictureAndAnalyze());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                // No errors need to be handled for this Future.
                // This should never be reached.
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder()
                .build();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = new ImageCapture.Builder()
                .build();

        cameraProvider.unbindAll();

        cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

        preview.setSurfaceProvider(previewView.getSurfaceProvider());
    }

    private void takePictureAndAnalyze() {
        File outputDirectory = getOutputDirectory();
        File outputFile = new File(outputDirectory, System.currentTimeMillis() + ".jpg");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(outputFile).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(this),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Uri savedUri = Uri.fromFile(outputFile);
                        analyzeImage(savedUri);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
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
        processTextRecognitionResult(recognizedText);
    }

    private void processTextRecognitionResult(String text) {
        // Extract the contact information from the recognized text.
        String displayName = "";
        String phoneNumber = "";
        String email = "";

        // Extract name
        Pattern namePattern = Pattern.compile("Name\\s*:\\s*(\\w+\\s*\\w+)", Pattern.CASE_INSENSITIVE);
        Matcher nameMatcher = namePattern.matcher(text);
        if (nameMatcher.find()) {
            displayName = nameMatcher.group(1).trim();
        }

        // Extract phone number
        Pattern phonePattern = Pattern.compile("(?:\\+\\d{1,2}\\s?)?\\(?\\d{1,4}?\\)?[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,4}[-.\\s]?\\d{1,9}");
        Matcher phoneMatcher = phonePattern.matcher(text);
        if (phoneMatcher.find()) {
            phoneNumber = phoneMatcher.group().trim();
        }

        // Extract email
        Pattern emailPattern = Pattern.compile("([a-zA-Z0-9._-]+@[a-zA-Z0-9._-]+\\.[a-zA-Z0-9_-]+)");
        Matcher emailMatcher = emailPattern.matcher(text);
        if (emailMatcher.find()) {
            email = emailMatcher.group().trim();
        }

        // Open the new contact screen and populate the fields.
        Intent intent = new Intent(Intent.ACTION_INSERT);
        intent.setType(ContactsContract.Contacts.CONTENT_TYPE);
        intent.putExtra(ContactsContract.Intents.Insert.NAME, displayName);
        intent.putExtra(ContactsContract.Intents.Insert.PHONE, phoneNumber);
        intent.putExtra(ContactsContract.Intents.Insert.EMAIL, email);

        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        }
    }

    private void copyTrainedData() {
        String language = "eng";
        String trainedDataFileName = "tessdata/" + language + ".traineddata";

        try {
            InputStream inputStream = getAssets().open(trainedDataFileName);
            File tessdataFolder = new File(getFilesDir(), "tessdata");
            tessdataFolder.mkdirs();
            File trainedDataFile = new File(tessdataFolder, language + ".traineddata");

            if (!trainedDataFile.exists()) {
                OutputStream outputStream = new FileOutputStream(trainedDataFile);
                byte[] buffer = new byte[1024];
                int read;
                while ((read = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, read);
                }

                outputStream.flush();
                outputStream.close();
            }
            inputStream.close();
        } catch (IOException e) {
            Log.e(TAG, "Error copying trained data file", e);
        }

        @Override
        protected void onDestroy () {
            super.onDestroy();
            if (tessBaseAPI != null) {
                tessBaseAPI.end();
            }
        }
    }