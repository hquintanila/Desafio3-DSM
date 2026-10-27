package com.example.desafio3dsm.controller

import com.example.desafio3dsm.R
import com.example.desafio3dsm.api.ApiService
import com.example.desafio3dsm.model.Recurso
import com.example.desafio3dsm.network.NetworkClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Controlador para la actualización y recálculo de calificaciones con estrellas en la API.
 */
class RatingController(
    private val apiService: ApiService = NetworkClient.apiService
) {

    interface RatingCallback {
        fun onSuccess(recursoActualizado: Recurso, messageResId: Int)
        fun onError(messageResId: Int)
    }

    /**
     * Calcula la nueva calificación promedio y total de votos y actualiza el recurso mediante PUT en /recursos/{id}.
     */
    fun calificarRecurso(recurso: Recurso, puntuacionIngresada: Float, callback: RatingCallback) {
        val recursoId = recurso.id
        if (recursoId.isNullOrEmpty()) {
            callback.onError(R.string.error_unknown)
            return
        }

        val totalAnterior = recurso.totalCalificaciones ?: 1
        val promedioAnterior = recurso.calificacionPromedio ?: recurso.calificacion ?: 0.0

        val nuevoTotal = totalAnterior + 1
        val nuevoPromedio = ((promedioAnterior * totalAnterior) + puntuacionIngresada.toDouble()) / nuevoTotal

        val recursoActualizado = recurso.copy(
            calificacionPromedio = nuevoPromedio,
            calificacion = nuevoPromedio,
            totalCalificaciones = nuevoTotal
        )

        apiService.updateRecurso(recursoId, recursoActualizado).enqueue(object : Callback<Recurso> {
            override fun onResponse(call: Call<Recurso>, response: Response<Recurso>) {
                if (response.isSuccessful && response.body() != null) {
                    callback.onSuccess(response.body()!!, R.string.toast_rating_saved)
                } else {
                    callback.onError(R.string.error_update_rating)
                }
            }

            override fun onFailure(call: Call<Recurso>, t: Throwable) {
                callback.onError(R.string.error_network)
            }
        })
    }
}
