package com.example.habloniloilo.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.habloniloilo.R;
import com.example.habloniloilo.database.HybridDatabaseManager;
import com.example.habloniloilo.cloud.NetworkStateMonitor;
import com.example.habloniloilo.databinding.FragmentDashboardBinding;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardFragment extends Fragment {

    private static final String TAG = "DashboardFragment";
    private FragmentDashboardBinding binding;
    private TextureView cameraPreview;
    private ImageButton captureButton;
    private CameraDevice cameraDevice;
    private CameraCaptureSession captureSession;
    private Size previewSize;
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;
    private HybridDatabaseManager hybridDbManager;
    private NetworkStateMonitor networkMonitor;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        try {
            binding = FragmentDashboardBinding.inflate(inflater, container, false);
            View root = binding.getRoot();

            // Initialize hybrid database manager
            hybridDbManager = new HybridDatabaseManager(requireContext());
            
            // Initialize network monitor
            networkMonitor = new NetworkStateMonitor(requireContext(), hybridDbManager);
            networkMonitor.startMonitoring();

            // Enable back button in action bar
            AppCompatActivity activity = (AppCompatActivity) requireActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().setTitle("Camera");
                activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            }

            // Check if we have the required views
            if (binding.cameraPreview != null) {
                cameraPreview = binding.cameraPreview;
                Log.d(TAG, "Camera preview found");
            } else {
                Log.e(TAG, "cameraPreview not found in layout");
                Toast.makeText(requireContext(), "Camera preview not found", Toast.LENGTH_SHORT).show();
                return root;
            }

            if (binding.captureButton != null) {
                captureButton = binding.captureButton;
                Log.d(TAG, "Capture button found");
            } else {
                Log.e(TAG, "captureButton not found in layout");
                Toast.makeText(requireContext(), "Capture button not found", Toast.LENGTH_SHORT).show();
                return root;
            }

            // Check camera permission
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    != PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Requesting camera permission");
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
            } else {
                Log.d(TAG, "Camera permission granted, starting camera");
                startCamera();
            }

            captureButton.setOnClickListener(v -> {
                Log.d(TAG, "Capture button clicked");
                captureImage();
            });

            // Show sync status
            String syncStatus = hybridDbManager.getSyncStatus();
            Toast.makeText(requireContext(), syncStatus, Toast.LENGTH_SHORT).show();

            return root;
        } catch (Exception e) {
            Log.e(TAG, "Error in onCreateView", e);
            Toast.makeText(requireContext(), "Error initializing camera: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return inflater.inflate(R.layout.fragment_dashboard, container, false);
        }
    }

    private void createCameraPreviewSession() throws CameraAccessException {
        try {
            SurfaceTexture texture = cameraPreview.getSurfaceTexture();
            texture.setDefaultBufferSize(1920, 1080);
            Surface surface = new Surface(texture);

            final CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            previewRequestBuilder.addTarget(surface);

            cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    if (cameraDevice == null) return;
                    captureSession = session;
                    try {
                        previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                        captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                        Log.d(TAG, "Camera preview session configured successfully");
                    } catch (CameraAccessException e) {
                        Log.e(TAG, "Error setting repeating request", e);
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Log.e(TAG, "Camera configuration failed");
                    Toast.makeText(requireContext(), "Configuration failed", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
        } catch (Exception e) {
            Log.e(TAG, "Error creating camera preview session", e);
            Toast.makeText(requireContext(), "Error creating camera preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void captureImage() {
        if (cameraDevice == null) {
            Log.e(TAG, "Camera device is null");
            Toast.makeText(requireContext(), "Camera not ready", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            final CaptureRequest.Builder captureBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(new Surface(cameraPreview.getSurfaceTexture()));

            captureSession.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session,
                                             @NonNull CaptureRequest request,
                                             @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Log.d(TAG, "Image captured successfully");
                    processImage();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access error", e);
            Toast.makeText(requireContext(), "Camera access error", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error capturing image", e);
            Toast.makeText(requireContext(), "Error capturing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void processImage() {
        try {
            Bitmap bitmap = cameraPreview.getBitmap();
            if (bitmap == null) {
                Log.e(TAG, "Failed to get bitmap from preview");
                Toast.makeText(requireContext(), "Failed to capture image", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Processing image: " + bitmap.getWidth() + "x" + bitmap.getHeight());

            // Scale down the bitmap to make processing faster
            Bitmap scaledBitmap = Bitmap.createScaledBitmap(bitmap, 
                bitmap.getWidth() / 4, 
                bitmap.getHeight() / 4, 
                true);

            List<Integer> dominantColors = extractDominantColors(scaledBitmap);
            if (dominantColors.isEmpty()) {
                Log.e(TAG, "No colors extracted");
                Toast.makeText(requireContext(), "No colors found", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Extracted " + dominantColors.size() + " colors");

            // Add colors to hybrid database (local + cloud sync)
            for (Integer color : dominantColors) {
                hybridDbManager.addColor(color);
            }

            // Compare with existing colors and get comparison results
            ColorComparisonResult comparisonResult = compareColorsWithExisting(dominantColors);
            
            // Navigate to color display with comparison results
            navigateToColorDisplay(dominantColors, comparisonResult);
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(requireContext(), "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private List<Integer> extractDominantColors(Bitmap bitmap) {
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            Map<Integer, Integer> colorCounts = new HashMap<>();

            // Sample pixels from the image
            for (int x = 0; x < width; x += 10) {
                for (int y = 0; y < height; y += 10) {
                    int pixel = bitmap.getPixel(x, y);
                    // Ignore transparent pixels
                    if ((pixel & 0xFF000000) != 0) {
                        // Quantize colors to reduce the number of unique colors
                        int quantizedColor = quantizeColor(pixel);
                        
                        // Check if this color is similar to any existing color
                        boolean isSimilar = false;
                        for (int existingColor : colorCounts.keySet()) {
                            if (isColorSimilar(quantizedColor, existingColor)) {
                                // Add to the count of the similar color
                                colorCounts.put(existingColor, colorCounts.get(existingColor) + 1);
                                isSimilar = true;
                                break;
                            }
                        }
                        
                        // If not similar to any existing color, add as new
                        if (!isSimilar) {
                            colorCounts.put(quantizedColor, 1);
                        }
                    }
                }
            }

            // Convert map to list
            return new ArrayList<>(colorCounts.keySet());
        } catch (Exception e) {
            Log.e(TAG, "Error extracting colors", e);
            return new ArrayList<>();
        }
    }

    private int quantizeColor(int color) {
        // More aggressive quantization - reduce to 3 bits per channel
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        // Quantize to 3 bits (8 levels) per channel
        r = (r >> 5) << 5;
        g = (g >> 5) << 5;
        b = (b >> 5) << 5;
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    private boolean isColorSimilar(int color1, int color2) {
        // Extract RGB components
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        // Calculate color difference using weighted RGB
        double diff = Math.sqrt(
            Math.pow(r1 - r2, 2) * 0.3 +  // Red weight
            Math.pow(g1 - g2, 2) * 0.59 + // Green weight
            Math.pow(b1 - b2, 2) * 0.11   // Blue weight
        );
        
        // Consider colors similar if their difference is less than 30
        return diff < 30;
    }

    /**
     * Compare new colors with existing colors in the database
     * @param newColors List of colors extracted from the captured image
     * @return ColorComparisonResult containing comparison details
     */
    private ColorComparisonResult compareColorsWithExisting(List<Integer> newColors) {
        try {
            List<Integer> existingColors = hybridDbManager.getAllColors();
            List<Integer> similarColors = new ArrayList<>();
            List<Integer> uniqueColors = new ArrayList<>();
            List<ColorMatch> colorMatches = new ArrayList<>();

            for (int newColor : newColors) {
                boolean foundSimilar = false;
                int bestMatch = -1;
                double bestSimilarity = Double.MAX_VALUE;

                // Find the most similar existing color
                for (int existingColor : existingColors) {
                    if (isColorSimilar(newColor, existingColor)) {
                        foundSimilar = true;
                        double similarity = calculateColorSimilarity(newColor, existingColor);
                        if (similarity < bestSimilarity) {
                            bestSimilarity = similarity;
                            bestMatch = existingColor;
                        }
                    }
                }

                if (foundSimilar) {
                    similarColors.add(newColor);
                    colorMatches.add(new ColorMatch(newColor, bestMatch, bestSimilarity));
                } else {
                    uniqueColors.add(newColor);
                }
            }

            return new ColorComparisonResult(similarColors, uniqueColors, colorMatches, existingColors.size());
        } catch (Exception e) {
            Log.e(TAG, "Error comparing colors", e);
            return new ColorComparisonResult(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), 0);
        }
    }

    /**
     * Calculate similarity score between two colors (lower = more similar)
     */
    private double calculateColorSimilarity(int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF;
        int g1 = (color1 >> 8) & 0xFF;
        int b1 = color1 & 0xFF;
        
        int r2 = (color2 >> 16) & 0xFF;
        int g2 = (color2 >> 8) & 0xFF;
        int b2 = color2 & 0xFF;
        
        return Math.sqrt(
            Math.pow(r1 - r2, 2) * 0.3 +
            Math.pow(g1 - g2, 2) * 0.59 +
            Math.pow(b1 - b2, 2) * 0.11
        );
    }

    private void navigateToColorDisplay(List<Integer> colors, ColorComparisonResult comparisonResult) {
        try {
            Bundle args = new Bundle();
            args.putIntegerArrayList("colors", new ArrayList<>(colors));
            args.putIntegerArrayList("similar_colors", new ArrayList<>(comparisonResult.getSimilarColors()));
            args.putIntegerArrayList("unique_colors", new ArrayList<>(comparisonResult.getUniqueColors()));
            args.putInt("existing_colors_count", comparisonResult.getExistingColorsCount());
            Navigation.findNavController(requireView())
                    .navigate(R.id.navigation_color_display, args);
            Log.d(TAG, "Navigation successful");
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            Toast.makeText(requireContext(), "Error displaying colors: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        try {
            backgroundThread = new HandlerThread("CameraBackground");
            backgroundThread.start();
            backgroundHandler = new Handler(backgroundThread.getLooper());

            cameraPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
                @Override
                public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "Surface texture available: " + width + "x" + height);
                    openCamera();
                }

                @Override
                public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                    Log.d(TAG, "Surface texture size changed: " + width + "x" + height);
                }

                @Override
                public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                    Log.d(TAG, "Surface texture destroyed");
                    return false;
                }

                @Override
                public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                    // Handle surface updates
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera", e);
            Toast.makeText(requireContext(), "Error starting camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        try {
            CameraManager manager = (CameraManager) requireContext().getSystemService(requireContext().CAMERA_SERVICE);
            String cameraId = manager.getCameraIdList()[0];
            Log.d(TAG, "Opening camera: " + cameraId);
            
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        Log.d(TAG, "Camera opened successfully");
                        cameraDevice = camera;
                        try {
                            createCameraPreviewSession();
                        } catch (CameraAccessException e) {
                            Log.e(TAG, "Error creating camera preview session", e);
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        Log.d(TAG, "Camera disconnected");
                        cameraDevice.close();
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        Log.e(TAG, "Camera error: " + error);
                        cameraDevice.close();
                        cameraDevice = null;
                    }
                }, backgroundHandler);
            } else {
                Log.e(TAG, "Camera permission not granted");
                Toast.makeText(requireContext(), "Camera permission not granted", Toast.LENGTH_SHORT).show();
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access exception", e);
            Toast.makeText(requireContext(), "Camera access error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera", e);
            Toast.makeText(requireContext(), "Error opening camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Camera permission granted");
                startCamera();
            } else {
                Log.e(TAG, "Camera permission denied");
                Toast.makeText(requireContext(), "Camera permission required", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraDevice != null) {
            cameraDevice.close();
            cameraDevice = null;
        }
        if (backgroundThread != null) {
            backgroundThread.quitSafely();
            try {
                backgroundThread.join();
                backgroundThread = null;
                backgroundHandler = null;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        if (networkMonitor != null) {
            networkMonitor.stopMonitoring();
        }
        if (hybridDbManager != null) {
            hybridDbManager.shutdown();
        }
        binding = null;
    }

    /**
     * Data class to hold color comparison results
     */
    public static class ColorComparisonResult {
        private final List<Integer> similarColors;
        private final List<Integer> uniqueColors;
        private final List<ColorMatch> colorMatches;
        private final int existingColorsCount;

        public ColorComparisonResult(List<Integer> similarColors, List<Integer> uniqueColors, 
                                   List<ColorMatch> colorMatches, int existingColorsCount) {
            this.similarColors = similarColors;
            this.uniqueColors = uniqueColors;
            this.colorMatches = colorMatches;
            this.existingColorsCount = existingColorsCount;
        }

        public List<Integer> getSimilarColors() { return similarColors; }
        public List<Integer> getUniqueColors() { return uniqueColors; }
        public List<ColorMatch> getColorMatches() { return colorMatches; }
        public int getExistingColorsCount() { return existingColorsCount; }
    }

    /**
     * Data class to represent a color match between new and existing colors
     */
    public static class ColorMatch {
        private final int newColor;
        private final int existingColor;
        private final double similarity;

        public ColorMatch(int newColor, int existingColor, double similarity) {
            this.newColor = newColor;
            this.existingColor = existingColor;
            this.similarity = similarity;
        }

        public int getNewColor() { return newColor; }
        public int getExistingColor() { return existingColor; }
        public double getSimilarity() { return similarity; }
    }
}
