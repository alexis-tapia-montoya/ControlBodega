package com.example.controlbodega;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.controlbodega.database.BodegaContract.ProductoEntry;
import com.example.controlbodega.database.BodegaContract.MovimientoEntry;
import com.example.controlbodega.database.BodegaDbHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private EditText edtProducto;
    private EditText edtCantidad;
    private Button btnIngresar;
    private Button btnRetirar;
    private Button btnCerrarSesion;
    private TextView txtMensaje;
    private TableLayout tableStock;

    private BodegaDbHelper dbHelper;
    private TableRow filaSeleccionadaAnterior = null;
    private String usuarioSesion = "Desconocido";

    private ExecutorService dbExecutor;
    private Handler mainHandler;

    private static class FilaProductoData {
        String nombre;
        int cantidad;
        String ultimoMovimiento;
        String responsable;

        FilaProductoData(String nombre, int cantidad, String ultimoMovimiento, String responsable) {
            this.nombre = nombre;
            this.cantidad = cantidad;
            this.ultimoMovimiento = ultimoMovimiento;
            this.responsable = responsable;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_SECURE,
                WindowManager.LayoutParams.FLAG_SECURE
        );
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new BodegaDbHelper(this);
        dbExecutor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        if (getIntent().hasExtra("USUARIO_ACTUAL")) {
            usuarioSesion = getIntent().getStringExtra("USUARIO_ACTUAL");
        }

        edtProducto = findViewById(R.id.edtProducto);
        edtCantidad = findViewById(R.id.edtCantidad);
        btnIngresar = findViewById(R.id.btnIngresar);
        btnRetirar = findViewById(R.id.btnRetirar);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        txtMensaje = findViewById(R.id.txtMensaje);
        tableStock = findViewById(R.id.tableStock);

        btnIngresar.setOnClickListener(v -> gestionarStock(true));
        btnRetirar.setOnClickListener(v -> gestionarStock(false));
        btnCerrarSesion.setOnClickListener(v -> mostrarDialogoCerrarSesion());

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                mostrarDialogoCerrarSesion();
            }
        });

        cargarProductosDesdeBD();
    }

    private void mostrarDialogoCerrarSesion() {
        new AlertDialog.Builder(this)
                .setTitle("Cerrar Sesión")
                .setMessage("¿Estás seguro de que deseas salir del turno actual, " + usuarioSesion + "?")
                .setPositiveButton("Sí, salir", (dialog, which) -> {
                    Intent intent = new Intent(MainActivity.this, LoginActivity.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void gestionarStock(boolean esIngreso) {
        String producto = edtProducto.getText().toString().trim();
        String cantidadStr = edtCantidad.getText().toString().trim();

        if (producto.isEmpty()) {
            edtProducto.setError("Ingrese el nombre del producto");
            return;
        }
        if (cantidadStr.isEmpty()) {
            edtCantidad.setError("Ingrese una cantidad");
            return;
        }

        int cantidad = Integer.parseInt(cantidadStr);
        if (cantidad <= 0) {
            edtCantidad.setError("La cantidad debe ser mayor a 0");
            return;
        }

        dbExecutor.execute(() -> {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            Cursor cursor = null;
            boolean errorStock = false;
            boolean productoNoExiste = false;
            int stockDisponible = 0;

            try {
                String selection = ProductoEntry.COLUMN_NOMBRE + " = ?";
                String[] selectionArgs = { producto };
                cursor = db.query(ProductoEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);

                String tipoMovimiento = esIngreso ? "INGRESO" : "RETIRO";

                if (cursor.moveToFirst()) {
                    int stockActual = cursor.getInt(cursor.getColumnIndexOrThrow(ProductoEntry.COLUMN_CANTIDAD));

                    if (esIngreso) {
                        dbHelper.actualizarStock(producto, stockActual + cantidad);
                        dbHelper.registrarMovimiento(producto, tipoMovimiento, cantidad, usuarioSesion);
                    } else {
                        if (stockActual < cantidad) {
                            errorStock = true;
                            stockDisponible = stockActual;
                        } else {
                            dbHelper.actualizarStock(producto, stockActual - cantidad);
                            dbHelper.registrarMovimiento(producto, tipoMovimiento, cantidad, usuarioSesion);
                        }
                    }
                } else {
                    if (esIngreso) {
                        dbHelper.insertarOActualizarProducto(producto, cantidad);
                        dbHelper.registrarMovimiento(producto, tipoMovimiento, cantidad, usuarioSesion);
                    } else {
                        productoNoExiste = true;
                    }
                }
            } finally {
                if (cursor != null) cursor.close();
            }

            final boolean fErrorStock = errorStock;
            final boolean fProductoNoExiste = productoNoExiste;
            final int fStockDisponible = stockDisponible;

            mainHandler.post(() -> {
                if (fErrorStock) {
                    edtCantidad.setError("Stock insuficiente (Disponible: " + fStockDisponible + ")");
                    return;
                }
                if (fProductoNoExiste) {
                    edtProducto.setError("El producto no existe en bodega");
                    return;
                }

                if (esIngreso) {
                    txtMensaje.setTextColor(Color.parseColor("#4CAF50"));
                    txtMensaje.setText("+" + cantidad + " a " + producto + " por @" + usuarioSesion);
                } else {
                    txtMensaje.setTextColor(Color.parseColor("#E53935"));
                    txtMensaje.setText("-" + cantidad + " a " + producto + " por @" + usuarioSesion);
                }

                edtProducto.setText("");
                edtCantidad.setText("");
                cargarProductosDesdeBD();
            });
        });
    }

    private void cargarProductosDesdeBD() {
        dbExecutor.execute(() -> {
            List<FilaProductoData> lista = new ArrayList<>();
            SQLiteDatabase db = dbHelper.getReadableDatabase();
            Cursor cursor = null;

            try {
                cursor = db.query(ProductoEntry.TABLE_NAME, null, null, null, null, null, null);

                while (cursor.moveToNext()) {
                    String nombreProd = cursor.getString(cursor.getColumnIndexOrThrow(ProductoEntry.COLUMN_NOMBRE));
                    int cantidad = cursor.getInt(cursor.getColumnIndexOrThrow(ProductoEntry.COLUMN_CANTIDAD));

                    String ultimoMovimientoTexto = "-";
                    String usuarioResponsable = "-";

                    Cursor cursorMov = db.query(
                            MovimientoEntry.TABLE_NAME,
                            null,
                            MovimientoEntry.COLUMN_PRODUCTO + " = ?",
                            new String[]{nombreProd},
                            null, null,
                            MovimientoEntry._ID + " DESC",
                            "1"
                    );

                    if (cursorMov.moveToFirst()) {
                        String tipo = cursorMov.getString(cursorMov.getColumnIndexOrThrow(MovimientoEntry.COLUMN_TIPO));
                        int cantMov = cursorMov.getInt(cursorMov.getColumnIndexOrThrow(MovimientoEntry.COLUMN_CANTIDAD));
                        usuarioResponsable = cursorMov.getString(cursorMov.getColumnIndexOrThrow(MovimientoEntry.COLUMN_USUARIO));
                        ultimoMovimientoTexto = (tipo.equals("INGRESO") ? "+" : "-") + cantMov;
                    }
                    cursorMov.close();

                    lista.add(new FilaProductoData(nombreProd, cantidad, ultimoMovimientoTexto, usuarioResponsable));
                }
            } finally {
                if (cursor != null) cursor.close();
            }

            mainHandler.post(() -> {
                int totalFilas = tableStock.getChildCount();
                if (totalFilas > 1) {
                    tableStock.removeViews(1, totalFilas - 1);
                }
                filaSeleccionadaAnterior = null;

                for (FilaProductoData item : lista) {
                    final TableRow fila = new TableRow(this);
                    fila.setPadding(0, 12, 0, 12);
                    fila.setClickable(true);
                    fila.setFocusable(true);

                    TextView colProducto = new TextView(this);
                    colProducto.setText(item.nombre);
                    colProducto.setTextSize(13);
                    colProducto.setGravity(Gravity.CENTER);
                    colProducto.setPadding(4, 4, 4, 4);

                    TextView colCantidad = new TextView(this);
                    colCantidad.setText(String.valueOf(item.cantidad));
                    colCantidad.setTextSize(13);
                    colCantidad.setGravity(Gravity.CENTER);
                    colCantidad.setPadding(4, 4, 4, 4);

                    TextView colUltMov = new TextView(this);
                    colUltMov.setText(item.ultimoMovimiento);
                    colUltMov.setTextSize(13);
                    colUltMov.setGravity(Gravity.CENTER);
                    colUltMov.setPadding(4, 4, 4, 4);
                    if (item.ultimoMovimiento.startsWith("+")) {
                        colUltMov.setTextColor(Color.parseColor("#388E3C"));
                    } else if (item.ultimoMovimiento.startsWith("-")) {
                        colUltMov.setTextColor(Color.parseColor("#D32F2F"));
                    }

                    TextView colUsuario = new TextView(this);
                    colUsuario.setText(item.responsable);
                    colUsuario.setTextSize(13);
                    colUsuario.setGravity(Gravity.CENTER);
                    colUsuario.setPadding(4, 4, 4, 4);

                    View.OnClickListener clickListener = v -> {
                        if (filaSeleccionadaAnterior != null) {
                            filaSeleccionadaAnterior.setBackgroundColor(Color.TRANSPARENT);
                        }
                        fila.setBackgroundColor(Color.parseColor("#BBDEFB"));
                        filaSeleccionadaAnterior = fila;

                        edtProducto.setText(item.nombre);
                        edtCantidad.requestFocus();

                        Toast.makeText(MainActivity.this, "Seleccionado: " + item.nombre, Toast.LENGTH_SHORT).show();
                        txtMensaje.setTextColor(Color.parseColor("#1976D2"));
                        txtMensaje.setText("Producto: " + item.nombre + " | Último cambio por: @" + item.responsable);
                    };

                    fila.setOnClickListener(clickListener);
                    colProducto.setOnClickListener(clickListener);
                    colCantidad.setOnClickListener(clickListener);
                    colUltMov.setOnClickListener(clickListener);
                    colUsuario.setOnClickListener(clickListener);

                    fila.addView(colProducto);
                    fila.addView(colCantidad);
                    fila.addView(colUltMov);
                    fila.addView(colUsuario);
                    tableStock.addView(fila);
                }
            });
        });
    }

    @Override
    protected void onDestroy() {
        if (dbExecutor != null) {
            dbExecutor.shutdown();
        }
        if (dbHelper != null) {
            dbHelper.close();
        }
        super.onDestroy();
    }
}