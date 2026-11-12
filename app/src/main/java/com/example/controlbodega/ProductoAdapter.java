package com.example.controlbodega;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    public static class ProductoModelo {
        public String nombre;
        public int stock;
        public String ultimoMovimiento;
        public String usuarioResponsable;

        public ProductoModelo(String nombre, int stock, String ultimoMovimiento, String usuarioResponsable) {
            this.nombre = nombre;
            this.stock = stock;
            this.ultimoMovimiento = ultimoMovimiento;
            this.usuarioResponsable = usuarioResponsable;
        }
    }

    public interface OnProductoClickListener {
        void onProductoClick(ProductoModelo producto);
    }

    private List<ProductoModelo> lista;
    private OnProductoClickListener listener;

    public ProductoAdapter(List<ProductoModelo> lista, OnProductoClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductoViewHolder holder, int position) {
        ProductoModelo p = lista.get(position);
        holder.txtNombre.setText(p.nombre);
        holder.txtStock.setText(String.valueOf(p.stock));

        String detalle = "Último: " + p.ultimoMovimiento + " por @" + p.usuarioResponsable;
        holder.txtDetalle.setText(detalle);

        if (p.ultimoMovimiento.startsWith("+")) {
            holder.txtStock.setTextColor(Color.parseColor("#388E3C"));
        } else if (p.ultimoMovimiento.startsWith("-")) {
            holder.txtStock.setTextColor(Color.parseColor("#D32F2F"));
        } else {
            holder.txtStock.setTextColor(Color.parseColor("#1976D2"));
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onProductoClick(p);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtDetalle, txtStock;

        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtItemNombre);
            txtDetalle = itemView.findViewById(R.id.txtItemDetalle);
            txtStock = itemView.findViewById(R.id.txtItemStock);
        }
    }
}