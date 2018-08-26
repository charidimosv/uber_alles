package com.team.eddie.uber_alles.ui.register

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.MapsActivity
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity(), RegisterView {

//    private val presenter by lazy { registerPresenter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)
//        presenter.setView(this)
//        initUi()
    }

//    private fun initUi() {
//        usernameInput.onTextChanged { presenter.onUsernameChanged(it) }
//        emailInput.onTextChanged { presenter.onEmailChanged(it) }
//        passwordInput.onTextChanged { presenter.onPasswordChanged(it) }
//        repeatPasswordInput.onTextChanged { presenter.onRepeatPasswordChanged(it) }
//
//        registerButton.onClick { presenter.onRegisterTapped() }
//    }

    override fun onRegisterSuccess() = startActivity(MapsActivity.getLaunchIntent(this))

    override fun showSignUpError() {
    }

    override fun showUsernameError() {
        usernameInput.error = getString(R.string.username_error)
    }

    override fun showEmailError() {
        emailInput.error = getString(R.string.email_error)
    }

    override fun showPasswordError() {
        passwordInput.error = getString(R.string.password_error)
    }

    override fun showPasswordMatchingError() {
        repeatPasswordInput.error = getString(R.string.repeat_password_error)
    }
}