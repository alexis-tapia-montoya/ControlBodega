#  ControlBodega - Sistema Móvil de Gestión de Inventario con Biometría Facial

Aplicación móvil nativa desarrollada en **Android (Java)** diseñada para el control, auditoría y trazabilidad de entradas y salidas de stock en tiempo real. Incorpora un motor de autenticación biométrica facial sobre el dispositivo utilizando **CameraX** y **Google ML Kit**, almacenamiento relacional local con **SQLite**, trazabilidad de acciones por operario y control de navegación seguro.

---

##  Características Principales

* **Autenticación Dual:** Acceso tradicional por credenciales (usuario/contraseña) y acceso biométrico mediante reconocimiento facial nativo en tiempo real.
* **Biometría Facial en el Dispositivo (On-Device):** Extracción de descriptores y puntos clave faciales (*landmarks*) mediante Google ML Kit y comparación basada en **distancia euclidiana**.
* **Gestión de Stock en Tiempo Real:** Registro de altas, bajas y actualización automática de existencias con alertas visuales de inventario insuficiente.
* **Auditoría y Trazabilidad:** Registro persistente de movimientos (`INGRESO` / `RETIRO`) asociando cantidad, marca de tiempo y el usuario en sesión responsable del cambio.
* **Seguridad y Control de Sesión:** Cierre de turno protegido con confirmación modal (`AlertDialog`), limpieza de la pila de navegación (`Intent Flags`) e intercepción del botón/gesto Atrás del sistema (`OnBackPressedDispatcher`).

---

## ️ Stack Tecnológico y Dependencias

* **Lenguaje:** Java 8+ / Android SDK
* **Arquitectura de Persistencia:** SQLite (`SQLiteOpenHelper`, `BodegaContract`, versionado estructurado)
* **Visión Artificial y Cámara:**
    * `androidx.camera:camera-camera2:1.3.1` (CameraX)
    * `androidx.camera:camera-lifecycle:1.3.1`
    * `androidx.camera:camera-view:1.3.1`
    * `com.google.mlkit:face-detection:16.1.6` (Google ML Kit Face Detection)
* **Concurrencia:** `ExecutorService` en segundo plano para evitar bloqueos del hilo principal (prevención de errores *ANR*).
* **Diseño UI:** `TableLayout`, `TableRow` dinámicos, `ScrollView` y componentes Material.

---

##  Estructura de la Base de Datos (SQLite)

La persistencia local se gestiona a través de `BodegaDbHelper.java`:

### 1. Tabla `usuarios`
| Campo | Tipo | Restricción / Propósito |
| :--- | :--- | :--- |
| `_id` | `INTEGER` | Clave primaria autoincremental |
| `username` | `TEXT` | Nombre de usuario (Único, Not Null) |
| `password` | `TEXT` | Contraseña del operario (Not Null) |
| `face_data` | `TEXT` | Vector numérico normalizado de características faciales |

### 2. Tabla `productos`
| Campo | Tipo | Restricción / Propósito |
| :--- | :--- | :--- |
| `_id` | `INTEGER` | Clave primaria autoincremental |
| `nombre` | `TEXT` | Nombre del artículo (Único, Not Null) |
| `cantidad` | `INTEGER` | Unidades disponibles en bodega (Not Null) |

### 3. Tabla `movimientos` (Auditoría)
| Campo | Tipo | Restricción / Propósito |
| :--- | :--- | :--- |
| `_id` | `INTEGER` | Clave primaria autoincremental |
| `producto` | `TEXT` | Artículo modificado |
| `tipo` | `TEXT` | Tipo de operación (`INGRESO` o `RETIRO`) |
| `cantidad` | `INTEGER` | Unidades alteradas |
| `usuario` | `TEXT` | Operario responsable que inició turno |
| `fecha` | `TIMESTAMP` | Marca temporal automática (`CURRENT_TIMESTAMP`) |

---

##  ¿Cómo Funciona el Reconocimiento Facial?

El flujo biométrico no sube imágenes a la nube ni almacena fotos directas del usuario; opera 100% de manera local:

1. **Captura y Pipeline:** `CameraX` procesa el flujo de la cámara frontal (`DEFAULT_FRONT_CAMERA`) y entrega los fotogramas al analizador (`ImageAnalysis`) en un hilo de trabajo (`cameraExecutor`).
2. **Detección de Rasgos:** ML Kit identifica la caja delimitadora (*Bounding Box*) y localiza las coordenadas de rasgos clave:
    * Ojo izquierdo y derecho (`FaceLandmark.LEFT_EYE`, `RIGHT_EYE`)
    * Base de la nariz (`FaceLandmark.NOSE_BASE`)
    * Centro inferior de la boca (`FaceLandmark.MOUTH_BOTTOM`)
3. **Normalización del Vector:** Se calculan las posiciones relativas divididas entre el ancho y alto del rostro detectado para generar un descriptor invariante a la distancia de la cámara.
4. **Registro vs. Identificación:**
    * **Modo Registro (Admin):** El vector se serializa a texto y se actualiza en el campo `face_data` del usuario.
    * **Modo Login:** Se calcula la **distancia euclidiana** entre el vector actual y todos los usuarios registrados con rostro:
      $$\text{Distancia} = \sqrt{\sum_{i=1}^{n} (v_{\text{actual}}[i] - v_{\text{guardado}}[i])^2}$$
      Si la menor distancia se encuentra dentro del umbral de tolerancia (`<= 0.22`), el sistema valida la identidad del operario y abre su turno. Si no hay rostros registrados o la distancia excede el margen, el acceso es denegado.

---

##  Guía de Uso de la Aplicación

### 1. Primer Inicio y Registro de Operarios (Solo Admin)
1. Inicie la aplicación.
2. En la sección **Gestión de Operarios (Solo Admin)**:
    * Ingrese el **Usuario** y **Contraseña** a crear.
    * Ingrese la clave maestra de administrador (`admin123`).
    * Presione **1. Registrar Usuario**.
3. Para asociar el rostro:
    * Mantenga el nombre de usuario y la clave de administrador ingresados.
    * Presione **2. Escanear y Asociar Rostro**.
    * Conceda los permisos de cámara y encuadre el rostro de frente hasta ver la confirmación en pantalla.

### 2. Inicio de Sesión
* **Método 1 (Biometría):** Presione **Ingresar con Reconocimiento Facial** y mire a la cámara frontal. El sistema identificará al operario automáticamente y cargará su sesión.
* **Método 2 (Credenciales):** Ingrese usuario y contraseña manuales y presione **Iniciar Sesión**.

### 3. Operación de Bodega
* **Ingresar Stock:** Escriba el producto, la cantidad y presione **Ingresar**. Si el producto no existía, se crea automáticamente; si ya existía, suma existencias.
* **Retirar Stock:** Seleccione un producto de la tabla (se autorrellenará en el formulario), escriba la cantidad a despachar y presione **Retirar**. Si no hay suficiente stock, el sistema alertará del error.
* **Tabla en Vivo:** La tabla detalla el producto, el stock actual, el último movimiento registrado (`+X` en verde o `-X` en rojo) y el usuario responsable.

### 4. Cierre de Turno
* Al presionar **Cerrar Sesión** o presionar la flecha/gesto **Atrás** del teléfono, se despliega un diálogo de confirmación. Al aceptar, se destruye la sesión y se limpian las banderas de la pila para impedir retornos no autorizados.

---

##  Permisos Requeridos (`AndroidManifest.xml`)

```xml
<uses-permission android:name="android.permission.CAMERA" />
<uses-feature android:name="android.hardware.camera" android:required="false" />