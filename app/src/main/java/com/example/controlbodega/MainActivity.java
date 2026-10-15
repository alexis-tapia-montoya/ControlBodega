package com.example.controlbodega;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.example.controlbodega.database.BodegaContract.ProductoEntry;
import com.example.controlbodega.database.BodegaDbHelper;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = new BodegaDbHelper(this);

        edtProducto = findViewById(R.id.edtProducto);
        edtCantidad = findViewById(R.id.edtCantidad);
        btnIngresar = findViewById(R.id.btnIngresar);
        btnRetirar = findViewById(R.id.btnRetirar);
        btnCerrarSesion = findViewById(R.id.btnCerrarSesion);
        txtMensaje = findViewById(R.id.txtMensaje);
        tableStock = findViewById(R.id.tableStock);

        btnIngresar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gestionarStock(true);
            }
        });

        btnRetirar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                gestionarStock(false);
            }
        });

        btnCerrarSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mostrarDialogoCerrarSesion();
            }
        });

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
                .setMessage("¿Estás seguro de que deseas salir del turno actual?")
                .setPositiveButton("Sí, salir", (dialog, which) -> {
                    // Redirigir a LoginActivity y limpiar la pila de actividades
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

        SQLiteDatabase db = dbHelper.getWritableDatabase();
        Cursor cursor = null;

        try {
            String selection = ProductoEntry.COLUMN_NOMBRE + " = ?";
            String[] selectionArgs = { producto };
            cursor = db.query(ProductoEntry.TABLE_NAME, null, selection, selectionArgs, null, null, null);

            if (cursor.moveToFirst()) {
                int stockActual = cursor.getInt(cursor.getColumnIndexOrThrow(ProductoEntry.COLUMN_CANTIDAD));

                if (esIngreso) {
                    ContentValues values = new ContentValues();
                    values.put(ProductoEntry.COLUMN_CANTIDAD, stockActual + cantidad);
                    db.update(ProductoEntry.TABLE_NAME, values, selection, selectionArgs);

                    txtMensaje.setTextColor(Color.parseColor("#4CAF50"));
                    txtMensaje.setText("Ingresadas " + cantidad + " unidades a: " + producto);
                } else {
                    if (stockActual < cantidad) {
                        edtCantidad.setError("Stock insuficiente (Disponible: " + stockActual + ")");
                        return;
                    }
                    int nuevoStock = stockActual - cantidad;
                    ContentValues values = new ContentValues();
                    values.put(ProductoEntry.COLUMN_CANTIDAD, nuevoStock);
                    db.update(ProductoEntry.TABLE_NAME, values, selection, selectionArgs);

                    txtMensaje.setTextColor(Color.parseColor("#E53935"));
                    txtMensaje.setText("Retiradas " + cantidad + " unidades de: " + producto);
                }
            } else {
                if (esIngreso) {
                    ContentValues values = new ContentValues();
                    values.put(ProductoEntry.COLUMN_NOMBRE, producto);
                    values.put(ProductoEntry.COLUMN_CANTIDAD, cantidad);
                    db.insert(ProductoEntry.TABLE_NAME, null, values);

                    txtMensaje.setTextColor(Color.parseColor("#4CAF50"));
                    txtMensaje.setText("Producto nuevo registrado: " + producto + " (" + cantidad + " un.)");
                } else {
                    edtProducto.setError("El producto no existe en bodega");
                    return;
                }
            }
        } finally {
            if (cursor != null) cursor.close();
            db.close();
        }

        cargarProductosDesdeBD();

        edtProducto.setText("");
        edtCantidad.setText("");
    }

    private void cargarProductosDesdeBD() {
        int totalFilas = tableStock.getChildCount();
        if (totalFilas > 1) {
            tableStock.removeViews(1, totalFilas - 1);
        }
        filaSeleccionadaAnterior = null;

        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = null;

        try {
            cursor = db.query(ProductoEntry.TABLE_NAME, null, null, null, null, null, null);

            while (cursor.moveToNext()) {
                final String nombreProd = cursor.getString(cursor.getColumnIndexOrThrow(ProductoEntry.COLUMN_NOMBRE));
                final int cantidad = cursor.getInt(cursor.getColumnIndexOrThrow(ProductoEntry.COLUMN_CANTIDAD));

                final TableRow fila = new TableRow(this);
                fila.setPadding(0, 14, 0, 14);
                fila.setClickable(true);
                fila.setFocusable(true);

                TextView colProducto = new TextView(this);
                colProducto.setText(nombreProd);
                colProducto.setTextSize(15);
                colProducto.setGravity(Gravity.CENTER);
                colProducto.setPadding(6, 6, 6, 6);

                TextView colCantidad = new TextView(this);
                colCantidad.setText(String.valueOf(cantidad));
                colCantidad.setTextSize(15);
                colCantidad.setGravity(Gravity.CENTER);
                colCantidad.setPadding(6, 6, 6, 6);

                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (filaSeleccionadaAnterior != null) {
                            filaSeleccionadaAnterior.setBackgroundColor(Color.TRANSPARENT);
                        }

                        fila.setBackgroundColor(Color.parseColor("#BBDEFB"));
                        filaSeleccionadaAnterior = fila;

                        edtProducto.setText(nombreProd);
                        edtCantidad.requestFocus();

                        Toast.makeText(MainActivity.this, "Seleccionado: " + nombreProd, Toast.LENGTH_SHORT).show();
                        txtMensaje.setTextColor(Color.parseColor("#1976D2"));
                        txtMensaje.setText("Producto seleccionado: " + nombreProd);
                    }
                };

                fila.setOnClickListener(clickListener);
                colProducto.setOnClickListener(clickListener);
                colCantidad.setOnClickListener(clickListener);

                fila.addView(colProducto);
                fila.addView(colCantidad);
                tableStock.addView(fila);
            }
        } finally {
            if (cursor != null) cursor.close();
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