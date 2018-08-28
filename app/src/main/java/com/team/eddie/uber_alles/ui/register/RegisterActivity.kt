package com.team.eddie.uber_alles.ui.register

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.MapsActivity
import com.team.eddie.uber_alles.utils.*
import kotlinx.android.synthetic.main.activity_register.*

class RegisterActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseAuthListener: FirebaseAuth.AuthStateListener

    private val DRIVER: String = "DRIVER";
    private val CUSTOMER: String = "CUSTOMER";

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        mAuth = FirebaseAuth.getInstance()

        firebaseAuthListener = FirebaseAuth.AuthStateListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) onRegisterSuccess()
        }

        registerButton.setOnClickListener { attemptRegister() }
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
        startActivity(MapsActivity.getLaunchIntent(this))
    }

    private fun attemptRegister() {

        if (localRegister()) {
            val email = emailTextInputEdit.text.toString()
            val password = passwordTextInputEdit.text.toString()

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) {
                    Toast.makeText(this, "Sign Up Error", Toast.LENGTH_SHORT).show()
                } else {
                    val typeUser: String = if (driverSwitch.isChecked) DRIVER else CUSTOMER

                    val userId = mAuth.currentUser!!.uid
                    val currentUserDb = FirebaseDatabase.getInstance().reference.child("Users").child(typeUser).child(userId).child("name")
                    currentUserDb.setValue(email)
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

        if (!(failedFocusView != null)) failedFocusView?.requestFocus()

        return failedFocusView == null
    }

    private fun checkUsername(username: String): Boolean {
        if (TextUtils.isEmpty(username)) {
            usernameTextInput.error = getString(R.string.error_field_required)
            return false
        } else if (!isUsernameValid(username)) {
            usernameTextInput.error = getString(R.string.error_invalid_email)
            return false
        }

        return true
    }

    private fun checkEmail(email: String): Boolean {
        if (TextUtils.isEmpty(email)) {
            emailTextInput.error = getString(R.string.error_field_required)
            return false
        } else if (!isEmailValid(email)) {
            emailTextInput.error = getString(R.string.error_invalid_email)
            return false
        }

        return true
    }

    private fun checkPassword(password: String): Boolean {
        if (TextUtils.isEmpty(password)) {
            passwordTextInput.error = getString(R.string.error_field_required)
            return false
        } else if (!isPasswordValid(password)) {
            passwordTextInput.error = getString(R.string.error_invalid_password)
            return false
        }

        return true
    }

    private fun checkRepeatPassword(password: String, repeatPassword: String): Boolean {
        if (TextUtils.isEmpty(repeatPassword)) {
            passwordTextInput.error = getString(R.string.error_field_required)
            return false
        } else if (!isPasswordValid(repeatPassword)) {
            passwordTextInput.error = getString(R.string.error_invalid_password)
            return false
        } else if (!arePasswordsSame(password, repeatPassword)) {
            passwordTextInput.error = getString(R.string.error_password_match)
            return false
        }

        return true
    }


    fun showSignUpError() {
    }

}