package com.team.eddie.uber_alles.ui.welcome

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.MapsActivity
import com.team.eddie.uber_alles.ui.login.LoginActivity
import com.team.eddie.uber_alles.ui.register.RegisterActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.onClick
import kotlinx.android.synthetic.main.activity_welcome.*

class WelcomeActivity : AppCompatActivity(), WelcomeView {

//    private val presenter by lazy { welcomePresenter() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)

        // Check if user is already LoggedIn
        if (SaveSharedPreference.isLoggedIn(applicationContext)) startMainScreen()
//        presenter.setView(this)

//        presenter.viewReady()

        registerButton.onClick { startActivity(Intent(this, RegisterActivity::class.java)) }
        loginButton.onClick { startActivity(Intent(this, LoginActivity::class.java)) }
    }

    override fun startMainScreen() = startActivity(MapsActivity.getLaunchIntent(this))
}
