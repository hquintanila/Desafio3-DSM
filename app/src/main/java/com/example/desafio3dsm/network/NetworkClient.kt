package com.example.desafio3dsm.network

import com.example.desafio3dsm.api.ApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Cliente de red Singleton para la configuración e instanciación de Retrofit.
 */
object NetworkClient {

    private const val BASE_URL = "https://6ae0c9dfbe3bc8fead97ec53.mockapi.io/"

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
