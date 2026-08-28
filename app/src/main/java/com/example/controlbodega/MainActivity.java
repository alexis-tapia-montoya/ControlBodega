package com.example.controlbodega;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private EditText edtProducto;
    private EditText edtCantidad;
    private Button btnRegistrar;
    private TextView txtMensaje;
    private TextView txtProd1;
    private TextView txtCant1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        edtProducto = findViewById(R.id.edtProducto);
        edtCantidad = findViewById(R.id.edtCantidad);
        btnRegistrar = findViewById(R.id.btnRegistrar);
        txtMensaje = findViewById(R.id.txtMensaje);
        txtProd1 = findViewById(R.id.txtProd1);
        txtCant1 = findViewById(R.id.txtCant1);


        btnRegistrar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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


                txtProd1.setText(producto);
                txtCant1.setText(cantidadStr);
                txtMensaje.setText("¡Producto registrado con éxito!");


                edtProducto.setText("");
                edtCantidad.setText("");
            }
        });
    }
}