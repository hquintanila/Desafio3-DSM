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
    val categoria: String? = null,

    @SerializedName("imagen")
    val imagen: String? = null,

    @SerializedName("calificacion")
    val calificacion: Double? = null,

    @SerializedName("calificacionPromedio")
    val calificacionPromedio: Double? = null,

    @SerializedName("totalCalificaciones")
    val totalCalificaciones: Int? = null
) {
    fun obtenerCalificacionPromedio(): Double {
        return calificacionPromedio ?: calificacion ?: 0.0
    }

    fun obtenerTotalVotos(): Int {
        return totalCalificaciones ?: 1
    }
}
