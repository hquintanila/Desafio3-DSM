package com.example.desafio3dsm

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desafio3dsm.adapter.RecursosAdapter
import com.example.desafio3dsm.controller.RecursosController
import com.example.desafio3dsm.controller.SessionManager
import com.example.desafio3dsm.model.Recurso
import com.example.desafio3dsm.view.LoginActivity
import com.google.android.material.chip.ChipGroup

/**
 * Pantalla Principal (MainActivity) de AprendeApp.
 * Administra el manejo de roles de usuario (Estudiante vs Docente), la visualización de Recursos Educativos
 * y el sistema de búsqueda/filtrado dinámico.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recursosController: RecursosController

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

        initViews()
        setupRoleManagement()
        setupRecyclerView()
        setupSearchAndFilters()
        setupListeners()

        // 2. Consumir el endpoint /recursos de la API
        cargarRecursos()
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

    /**
     * 1. Manejo y Verificación de Roles ("Estudiante" vs "Docente").
     */
    private fun setupRoleManagement() {
        val userName = sessionManager.getUserName() ?: ""
        val userRole = sessionManager.getUserRole() ?: getString(R.string.text_role_student)

        // Mostrar Badge con Nombre y Rol del Usuario
        tvUserRoleBadge.text = getString(R.string.text_role_badge, userName, userRole)

        // Si el usuario es Docente, activar las opciones avanzadas de edición y creación
        if (userRole.equals("Docente", ignoreCase = true)) {
            layoutDocentePanel.visibility = View.VISIBLE
        } else {
            // Estudiante: Vista de sólo lectura de recursos
            layoutDocentePanel.visibility = View.GONE
        }
    }

    /**
     * 3. Configurar RecyclerView y su Adapter.
     */
    private fun setupRecyclerView() {
        recursosAdapter = RecursosAdapter { recurso ->
            // Acción al hacer clic en un recurso
            Toast.makeText(this, recurso.titulo ?: getString(R.string.text_no_title), Toast.LENGTH_SHORT).show()
        }
        rvRecursos.layoutManager = LinearLayoutManager(this)
        rvRecursos.adapter = recursosAdapter
    }

    /**
     * 4. Búsqueda avanzada y Filtrado por título o tipo.
     */
    private fun setupSearchAndFilters() {
        // Listener de la barra de búsqueda
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

        // Listener del grupo de Chips de filtro por tipo
        chipGroupFiltros.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilterType = if (checkedIds.isNotEmpty()) {
                when (checkedIds.first()) {
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
            sessionManager.logoutUser()
            redirectToLogin()
        }

        btnCrearRecurso.setOnClickListener {
            Toast.makeText(this, getString(R.string.btn_create_resource), Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * 2. Consumir el endpoint /recursos mediante Retrofit
     */
    private fun cargarRecursos() {
        showLoading(true)

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
     * Aplica la combinación de filtro de búsqueda y tipo sobre la lista de recursos.
     */
    private fun aplicarFiltros() {
        val listaFiltrada = recursosController.filtrarRecursos(
            listaOriginalRecursos,
            currentSearchQuery,
            currentFilterType
        )

        recursosAdapter.actualizarLista(listaFiltrada)

        if (listaFiltrada.isEmpty()) {
            tvEmptyRecursos.text = getString(R.string.empty_resources_list)
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
