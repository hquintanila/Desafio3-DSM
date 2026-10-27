package com.example.desafio3dsm.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos que representa a un Usuario en la plataforma AprendeApp.
 */
data class Usuario(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("nombre")
    val nombre: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName(value = "contrasena", alternate = ["clave", "password"])
    val clave: String? = null,

    @SerializedName("rol")
    val rol: String? = null,

    @SerializedName("avatar")
    val avatar: String? = null,

    @SerializedName("favoritos")
    val favoritos: List<String>? = emptyList()
)
