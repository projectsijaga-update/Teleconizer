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

        viewModel = ViewModelProvider(this)[LoginViewModel::class.java]

        // Tombol Login
        binding.btnLogin.setOnClickListener {
            val username = binding.etUsername.text.toString().trim() // ID berubah jadi etUsername
            val password = binding.etPassword.text.toString().trim()

            if (username.isNotEmpty() && password.isNotEmpty()) {
                viewModel.login(username, password)
            } else {
                Toast.makeText(this, "Mohon isi Username dan Password", Toast.LENGTH_SHORT).show()
            }
        }

        // Tombol Register sudah dihapus dari XML, jadi kodenya dihapus juga.

        observeViewModel()
    }

    private fun observeViewModel() {
        viewModel.loginState.observe(this) { state ->
            when (state) {
                is LoginViewModel.LoginState.Success -> {
                    Toast.makeText(this, "Login Berhasil!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, DashboardActivity::class.java))
                    finish()
                }
                is LoginViewModel.LoginState.Error -> {
                    Toast.makeText(this, state.message, Toast.LENGTH_SHORT).show()
                }
                else -> {}
            }
        }
    }
}
