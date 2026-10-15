package com.example.controlbodega;

import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ExperimentalGetImage;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.Rect;
import android.media.Image;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.example.controlbodega.database.BodegaContract.UsuarioEntry;
import com.example.controlbodega.database.BodegaDbHelper;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText edtUsuario, edtClave, edtClaveAdmin;
    private Button btnLogin, btnRegistrarUsuario, btnBiometrico, btnRegistrarRostro;
    private TextView txtMensajeLogin;
    private PreviewView previewCamara;

    private static final String CLAVE_MAESTRA_ADMIN = "admin123";
    private static final int CODIGO_PERMISO_CAMARA = 200;

    private BodegaDbHelper dbHelper;
    private ExecutorService cameraExecutor;
    private ProcessCameraProvider cameraProvider;

    private boolean modoRegistroFacial = false;
    private boolean procesoCompletado = false;
    private String usuarioParaRegistrarRostro = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        dbHelper = new BodegaDbHelper(this);
        cameraExecutor = Executors.newSingleThreadExecutor();

        edtUsuario = findViewById(R.id.edtUsuario);
        edtClave = findViewById(R.id.edtClave);
        edtClaveAdmin = findViewById(R.id.edtClaveAdmin);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegistrarUsuario = findViewById(R.id.btnRegistrarUsuario);
        btnBiometrico = findViewById(R.id.btnBiometrico);
        btnRegistrarRostro = findViewById(R.id.btnRegistrarRostro);
        txtMensajeLogin = findViewById(R.id.txtMensajeLogin);
        previewCamara = findViewById(R.id.previewCamara);

        btnLogin.setOnClickListener(v -> iniciarSesion());
        btnRegistrarUsuario.setOnClickListener(v -> registrarUsuario());

        btnBiometrico.setOnClickListener(v -> {
            modoRegistroFacial = false;
            verificarPermisosYCamara();
        });

        btnRegistrarRostro.setOnClickListener(v -> {
            String user = edtUsuario.getText().toString().trim();
            String claveAdmin = edtClaveAdmin.getText().toString().trim();

            if (user.isEmpty()) {
                txtMensajeLogin.setTextColor(Color.RED);
                txtMensajeLogin.setText("Escriba el nombre de usuario a asociar");
                return;
            }
            if (!claveAdmin.equals(CLAVE_MAESTRA_ADMIN)) {
                txtMensajeLogin.setTextColor(Color.RED);
                txtMensajeLogin.setText("Clave de administrador incorrecta");
                return;
            }

            usuarioParaRegistrarRostro = user;
            modoRegistroFacial = true;
            verificarPermisosYCamara();
        });
    }

    private void verificarPermisosYCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            iniciarCamaraFacial();
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CODIGO_PERMISO_CAMARA);
        }
    }

    private void iniciarCamaraFacial() {
        previewCamara.setVisibility(View.VISIBLE);
        procesoCompletado = false;
        txtMensajeLogin.setTextColor(Color.parseColor("#1E88E5"));
        txtMensajeLogin.setText(modoRegistroFacial ? "Encuadre el rostro del nuevo usuario..." : "Mirando a la cámara para identificar...");

        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewCamara.getSurfaceProvider());

                FaceDetectorOptions options = new FaceDetectorOptions.Builder()
                        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                        .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                        .build();
                FaceDetector detector = FaceDetection.getClient(options);

                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    @OptIn(markerClass = ExperimentalGetImage.class)
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        Image mediaImage = imageProxy.getImage();
                        if (mediaImage != null && !procesoCompletado) {
                            InputImage image = InputImage.fromMediaImage(mediaImage, imageProxy.getImageInfo().getRotationDegrees());

                            detector.process(image)
                                    .addOnSuccessListener(faces -> {
                                        if (!faces.isEmpty() && !procesoCompletado) {
                                            Face cara = faces.get(0);
                                            String vectorFacial = extraerVectorFacial(cara);

                                            if (vectorFacial != null) {
                                                procesoCompletado = true;
                                                runOnUiThread(() -> {
                                                    if (modoRegistroFacial) {
                                                        guardarRostroEnBD(usuarioParaRegistrarRostro, vectorFacial);
                                                    } else {
                                                        identificarYAcceder(vectorFacial);
                                                    }
                                                    apagarCamara();
                                                });
                                            }
                                        }
                                    })
                                    .addOnCompleteListener(task -> imageProxy.close());
                        } else {
                            imageProxy.close();
                        }
                    }
                });

                CameraSelector cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis);

            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private String extraerVectorFacial(Face face) {
        Rect bounds = face.getBoundingBox();
        float width = Math.max(1f, (float) bounds.width());
        float height = Math.max(1f, (float) bounds.height());

        FaceLandmark ojoIzq = face.getLandmark(FaceLandmark.LEFT_EYE);
        FaceLandmark ojoDer = face.getLandmark(FaceLandmark.RIGHT_EYE);
        FaceLandmark nariz = face.getLandmark(FaceLandmark.NOSE_BASE);
        FaceLandmark boca = face.getLandmark(FaceLandmark.MOUTH_BOTTOM);

        if (ojoIzq == null || ojoDer == null || nariz == null || boca == null) {
            return null;
        }

        float x1 = (ojoIzq.getPosition().x - bounds.left) / width;
        float y1 = (ojoIzq.getPosition().y - bounds.top) / height;

        float x2 = (ojoDer.getPosition().x - bounds.left) / width;
        float y2 = (ojoDer.getPosition().y - bounds.top) / height;

        float x3 = (nariz.getPosition().x - bounds.left) / width;
        float y3 = (nariz.getPosition().y - bounds.top) / height;

        float x4 = (boca.getPosition().x - bounds.left) / width;
        float y4 = (boca.getPosition().y - bounds.top) / height;

        return x1 + "," + y1 + "," + x2 + "," + y2 + "," + x3 + "," + y3 + "," + x4 + "," + y4;
    }

    private void guardarRostroEnBD(String usuario, String vectorFacial) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(UsuarioEntry.COLUMN_FACE_DATA, vectorFacial);

        int filas = db.update(UsuarioEntry.TABLE_NAME, values, UsuarioEntry.COLUMN_USERNAME + " = ?", new String[]{usuario});
        db.close();

        if (filas > 0) {
            txtMensajeLogin.setTextColor(Color.parseColor("#4CAF50"));
            txtMensajeLogin.setText("¡Rostro asociado con éxito a: " + usuario + "!");
            Toast.makeText(this, "Rostro guardado en SQLite", Toast.LENGTH_SHORT).show();
            edtClaveAdmin.setText("");
        } else {
            txtMensajeLogin.setTextColor(Color.RED);
            txtMensajeLogin.setText("El usuario " + usuario + " no existe. Regístrelo en el paso 1.");
        }
    }

    private void identificarYAcceder(String vectorActualStr) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;
        String usuarioIdentificado = null;
        double menorDistancia = Double.MAX_VALUE;
        double UMBRAL_TOLERANCIA = 0.22;
        int totalRostrosRegistrados = 0;

        float[] vActual = parsearVector(vectorActualStr);

        try {
            String query = "SELECT " + UsuarioEntry.COLUMN_USERNAME + ", " + UsuarioEntry.COLUMN_FACE_DATA +
                    " FROM " + UsuarioEntry.TABLE_NAME +
                    " WHERE " + UsuarioEntry.COLUMN_FACE_DATA + " IS NOT NULL AND " +
                    UsuarioEntry.COLUMN_FACE_DATA + " != ''";

            cursor = db.rawQuery(query, null);

            while (cursor.moveToNext()) {
                String usuario = cursor.getString(cursor.getColumnIndexOrThrow(UsuarioEntry.COLUMN_USERNAME));
                String faceData = cursor.getString(cursor.getColumnIndexOrThrow(UsuarioEntry.COLUMN_FACE_DATA));

                if (faceData != null && faceData.trim().length() > 5) {
                    totalRostrosRegistrados++;
                    float[] vGuardado = parsearVector(faceData);
                    double dist = calcularDistanciaEuclidiana(vActual, vGuardado);

                    if (dist < menorDistancia) {
                        menorDistancia = dist;
                        usuarioIdentificado = usuario;
                    }
                }
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        if (totalRostrosRegistrados == 0) {
            txtMensajeLogin.setTextColor(Color.RED);
            txtMensajeLogin.setText("No hay rostros registrados. Solicite al Admin que registre uno.");
            Toast.makeText(this, "Base de datos sin registros faciales", Toast.LENGTH_LONG).show();
            return;
        }

        if (usuarioIdentificado == null || menorDistancia > UMBRAL_TOLERANCIA) {
            txtMensajeLogin.setTextColor(Color.RED);
            txtMensajeLogin.setText("Rostro no coincide con ningún operario");
            Toast.makeText(this, "Acceso Denegado: Rostro no reconocido", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "¡Bienvenido/a " + usuarioIdentificado + "!", Toast.LENGTH_SHORT).show();
        accederSistema(usuarioIdentificado);
    }

    private float[] parsearVector(String str) {
        String[] partes = str.split(",");
        float[] v = new float[partes.length];
        for (int i = 0; i < partes.length; i++) {
            v[i] = Float.parseFloat(partes[i]);
        }
        return v;
    }

    private double calcularDistanciaEuclidiana(float[] v1, float[] v2) {
        double suma = 0;
        for (int i = 0; i < Math.min(v1.length, v2.length); i++) {
            double dif = v1[i] - v2[i];
            suma += dif * dif;
        }
        return Math.sqrt(suma);
    }

    private void apagarCamara() {
        if (cameraProvider != null) {
            cameraProvider.unbindAll();
        }
        if (previewCamara != null) {
            previewCamara.setVisibility(View.GONE);
        }
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
                accederSistema(user);
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
                txtMensajeLogin.setText("¡Usuario creado! Ahora presione '2. Escanear y Asociar Rostro'");
                edtClaveAdmin.setText("");
            } else {
                txtMensajeLogin.setTextColor(Color.RED);
                txtMensajeLogin.setText("El usuario ya existe");
            }
        } finally {
            db.close();
        }
    }

    private void accederSistema(String nombreUsuario) {
        apagarCamara();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.putExtra("USUARIO_ACTUAL", nombreUsuario);
        startActivity(intent);
        finish();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CODIGO_PERMISO_CAMARA && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            iniciarCamaraFacial();
        } else {
            Toast.makeText(this, "Permiso de cámara necesario para reconocimiento facial", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        apagarCamara();
        if (cameraExecutor != null) {
            cameraExecutor.shutdown();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}