package com.example.cardalog;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

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
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.example.cardalog.BusinessCardInfo;

import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;

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
    private static final String TESS_LANG = "eng"; // Replace "eng" with your desired language
    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final String[] PERMISSIONS_REQUIRED = {
            Manifest.permission.CAMERA,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_EXTERNAL_STORAGE
    };

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
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "Unable to load OpenCV!");
        } else {
            Log.d("OpenCV", "OpenCV loaded successfully!");
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        Button takePictureButton = findViewById(R.id.takePictureButton);
        takePictureButton.setOnClickListener(v -> takePictureAndAnalyze());

        String language = "eng";
        tessBaseAPI = new TessBaseAPI();

        copyTrainedData();
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

        BusinessCardInfo info = new BusinessCardInfo(
                "John Doe",
                "Software Engineer",
                "Example Company",
                "123-456-7890",
                "john.doe@example.com",
                "www.example.com",
                "123 Example Street, City, Country"
        );
        updateTextViews(info);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startCamera();
        } else {
            requestPermissions();
        }


        nameTextView.setText(info.getName());
        titleTextView.setText(info.getJobTitle());
        companyTextView.setText(info.getBusinessName());
        phoneNumberTextView.setText(info.getPhoneNumber());
        emailTextView.setText(info.getEmail());
        websiteTextView.setText(info.getWebsite());
        addressTextView.setText(info.getAddress());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera: ", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindPreview(@NonNull ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build();

        imageCapture = new ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build();

        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        Camera camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
    }

    private void takePictureAndAnalyze() {
        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(new File(getCacheDir(), "tmp.jpg")).build();
        imageCapture.takePicture(outputFileOptions, ContextCompat.getMainExecutor(this), new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                Uri savedUri = outputFileResults.getSavedUri();
                if (savedUri == null) {
                    savedUri = Uri.fromFile(new File(getCacheDir(), "tmp.jpg"));
                }
                analyzeImage(savedUri);
            }

            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                Log.e(TAG, "Error capturing image: ", exception);
            }
        });
    }

    private void analyzeImage(Uri uri) {
        Bitmap bitmap = BitmapFactory.decodeFile(uri.getPath());
        Mat src = new Mat(bitmap.getHeight(), bitmap.getWidth(), CvType.CV_8UC4);
        org.opencv.android.Utils.bitmapToMat(bitmap, src);
        Mat gray = new Mat();
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY);
        Imgproc.adaptiveThreshold(gray, gray, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY_INV, 11, 2);

        Bitmap processedBitmap = Bitmap.createBitmap(gray.cols(), gray.rows(), Bitmap.Config.ARGB_8888);
        org.opencv.android.Utils.matToBitmap(gray, processedBitmap);

        tessBaseAPI.setImage(processedBitmap);
        String recognizedText = tessBaseAPI.getUTF8Text();

        processRecognizedText(recognizedText);
    }

    private void processRecognizedText(String text) {
        Log.d(TAG, "Recognized text: " + text);

        // You can now use the recognized text to extract relevant information from the business card
        // For example, you can use regular expressions to extract phone numbers, email addresses, and more

        // Parse the recognized text into a BusinessCardInfo object.
        BusinessCardInfo extractedInfo = extractBusinessCardInfo(text);
        updateTextViews(extractedInfo);
    }

    /**
     * Extracts common business card fields from the raw OCR text.
     * The method performs best‑effort parsing and might return empty strings
     * for fields that cannot be detected.
     */
    private BusinessCardInfo extractBusinessCardInfo(String text) {
        String name = "";
        String phone = "";
        String email = "";
        String website = "";
        String company = "";
        String title = "";
        String address = "";

        // Regular expressions for phone numbers, emails and websites
        Pattern phonePattern = Pattern.compile("(\\+?\\d[\\d\\s().-]{7,}\\d)");
        Pattern emailPattern = Pattern.compile("[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}", Pattern.CASE_INSENSITIVE);
        Pattern websitePattern = Pattern.compile("(https?://)?(www\\.)?[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");

        Matcher matcher = phonePattern.matcher(text);
        if (matcher.find()) {
            phone = matcher.group(1).trim();
        }

        matcher = emailPattern.matcher(text);
        if (matcher.find()) {
            email = matcher.group().trim();
        }

        matcher = websitePattern.matcher(text);
        if (matcher.find()) {
            website = matcher.group().trim();
        }

        // Split text into lines for easier heuristics on name, title, etc.
        String[] lines = text.split("\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                continue;
            }

            if (name.isEmpty() && !line.matches(".*\\d.*") && !line.contains("@")
                    && !line.toLowerCase().contains("www")) {
                name = line;
                continue;
            }

            if (title.isEmpty() && !line.matches(".*\\d.*") && !line.contains("@")
                    && !line.toLowerCase().contains("www")) {
                title = line;
                continue;
            }

            if (company.isEmpty() && !line.contains("@") && !line.toLowerCase().contains("www")) {
                company = line;
                continue;
            }

            if (address.isEmpty() && (line.matches(".*\\d+.*") || line.toLowerCase().contains("street")
                    || line.toLowerCase().contains("road") || line.toLowerCase().contains("ave"))) {
                address = line;
            }
        }

        return new BusinessCardInfo(name, title, company, phone, email, website, address);
    }

    private void updateTextViews(BusinessCardInfo info) {
        TextView nameTextView = findViewById(R.id.name);
        TextView titleTextView = findViewById(R.id.Title);
        TextView companyTextView = findViewById(R.id.company);
        TextView phoneNumberTextView = findViewById(R.id.phone_number);
        TextView emailTextView = findViewById(R.id.email);
        TextView websiteTextView = findViewById(R.id.website);
        TextView addressTextView = findViewById(R.id.address);

        nameTextView.setText(info.getName());
        titleTextView.setText(info.getJobTitle());
        companyTextView.setText(info.getBusinessName());
        phoneNumberTextView.setText(info.getPhoneNumber());
        emailTextView.setText(info.getEmail());
        websiteTextView.setText(info.getWebsite());
        addressTextView.setText(info.getAddress());
    }

    private void initializeTesseract(String language) {
        String datapath = getFilesDir() + "/tesseract/";
        tessBaseAPI.init(datapath, language);
    }

    private void copyTrainedData() {
        String[] paths = new String[]{getFilesDir() + "/tesseract/", getFilesDir() + "/tesseract/tessdata/"};
        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.e(TAG, "ERROR: Creation of directory " + path + " failed");
                    return;
                }
            }
        }
        String trainedData = getFilesDir() + "/tesseract/tessdata/" + TESS_LANG + ".traineddata";
        File file = new File(trainedData);
        if (!file.exists()) {
            try {
                InputStream in = getAssets().open("tessdata/" + TESS_LANG + ".traineddata");
                OutputStream out = new FileOutputStream(trainedData);

                byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                Log.e(TAG, "Error copying trained data: ", e);
            }
        }
    }

    private void requestPermissions() {
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, REQUEST_CODE_PERMISSIONS);
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : PERMISSIONS_REQUIRED) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
                finish();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }
}

