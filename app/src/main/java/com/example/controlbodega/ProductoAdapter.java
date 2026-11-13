package com.example.controlbodega;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * Adaptador de RecyclerView encargado de renderizar el catálogo de productos
 * en tarjetas individuales (CardView) con indicadores visuales de último movimiento.
 */
public class ProductoAdapter extends RecyclerView.Adapter<ProductoAdapter.ProductoViewHolder> {

    /**
     * Modelo de datos que representa un artículo de inventario junto con su auditoría reciente.
     */
    public static class ProductoModelo {
        public String nombre;
        public int stock;
        public String ultimoMovimiento;
        public String usuarioResponsable;

        /**
         * Construye un objeto modelo de producto para visualización en lista.
         *
         * @param nombre             Nombre del artículo.
         * @param stock              Cantidad actual disponible en bodega.
         * @param ultimoMovimiento   Variación del último movimiento registrado (ej. "+5" o "-2").
         * @param usuarioResponsable Nombre del operario que efectuó la última operación.
         */
        public ProductoModelo(String nombre, int stock, String ultimoMovimiento, String usuarioResponsable) {
            this.nombre = nombre;
            this.stock = stock;
            this.ultimoMovimiento = ultimoMovimiento;
            this.usuarioResponsable = usuarioResponsable;
        }
    }

    /**
     * Interfaz de escucha para capturar eventos de selección sobre una tarjeta de producto.
     */
    public interface OnProductoClickListener {
        /**
         * Se invoca cuando el usuario presiona un elemento de la lista de inventario.
         *
         * @param producto Instancia del producto seleccionado.
         */
        void onProductoClick(ProductoModelo producto);
    }

    private List<ProductoModelo> lista;
    private OnProductoClickListener listener;

    /**
     * Inicializa el adaptador con la lista de productos y el manejador de clics.
     *
     * @param lista    Colección de productos cargados desde SQLite.
     * @param listener Callback ejecutado al seleccionar un producto.
     */
    public ProductoAdapter(List<ProductoModelo> lista, OnProductoClickListener listener) {
        this.lista = lista;
        this.listener = listener;
    }

    /**
     * Infla el diseño XML de cada tarjeta de producto (item_producto) y crea su ViewHolder.
     *
     * @param parent   Contenedor padre al que se adjuntará la vista.
     * @param viewType Tipo de vista del elemento.
     * @return Nueva instancia de ProductoViewHolder.
     */
    @NonNull
    @Override
    public ProductoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_producto, parent, false);
        return new ProductoViewHolder(v);
    }

    /**
     * Vincula los datos de un producto específico con los componentes visuales de su tarjeta.
     *
     * @param holder   ViewHolder que contiene las referencias a las vistas de la tarjeta.
     * @param position Índice del producto dentro de la lista.
     */
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

    /**
     * Retorna la cantidad total de productos en la colección del adaptador.
     *
     * @return Número de elementos en la lista.
     */
    @Override
    public int getItemCount() {
        return lista.size();
    }

    /**
     * Contenedor de referencias de vistas para optimizar el reciclaje de tarjetas.
     */
    static class ProductoViewHolder extends RecyclerView.ViewHolder {
        TextView txtNombre, txtDetalle, txtStock;

        /**
         * Enlaza los controles visuales de la tarjeta de producto.
         *
         * @param itemView Vista raíz de la tarjeta inflada.
         */
        public ProductoViewHolder(@NonNull View itemView) {
            super(itemView);
            txtNombre = itemView.findViewById(R.id.txtItemNombre);
            txtDetalle = itemView.findViewById(R.id.txtItemDetalle);
            txtStock = itemView.findViewById(R.id.txtItemStock);
        }
    }
}