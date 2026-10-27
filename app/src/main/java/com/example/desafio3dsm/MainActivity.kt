package com.example.desafio3dsm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.desafio3dsm.controller.SessionManager
import com.example.desafio3dsm.view.LoginActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sessionManager: SessionManager
    private lateinit var tvUserInfo: TextView
    private lateinit var btnLogout: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sessionManager = SessionManager(this)

        // Verificar sesión activa
        if (!sessionManager.isLoggedIn()) {
            redirectToLogin()
            return
        }

        setContentView(R.layout.activity_main)

        tvUserInfo = findViewById(R.id.tvUserInfo)
        btnLogout = findViewById(R.id.btnLogout)

        displaySessionInfo()

        btnLogout.setOnClickListener {
            sessionManager.logoutUser()
            redirectToLogin()
        }
    }

    private fun displaySessionInfo() {
        val name = sessionManager.getUserName() ?: ""
        val email = sessionManager.getUserEmail() ?: ""
        val role = sessionManager.getUserRole() ?: ""
        val id = sessionManager.getUserId() ?: ""

        tvUserInfo.text = getString(R.string.logged_in_info, name, email, role, id)
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}
