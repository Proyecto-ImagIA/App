package com.example.appimagia;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.appimagia.databinding.ActivityMainBinding;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private String phone;
    private boolean verificado;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        showAlertDialog();

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home,
                R.id.navigation_dashboard,
                R.id.navigation_notifications)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        navView.setSelectedItemId(R.id.navigation_dashboard);
    }

    private void showAlertDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("¿Tienes cuenta?");
        builder.setPositiveButton("Sí", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showLogin();
            }
        });
        builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                showRegistration();
            }
        });
        builder.setCancelable(false);
        builder.show();
    }

    private void showRegistration() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.register_dialog_layout, null);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        EditText editTextNickname = dialogView.findViewById(R.id.editTextNickname);
        EditText editTextEmail = dialogView.findViewById(R.id.editTextEmail);
        EditText editTextPassword = dialogView.findViewById(R.id.editTextPassword);
        EditText editTextPhone = dialogView.findViewById(R.id.editTextPhone);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);
        Button buttonSend = dialogView.findViewById(R.id.buttonSend);

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
                alertDialog.dismiss();
            }
        });

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                phone = editTextPhone.getText().toString();
                registrationPost(
                        editTextNickname.getText().toString(),
                        editTextEmail.getText().toString(),
                        editTextPassword.getText().toString(),
                        editTextPhone.getText().toString());
                alertDialog.dismiss();
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void showVerificationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.verificacion_dialog_layout, null);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        EditText editTextCode = dialogView.findViewById(R.id.editTextCode);
        Button buttonVerify = dialogView.findViewById(R.id.buttonVerify);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        buttonVerify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                verificationPost(phone, editTextCode.getText().toString(), new VerificationCallback() {
                    @Override
                    public void onVerificationResult(boolean verified) {
                        if (verified) {
                            alertDialog.dismiss();
                        } else {
                            showToast("Código de verificación incorrecto");
                        }
                    }
                });
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRegistration();
                alertDialog.dismiss();
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void showLogin() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.login_dialog_layout, null);
        builder.setView(dialogView);
        AlertDialog alertDialog = builder.create();

        EditText editTextEmail = dialogView.findViewById(R.id.editTextEmail);
        EditText editTextPassword = dialogView.findViewById(R.id.editTextPassword);
        Button buttonSend = dialogView.findViewById(R.id.buttonSend);
        Button buttonCancel = dialogView.findViewById(R.id.buttonCancel);

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //handleLoginButtonClick(editTextEmail.getText().toString(), editTextPassword.getText().toString());
                loginPost(editTextEmail.getText().toString(),editTextPassword.getText().toString());
            }
        });

        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAlertDialog();
                alertDialog.dismiss();
            }
        });

        alertDialog.setCancelable(false);
        alertDialog.show();
    }

    private void registrationPost(String nickname, String email, String contrasenya, String telefon) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("nickname", nickname);
                    jsonBody.put("email", email);
                    jsonBody.put("contrasenya", contrasenya);
                    jsonBody.put("telefon", telefon);

                    URL url = new URL(getString(R.string.server_url) + "/api/user/register"); ///api/user/validate
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    try (OutputStream outputStream = conn.getOutputStream()) {
                        outputStream.write(jsonBody.toString().getBytes());
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Leer la respuesta del servidor
                        InputStream inputStream = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        inputStream.close();

                        // Registro de información (Log) para mostrar el JSON recibido del servidor
                        Log.i("Server Response", "JSON recibido del servidor: " + response.toString());

                        // Procesar la respuesta del servidor
                        String serverResponse = response.toString();
                        // Hacer algo con la respuesta del servidor aquí

                        //guardarArchivoJson(getContext(),serverResponse,"key.json");
                        //showVerificationDialog();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showVerificationDialog();
                            }
                        });

                    } else {
                        // Manejar el error de respuesta del servidor
                        showToast("Error al enviar SMS: " + responseCode);
                    }

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void verificationPost(String phone, String code, VerificationCallback callback) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                boolean verificado = false;
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("phone", phone);
                    jsonBody.put("code", code);

                    URL url = new URL(getString(R.string.server_url) + "/api/user/validate");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    try (OutputStream outputStream = conn.getOutputStream()) {
                        outputStream.write(jsonBody.toString().getBytes());
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Leer la respuesta del servidor
                        InputStream inputStream = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        inputStream.close();

                        // Registro de información (Log) para mostrar el JSON recibido del servidor
                        Log.i("Server Response", "JSON recibido del servidor: " + response.toString());

                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String apiKey = jsonResponse.getString("apiKey");

                        guardarApiKeyEnSharedPreferences(apiKey);
                        verificado = true;
                    } else {
                    }
                    conn.disconnect();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                callback.onVerificationResult(verificado);

            }
        }).start();
    }

    private void guardarApiKeyEnSharedPreferences(String apiKey) {
        SharedPreferences sharedPreferences = getSharedPreferences("MiArchivo", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("apiKey", apiKey);
        editor.apply();
    }

    interface VerificationCallback {
        void onVerificationResult(boolean verified);
    }

    /*
    ARREGLAR URL
     */
    private void loginPost(String email, String password) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    JSONObject jsonBody = new JSONObject();
                    jsonBody.put("email", email);
                    jsonBody.put("password", password);

                    URL url = new URL(getString(R.string.server_url) + "/api/user/validate");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    try (OutputStream outputStream = conn.getOutputStream()) {
                        outputStream.write(jsonBody.toString().getBytes());
                    }

                    int responseCode = conn.getResponseCode();
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        // Leer la respuesta del servidor
                        InputStream inputStream = conn.getInputStream();
                        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder response = new StringBuilder();
                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line);
                        }
                        reader.close();
                        inputStream.close();

                        // Registro de información (Log) para mostrar el JSON recibido del servidor
                        Log.i("Server Response", "JSON recibido del servidor: " + response.toString());

                        // Procesar la respuesta del servidor
                        String serverResponse = response.toString();
                        // Hacer algo con la respuesta del servidor aquí

                    } else {
                        // Manejar el error de respuesta del servidor
                        showToast("Error al enviar SMS: " + responseCode);
                    }

                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void showToast(final String message) {
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(MainActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
}