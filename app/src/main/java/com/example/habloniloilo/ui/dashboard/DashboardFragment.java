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
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private Handler backgroundHandler;
    private HandlerThread backgroundThread;

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

        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 1);
        } else {
            startCamera();
        }

        captureButton.setOnClickListener(v -> captureImage());

        return root;
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
            Map<Integer, Integer> colorCount = new HashMap<>();

            // Sample pixels from the image
            for (int x = 0; x < width; x += 10) {
                for (int y = 0; y < height; y += 10) {
                    int pixel = bitmap.getPixel(x, y);
                    // Ignore transparent pixels
                    if ((pixel & 0xFF000000) != 0) {
                        // Quantize colors to reduce the number of unique colors
                        int quantizedColor = quantizeColor(pixel);
                        colorCount.put(quantizedColor, colorCount.getOrDefault(quantizedColor, 0) + 1);
                    }
                }
            }

            // Sort colors by frequency
            List<Map.Entry<Integer, Integer>> sortedColors = new ArrayList<>(colorCount.entrySet());
            Collections.sort(sortedColors, (a, b) -> b.getValue().compareTo(a.getValue()));

            // Get top 5 colors
            List<Integer> dominantColors = new ArrayList<>();
            for (int i = 0; i < Math.min(5, sortedColors.size()); i++) {
                dominantColors.add(sortedColors.get(i).getKey());
            }

            return dominantColors;
        } catch (Exception e) {
            Log.e(TAG, "Error extracting colors", e);
            return new ArrayList<>();
        }
    }

    private int quantizeColor(int color) {
        // Quantize to 4 bits per channel
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        
        r = (r >> 4) << 4;
        g = (g >> 4) << 4;
        b = (b >> 4) << 4;
        
        return 0xFF000000 | (r << 16) | (g << 8) | b;
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
                        createCameraPreview();
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

    private void createCameraPreview() {
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
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(requireContext(), "Configuration failed", Toast.LENGTH_SHORT).show();
                }
            }, backgroundHandler);
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