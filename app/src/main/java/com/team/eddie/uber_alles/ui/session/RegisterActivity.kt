package com.team.eddie.uber_alles.ui.session

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.customer.CustomerActivity
import com.team.eddie.uber_alles.ui.driver.DriverActivity
import com.team.eddie.uber_alles.utils.*
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.EMAIL
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.IS_DRIVER
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.PASSWORD
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.USERNAME
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseAuthListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()

        firebaseAuthListener = FirebaseAuth.AuthStateListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) onRegisterSuccess()
        }

        registerButton.setOnClickListener { attemptRegister() }
        backButton.setOnClickListener { startActivity(WelcomeActivity.getLaunchIntent(this)) }

    }

    override fun onStart() {
        super.onStart()
        mAuth.addAuthStateListener(firebaseAuthListener)
    }

    override fun onStop() {
        super.onStop()
        mAuth.removeAuthStateListener(firebaseAuthListener)
    }

    private fun onRegisterSuccess() {
        SaveSharedPreference.setLoggedIn(applicationContext, emailTextInputEdit.text.toString())
        SaveSharedPreference.setUserType(applicationContext, driverSwitch.isChecked)

        if (driverSwitch.isChecked)
            startActivity(DriverActivity.getLaunchIntent(this))
        else
            startActivity(CustomerActivity.getLaunchIntent(this))
    }

    private fun attemptRegister() {

        if (localRegister()) {
            val email = emailTextInputEdit.text.toString()
            val password = passwordTextInputEdit.text.toString()
            val username = usernameTextInputEdit.text.toString()

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (!task.isSuccessful)
                    Toast.makeText(this, "Couldn't Sign Up", Toast.LENGTH_SHORT).show()
                else {
                    val userId = mAuth.currentUser!!.uid
                    val userReference = FirebaseHelper.getUserInfo(userId)

                    val userInfo: HashMap<String, *> = hashMapOf(
                            EMAIL to email,
                            USERNAME to username,
                            PASSWORD to password,
                            IS_DRIVER to driverSwitch.isChecked)
                    userReference.updateChildren(userInfo)
                }
            }

        }
    }

    private fun localRegister(): Boolean {

        var failedFocusView: View? = null

        usernameTextInput.error = null
        emailTextInput.error = null
        passwordTextInput.error = null
        repeatPasswordTextInput.error = null

        if (!checkRepeatPassword(passwordTextInputEdit.text.toString(), repeatPasswordTextInputEdit.text.toString()))
            failedFocusView = repeatPasswordTextInput

        if (!checkPassword(passwordTextInputEdit.text.toString()))
            failedFocusView = passwordTextInput

        if (!checkEmail(emailTextInputEdit.text.toString()))
            failedFocusView = emailTextInput

        if (!checkUsername(usernameTextInputEdit.text.toString()))
            failedFocusView = usernameTextInput

        failedFocusView?.requestFocus()

        return failedFocusView == null
    }

    private fun checkUsername(username: String): Boolean {
        if (TextUtils.isEmpty(username))
            usernameTextInput.error = getString(R.string.error_field_required)
        else if (!isUsernameValid(username))
            usernameTextInput.error = getString(R.string.error_invalid_email)

        return usernameTextInput.error == null
    }

    private fun checkEmail(email: String): Boolean {
        if (TextUtils.isEmpty(email))
            emailTextInput.error = getString(R.string.error_field_required)
        else if (!isEmailValid(email))
            emailTextInput.error = getString(R.string.error_invalid_email)

        return emailTextInput.error == null
    }

    private fun checkPassword(password: String): Boolean {
        if (TextUtils.isEmpty(password))
            passwordTextInput.error = getString(R.string.error_field_required)
        else if (!isPasswordValid(password))
            passwordTextInput.error = getString(R.string.error_invalid_password)

        return passwordTextInput.error == null
    }

    private fun checkRepeatPassword(password: String, repeatPassword: String): Boolean {
        if (TextUtils.isEmpty(repeatPassword))
            repeatPasswordTextInput.error = getString(R.string.error_field_required)
        else if (!isPasswordValid(repeatPassword))
            repeatPasswordTextInput.error = getString(R.string.error_invalid_password)
        else if (!arePasswordsSame(password, repeatPassword))
            repeatPasswordTextInput.error = getString(R.string.error_password_match)

        return repeatPasswordTextInput.error == null
    }

}