package com.example.controlbodega;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsuario, edtClave, edtClaveAdmin;
    private Button btnLogin, btnRegistrarUsuario;
    private TextView txtMensajeLogin;

    private static final HashMap<String, String> usuarios = new HashMap<>();
    private static final String CLAVE_MAESTRA_ADMIN = "admin123";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        if (usuarios.isEmpty()) {
            usuarios.put("admin", "admin123");
            usuarios.put("operario1", "1234");
        }

        edtUsuario = findViewById(R.id.edtUsuario);
        edtClave = findViewById(R.id.edtClave);
        edtClaveAdmin = findViewById(R.id.edtClaveAdmin);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegistrarUsuario = findViewById(R.id.btnRegistrarUsuario);
        txtMensajeLogin = findViewById(R.id.txtMensajeLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String user = edtUsuario.getText().toString().trim();
                String pass = edtClave.getText().toString().trim();

                if (user.isEmpty() || pass.isEmpty()) {
                    txtMensajeLogin.setTextColor(Color.RED);
                    txtMensajeLogin.setText("Complete usuario y contraseña");
                    return;
                }

                if (usuarios.containsKey(user) && usuarios.get(user).equals(pass)) {
                    Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                    intent.putExtra("USUARIO_ACTUAL", user);
                    startActivity(intent);
                    finish();
                } else {
                    txtMensajeLogin.setTextColor(Color.RED);
                    txtMensajeLogin.setText("Usuario o contraseña incorrectos");
                }
            }
        });

        btnRegistrarUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String nuevoUser = edtUsuario.getText().toString().trim();
                String nuevaPass = edtClave.getText().toString().trim();
                String claveAdmin = edtClaveAdmin.getText().toString().trim();

                if (nuevoUser.isEmpty() || nuevaPass.isEmpty()) {
                    txtMensajeLogin.setTextColor(Color.RED);
                    txtMensajeLogin.setText("Escriba el usuario y clave a registrar arriba");
                    return;
                }

                if (!claveAdmin.equals(CLAVE_MAESTRA_ADMIN)) {
                    txtMensajeLogin.setTextColor(Color.RED);
                    txtMensajeLogin.setText("Clave de administrador incorrecta");
                    return;
                }

                if (usuarios.containsKey(nuevoUser)) {
                    txtMensajeLogin.setTextColor(Color.RED);
                    txtMensajeLogin.setText("El usuario ya existe");
                    return;
                }

                usuarios.put(nuevoUser, nuevaPass);
                txtMensajeLogin.setTextColor(Color.parseColor("#4CAF50"));
                txtMensajeLogin.setText("¡Usuario registrado con éxito!");
                edtClaveAdmin.setText("");
            }
        });
    }
}