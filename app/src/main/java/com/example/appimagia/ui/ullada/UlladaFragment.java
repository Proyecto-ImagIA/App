package com.example.appimagia.ui.ullada;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class UlladaFragment extends Fragment{
    private static final String[] CAMERA_PERMISSION = new String[]{android.Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private FragmentUlladaBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private Preview preview;
    private ProcessCameraProvider cameraProvider;
    private Camera camera;
    private Button btnCapture;
    private ImageCapture imageCapture = new ImageCapture.Builder().build();
    private ImageView imageView;
    private String serverUrl;
    private TextToSpeech t1;
    private SensorEventListener sensorListener;
    private float xValue;
    private float yValue;
    private float zValue;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUlladaBinding.inflate(inflater, container, false);
        serverUrl = getString(R.string.server_url);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
//
        sensorListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent sensorEvent) {
                // Aquí puedes colocar la lógica para detectar los movimientos del dispositivo
                // y tomar una foto si se cumple cierta condición.
                // Por ejemplo:
                float xValue = sensorEvent.values[0];
                float yValue = sensorEvent.values[1];
                float zValue = sensorEvent.values[2];


                // Realizar la lógica para detectar el movimiento deseado y tomar la foto
                /*
                if () {
                    capturePhoto();
                }

                 */
            }

            @Override
            public void onAccuracyChanged(Sensor sensor, int i) {

            }
        };

        if (hasCameraPermission()) {
            initializeCamera();
        } else {
            requestPermission();
        }

        t1 = new TextToSpeech(requireContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.UK);
                }
            }
        });

        imageView = view.findViewById(R.id.imageView3);
        cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext());
        btnCapture = view.findViewById(R.id.btnCapture);
        btnCapture.setOnClickListener(v -> capturePhoto());
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

    private void bindCameraUseCases() {
        preview = new Preview.Builder().
                build();

        CameraSelector cameraSelector = new CameraSelector.
                Builder().
                requireLensFacing(CameraSelector.LENS_FACING_BACK).
                build();

        preview.setSurfaceProvider(binding.previewView.getSurfaceProvider());
        camera = cameraProvider.bindToLifecycle(
                getViewLifecycleOwner(),
                cameraSelector,
                preview,
                imageCapture);
    }

    private void initializeCamera() {
        if (cameraProviderFuture != null) {
            cameraProviderFuture.addListener(() -> {
                try {
                    cameraProvider = cameraProviderFuture.get();
                    if (binding != null) {
                        bindCameraUseCases();
                    }
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }, ContextCompat.getMainExecutor(requireContext()));
        }
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
                            String imagePath = outputFileResults.getSavedUri().toString();
                            if (!imagePath.isEmpty()) {
                                handleImageCaptureSuccess(imagePath);
                            }
                        }

                        @Override
                        public void onError(@NonNull ImageCaptureException exception) {
                            showToast("Error saving photo");
                        }
                    });
        }
    }

    private void handleImageCaptureSuccess(String imagePath) {
        Toast.makeText(requireContext(), imagePath, Toast.LENGTH_SHORT).show();
        binding.imageView3.setImageURI(Uri.parse(imagePath));
        sendImageToServer(imagePath);
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(requireContext());
    }

    private void sendImageToServer(String imagePath) {
        new Thread(() -> {
            try {
                ContentResolver contentResolver = requireContext().getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(Uri.parse(imagePath));
                if (inputStream != null) {
                    String fotoBase64 = inputStreamToBase64(inputStream);
                    inputStream.close();
                    String data = "{\"foto\": \"" + fotoBase64 + "\"}";

                    URL url = new URL(serverUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                        wr.writeBytes(data);
                        wr.flush();
                    }

                    handleServerResponse(conn);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void handleServerResponse(HttpURLConnection conn) throws IOException, JSONException {
        int responseCode = conn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {
            InputStream inputStreamServer = conn.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStreamServer));
            StringBuilder response = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                response.append(line);
            }

            reader.close();
            inputStreamServer.close();

            String responseData = response.toString();
            JSONObject jsonResponse = new JSONObject(responseData);
            String serverMessage = jsonResponse.getString("mensaje");

            readMessageAloud(serverMessage);
        } else {
            showToast("Error en la solicitud al servidor: " + responseCode);
        }
        conn.disconnect();
    }

    private void readMessageAloud(String message) {
        if (t1 != null) {
            t1.speak(message, TextToSpeech.QUEUE_FLUSH, null);
        }
    }

    private String inputStreamToBase64(InputStream inputStream) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            byte[] buf = new byte[1024];
            int bytesRead;

            while ((bytesRead = inputStream.read(buf)) != -1) {
                bos.write(buf, 0, bytesRead);
            }

            byte[] imageBytes = bos.toByteArray();
            inputStream.close();
            bos.close();
            return android.util.Base64.encodeToString(imageBytes, android.util.Base64.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showToast(final String message) {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
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
    public void onResume() {
        super.onResume();
        //registerSensorListener();
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