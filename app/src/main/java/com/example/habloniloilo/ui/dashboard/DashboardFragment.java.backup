package com.example.habloniloilo.ui.dashboard;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.util.Rational;
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
import com.example.habloniloilo.databinding.FragmentDashboardBinding;
import com.google.android.material.button.MaterialButton;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private String currentFilter = null; // Track current color filter

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDashboardBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Enable back button in action bar
        AppCompatActivity activity = (AppCompatActivity) requireActivity();
        if (activity.getSupportActionBar() != null) {
            activity.getSupportActionBar().setTitle("Camera");
            activity.getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        cameraPreview = binding.cameraPreview;
        captureButton = binding.captureButton;

        // Set up color filter buttons
        setupColorFilters(root);

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            startCamera();
        }

        captureButton.setOnClickListener(v -> captureImage());

        return root;
    }

    private void setupColorFilters(View root) {
        int[] filterButtonIds = {
            R.id.filter_red, R.id.filter_blue, R.id.filter_yellow,
            R.id.filter_green, R.id.filter_orange, R.id.filter_violet,
            R.id.filter_brown
        };

        for (int buttonId : filterButtonIds) {
            MaterialButton button = root.findViewById(buttonId);
            button.setOnClickListener(v -> {
                // Toggle filter
                if (currentFilter != null && currentFilter.equals(button.getText().toString())) {
                    currentFilter = null;
                    button.setStrokeWidth(0);
                } else {
                    // Reset all buttons
                    for (int id : filterButtonIds) {
                        MaterialButton btn = root.findViewById(id);
                        btn.setStrokeWidth(0);
                    }
                    currentFilter = button.getText().toString();
                    button.setStrokeWidth(4);
                }
                // Restart preview with new filter
                if (captureSession != null) {
                    try {
                        captureSession.stopRepeating();
                        createCameraPreviewSession();
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    private void createCameraPreviewSession() throws CameraAccessException {
        SurfaceTexture texture = cameraPreview.getSurfaceTexture();
        texture.setDefaultBufferSize(1920, 1080);
        Surface surface = new Surface(texture);

        final CaptureRequest.Builder previewRequestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        previewRequestBuilder.addTarget(surface);

        // Add color filter to the preview request
        if (currentFilter != null) {
            // Apply color filter to remove the selected color
            switch (currentFilter) {
                case "Red":
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, getColorMatrix(0.0f, 1.0f, 1.0f));
                    break;
                case "Blue":
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, getColorMatrix(1.0f, 1.0f, 0.0f));
                    break;
                case "Yellow":
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, getColorMatrix(0.0f, 0.0f, 1.0f));
                    break;
                case "Green":
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, getColorMatrix(1.0f, 0.0f, 1.0f));
                    break;
                case "Orange":
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, getColorMatrix(0.0f, 0.5f, 1.0f));
                    break;
                case "Violet":
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, getColorMatrix(0.5f, 1.0f, 0.0f));
                    break;
                case "Brown":
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
                    previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, getColorMatrix(0.4f, 0.7f, 1.0f));
                    break;
            }
        }

        cameraDevice.createCaptureSession(Arrays.asList(surface), new CameraCaptureSession.StateCallback() {
            @Override
            public void onConfigured(@NonNull CameraCaptureSession session) {
                if (cameraDevice == null) return;
                captureSession = session;
                try {
                    previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                            CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                    captureSession.setRepeatingRequest(previewRequestBuilder.build(), null, backgroundHandler);
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                Toast.makeText(requireContext(), "Configuration failed", Toast.LENGTH_SHORT).show();
            }
        }, backgroundHandler);
    }

    private ColorSpaceTransform getColorMatrix(float r, float g, float b) {
        Rational[] matrix = new Rational[9];
        matrix[0] = new Rational((int)(r * 100), 100); matrix[1] = new Rational(0, 1); matrix[2] = new Rational(0, 1);
        matrix[3] = new Rational(0, 1); matrix[4] = new Rational((int)(g * 100), 100); matrix[5] = new Rational(0, 1);
        matrix[6] = new Rational(0, 1); matrix[7] = new Rational(0, 1); matrix[8] = new Rational((int)(b * 100), 100);
        return new ColorSpaceTransform(matrix);
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
                    processImage();
                }
            }, backgroundHandler);
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access error", e);
            Toast.makeText(requireContext(), "Camera access error", Toast.LENGTH_SHORT).show();
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

            navigateToColorDisplay(dominantColors);
        } catch (Exception e) {
            Log.e(TAG, "Error processing image", e);
            Toast.makeText(requireContext(), "Error processing image", Toast.LENGTH_SHORT).show();
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

    private void navigateToColorDisplay(List<Integer> colors) {
        try {
            Bundle args = new Bundle();
            args.putIntegerArrayList("colors", new ArrayList<>(colors));
            Navigation.findNavController(requireView())
                    .navigate(R.id.navigation_color_display, args);
        } catch (Exception e) {
            Log.e(TAG, "Navigation error", e);
            Toast.makeText(requireContext(), "Error displaying colors", Toast.LENGTH_SHORT).show();
        }
    }

    private void startCamera() {
        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());

        cameraPreview.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surface, int width, int height) {
                openCamera();
            }

            @Override
            public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surface, int width, int height) {
                // Handle surface size change
            }

            @Override
            public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surface) {
                return false;
            }

            @Override
            public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surface) {
                // Handle surface updates
            }
        });
    }

    private void openCamera() {
        CameraManager manager = (CameraManager) requireContext().getSystemService(requireContext().CAMERA_SERVICE);
        try {
            String cameraId = manager.getCameraIdList()[0];
            if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                    == PackageManager.PERMISSION_GRANTED) {
                manager.openCamera(cameraId, new CameraDevice.StateCallback() {
                    @Override
                    public void onOpened(@NonNull CameraDevice camera) {
                        cameraDevice = camera;
                        try {
                            createCameraPreviewSession();
                        } catch (CameraAccessException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onDisconnected(@NonNull CameraDevice camera) {
                        cameraDevice.close();
                    }

                    @Override
                    public void onError(@NonNull CameraDevice camera, int error) {
                        cameraDevice.close();
                        cameraDevice = null;
                    }
                }, backgroundHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
        if (requestCode == 1) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
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
        binding = null;
    }
}