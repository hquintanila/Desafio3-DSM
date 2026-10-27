package com.example.desafio3dsm.controller

import android.content.Context
import android.content.SharedPreferences
import com.example.desafio3dsm.model.Usuario

/**
 * Controlador de Sesión que administra SharedPreferences para la persistencia del usuario activo.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "AprendeAppSession"
        private const val KEY_IS_LOGGED_IN = "isLoggedIn"
        private const val KEY_USER_ID = "userId"
        private const val KEY_USER_EMAIL = "userEmail"
        private const val KEY_USER_ROLE = "userRole"
        private const val KEY_USER_NAME = "userName"
    }

    /**
     * Guarda la sesión del usuario al iniciar sesión exitosamente.
     */
    fun createLoginSession(id: String, email: String, rol: String, nombre: String? = null) {
        val editor = prefs.edit()
        editor.putBoolean(KEY_IS_LOGGED_IN, true)
        editor.putString(KEY_USER_ID, id)
        editor.putString(KEY_USER_EMAIL, email)
        editor.putString(KEY_USER_ROLE, rol)
        editor.putString(KEY_USER_NAME, nombre ?: "")
        editor.apply()
    }

    /**
     * Verifica si existe una sesión activa.
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getUserId(): String? = prefs.getString(KEY_USER_ID, null)
    fun getUserEmail(): String? = prefs.getString(KEY_USER_EMAIL, null)
    fun getUserRole(): String? = prefs.getString(KEY_USER_ROLE, null)
    fun getUserName(): String? = prefs.getString(KEY_USER_NAME, null)

    /**
     * Devuelve el objeto Usuario de la sesión activa o null si no está autenticado.
     */
    fun getUserSession(): Usuario? {
        if (!isLoggedIn()) return null
        return Usuario(
            id = getUserId(),
            nombre = getUserName(),
            email = getUserEmail(),
            rol = getUserRole()
        )
    }

    /**
     * Cierra la sesión activa borrando los datos de SharedPreferences.
     */
    fun logoutUser() {
        val editor = prefs.edit()
        editor.clear()
        editor.apply()
    }
}
