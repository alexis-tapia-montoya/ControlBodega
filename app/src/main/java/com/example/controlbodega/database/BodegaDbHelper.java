package com.example.controlbodega.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.controlbodega.database.BodegaContract.UsuarioEntry;
import com.example.controlbodega.database.BodegaContract.ProductoEntry;
import com.example.controlbodega.database.BodegaContract.MovimientoEntry;

public class BodegaDbHelper extends SQLiteOpenHelper {

    public static final int DATABASE_VERSION = 3;
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

    private static final String SQL_CREATE_MOVIMIENTOS =
            "CREATE TABLE " + MovimientoEntry.TABLE_NAME + " (" +
                    MovimientoEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    MovimientoEntry.COLUMN_PRODUCTO + " TEXT NOT NULL, " +
                    MovimientoEntry.COLUMN_TIPO + " TEXT NOT NULL, " +
                    MovimientoEntry.COLUMN_CANTIDAD + " INTEGER NOT NULL, " +
                    MovimientoEntry.COLUMN_USUARIO + " TEXT NOT NULL, " +
                    MovimientoEntry.COLUMN_FECHA + " TIMESTAMP DEFAULT CURRENT_TIMESTAMP);";

    /**
     * @param context Contexto de la aplicación o actividad.
     */
    public BodegaDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * @param db Instancia de la base de datos SQLite recién creada.
     * @throws SQLException Si ocurre un error al ejecutar las sentencias CREATE TABLE.
     */
    @Override
    public void onCreate(SQLiteDatabase db) throws SQLException {
        db.execSQL(SQL_CREATE_USUARIOS);
        db.execSQL(SQL_CREATE_PRODUCTOS);
        db.execSQL(SQL_CREATE_MOVIMIENTOS);

        insertarUsuario(db, "admin", "admin123", null);
        insertarUsuario(db, "operario1", "1234", null);
    }

    /**
     * @param db Instancia en modo escritura de SQLiteDatabase.
     * @param user Nombre de usuario único.
     * @param pass Contraseña del operario.
     * @param faceData Vector facial normalizado.
     */
    private void insertarUsuario(SQLiteDatabase db, String user, String pass, String faceData) {
        ContentValues values = new ContentValues();
        values.put(UsuarioEntry.COLUMN_USERNAME, user);
        values.put(UsuarioEntry.COLUMN_PASSWORD, pass);
        values.put(UsuarioEntry.COLUMN_FACE_DATA, faceData);
        db.insert(UsuarioEntry.TABLE_NAME, null, values);
    }

    /**
     * @param db Instancia de la base de datos SQLite.
     * @param oldVersion Versión anterior de la base de datos.
     * @param newVersion Versión nueva de la base de datos.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MovimientoEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ProductoEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UsuarioEntry.TABLE_NAME);
        onCreate(db);
    }

    /**
     * @param nombre Nombre único del producto.
     * @param cantidad Cantidad de unidades a registrar o sumar.
     * @return ID de la fila insertada o número de filas modificadas.
     * @throws IllegalArgumentException Si la columna solicitada no existe en el Cursor.
     */
    public long insertarOActualizarProducto(String nombre, int cantidad) throws IllegalArgumentException {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(ProductoEntry.TABLE_NAME, null,
                ProductoEntry.COLUMN_NOMBRE + " = ?", new String[]{nombre}, null, null, null);

        long resultado;
        if (cursor.moveToFirst()) {
            int stockActual = cursor.getInt(cursor.getColumnIndexOrThrow(ProductoEntry.COLUMN_CANTIDAD));
            ContentValues values = new ContentValues();
            values.put(ProductoEntry.COLUMN_CANTIDAD, stockActual + cantidad);
            resultado = db.update(ProductoEntry.TABLE_NAME, values,
                    ProductoEntry.COLUMN_NOMBRE + " = ?", new String[]{nombre});
        } else {
            ContentValues values = new ContentValues();
            values.put(ProductoEntry.COLUMN_NOMBRE, nombre);
            values.put(ProductoEntry.COLUMN_CANTIDAD, cantidad);
            resultado = db.insert(ProductoEntry.TABLE_NAME, null, values);
        }
        cursor.close();
        return resultado;
    }

    /**
     * @param nombre Nombre exacto del producto.
     * @param nuevoStock Nueva cantidad total disponible en inventario.
     * @return Número de filas actualizadas.
     */
    public int actualizarStock(String nombre, int nuevoStock) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ProductoEntry.COLUMN_CANTIDAD, nuevoStock);
        return db.update(ProductoEntry.TABLE_NAME, values,
                ProductoEntry.COLUMN_NOMBRE + " = ?", new String[]{nombre});
    }

    /**
     * @param nombre Nombre del producto a eliminar.
     * @return Número de filas eliminadas.
     */
    public int eliminarProducto(String nombre) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(ProductoEntry.TABLE_NAME,
                ProductoEntry.COLUMN_NOMBRE + " = ?", new String[]{nombre});
    }

    /**
     * @param producto Nombre del producto afectado.
     * @param tipo Tipo de movimiento ("INGRESO" o "RETIRO").
     * @param cantidad Cantidad de unidades movilizadas.
     * @param usuario Nombre del operario autenticado.
     * @return ID del nuevo registro de auditoría insertado.
     */
    public long registrarMovimiento(String producto, String tipo, int cantidad, String usuario) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(MovimientoEntry.COLUMN_PRODUCTO, producto);
        values.put(MovimientoEntry.COLUMN_TIPO, tipo);
        values.put(MovimientoEntry.COLUMN_CANTIDAD, cantidad);
        values.put(MovimientoEntry.COLUMN_USUARIO, usuario);
        return db.insert(MovimientoEntry.TABLE_NAME, null, values);
    }
}