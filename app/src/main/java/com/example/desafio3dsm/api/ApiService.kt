package com.example.desafio3dsm.api

import com.example.desafio3dsm.model.Recurso
import com.example.desafio3dsm.model.Usuario
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/**
 * Interfaz de Retrofit que define los endpoints para la API de AprendeApp en MockAPI.
 */
interface ApiService {

    // ==========================================
    // ENDPOINTS PARA USUARIOS (/usuarios)
    // ==========================================

    @GET("usuarios")
    fun getUsuarios(): Call<List<Usuario>>

    @GET("usuarios/{id}")
    fun getUsuarioById(@Path("id") id: String): Call<Usuario>

    @POST("usuarios")
    fun createUsuario(@Body usuario: Usuario): Call<Usuario>

    @PUT("usuarios/{id}")
    fun updateUsuario(@Path("id") id: String, @Body usuario: Usuario): Call<Usuario>

    @DELETE("usuarios/{id}")
    fun deleteUsuario(@Path("id") id: String): Call<Void>

    // ==========================================
    // ENDPOINTS PARA RECURSOS (/recursos)
    // ==========================================

    @GET("recursos")
    fun getRecursos(): Call<List<Recurso>>

    @GET("recursos/{id}")
    fun getRecursoById(@Path("id") id: String): Call<Recurso>

    @POST("recursos")
    fun createRecurso(@Body recurso: Recurso): Call<Recurso>

    @PUT("recursos/{id}")
    fun updateRecurso(@Path("id") id: String, @Body recurso: Recurso): Call<Recurso>

    @DELETE("recursos/{id}")
    fun deleteRecurso(@Path("id") id: String): Call<Void>
}
