package com.team.eddie.uber_alles.ui.register

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.map.CustomerMapActivity
import com.team.eddie.uber_alles.ui.map.DriverMapActivity
import com.team.eddie.uber_alles.ui.welcome.WelcomeActivity
import com.team.eddie.uber_alles.utils.*
import com.team.eddie.uber_alles.utils.FirebaseUtility.ALL_USER
import com.team.eddie.uber_alles.utils.FirebaseUtility.CUSTOMER
import com.team.eddie.uber_alles.utils.FirebaseUtility.DRIVER
import com.team.eddie.uber_alles.utils.FirebaseUtility.EMAIL
import com.team.eddie.uber_alles.utils.FirebaseUtility.IS_DRIVER
import com.team.eddie.uber_alles.utils.FirebaseUtility.PASSWORD
import com.team.eddie.uber_alles.utils.FirebaseUtility.USER
import com.team.eddie.uber_alles.utils.FirebaseUtility.USERNAME
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
            startActivity(DriverMapActivity.getLaunchIntent(this))
        else
            startActivity(CustomerMapActivity.getLaunchIntent(this))
    }

    private fun attemptRegister() {

        if (localRegister()) {
            val email = emailTextInputEdit.text.toString()
            val password = passwordTextInputEdit.text.toString()

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (!task.isSuccessful)
                    Toast.makeText(this, "Couldn't Sign Up", Toast.LENGTH_SHORT).show()
                else {
                    val userId = mAuth.currentUser!!.uid
                    val typeUser: String = if (driverSwitch.isChecked) DRIVER else CUSTOMER

                    FirebaseDatabase.getInstance().reference.child(USER).child(typeUser).child(userId).child("name").setValue(email)

                    FirebaseDatabase.getInstance().reference.child(ALL_USER).child(userId).child(EMAIL).setValue(email)
                    FirebaseDatabase.getInstance().reference.child(ALL_USER).child(userId).child(PASSWORD).setValue(password)
                    FirebaseDatabase.getInstance().reference.child(ALL_USER).child(userId).child(USERNAME).setValue(usernameTextInputEdit.text.toString())
                    FirebaseDatabase.getInstance().reference.child(ALL_USER).child(userId).child(IS_DRIVER).setValue(driverSwitch.isChecked)
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
            repeatPasswordTextInput.error = getString(R.string.error_field_required)
            return false
        } else if (!isPasswordValid(repeatPassword)) {
            repeatPasswordTextInput.error = getString(R.string.error_invalid_password)
            return false
        } else if (!arePasswordsSame(password, repeatPassword)) {
            repeatPasswordTextInput.error = getString(R.string.error_password_match)
            return false
        }

        return true
    }


    fun showSignUpError() {
    }

}