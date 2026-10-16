package com.example.controlbodega.database;

import android.provider.BaseColumns;

public final class BodegaContract {

    private BodegaContract() {}

    public static class UsuarioEntry implements BaseColumns {
        public static final String TABLE_NAME = "usuarios";
        public static final String COLUMN_USERNAME = "username";
        public static final String COLUMN_PASSWORD = "password";
        public static final String COLUMN_FACE_DATA = "face_data";
    }

    public static class ProductoEntry implements BaseColumns {
        public static final String TABLE_NAME = "productos";
        public static final String COLUMN_NOMBRE = "nombre";
        public static final String COLUMN_CANTIDAD = "cantidad";
    }

    public static class MovimientoEntry implements BaseColumns {
        public static final String TABLE_NAME = "movimientos";
        public static final String COLUMN_PRODUCTO = "producto";
        public static final String COLUMN_TIPO = "tipo"; // "INGRESO" o "RETIRO"
        public static final String COLUMN_CANTIDAD = "cantidad";
        public static final String COLUMN_USUARIO = "usuario";
        public static final String COLUMN_FECHA = "fecha";
    }
}