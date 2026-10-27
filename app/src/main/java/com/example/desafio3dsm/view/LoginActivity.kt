package com.example.desafio3dsm.view

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.desafio3dsm.MainActivity
import com.example.desafio3dsm.R
import com.example.desafio3dsm.controller.AuthController
import com.example.desafio3dsm.controller.SessionManager
import com.example.desafio3dsm.model.Usuario

/**
 * Vista / Actividad para el Inicio de Sesión de Usuarios.
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var etEmail: EditText
    private lateinit var etPassword: EditText
    private lateinit var btnLogin: Button
    private lateinit var tvGoToRegister: TextView
    private lateinit var progressBar: ProgressBar

    private lateinit var authController: AuthController
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        authController = AuthController()
        sessionManager = SessionManager(this)

        // Si ya hay sesión activa, redirigir inmediatamente a MainActivity
        if (sessionManager.isLoggedIn()) {
            navigateToMain()
            return
        }

        initViews()
        setupListeners()
    }

    private fun initViews() {
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoToRegister = findViewById(R.id.tvGoToRegister)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            performLogin()
        }

        tvGoToRegister.setOnClickListener {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin() {
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        if (email.isEmpty() || password.isEmpty()) {
            showError(getString(R.string.error_empty_fields))
            return
        }

        showLoading(true)

        authController.login(email, password, object : AuthController.AuthCallback<Usuario> {
            override fun onSuccess(data: Usuario) {
                showLoading(false)
                val userId = data.id ?: ""
                val userEmail = data.email ?: email
                val userRole = data.rol ?: "Estudiante"
                val userName = data.nombre ?: ""

                sessionManager.createLoginSession(userId, userEmail, userRole, userName)
                Toast.makeText(this@LoginActivity, getString(R.string.success_login), Toast.LENGTH_SHORT).show()
                navigateToMain()
            }

            override fun onError(messageResId: Int) {
                showLoading(false)
                showError(getString(messageResId))
            }
        })
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun showLoading(isLoading: Boolean) {
        progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        btnLogin.isEnabled = !isLoading
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}
