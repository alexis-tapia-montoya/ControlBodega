package com.example.controlbodega;

import androidx.appcompat.app.AppCompatActivity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.example.controlbodega.database.BodegaContract.UsuarioEntry;
import com.example.controlbodega.database.BodegaDbHelper;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsuario, edtClave, edtClaveAdmin;
    private Button btnLogin, btnRegistrarUsuario;
    private TextView txtMensajeLogin;

    private static final String CLAVE_MAESTRA_ADMIN = "admin123";
    private BodegaDbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new BodegaDbHelper(this);

        edtUsuario = findViewById(R.id.edtUsuario);
        edtClave = findViewById(R.id.edtClave);
        edtClaveAdmin = findViewById(R.id.edtClaveAdmin);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegistrarUsuario = findViewById(R.id.btnRegistrarUsuario);
        txtMensajeLogin = findViewById(R.id.txtMensajeLogin);

        btnLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarSesion();
            }
        });

        btnRegistrarUsuario.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                registrarUsuario();
            }
        });
    }

    private void iniciarSesion() {
        String user = edtUsuario.getText().toString().trim();
        String pass = edtClave.getText().toString().trim();

        if (user.isEmpty() || pass.isEmpty()) {
            txtMensajeLogin.setTextColor(Color.RED);
            txtMensajeLogin.setText("Complete usuario y contraseña");
            return;
        }

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            String selection = UsuarioEntry.COLUMN_USERNAME + " = ? AND " + UsuarioEntry.COLUMN_PASSWORD + " = ?";
            String[] selectionArgs = { user, pass };

            cursor = db.query(UsuarioEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);

            if (cursor.moveToFirst()) {
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.putExtra("USUARIO_ACTUAL", user);
                startActivity(intent);
                finish();
            } else {
                txtMensajeLogin.setTextColor(Color.RED);
                txtMensajeLogin.setText("Usuario o contraseña incorrectos");
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }
    }

    private void registrarUsuario() {
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

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        try {
            ContentValues values = new ContentValues();
            values.put(UsuarioEntry.COLUMN_USERNAME, nuevoUser);
            values.put(UsuarioEntry.COLUMN_PASSWORD, nuevaPass);

            long resultado = db.insert(UsuarioEntry.TABLE_NAME, null, values);

            if (resultado != -1) {
                txtMensajeLogin.setTextColor(Color.parseColor("#4CAF50"));
                txtMensajeLogin.setText("¡Usuario registrado con éxito en SQLite!");
                edtClaveAdmin.setText("");
            } else {
                txtMensajeLogin.setTextColor(Color.RED);
                txtMensajeLogin.setText("El usuario ya existe");
            }
        } finally {
            db.close();
        }
    }

    @Override
    protected void onDestroy() {
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}