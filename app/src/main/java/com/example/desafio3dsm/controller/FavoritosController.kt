package com.example.desafio3dsm.controller

import com.example.desafio3dsm.R
import com.example.desafio3dsm.api.ApiService
import com.example.desafio3dsm.model.Usuario
import com.example.desafio3dsm.network.NetworkClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Controlador para la gestión y persistencia de Favoritos del Usuario en arquitectura MVC.
 */
class FavoritosController(
    private val apiService: ApiService = NetworkClient.apiService
) {

    interface FavoritosCallback {
        fun onSuccess(isFavorito: Boolean, nuevosFavoritos: List<String>, messageResId: Int)
        fun onError(messageResId: Int)
    }

    interface CargarFavoritosCallback {
        fun onSuccess(favoritos: List<String>)
        fun onError(messageResId: Int)
    }

    /**
     * Carga los IDs de recursos favoritos guardados del usuario actual.
     */
    fun cargarFavoritos(userId: String, callback: CargarFavoritosCallback) {
        if (userId.isEmpty()) {
            callback.onSuccess(emptyList())
            return
        }

        apiService.getUsuarioById(userId).enqueue(object : Callback<Usuario> {
            override fun onResponse(call: Call<Usuario>, response: Response<Usuario>) {
                if (response.isSuccessful && response.body() != null) {
                    val favs = response.body()?.favoritos ?: emptyList()
                    callback.onSuccess(favs)
                } else {
                    callback.onSuccess(emptyList())
                }
            }

            override fun onFailure(call: Call<Usuario>, t: Throwable) {
                callback.onError(R.string.error_network)
            }
        })
    }

    /**
     * Alterna la marcación/desmarcación de un recurso como favorito y sincroniza con el endpoint /usuarios/{id}.
     */
    fun toggleFavorito(userId: String, recursoId: String, callback: FavoritosCallback) {
        if (userId.isEmpty() || recursoId.isEmpty()) {
            callback.onError(R.string.error_unknown)
            return
        }

        apiService.getUsuarioById(userId).enqueue(object : Callback<Usuario> {
            override fun onResponse(call: Call<Usuario>, response: Response<Usuario>) {
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!
                    val favList = (usuario.favoritos ?: emptyList()).toMutableList()

                    val yaEsFavorito = favList.contains(recursoId)
                    val esNuevoFavorito: Boolean
                    val msgResId: Int

                    if (yaEsFavorito) {
                        favList.remove(recursoId)
                        esNuevoFavorito = false
                        msgResId = R.string.toast_favorite_removed
                    } else {
                        favList.add(recursoId)
                        esNuevoFavorito = true
                        msgResId = R.string.toast_favorite_added
                    }

                    val usuarioActualizado = usuario.copy(favoritos = favList)

                    apiService.updateUsuario(userId, usuarioActualizado).enqueue(object : Callback<Usuario> {
                        override fun onResponse(call: Call<Usuario>, response: Response<Usuario>) {
                            if (response.isSuccessful) {
                                callback.onSuccess(esNuevoFavorito, favList, msgResId)
                            } else {
                                callback.onError(R.string.error_update_favorite)
                            }
                        }

                        override fun onFailure(call: Call<Usuario>, t: Throwable) {
                            callback.onError(R.string.error_network)
                        }
                    })

                } else {
                    callback.onError(R.string.error_user_not_found)
                }
            }

            override fun onFailure(call: Call<Usuario>, t: Throwable) {
                callback.onError(R.string.error_network)
            }
        })
    }
}
