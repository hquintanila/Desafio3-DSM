package com.example.desafio3dsm.view

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.desafio3dsm.R
import com.example.desafio3dsm.controller.AuthController
import com.example.desafio3dsm.model.Usuario

/**
 * Vista / Actividad para el Registro de Nuevos Usuarios.
 */
class RegisterActivity : AppCompatActivity() {

    private lateinit var etNombre: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var etConfirmPassword: EditText
    private lateinit var spRol: Spinner
    private lateinit var btnRegister: Button
    private lateinit var tvGoToLogin: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var authController: AuthController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        authController = AuthController()

        initViews()
        setupSpinner()
        setupListeners()
    }

    private fun initViews() {
        etNombre = findViewById(R.id.etNombre)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        etConfirmPassword = findViewById(R.id.etConfirmPassword)
        spRol = findViewById(R.id.spRol)
        btnRegister = findViewById(R.id.btnRegister)
        tvGoToLogin = findViewById(R.id.tvGoToLogin)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupSpinner() {
        val adapter = ArrayAdapter.createFromResource(
            this,
            R.array.roles_array,
            android.R.layout.simple_spinner_item
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spRol.adapter = adapter
    }

    private fun setupListeners() {
        btnRegister.setOnClickListener {
            performRegister()
        }

        tvGoToLogin.setOnClickListener {
            finish()
        }
    }

    private fun performRegister() {
        val nombre = etNombre.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()
        val confirmPassword = etConfirmPassword.text.toString().trim()
        val rol = spRol.selectedItem?.toString() ?: "Estudiante"

        if (nombre.isEmpty() || email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError(getString(R.string.error_empty_fields))
            return
        }

        if (password != confirmPassword) {
            showError(getString(R.string.error_password_mismatch))
            return
        }

        // Validación estricta de contraseña
        if (!authController.isPasswordValid(password)) {
            showError(getString(R.string.error_password_requirements))
            return
        }

        showLoading(true)

        val nuevoUsuario = Usuario(
            nombre = nombre,
            email = email,
            clave = password,
            rol = rol
        )

        authController.register(nuevoUsuario, object : AuthController.AuthCallback<Usuario> {
            override fun onSuccess(data: Usuario) {
                showLoading(false)
                Toast.makeText(this@RegisterActivity, getString(R.string.success_register), Toast.LENGTH_LONG).show()
                finish()
            }

            override fun onError(messageResId: Int) {
                showLoading(false)
                showError(getString(messageResId))
            }
        })
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnRegister.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
