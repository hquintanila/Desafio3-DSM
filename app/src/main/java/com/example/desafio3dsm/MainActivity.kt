package com.example.desafio3dsm

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RatingBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.desafio3dsm.adapter.RecursosAdapter
import com.example.desafio3dsm.controller.AuthController
import com.example.desafio3dsm.controller.FavoritosController
import com.example.desafio3dsm.controller.RatingController
import com.example.desafio3dsm.controller.RecursosController
import com.example.desafio3dsm.controller.SessionManager
import com.example.desafio3dsm.model.Recurso
import com.example.desafio3dsm.model.Usuario
import com.example.desafio3dsm.network.NetworkClient
import com.example.desafio3dsm.view.AddEditResourceActivity
import com.example.desafio3dsm.view.LoginActivity
import com.google.android.material.chip.ChipGroup
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * Pantalla Principal (MainActivity) de AprendeApp.
 * Incluye gestión de roles, listado dinámico, favoritos persistentes, calificación
 * y operaciones CRUD (Crear, Editar, Eliminar) exclusivas para el rol Docente.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var recursosController: RecursosController
    private lateinit var favoritosController: FavoritosController
    private lateinit var ratingController: RatingController
    private lateinit var authController: AuthController

    private lateinit var tvUserRoleBadge: TextView
    private lateinit var tvProfileAvatar: TextView
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
    private var isDocenteUser: Boolean = false

    private val addEditResourceLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            cargarRecursos()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        if (!sessionManager.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        recursosController = RecursosController()
        favoritosController = FavoritosController()
        ratingController = RatingController()
        authController = AuthController()

        initViews()
        setupRoleManagement()
        setupRecyclerView()
        setupSearchAndFilters()
        setupListeners()

        cargarFavoritosYRecursos()
    }

    private fun initViews() {
        tvUserRoleBadge = findViewById(R.id.tvUserRoleBadge)
        tvProfileAvatar = findViewById(R.id.tvProfileAvatar)
        layoutDocentePanel = findViewById(R.id.layoutDocentePanel)
        btnCrearRecurso = findViewById(R.id.btnCrearRecurso)

        searchViewRecursos = findViewById(R.id.searchViewRecursos)
        chipGroupFiltros = findViewById(R.id.chipGroupFiltros)

        rvRecursos = findViewById(R.id.rvRecursos)
        progressBarRecursos = findViewById(R.id.progressBarRecursos)
        tvEmptyRecursos = findViewById(R.id.tvEmptyRecursos)
    }

    private fun setupRoleManagement() {
        val userName = sessionManager.getUserName() ?: "Usuario"
        val userRole = sessionManager.getUserRole() ?: getString(R.string.text_role_student)

        isDocenteUser = userRole.equals("Docente", ignoreCase = true)
        tvUserRoleBadge.text = getString(R.string.text_role_badge, userName, userRole)
        tvProfileAvatar.text = obtenerIniciales(userName)

        if (isDocenteUser) {
            layoutDocentePanel.visibility = View.VISIBLE
        } else {
            layoutDocentePanel.visibility = View.GONE
        }
    }

    private fun obtenerIniciales(nombre: String?): String {
        if (nombre.isNullOrBlank()) return "U"
        val partes = nombre.trim().split("\\s+".toRegex())
        return if (partes.size >= 2) {
            "${partes[0].take(1)}${partes[1].take(1)}".uppercase()
        } else {
            partes[0].take(2).uppercase()
        }
    }

    private fun setupRecyclerView() {
        recursosAdapter = RecursosAdapter(
            listaRecursos = emptyList(),
            listaFavoritosIds = listaFavoritosIds,
            esDocente = isDocenteUser,
            onFavoritoClick = { recurso ->
                toggleFavorito(recurso)
            },
            onCalificarClick = { recurso ->
                mostrarDialogoCalificar(recurso)
            },
            onEditarClick = { recurso ->
                abrirEditarRecurso(recurso)
            },
            onEliminarClick = { recurso ->
                mostrarDialogoEliminar(recurso)
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
        tvProfileAvatar.setOnClickListener {
            mostrarDialogoPerfil()
        }

        btnCrearRecurso.setOnClickListener {
            val intent = Intent(this, AddEditResourceActivity::class.java)
            addEditResourceLauncher.launch(intent)
        }
    }

    private fun mostrarDialogoPerfil() {
        val name = sessionManager.getUserName() ?: "Usuario"
        val email = sessionManager.getUserEmail() ?: ""
        val role = sessionManager.getUserRole() ?: "Estudiante"
        val lastLogin = sessionManager.getLastLoginTime()
        val initials = obtenerIniciales(name)

        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_perfil, null)
        val tvAvatar = dialogView.findViewById<TextView>(R.id.tvDialogAvatar)
        val tvInfo = dialogView.findViewById<TextView>(R.id.tvDialogInfo)
        val btnCambiarPass = dialogView.findViewById<Button>(R.id.btnCambiarPassword)
        val btnLogoutDialog = dialogView.findViewById<Button>(R.id.btnDialogLogout)

        tvAvatar.text = initials
        tvInfo.text = getString(R.string.profile_info, name, email, role, lastLogin)

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        btnCambiarPass.setOnClickListener {
            dialog.dismiss()
            mostrarDialogoCambiarPassword()
        }

        btnLogoutDialog.setOnClickListener {
            dialog.dismiss()
            mostrarDialogoCerrarSesion()
        }

        dialog.show()
    }

    private fun mostrarDialogoCambiarPassword() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_cambiar_password, null)
        val etNewPass = dialogView.findViewById<EditText>(R.id.etNewPassword)
        val etConfirmPass = dialogView.findViewById<EditText>(R.id.etConfirmNewPassword)

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_change_password))
            .setView(dialogView)
            .setPositiveButton(getString(R.string.btn_save)) { dialog, _ ->
                val newPass = etNewPass.text.toString().trim()
                val confirmPass = etConfirmPass.text.toString().trim()

                if (newPass.isEmpty() || confirmPass.isEmpty()) {
                    Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (newPass != confirmPass) {
                    Toast.makeText(this, getString(R.string.error_password_mismatch), Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                if (!authController.isPasswordValid(newPass)) {
                    Toast.makeText(this, getString(R.string.error_password_requirements), Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }

                actualizarPasswordEnApi(newPass)
                dialog.dismiss()
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .create()
            .show()
    }

    private fun actualizarPasswordEnApi(nuevaClave: String) {
        showLoading(true)
        val userId = sessionManager.getUserId() ?: return

        NetworkClient.apiService.getUsuarioById(userId).enqueue(object : Callback<Usuario> {
            override fun onResponse(call: Call<Usuario>, response: Response<Usuario>) {
                if (response.isSuccessful && response.body() != null) {
                    val usuario = response.body()!!
                    val usuarioActualizado = usuario.copy(clave = nuevaClave)

                    NetworkClient.apiService.updateUsuario(userId, usuarioActualizado).enqueue(object : Callback<Usuario> {
                        override fun onResponse(call: Call<Usuario>, response: Response<Usuario>) {
                            showLoading(false)
                            if (response.isSuccessful) {
                                Toast.makeText(this@MainActivity, getString(R.string.toast_password_changed), Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this@MainActivity, getString(R.string.error_update_password), Toast.LENGTH_LONG).show()
                            }
                        }

                        override fun onFailure(call: Call<Usuario>, t: Throwable) {
                            showLoading(false)
                            Toast.makeText(this@MainActivity, getString(R.string.error_network), Toast.LENGTH_LONG).show()
                        }
                    })
                } else {
                    showLoading(false)
                    Toast.makeText(this@MainActivity, getString(R.string.error_server), Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<Usuario>, t: Throwable) {
                showLoading(false)
                Toast.makeText(this@MainActivity, getString(R.string.error_network), Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun abrirEditarRecurso(recurso: Recurso) {
        val intent = Intent(this, AddEditResourceActivity::class.java).apply {
            putExtra(AddEditResourceActivity.EXTRA_RECURSO_ID, recurso.id)
            putExtra(AddEditResourceActivity.EXTRA_RECURSO_TITULO, recurso.titulo)
            putExtra(AddEditResourceActivity.EXTRA_RECURSO_DESCRIPCION, recurso.descripcion)
            putExtra(AddEditResourceActivity.EXTRA_RECURSO_TIPO, recurso.tipo ?: recurso.categoria)
            putExtra(AddEditResourceActivity.EXTRA_RECURSO_URL, recurso.url)
            putExtra(AddEditResourceActivity.EXTRA_RECURSO_IMAGEN, recurso.imagen)
        }
        addEditResourceLauncher.launch(intent)
    }

    private fun mostrarDialogoEliminar(recurso: Recurso) {
        val id = recurso.id ?: return

        AlertDialog.Builder(this)
            .setTitle(getString(R.string.dialog_delete_title))
            .setMessage(getString(R.string.dialog_delete_message))
            .setPositiveButton(getString(R.string.btn_delete)) { _, _ ->
                eliminarRecurso(id)
            }
            .setNegativeButton(getString(R.string.btn_cancel), null)
            .show()
    }

    private fun eliminarRecurso(id: String) {
        showLoading(true)

        recursosController.eliminarRecurso(id, object : RecursosController.RecursoDeleteCallback {
            override fun onSuccess(messageResId: Int) {
                showLoading(false)
                Toast.makeText(this@MainActivity, getString(messageResId), Toast.LENGTH_SHORT).show()
                cargarRecursos()
            }

            override fun onError(messageResId: Int) {
                showLoading(false)
                Toast.makeText(this@MainActivity, getString(messageResId), Toast.LENGTH_LONG).show()
            }
        })
    }

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
                cargarRecursos()
            }
        })
    }

    private fun cargarRecursos() {
        recursosController.obtenerRecursos(object : RecursosController.RecursosCallback {
            override fun onSuccess(recursos: List<Recurso>) {
                showLoading(false)
                listaOriginalRecursos = recursos
                recursosAdapter.actualizarModoDocente(isDocenteUser)
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
