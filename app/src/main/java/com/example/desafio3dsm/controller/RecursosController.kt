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
 * Gestiona la carga de la API, el filtrado dinámico y la búsqueda.
 */
class RecursosController(
    private val apiService: ApiService = NetworkClient.apiService
) {

    interface RecursosCallback {
        fun onSuccess(recursos: List<Recurso>)
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
