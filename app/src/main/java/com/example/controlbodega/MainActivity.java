package com.example.controlbodega;

import androidx.appcompat.app.AppCompatActivity;
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
import java.util.LinkedHashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private EditText edtProducto;
    private EditText edtCantidad;
    private Button btnIngresar;
    private Button btnRetirar;
    private TextView txtMensaje;
    private TableLayout tableStock;

    private final LinkedHashMap<String, Integer> inventario = new LinkedHashMap<>();
    private TableRow filaSeleccionadaAnterior = null; // Para deseleccionar la fila previa

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        edtProducto = findViewById(R.id.edtProducto);
        edtCantidad = findViewById(R.id.edtCantidad);
        btnIngresar = findViewById(R.id.btnIngresar);
        btnRetirar = findViewById(R.id.btnRetirar);
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

        int stockActual = inventario.getOrDefault(producto, 0);

        if (esIngreso) {
            inventario.put(producto, stockActual + cantidad);
            txtMensaje.setTextColor(Color.parseColor("#4CAF50"));
            txtMensaje.setText("Ingresadas " + cantidad + " unidades a: " + producto);
        } else {
            if (!inventario.containsKey(producto)) {
                edtProducto.setError("El producto no existe en bodega");
                return;
            }
            if (stockActual < cantidad) {
                edtCantidad.setError("Stock insuficiente (Disponible: " + stockActual + ")");
                return;
            }

            int nuevoStock = stockActual - cantidad;
            inventario.put(producto, nuevoStock);
            txtMensaje.setTextColor(Color.parseColor("#E53935"));
            txtMensaje.setText("Retiradas " + cantidad + " unidades de: " + producto);
        }

        actualizarTabla();

        edtProducto.setText("");
        edtCantidad.setText("");
    }

    private void actualizarTabla() {
        int totalFilas = tableStock.getChildCount();
        if (totalFilas > 1) {
            tableStock.removeViews(1, totalFilas - 1);
        }
        filaSeleccionadaAnterior = null;

        for (Map.Entry<String, Integer> item : inventario.entrySet()) {
            final String nombreProd = item.getKey();
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
            colCantidad.setText(String.valueOf(item.getValue()));
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
    }
}