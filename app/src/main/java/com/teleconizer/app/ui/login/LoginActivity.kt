package com.teleconizer.app.ui.login

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.teleconizer.app.databinding.ActivityLoginBinding
import com.teleconizer.app.ui.main.DashboardActivity

class LoginActivity : AppCompatActivity() {
    
    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupViewModel()
        setupClickListeners()
        observeViewModel()
    }
    
    private fun setupViewModel() {
        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]
    }
    
    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            
            // For UI testing, allow static dummy credentials without strict email/password rules
            if (username == "admin" && password == "1234") {
                viewModel.login(username, password)
            } else if (username.isEmpty() || password.isEmpty()) {
                showError("Invalid credentials")
            } else {
                // Fall back to dummy auth attempt (will show error inside ViewModel if not match)
                viewModel.login(username, password)
            }
        }
    }
    
    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            when (result) {
                is LoginResult.Success -> {
                    navigateToMain()
                }
                is LoginResult.Error -> {
                    showError(result.message)
                }
                is LoginResult.Loading -> {
                    // Show loading state if needed
                }
            }
        }
    }
    
    // Removed strict validation to accommodate dummy login (admin / 1234)
    
    private fun navigateToMain() {
        val intent = Intent(this, DashboardActivity::class.java)
        startActivity(intent)
        finish()
    }
    
    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

