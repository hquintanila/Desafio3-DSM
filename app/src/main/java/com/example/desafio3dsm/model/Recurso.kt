package com.example.desafio3dsm.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de datos que representa un Recurso educativo en la plataforma AprendeApp.
 */
data class Recurso(
    @SerializedName("id")
    val id: String? = null,

    @SerializedName("titulo")
    val titulo: String? = null,

    @SerializedName("descripcion")
    val descripcion: String? = null,

    @SerializedName("tipo")
    val tipo: String? = null,

    @SerializedName("url")
    val url: String? = null,

    @SerializedName("categoria")
    val categoria: String? = null
)
