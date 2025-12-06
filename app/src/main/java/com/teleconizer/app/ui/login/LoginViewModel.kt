package com.teleconizer.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.os.Handler
import android.os.Looper

class LoginViewModel : ViewModel() {

    // Kita tidak butuh FirebaseAuth lagi untuk mode hardcode ini
    // private val auth = FirebaseAuth.getInstance() 

    sealed class LoginState {
        object Loading : LoginState()
        object Success : LoginState()
        data class Error(val message: String) : LoginState()
        object Idle : LoginState()
    }

    private val _loginState = MutableLiveData<LoginState>(LoginState.Idle)
    val loginState: LiveData<LoginState> = _loginState

    fun login(username: String, pass: String) {
        _loginState.value = LoginState.Loading
        
        // Simulasi loading sebentar agar terasa seperti aplikasi beneran
        Handler(Looper.getMainLooper()).postDelayed({
            if (username == "admin" && pass == "1234") {
                _loginState.value = LoginState.Success
            } else {
                _loginState.value = LoginState.Error("Username atau Password salah!")
            }
        }, 1000) // Delay 1 detik
    }
}
