package com.example.controlbodega.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.controlbodega.database.BodegaContract.UsuarioEntry;
import com.example.controlbodega.database.BodegaContract.ProductoEntry;

public class BodegaDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 2;
    public static final String DATABASE_NAME = "ControlBodega.db";

    private static final String SQL_CREATE_USUARIOS =
            "CREATE TABLE " + UsuarioEntry.TABLE_NAME + " (" +
                    UsuarioEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    UsuarioEntry.COLUMN_USERNAME + " TEXT UNIQUE NOT NULL, " +
                    UsuarioEntry.COLUMN_PASSWORD + " TEXT NOT NULL, " +
                    UsuarioEntry.COLUMN_FACE_DATA + " TEXT);";

    private static final String SQL_CREATE_PRODUCTOS =
            "CREATE TABLE " + ProductoEntry.TABLE_NAME + " (" +
                    ProductoEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    ProductoEntry.COLUMN_NOMBRE + " TEXT UNIQUE NOT NULL, " +
                    ProductoEntry.COLUMN_CANTIDAD + " INTEGER NOT NULL);";

    public BodegaDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_USUARIOS);
        db.execSQL(SQL_CREATE_PRODUCTOS);

        insertarUsuario(db, "admin", "admin123", null);
        insertarUsuario(db, "operario1", "1234", null);
    }

    private void insertarUsuario(SQLiteDatabase db, String user, String pass, String faceData) {
        ContentValues values = new ContentValues();
        values.put(UsuarioEntry.COLUMN_USERNAME, user);
        values.put(UsuarioEntry.COLUMN_PASSWORD, pass);
        values.put(UsuarioEntry.COLUMN_FACE_DATA, faceData);
        db.insert(UsuarioEntry.TABLE_NAME, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Al subir de versión 1 a 2, recrea las tablas con la nueva estructura
        db.execSQL("DROP TABLE IF EXISTS " + UsuarioEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ProductoEntry.TABLE_NAME);
        onCreate(db);
    }
}