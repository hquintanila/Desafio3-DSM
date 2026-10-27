package com.example.desafio3dsm.controller

import com.example.desafio3dsm.R
import com.example.desafio3dsm.api.ApiService
import com.example.desafio3dsm.model.Recurso
import com.example.desafio3dsm.network.NetworkClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Controlador de Recursos Educativos en arquitectura MVC.
 * Administra las peticiones de consulta, creación (POST), edición (PUT), eliminación (DELETE) y filtrado.
 */
class RecursosController(
    private val apiService: ApiService = NetworkClient.apiService
) {

    interface RecursosCallback {
        fun onSuccess(recursos: List<Recurso>)
        fun onError(messageResId: Int)
    }

    interface RecursoCrudCallback {
        fun onSuccess(recurso: Recurso, messageResId: Int)
        fun onError(messageResId: Int)
    }

    interface RecursoDeleteCallback {
        fun onSuccess(messageResId: Int)
        fun onError(messageResId: Int)
    }

    /**
     * Obtiene la lista completa de recursos desde el endpoint /recursos de la API.
     */
    fun obtenerRecursos(callback: RecursosCallback) {
        apiService.getRecursos().enqueue(object : Callback<List<Recurso>> {
            override fun onResponse(call: Call<List<Recurso>>, response: Response<List<Recurso>>) {
                if (response.isSuccessful) {
                    val lista = response.body() ?: emptyList()
                    callback.onSuccess(lista)
                } else {
                    callback.onError(R.string.error_loading_resources)
                }
            }

            override fun onFailure(call: Call<List<Recurso>>, t: Throwable) {
                callback.onError(R.string.error_network)
            }
        })
    }

    /**
     * Crea un nuevo recurso educativo consumiendo POST /recursos en MockAPI.
     */
    fun crearRecurso(nuevoRecurso: Recurso, callback: RecursoCrudCallback) {
        apiService.createRecurso(nuevoRecurso).enqueue(object : Callback<Recurso> {
            override fun onResponse(call: Call<Recurso>, response: Response<Recurso>) {
                if (response.isSuccessful && response.body() != null) {
                    callback.onSuccess(response.body()!!, R.string.toast_resource_created)
                } else {
                    callback.onError(R.string.error_create_resource)
                }
            }

            override fun onFailure(call: Call<Recurso>, t: Throwable) {
                callback.onError(R.string.error_network)
            }
        })
    }

    /**
     * Actualiza un recurso existente consumiendo PUT /recursos/{id} en MockAPI.
     */
    fun actualizarRecurso(id: String, recurso: Recurso, callback: RecursoCrudCallback) {
        apiService.updateRecurso(id, recurso).enqueue(object : Callback<Recurso> {
            override fun onResponse(call: Call<Recurso>, response: Response<Recurso>) {
                if (response.isSuccessful && response.body() != null) {
                    callback.onSuccess(response.body()!!, R.string.toast_resource_updated)
                } else {
                    callback.onError(R.string.error_update_resource)
                }
            }

            override fun onFailure(call: Call<Recurso>, t: Throwable) {
                callback.onError(R.string.error_network)
            }
        })
    }

    /**
     * Elimina un recurso consumiendo DELETE /recursos/{id} en MockAPI.
     */
    fun eliminarRecurso(id: String, callback: RecursoDeleteCallback) {
        apiService.deleteRecurso(id).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    callback.onSuccess(R.string.toast_resource_deleted)
                } else {
                    callback.onError(R.string.error_delete_resource)
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                callback.onError(R.string.error_network)
            }
        })
    }

    /**
     * Filtra la lista de recursos por consulta textual y por tipo de recurso.
     */
    fun filtrarRecursos(
        listaOriginal: List<Recurso>,
        query: String,
        tipoFiltro: String
    ): List<Recurso> {
        val q = query.trim().lowercase()
        val filtroNorm = tipoFiltro.trim().lowercase()

        return listaOriginal.filter { recurso ->
            val titulo = recurso.titulo?.lowercase() ?: ""
            val descripcion = recurso.descripcion?.lowercase() ?: ""
            val tipo = recurso.tipo?.lowercase() ?: ""
            val categoria = recurso.categoria?.lowercase() ?: ""

            val coincideQuery = q.isEmpty() || titulo.contains(q) || descripcion.contains(q)

            val coincideTipo = filtroNorm.isEmpty() ||
                filtroNorm == "todos" ||
                tipo == filtroNorm ||
                categoria == filtroNorm ||
                (filtroNorm == "libro" && (tipo.contains("libro") || categoria.contains("libro"))) ||
                (filtroNorm == "video" && (tipo.contains("video") || categoria.contains("video"))) ||
                (filtroNorm == "artículo" && (tipo.contains("articulo") || tipo.contains("artículo") || categoria.contains("articulo") || categoria.contains("artículo")))

            coincideQuery && coincideTipo
        }
    }
}
