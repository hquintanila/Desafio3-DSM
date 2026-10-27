package com.example.desafio3dsm

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desafio3dsm.adapter.RecursosAdapter
import com.example.desafio3dsm.controller.FavoritosController
import com.example.desafio3dsm.controller.RatingController
import com.example.desafio3dsm.controller.RecursosController
import com.example.desafio3dsm.controller.SessionManager
import com.example.desafio3dsm.model.Recurso
import com.example.desafio3dsm.view.LoginActivity
import com.google.android.material.chip.ChipGroup

/**
 * Pantalla Principal (MainActivity) de AprendeApp.
 * Incluye gestión de roles, listado dinámico, favoritos persistentes en API,
 * sistema de calificación con RatingBar y AlertDialogs de confirmación.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recursosController: RecursosController
    private lateinit var favoritosController: FavoritosController
    private lateinit var ratingController: RatingController

    private lateinit var tvUserRoleBadge: TextView
    private lateinit var btnLogout: ImageButton
    private lateinit var layoutDocentePanel: LinearLayout
    private lateinit var btnCrearRecurso: Button

    private lateinit var searchViewRecursos: SearchView
    private lateinit var chipGroupFiltros: ChipGroup

    private lateinit var rvRecursos: RecyclerView
    private lateinit var progressBarRecursos: ProgressBar
    private lateinit var tvEmptyRecursos: TextView

    private lateinit var recursosAdapter: RecursosAdapter
    private var listaOriginalRecursos: List<Recurso> = emptyList()
    private var listaFavoritosIds: MutableList<String> = mutableListOf()

    private var currentSearchQuery: String = ""
    private var currentFilterType: String = "Todos"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // 1. Verificar sesión activa
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        recursosController = RecursosController()
        favoritosController = FavoritosController()
        ratingController = RatingController()

        initViews()
        setupRoleManagement()
        setupRecyclerView()
        setupSearchAndFilters()
        setupListeners()

        // 2. Cargar favoritos del usuario y recursos de la API
        cargarFavoritosYRecursos()
    }

    private fun initViews() {
        tvUserRoleBadge = findViewById(R.id.tvUserRoleBadge)
        btnLogout = findViewById(R.id.btnLogout)
        layoutDocentePanel = findViewById(R.id.layoutDocentePanel)
        btnCrearRecurso = findViewById(R.id.btnCrearRecurso)

        searchViewRecursos = findViewById(R.id.searchViewRecursos)
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros)

        rvRecursos = findViewById(R.id.rvRecursos)
        progressBarRecursos = findViewById(R.id.progressBarRecursos)
        tvEmptyRecursos = findViewById(R.id.tvEmptyRecursos)
    }

    private fun setupRoleManagement() {
        val userName = sessionManager.getUserName() ?: ""
        val userRole = sessionManager.getUserRole() ?: getString(R.string.text_role_student)

        tvUserRoleBadge.text = getString(R.string.text_role_badge, userName, userRole)

        if (userRole.equals("Docente", ignoreCase = true)) {
            layoutDocentePanel.visibility = View.VISIBLE
        } else {
            layoutDocentePanel.visibility = View.GONE
        }
    }

    private fun setupRecyclerView() {
        recursosAdapter = RecursosAdapter(
            listaRecursos = emptyList(),
            listaFavoritosIds = listaFavoritosIds,
            onFavoritoClick = { recurso ->
                toggleFavorito(recurso)
            },
            onCalificarClick = { recurso ->
                mostrarDialogoCalificar(recurso)
            },
            onItemClick = { recurso ->
                Toast.makeText(this, recurso.titulo ?: getString(R.string.text_no_title), Toast.LENGTH_SHORT).show()
            }
        )
        rvRecursos.layoutManager = LinearLayoutManager(this)
        rvRecursos.adapter = recursosAdapter
    }

    private fun setupSearchAndFilters() {
        searchViewRecursos.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentSearchQuery = query.orEmpty()
                aplicarFiltros()
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText.orEmpty()
                aplicarFiltros()
                return true
            }
        })

        chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterType = if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
                    R.id.chipFavoritos -> getString(R.string.filter_favorites)
                    R.id.chipLibro -> getString(R.string.filter_book)
                    R.id.chipVideo -> getString(R.string.filter_video)
                    R.id.chipArticulo -> getString(R.string.filter_article)
                    else -> getString(R.string.filter_all)
                }
            } else {
                getString(R.string.filter_all)
            }
            aplicarFiltros()
        }
    }

    private fun setupListeners() {
        btnLogout.setOnClickListener {
            // AlertDialog de Confirmación para Cerrar Sesión
            mostrarDialogoCerrarSesion()
        }

        btnCrearRecurso.setOnClickListener {
            Toast.makeText(this, getString(R.string.btn_create_resource), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Carga inicial de datos: primero recupera los favoritos guardados y luego la lista de recursos.
     */
    private fun cargarFavoritosYRecursos() {
        showLoading(true)
        val userId = sessionManager.getUserId() ?: ""

        favoritosController.cargarFavoritos(userId, object : FavoritosController.CargarFavoritosCallback {
            override fun onSuccess(favoritos: List<String>) {
                listaFavoritosIds = favoritos.toMutableList()
                recursosAdapter.actualizarFavoritos(listaFavoritosIds)
                cargarRecursos()
            }

            override fun onError(messageResId: Int) {
                // Si falla la carga de favoritos, continuar cargando recursos
                cargarRecursos()
            }
        })
    }

    private fun cargarRecursos() {
        recursosController.obtenerRecursos(object : RecursosController.RecursosCallback {
            override fun onSuccess(recursos: List<Recurso>) {
                showLoading(false)
                listaOriginalRecursos = recursos
                aplicarFiltros()
            }

            override fun onError(messageResId: Int) {
                showLoading(false)
                tvEmptyRecursos.text = getString(messageResId)
                tvEmptyRecursos.visibility = View.VISIBLE
                recursosAdapter.actualizarLista(emptyList())
            }
        })
    }

    /**
     * Alterna un recurso entre favoritos y guarda la actualización en /usuarios/{id}
     */
    private fun toggleFavorito(recurso: Recurso) {
        val userId = sessionManager.getUserId() ?: return
        val recursoId = recurso.id ?: return

        showLoading(true)

        favoritosController.toggleFavorito(userId, recursoId, object : FavoritosController.FavoritosCallback {
            override fun onSuccess(isFavorito: Boolean, nuevosFavoritos: List<String>, messageResId: Int) {
                showLoading(false)
                listaFavoritosIds = nuevosFavoritos.toMutableList()
                recursosAdapter.actualizarFavoritos(listaFavoritosIds)
                Toast.makeText(this@MainActivity, getString(messageResId), Toast.LENGTH_SHORT).show()

                if (currentFilterType == getString(R.string.filter_favorites)) {
                    aplicarFiltros()
                }
            }

            override fun onError(messageResId: Int) {
                showLoading(false)
                Toast.makeText(this@MainActivity, getString(messageResId), Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Muestra un AlertDialog con RatingBar para calificar un recurso y actualizarlo en /recursos/{id}
     */
    private fun mostrarDialogoCalificar(recurso: Recurso) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_calificar, null)
        val ratingBar = dialogView.findViewById<RatingBar>(R.id.ratingBar)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton(getString(R.string.btn_save_rating)) { dialog, _ ->
                val ratingValue = ratingBar.rating
                if (ratingValue > 0) {
                    guardarCalificacion(recurso, ratingValue)
                }
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.btn_cancel)) { dialog, _ ->
                dialog.dismiss()
            }
            .create()
            .show()
    }

    private fun guardarCalificacion(recurso: Recurso, puntuacion: Float) {
        showLoading(true)

        ratingController.calificarRecurso(recurso, puntuacion, object : RatingController.RatingCallback {
            override fun onSuccess(recursoActualizado: Recurso, messageResId: Int) {
                showLoading(false)
                Toast.makeText(this@MainActivity, getString(messageResId), Toast.LENGTH_SHORT).show()

                // Actualizar recurso en la lista local
                listaOriginalRecursos = listaOriginalRecursos.map {
                    if (it.id == recursoActualizado.id) recursoActualizado else it
                }
                aplicarFiltros()
            }

            override fun onError(messageResId: Int) {
                showLoading(false)
                Toast.makeText(this@MainActivity, getString(messageResId), Toast.LENGTH_LONG).show()
            }
        })
    }

    /**
     * Muestra un AlertDialog de confirmación para cerrar sesión.
     */
    private fun mostrarDialogoCerrarSesion() {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_logout_title))
            .setMessage(getString(R.string.dialog_logout_message))
            .setPositiveButton(getString(R.string.dialog_confirm)) { _, _ ->
                sessionManager.logoutUser()
                redirectToLogin()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    /**
     * Aplica los filtros combinados de búsqueda por texto, categoría y pestaña de favoritos.
     */
    private fun aplicarFiltros() {
        val esFiltroFavoritos = currentFilterType == getString(R.string.filter_favorites)

        val listaBase = if (esFiltroFavoritos) {
            listaOriginalRecursos.filter { recurso ->
                recurso.id != null && listaFavoritosIds.contains(recurso.id)
            }
        } else {
            listaOriginalRecursos
        }

        val listaFiltrada = if (esFiltroFavoritos) {
            if (currentSearchQuery.isEmpty()) {
                listaBase
            } else {
                recursosController.filtrarRecursos(listaBase, currentSearchQuery, "Todos")
            }
        } else {
            recursosController.filtrarRecursos(listaBase, currentSearchQuery, currentFilterType)
        }

        recursosAdapter.actualizarLista(listaFiltrada)

        if (listaFiltrada.isEmpty()) {
            tvEmptyRecursos.text = if (esFiltroFavoritos) {
                getString(R.string.empty_favorites_list)
            } else {
                getString(R.string.empty_resources_list)
            }
            tvEmptyRecursos.visibility = View.VISIBLE
        } else {
            tvEmptyRecursos.visibility = View.GONE
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBarRecursos.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
