# ConectaEduSV (AprendeApp)

Aplicación móvil desarrollada en **Android Studio** utilizando **Kotlin**, diseñada bajo el patrón de arquitectura **MVC (Modelo-Vista-Controlador)** y conectada a un backend simulado en **MockAPI.io** mediante **Retrofit**.

---

## 🏛️ 1. Arquitectura del Software (Patrón MVC)

El proyecto **ConectaEduSV** implementa estrictamente el patrón de diseño **Modelo-Vista-Controlador (MVC)**, logrando una separación de incumbencias limpia y modular:

*   **Modelo (`model`)**: Representa las estructuras de datos y entidades de la aplicación (`Usuario` y `Recurso`). Estas clases emplean anotaciones de Gson (`@SerializedName`) para un mapeo robusto con el backend JSON de MockAPI.
*   **Vista (`view`)**: Compuesta por las interfaces de usuario construidas en XML (`activity_login.xml`, `activity_register.xml`, `activity_main.xml`, `activity_add_edit_resource.xml`, etc.) y las Actividades correspondientes (`LoginActivity`, `RegisterActivity`, `AddEditResourceActivity`, `MainActivity`). Las vistas renderizan los componentes visuales y capturan las interacciones del usuario, delegando la lógica de negocio y comunicación.
*   **Controladores (`controller`)**: Contienen la lógica de negocio, validaciones, gestión de sesiones (`SessionManager`) y procesamiento de peticiones de red asíncronas (`AuthController`, `RecursosController`, `FavoritosController`, `RatingController`). Actúan como intermediarios entre el Modelo y la Vista.

---

## 📂 2. Estructura de Paquetes y Carpetas

El código fuente se encuentra organizado de forma escalable dentro del paquete principal `com.example.desafio3dsm`:

```text
com.example.desafio3dsm/
│
├── adapter/
│   └── RecursosAdapter.kt         # Adaptador de RecyclerView para renderizar tarjetas de recursos educativos.
│
├── api/
│   └── ApiService.kt              # Interfaz de Retrofit con los endpoints REST (/usuarios y /recursos).
│
├── controller/
│   ├── AuthController.kt          # Lógica de negocio para autenticación, registro y validación de contraseñas.
│   ├── FavoritosController.kt     # Gestión y persistencia de recursos favoritos por usuario.
│   ├── RatingController.kt        # Recálculo y actualización de calificaciones con estrellas.
│   ├── RecursosController.kt      # Operaciones CRUD (Crear, Leer, Actualizar, Eliminar) y filtros de recursos.
│   └── SessionManager.kt          # Manejo de SharedPreferences para la sesión activa y tiempos de acceso.
│
├── model/
│   ├── Recurso.kt                 # Data class de recursos (título, descripción, tipo, imagen, URL, rating).
│   └── Usuario.kt                 # Data class de usuarios (id, nombre, email, clave, rol, favoritos).
│
├── network/
│   └── NetworkClient.kt           # Cliente Singleton para la configuración de Retrofit y GsonConverterFactory.
│
├── view/
│   ├── AddEditResourceActivity.kt # Formulario CRUD de creación y edición de recursos (Rol Docente).
│   ├── LoginActivity.kt           # Pantalla de inicio de sesión con validación y diseño en tarjeta.
│   └── RegisterActivity.kt        # Pantalla de registro con selector de roles y validación estricta de contraseña.
│
└── MainActivity.kt                # Pantalla principal con listado de recursos, búsqueda, filtros y perfil de usuario.
```

---

## 🌐 3. Integración con Retrofit y MockAPI.io

La comunicación con el servidor remoto se realiza mediante **Retrofit** y **Gson**, centralizada a través de la URL base provista por **MockAPI.io**:

*   **URL Base**: `https://6ae0c9dfbe3bc8fead97ec52.mockapi.io/`
*   **Cliente Singleton (`NetworkClient`)**: Configura la instancia única de Retrofit agregando el convertidor de Gson:
    ```kotlin
    object NetworkClient {
        private const val BASE_URL = "https://6ae0c9dfbe3bc8fead97ec52.mockapi.io/"
        private val retrofit: Retrofit by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        val apiService: ApiService by lazy {
            retrofit.create(ApiService::class.java)
        }
    }
    ```
*   **Endpoints (`ApiService`)**: Define las operaciones HTTP para ambos recursos de la plataforma:
    *   **Módulo de Usuarios (`/usuarios`)**:
        *   `GET("usuarios")`: Obtiene la lista completa de usuarios registrados para validar credenciales y evitar correos duplicados.
        *   `POST("usuarios")`: Registra un nuevo usuario en la plataforma.
        *   `PUT("usuarios/{id}")`: Actualiza el perfil de usuario (incluyendo la lista sincronizada de recursos `favoritos`).
    *   **Módulo de Recursos Educativos (`/recursos`)**:
        *   `GET("recursos")`: Consulta el catálogo completo de recursos disponibles.
        *   `POST("recursos")`: Publica un nuevo recurso educativo (exclusivo para el rol **Docente**).
        *   `PUT("recursos/{id}")`: Actualiza los datos de un recurso, incluyendo el recálculo de calificaciones por estrellas.
        *   `DELETE("recursos/{id}")`: Elimina un recurso del sistema (exclusivo para el rol **Docente**).
