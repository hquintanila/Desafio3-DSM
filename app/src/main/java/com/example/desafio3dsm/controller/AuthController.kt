package com.example.desafio3dsm.controller

import android.util.Log
import com.example.desafio3dsm.R
import com.example.desafio3dsm.api.ApiService
import com.example.desafio3dsm.model.Usuario
import com.example.desafio3dsm.network.NetworkClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Controlador de Autenticación en la arquitectura MVC.
 * Se encarga del procesamiento de inicio de sesión, registro y validación estricta de contraseñas.
 */
class AuthController(
    private val apiService: ApiService = NetworkClient.apiService
) {

    interface AuthCallback<T> {
        fun onSuccess(data: T)
        fun onError(messageResId: Int)
    }

    /**
     * Validación Estricta de Contraseña:
     * - Mínimo 12 caracteres.
     * - Al menos una letra mayúscula.
     * - Al menos una letra minúscula.
     * - Al menos un número.
     * - Al menos un carácter especial (!@#$%^&*).
     */
    fun isPasswordValid(password: String): Boolean {
        if (password.length < 12) return false
        val hasUpper = password.any { it.isUpperCase() }
        val hasLower = password.any { it.isLowerCase() }
        val hasDigit = password.any { it.isDigit() }
        val hasSpecial = password.any { it in "!@#$%^&*" }
        return hasUpper && hasLower && hasDigit && hasSpecial
    }

    /**
     * Inicia sesión consumiendo el endpoint /usuarios en MockAPI con limpieza de espacios (trim)
     * y logging detallado de la respuesta HTTP en Logcat.
     */
    fun login(email: String, clave: String, callback: AuthCallback<Usuario>) {
        val cleanEmail = email.trim()
        val cleanPassword = clave.trim()

        Log.d("AuthController", "Intentando login con Email: '$cleanEmail'")

        apiService.getUsuarios().enqueue(object : Callback<List<Usuario>> {
            override fun onResponse(call: Call<List<Usuario>>, response: Response<List<Usuario>>) {
                Log.d("AuthController", "Login Response Code: ${response.code()}")
                Log.d("AuthController", "Login Is Successful: ${response.isSuccessful}")

                if (response.isSuccessful) {
                    val usuarios = response.body() ?: emptyList()
                    Log.d("AuthController", "Total usuarios recibidos de MockAPI: ${usuarios.size}")
                    usuarios.forEach { u ->
                        Log.d("AuthController", "Usuario DB -> ID: ${u.id}, Email: '${u.email}', Clave: '${u.clave}', Rol: ${u.rol}")
                    }

                    val usuarioEncontrado = usuarios.find { user ->
                        val dbEmail = user.email?.trim() ?: ""
                        val dbClave = user.clave?.trim() ?: ""
                        dbEmail.equals(cleanEmail, ignoreCase = true) && dbClave == cleanPassword
                    }

                    if (usuarioEncontrado != null) {
                        Log.d("AuthController", "¡Usuario autenticado con éxito: ${usuarioEncontrado.nombre}!")
                        callback.onSuccess(usuarioEncontrado)
                    } else {
                        Log.d("AuthController", "Usuario no encontrado o credenciales incorrectas para: '$cleanEmail'")
                        callback.onError(R.string.error_user_not_found)
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Sin error body"
                    Log.d("AuthController", "Error HTTP en servidor: Code ${response.code()}, ErrorBody: $errorBody")
                    callback.onError(R.string.error_server)
                }
            }

            override fun onFailure(call: Call<List<Usuario>>, t: Throwable) {
                Log.d("AuthController", "Fallo en la red (onFailure): ${t.message}", t)
                callback.onError(R.string.error_network)
            }
        })
    }

    /**
     * Registra un nuevo usuario verificando que el email no se encuentre registrado previamente.
     */
    fun register(nuevoUsuario: Usuario, callback: AuthCallback<Usuario>) {
        apiService.getUsuarios().enqueue(object : Callback<List<Usuario>> {
            override fun onResponse(call: Call<List<Usuario>>, response: Response<List<Usuario>>) {
                if (response.isSuccessful) {
                    val usuarios = response.body() ?: emptyList()
                    val correoExistente = usuarios.any {
                        it.email.equals(nuevoUsuario.email, ignoreCase = true)
                    }

                    if (correoExistente) {
                        callback.onError(R.string.error_email_already_registered)
                        return
                    }

                    // Proceder a crear el usuario en la API
                    apiService.createUsuario(nuevoUsuario).enqueue(object : Callback<Usuario> {
                        override fun onResponse(call: Call<Usuario>, response: Response<Usuario>) {
                            if (response.isSuccessful && response.body() != null) {
                                callback.onSuccess(response.body()!!)
                            } else {
                                callback.onError(R.string.error_register_failed)
                            }
                        }

                        override fun onFailure(call: Call<Usuario>, t: Throwable) {
                            callback.onError(R.string.error_network)
                        }
                    })

                } else {
                    callback.onError(R.string.error_server)
                }
            }

            override fun onFailure(call: Call<List<Usuario>>, t: Throwable) {
                callback.onError(R.string.error_network)
            }
        })
    }
}
