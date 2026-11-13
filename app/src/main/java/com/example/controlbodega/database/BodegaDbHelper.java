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

/**
 * Gestor de persistencia relacional en SQLite para la aplicación ControlBodega.
 * Administra la creación, migración de versiones y operaciones CRUD parametrizadas
 * sobre las tablas de usuarios, productos y auditoría de movimientos.
 */
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
     * Construye una nueva instancia del asistente de base de datos SQLite.
     *
     * @param context Contexto de la aplicación o actividad invocadora.
     */
    public BodegaDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * Inicializa el esquema relacional por primera vez y carga los operarios por defecto.
     *
     * @param db Instancia de la base de datos SQLite recién creada.
     * @throws SQLException Si ocurre un error de sintaxis o restricción al ejecutar los DDL.
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
     * Inserta un registro de operario en la tabla usuarios mediante ContentValues.
     *
     * @param db       Instancia en modo escritura de SQLiteDatabase.
     * @param user     Nombre de usuario único.
     * @param pass     Contraseña asociada a la cuenta.
     * @param faceData Descriptor facial normalizado (puede ser null en el alta inicial).
     */
    private void insertarUsuario(SQLiteDatabase db, String user, String pass, String faceData) {
        ContentValues values = new ContentValues();
        values.put(UsuarioEntry.COLUMN_USERNAME, user);
        values.put(UsuarioEntry.COLUMN_PASSWORD, pass);
        values.put(UsuarioEntry.COLUMN_FACE_DATA, faceData);
        db.insert(UsuarioEntry.TABLE_NAME, null, values);
    }

    /**
     * Gestiona la actualización estructural del esquema cuando se incrementa DATABASE_VERSION.
     *
     * @param db         Instancia de la base de datos SQLite.
     * @param oldVersion Número de versión anterior almacenada en el dispositivo.
     * @param newVersion Número de versión objetivo definida en la aplicación.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + MovimientoEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + ProductoEntry.TABLE_NAME);
        db.execSQL("DROP TABLE IF EXISTS " + UsuarioEntry.TABLE_NAME);
        onCreate(db);
    }

    /**
     * Inserta un nuevo artículo en inventario o suma unidades si el producto ya existe.
     *
     * @param nombre   Nombre único del producto a registrar o abastecer.
     * @param cantidad Cantidad de unidades a incorporar al stock.
     * @return ID de la nueva fila insertada o cantidad de filas actualizadas; -1 si falla.
     * @throws IllegalArgumentException Si alguna columna solicitada en el cursor no existe.
     */
    public long insertarOActualizarProducto(String nombre, int cantidad) throws IllegalArgumentException {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = null;
        try {
            cursor = db.query(ProductoEntry.TABLE_NAME, null,
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
            return resultado;
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
    }

    /**
     * Actualiza de forma parametrizada el stock disponible de un producto existente.
     *
     * @param nombre     Nombre exacto del producto en bodega.
     * @param nuevoStock Nueva cantidad total de existencias tras el movimiento.
     * @return Número de filas afectadas por la operación de actualización.
     */
    public int actualizarStock(String nombre, int nuevoStock) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(ProductoEntry.COLUMN_CANTIDAD, nuevoStock);
        return db.update(ProductoEntry.TABLE_NAME, values,
                ProductoEntry.COLUMN_NOMBRE + " = ?", new String[]{nombre});
    }

    /**
     * Elimina de forma permanente un producto del catálogo de inventario.
     *
     * @param nombre Nombre del producto a suprimir.
     * @return Número de registros eliminados en la tabla productos.
     */
    public int eliminarProducto(String nombre) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(ProductoEntry.TABLE_NAME,
                ProductoEntry.COLUMN_NOMBRE + " = ?", new String[]{nombre});
    }

    /**
     * Registra un evento de auditoría inmutable en el historial de movimientos de bodega.
     *
     * @param producto Nombre del artículo involucrado en la transacción.
     * @param tipo     Tipo de movimiento ejecutado ("INGRESO" o "RETIRO").
     * @param cantidad Cantidad de unidades movilizadas.
     * @param usuario  Identificador del operario autenticado responsable del cambio.
     * @return ID de la fila insertada en la tabla movimientos, o -1 si ocurrió un error.
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