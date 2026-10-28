package com.example.desafio3dsm.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.desafio3dsm.R
import com.example.desafio3dsm.controller.RecursosController
import com.example.desafio3dsm.model.Recurso

/**
 * Vista / Actividad para el Formulario de Creación y Edición de Recursos Educativos (Rol Docente).
 */
class AddEditResourceActivity : AppCompatActivity() {

    private lateinit var tvFormTitle: TextView
    private lateinit var btnCloseForm: ImageButton
    private lateinit var etTitulo: EditText
    private lateinit var etDescripcion: EditText
    private lateinit var spTipo: Spinner
    private lateinit var etUrl: EditText
    private lateinit var etImagen: EditText
    private lateinit var btnGaleria: Button
    private lateinit var btnGuardarRecurso: Button
    private lateinit var progressBar: ProgressBar

    private lateinit var recursosController: RecursosController

    private var editMode = false
    private var recursoIdToEdit: String? = null

    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } catch (e: Exception) {
                // Ignorar si no soporta persistencia
            }
            etImagen.setText(it.toString())
            Toast.makeText(this, getString(R.string.toast_image_selected), Toast.LENGTH_SHORT).show()
        }
    }

    companion object {
        const val EXTRA_RECURSO_ID = "extra_recurso_id"
        const val EXTRA_RECURSO_TITULO = "extra_recurso_titulo"
        const val EXTRA_RECURSO_DESCRIPCION = "extra_recurso_descripcion"
        const val EXTRA_RECURSO_TIPO = "extra_recurso_tipo"
        const val EXTRA_RECURSO_URL = "extra_recurso_url"
        const val EXTRA_RECURSO_IMAGEN = "extra_recurso_imagen"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_edit_resource)

        recursosController = RecursosController()

        initViews()
        setupSpinner()
        checkIntentExtras()
        setupListeners()
    }

    private fun initViews() {
        tvFormTitle = findViewById(R.id.tvFormTitle)
        btnCloseForm = findViewById(R.id.btnCloseForm)
        etTitulo = findViewById(R.id.etTitulo)
        etDescripcion = findViewById(R.id.etDescripcion)
        spTipo = findViewById(R.id.spTipo)
        etUrl = findViewById(R.id.etUrl)
        etImagen = findViewById(R.id.etImagen)
        btnGaleria = findViewById(R.id.btnGaleria)
        btnGuardarRecurso = findViewById(R.id.btnGuardarRecurso)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.types_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTipo.adapter = adapter
    }

    private fun checkIntentExtras() {
        recursoIdToEdit = intent.getStringExtra(EXTRA_RECURSO_ID)

        if (!recursoIdToEdit.isNullOrEmpty()) {
            editMode = true
            tvFormTitle.text = getString(R.string.title_recurso_editar)

            val titulo = intent.getStringExtra(EXTRA_RECURSO_TITULO) ?: ""
            val descripcion = intent.getStringExtra(EXTRA_RECURSO_DESCRIPCION) ?: ""
            val tipo = intent.getStringExtra(EXTRA_RECURSO_TIPO) ?: "Libro"
            val url = intent.getStringExtra(EXTRA_RECURSO_URL) ?: ""
            val imagen = intent.getStringExtra(EXTRA_RECURSO_IMAGEN) ?: ""

            etTitulo.setText(titulo)
            etDescripcion.setText(descripcion)
            etUrl.setText(url)
            etImagen.setText(imagen)

            val spinnerPosition = (spTipo.adapter as ArrayAdapter<String>).getPosition(tipo)
            if (spinnerPosition >= 0) {
                spTipo.setSelection(spinnerPosition)
            }
        } else {
            editMode = false
            tvFormTitle.text = getString(R.string.title_recurso_crear)
        }
    }

    private fun setupListeners() {
        btnCloseForm.setOnClickListener {
            finish()
        }

        btnGaleria.setOnClickListener {
            galleryLauncher.launch("image/*")
        }

        btnGuardarRecurso.setOnClickListener {
            guardarRecurso()
        }
    }

    private fun guardarRecurso() {
        val titulo = etTitulo.text.toString().trim()
        val descripcion = etDescripcion.text.toString().trim()
        val tipo = spTipo.selectedItem?.toString() ?: "Libro"
        val url = etUrl.text.toString().trim()
        val imagen = etImagen.text.toString().trim()

        if (titulo.isEmpty() || descripcion.isEmpty() || url.isEmpty()) {
            showError(getString(R.string.error_fill_required_fields))
            return
        }

        showLoading(true)

        val recurso = Recurso(
            id = recursoIdToEdit,
            titulo = titulo,
            descripcion = descripcion,
            tipo = tipo,
            categoria = tipo,
            url = url,
            imagen = if (imagen.isEmpty()) null else imagen,
            calificacion = 5.0,
            calificacionPromedio = 5.0,
            totalCalificaciones = 1
        )

        val crudCallback = object : RecursosController.RecursoCrudCallback {
            override fun onSuccess(recurso: Recurso, messageResId: Int) {
                showLoading(false)
                Toast.makeText(this@AddEditResourceActivity, getString(messageResId), Toast.LENGTH_SHORT).show()
                setResult(RESULT_OK)
                finish()
            }

            override fun onError(messageResId: Int) {
                showLoading(false)
                showError(getString(messageResId))
            }
        }

        if (editMode && !recursoIdToEdit.isNullOrEmpty()) {
            recursosController.actualizarRecurso(recursoIdToEdit!!, recurso, crudCallback)
        } else {
            recursosController.crearRecurso(recurso, crudCallback)
        }
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnGuardarRecurso.isEnabled = !isLoading
        btnGaleria.isEnabled = !isLoading
        btnCloseForm.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
