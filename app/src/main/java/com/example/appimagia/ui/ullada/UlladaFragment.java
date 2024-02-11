package com.example.appimagia.ui.ullada;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.icu.text.SimpleDateFormat;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.example.appimagia.R;
import com.example.appimagia.databinding.FragmentUlladaBinding;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class UlladaFragment extends Fragment{
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Preview preview;
    private static final String[] CAMERA_PERMISSION = new String[]{android.Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private FragmentUlladaBinding binding;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private Button btnCapture;
    private ImageCapture imageCapture = new ImageCapture.Builder().build();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUlladaBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (hasCameraPermission()) {
            initializeCamera();
        } else {
            requestPermission();
        }
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        btnCapture = view.findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(v -> capturePhoto());
    }

    void bindPreview() {
        preview = new Preview.Builder().
                build();
        CameraSelector cameraSelector = new CameraSelector.
                Builder().
                requireLensFacing(CameraSelector.LENS_FACING_BACK).
                build();
        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
        camera = cameraProvider.bindToLifecycle( getViewLifecycleOwner(), cameraSelector, preview,imageCapture);
    }

    private void initializeCamera() {
        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                if (binding != null) {
                    bindPreview();
                }
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(requireContext()));
    }

    private boolean hasCameraPermission() {
        return ContextCompat.checkSelfPermission(
                requireActivity(),
                android.Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestPermission() {
        ActivityCompat.requestPermissions(
                requireActivity(),
                CAMERA_PERMISSION,
                CAMERA_REQUEST_CODE);
    }

    private void capturePhoto() {
        if (cameraProvider != null) {
            long timeStamp = System.currentTimeMillis();
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timeStamp);
            contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

            imageCapture.takePicture(
                    new ImageCapture.OutputFileOptions.Builder(
                            requireContext().getContentResolver(),
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                            contentValues
                    ).build(),
                    getExecutor(),
                    new ImageCapture.OnImageSavedCallback() {
                        @Override
                        public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                            Toast.makeText(requireContext(), "Photo saved", Toast.LENGTH_SHORT).show();
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            Toast.makeText(requireContext(), "Error saving photo", Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(requireContext());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (hasCameraPermission()) {
            initializeCamera();
        } else {
            requestPermission();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
            cameraProvider = null;
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        binding = null;
    }
}