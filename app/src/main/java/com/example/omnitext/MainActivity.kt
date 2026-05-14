package com.example.omnitext

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

class MainActivity : AppCompatActivity() {

    private lateinit var etUsername: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var btnSignUp: MaterialButton
    private lateinit var tilUsername: TextInputLayout
    private lateinit var tilPassword: TextInputLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        bindViews()
        setupListeners()
    }

    private fun bindViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        btnLogin   = findViewById(R.id.btnLogin)
        btnSignUp  = findViewById(R.id.btnSignUp)
        tilUsername = etUsername.parent.parent as TextInputLayout
        tilPassword = etPassword.parent.parent as TextInputLayout
    }

    private fun setupListeners() {
        btnLogin.setOnClickListener {
            if (validateInputs()) {
                performLogin(
                    username = etUsername.text.toString().trim(),
                    password = etPassword.text.toString()
                )
            }
        }

        // Naviga alla SignUpActivity
        btnSignUp.setOnClickListener {
            startActivity(Intent(this, signup_screen_activity::class.java))
        }

        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilUsername.error = null
        }
        etPassword.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) tilPassword.error = null
        }
    }

    private fun validateInputs(): Boolean {
        var isValid = true
        val username = etUsername.text.toString().trim()
        val password = etPassword.text.toString()

        if (TextUtils.isEmpty(username)) {
            tilUsername.error = "Inserisci il tuo username"
            isValid = false
        } else {
            tilUsername.error = null
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.error = "Inserisci la tua password"
            isValid = false
        } else if (password.length < 6) {
            tilPassword.error = "La password deve contenere almeno 6 caratteri"
            isValid = false
        } else {
            tilPassword.error = null
        }

        return isValid
    }

    private fun performLogin(username: String, password: String) {
        btnLogin.isEnabled = false
        btnLogin.text = "Accesso in corso\u2026"

        android.os.Handler(mainLooper).postDelayed({
            btnLogin.isEnabled = true
            btnLogin.text = "ACCEDI"
            if (username == "admin" && password == "123456") {
                onLoginSuccess()
            } else {
                onLoginFailure()
            }
        }, 1500)
    }

    private fun onLoginSuccess() {
        Toast.makeText(this, "Benvenuto!", Toast.LENGTH_SHORT).show()
        // TODO: val intent = Intent(this, HomeActivity::class.java)
        // intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        // startActivity(intent); finish()
    }

    private fun onLoginFailure() {
        tilPassword.error = "Username o password non corretti"
        etPassword.text?.clear()
        etPassword.requestFocus()
    }
}