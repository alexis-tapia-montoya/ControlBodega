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
}