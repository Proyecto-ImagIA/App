package com.example.appimagia.ui.ullada;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Log;
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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class UlladaFragment extends Fragment{
    private static final String[] CAMERA_PERMISSION = new String[]{android.Manifest.permission.CAMERA};
    private static final int CAMERA_REQUEST_CODE = 10;
    private FragmentUlladaBinding binding;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ProcessCameraProvider cameraProvider;
    private ImageCapture imageCapture = new ImageCapture.Builder().build();
    private Preview preview;
    private Camera camera;
    private Button btnCapture;
    private ImageView imageView;
    private String serverUrl;
    private TextToSpeech t1;
    // Sensores
    private SensorEventListener sensorListener;
    private float xValue;
    private float yValue;
    private float zValue;
    private float xValueT;
    private float yValueT;
    private float zValueT;
    private int golpe = 0;
    private Timer timer;
    private GolpeSensor golpeSensor;
    private static final int GOLPE_THRESHOLD = 10;
    private boolean primerGolpe = false;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentUlladaBinding.inflate(inflater, container, false);
        serverUrl = getString(R.string.server_url)+"/data";
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Configurar el sensor de golpes
        golpeSensor = new GolpeSensor((SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE));
        golpeSensor.setOnGolpeListener(() -> {
            if (!primerGolpe) {
                primerGolpe = true;
            } else {
                capturePhoto();
                primerGolpe = false;
            }
        });

        golpeSensor.iniciar();

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
                    //t1.setLanguage(new Locale("es", "ES"));
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

    private void compressImage(String imagePath) {
        File imageFile = new File(imagePath);
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream); // Ajusta la calidad de compresión según tus necesidades
            FileOutputStream fileOutputStream = new FileOutputStream(imageFile);
            fileOutputStream.write(outputStream.toByteArray());
            fileOutputStream.flush();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
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
                //compressImage(imagePath);
                ContentResolver contentResolver = requireContext().getContentResolver();
                InputStream inputStream = contentResolver.openInputStream(Uri.parse(imagePath));

                ////////////////////////////////////////////////////////////
                //Bitmap capturedImage = BitmapFactory.decodeFile(imagePath);
                //int newWidth = 400; // ancho deseado
                //int newHeight = 300; // alto deseado
                //Bitmap compressedBitmap = Bitmap.createScaledBitmap(capturedImage, newWidth, newHeight, true);
//
                //String compressedImageBase64 = bitmapToBase64(compressedBitmap);

                ////////////////////////////////////////////////////////////

                if (inputStream != null) {
                    String fotoBase64 = inputStreamToBase64(inputStream);
                    inputStream.close();

                    String data = "{" +
                            "\"type\": \"imatge\"" +
                            ",\"prompt\":\"Que ves en la foto?\"" +
                            ",\"imatge\": \"" + fotoBase64+"\"}";

                    URL url = new URL(serverUrl);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    JSONObject data1 = new JSONObject(data);

                    try (DataOutputStream wr = new DataOutputStream(conn.getOutputStream())) {
                        wr.writeBytes(data1.toString());
                        wr.flush();
                    }
                    handleServerResponse(conn, data1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();
        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }


    private void handleServerResponse(HttpURLConnection conn, JSONObject data) throws IOException, JSONException {
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
            responseData = "[" + responseData.replace("}{", "},{") + "]";

            JSONArray jsonArrayResponse = new JSONArray(responseData);
            String mensajeCompleto = "";
            // Iterar sobre cada objeto JSON en el arreglo y extraer el valor de la clave "response"
            for (int i = 0; i < jsonArrayResponse.length(); i++) {
                JSONObject jsonObject = jsonArrayResponse.getJSONObject(i);
                String serverMessage = jsonObject.getString("response");
                Log.i("info",serverMessage);
                mensajeCompleto += serverMessage;
            }
            readMessageAloud(mensajeCompleto);
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
/*
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
            return android.util.Base64.encodeToString(imageBytes, android.util.Base64.NO_WRAP);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

 */
private String inputStreamToBase64(InputStream inputStream) {
    try {
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

        int newWidth = bitmap.getWidth() / 2;
        int newHeight = bitmap.getHeight() / 2;
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 15, byteArrayOutputStream);
        byte[] imageBytes = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(imageBytes, Base64.NO_WRAP);
    } catch (Exception e) {
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
        registerSensorListener();
    }

    private void registerSensorListener() {
        SensorManager sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        } else {
            showToast("El dispositivo no tiene acelerÃ³metro");
        }
    }

    private void unregisterSensorListener() {
        SensorManager sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        sensorManager.unregisterListener(sensorListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        golpeSensor.detener();

        binding = null;
    }
}

class GolpeSensor implements SensorEventListener {
    private static final float GRAVITY_THRESHOLD = 15.0f;
    private static final long TIME_THRESHOLD = 800;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean golpe1Detectado = false;
    private long tiempoGolpe1 = 0;
    private OnGolpeListener onGolpeListener;
    private Handler handler = new Handler();

    public GolpeSensor(SensorManager sensorManager) {
        this.sensorManager = sensorManager;
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    public void setOnGolpeListener(OnGolpeListener listener) {
        this.onGolpeListener = listener;
    }

    public void iniciar() {
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    public void detener() {
        sensorManager.unregisterListener(this);
        handler.removeCallbacksAndMessages(null);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            float aceleracion = (float) Math.sqrt(x * x + y * y + z * z);

            if (aceleracion > GRAVITY_THRESHOLD) {
                if (!golpe1Detectado) {
                    golpe1Detectado = true;
                    tiempoGolpe1 = System.currentTimeMillis();

                    handler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            golpe1Detectado = false;
                        }
                    }, TIME_THRESHOLD);
                } else {
                    long tiempoActual = System.currentTimeMillis();
                    if (tiempoActual - tiempoGolpe1 <= TIME_THRESHOLD) {
                        if (onGolpeListener != null) {
                            onGolpeListener.onDosGolpes();
                        }
                        // Restablecer estado
                        golpe1Detectado = false;
                        tiempoGolpe1 = 0;
                    }
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    public interface OnGolpeListener {
        void onDosGolpes();
    }
}
