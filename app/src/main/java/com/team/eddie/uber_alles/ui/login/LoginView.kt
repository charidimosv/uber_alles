package com.team.eddie.uber_alles.ui.login

interface LoginView {

    fun showPasswordError()

    fun showEmailError()

    fun onLoginSuccess()

    fun showLoginError()
}