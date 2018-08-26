package com.team.eddie.uber_alles.ui.login

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Intent
import android.os.AsyncTask
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.MapsActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.isEmailValid
import com.team.eddie.uber_alles.utils.isPasswordValid
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    // Keep track of the login task to ensure we can cancel it if requested.
    private var mAuthTask: UserLoginTask? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Check if user is already LoggedIn
        if (SaveSharedPreference.isLoggedIn(applicationContext)) moveNextActivity()
        else login_form.visibility = View.VISIBLE

        log_in_button.setOnClickListener { attemptLogin() }
    }

    private fun moveNextActivity() {
        startActivity(Intent(this, MapsActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
    }

    private fun attemptLogin() {
        if (mAuthTask == null && localLogin()) {
            showProgress(true)

            mAuthTask = UserLoginTask(email.text.toString(), password.text.toString())
            mAuthTask!!.execute(null as Void?)
        }
    }

    private fun localLogin(): Boolean {

        var success = true
        var failedFocusView: View? = null

        email_text_input.error = null
        password_text_input.error = null

        if (!checkPassword(password.text.toString())) {
            failedFocusView = password
            success = false
        }

        if (!checkEmail(email.text.toString())) {
            failedFocusView = email
            success = false
        }

        if (!success) failedFocusView?.requestFocus()

        return success
    }

    private fun checkEmail(email: String): Boolean {
        if (TextUtils.isEmpty(email)) {
            email_text_input.error = getString(R.string.error_field_required)
            return false
        } else if (!isEmailValid(email)) {
            email_text_input.error = getString(R.string.error_invalid_email)
            return false
        }

        return true;
    }

    private fun checkPassword(password: String): Boolean {
        if (TextUtils.isEmpty(password)) {
            password_text_input.error = getString(R.string.error_field_required)
            return false
        } else if (!isPasswordValid(password)) {
            password_text_input.error = getString(R.string.error_invalid_password)
            return false
        }

        return true
    }

    private fun showProgress(show: Boolean) {
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        login_progress.visibility = if (show) View.VISIBLE else View.GONE
        login_form.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_form.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

        login_form.visibility = if (show) View.GONE else View.VISIBLE
        login_progress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
    }

    inner class UserLoginTask internal constructor(private val mEmail: String, private val mPassword: String) : AsyncTask<Void, Void, Boolean>() {

        override fun doInBackground(vararg params: Void): Boolean? {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000)
            } catch (e: InterruptedException) {
                return false
            }

            return true
        }

        override fun onPostExecute(success: Boolean?) {
            mAuthTask = null
            showProgress(false)

            if (success!!) {
                SaveSharedPreference.setLoggedIn(applicationContext, mEmail)
                moveNextActivity()
                // finish()
            } else {
                password_text_input.error = getString(R.string.error_incorrect_password)
                password.requestFocus()
            }
        }

        override fun onCancelled() {
            mAuthTask = null
            showProgress(false)
        }
    }
}
