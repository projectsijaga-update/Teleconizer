package com.teleconizer.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth

class LoginViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    // State untuk Login/Register
    sealed class LoginState {
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
        object Idle : LoginState()
    }

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(email: String, pass: String) {
        _loginState.value = LoginState.Loading
        auth.signInWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _loginState.value = LoginState.Success
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error("Login Gagal: ${e.message}")
            }
    }

    fun register(email: String, pass: String) {
        _loginState.value = LoginState.Loading
        auth.createUserWithEmailAndPassword(email, pass)
            .addOnSuccessListener {
                _loginState.value = LoginState.Success
            }
            .addOnFailureListener { e ->
                _loginState.value = LoginState.Error("Register Gagal: ${e.message}")
            }
    }
}
