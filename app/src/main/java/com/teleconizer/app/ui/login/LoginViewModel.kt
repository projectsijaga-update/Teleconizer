package com.teleconizer.app.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

sealed class LoginResult {
    object Success : LoginResult()
    data class Error(val message: String) : LoginResult()
    object Loading : LoginResult()
}

class LoginViewModel : ViewModel() {
    
    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult
    
    fun login(email: String, password: String) {
        _loginResult.value = LoginResult.Loading
        
        // Dummy authentication for UI testing: accept only admin / 1234
        CoroutineScope(Dispatchers.IO).launch {
            // Simulate short delay to keep UI behavior
            kotlinx.coroutines.delay(300)
            if (email == "admin" && password == "1234") {
                _loginResult.postValue(LoginResult.Success)
            } else {
                _loginResult.postValue(LoginResult.Error("Invalid credentials"))
            }
        }
    }
    
    // Removed: server-side style validation; replaced by static credential check above
}

