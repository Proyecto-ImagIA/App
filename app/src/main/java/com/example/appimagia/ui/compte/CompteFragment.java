package com.example.appimagia.ui.compte;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.appimagia.R;
import com.example.appimagia.databinding.FragmentCompteBinding;
import com.google.gson.Gson;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class CompteFragment extends Fragment {

    private FragmentCompteBinding binding;
    private EditText editTextNickname;
    private EditText editTextEmail;
    private EditText editTextPhone;
    private EditText editTextPassword;
    private Button buttonSend;
    private String serverUrl;


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        CompteViewModel compteViewModel =
                new ViewModelProvider(this).get(CompteViewModel.class);

        binding = FragmentCompteBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        editTextNickname = root.findViewById(R.id.editTextNickname);
        editTextEmail = root.findViewById(R.id.editTextEmail);
        editTextPhone = root.findViewById(R.id.editTextPhone);
        editTextPassword = root.findViewById(R.id.editTextPassword);
        buttonSend = root.findViewById(R.id.buttonSend);
        serverUrl = getString(R.string.server_url)+"/api/user/register";

        buttonSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                enviarCredenciales();
            }
        });

        return root;
    }

    private void enviarCredenciales() {
        //mostrarDialogo();
        String nickname = editTextNickname.getText().toString();
        String email = editTextEmail.getText().toString();
        String telefon = editTextPhone.getText().toString();
        String password = editTextPassword.getText().toString();

        if (nickname.isEmpty() || email.isEmpty() || telefon.isEmpty() || password.isEmpty()) {
            Toast.makeText(getContext(), "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        RequestBody requestBody = new RequestBody();
        requestBody.setNickname(nickname);
        requestBody.setEmail(email);
        requestBody.setPhone(telefon);
        requestBody.setPassword(password);

        Gson gson = new Gson();
        String json = gson.toJson(requestBody);

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String data = "{" +
                            "\"nickname\": \""+nickname+"\"" +
                            ",\"email\":\""+email+"\"" +
                            ",\"contrasenya\":\""+password+"\"" +
                            ",\"telefon\": \""+ telefon+"\"}";

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

                    handleServerResponse(conn);
                    /*
                    try (OutputStream os = conn.getOutputStream()) {
                        byte[] input = json.getBytes(StandardCharsets.UTF_8);
                        os.write(input, 0, input.length);
                    }

                    int responseCode = conn.getResponseCode();

                    conn.disconnect();
                    */
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
        Toast.makeText(getContext(), "Enviando credenciales...", Toast.LENGTH_SHORT).show();
    }

    private void handleServerResponse(HttpURLConnection conn) throws IOException, JSONException {
        int responseCode = conn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {

            showToast("OKKKKK");
            //mostrarDialogo();

            /*
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
            //readMessageAloud(mensajeCompleto);
             */
        } else {

            showToast("Error en la solicitud al servidor: " + responseCode);
        }
        conn.disconnect();
    }

    private void handleServer(HttpURLConnection conn) throws IOException, JSONException {
        int responseCode = conn.getResponseCode();

        if (responseCode == HttpURLConnection.HTTP_OK) {

/*
Almacena KEY
 */
        } else {

            showToast("Error en la solicitud al servidor: " + responseCode);
        }
        conn.disconnect();
    }



    /*
    private void mostrarDialogo() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_sms, null);
        builder.setView(dialogView);

        EditText editTextSMS = dialogView.findViewById(R.id.editTextSMS);

        builder.setMessage("Escribe tu mensaje:");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String smsText = editTextSMS.getText().toString();
                // Aquí puedes hacer algo con el texto del SMS
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
*/


    private void showToast(final String message) {
        requireActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    static class RequestBody {
        private String nickname;
        private String email;
        private String telefon;
        private String contrasenya;

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return telefon;
        }

        public void setPhone(String phone) {
            this.telefon = phone;
        }

        public void setPassword(String password){
            this.contrasenya = password;
        }

        private String getPassword(){
            return contrasenya;
        }
    }

    static class VerificarBody {
        private String phone;
        private String code;

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
