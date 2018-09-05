package com.team.eddie.uber_alles.ui.session

import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.ui.customer.CustomerActivity
import com.team.eddie.uber_alles.ui.driver.DriverActivity
import com.team.eddie.uber_alles.utils.FirebaseHelper
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.isEmailValid
import com.team.eddie.uber_alles.utils.isPasswordValid
import kotlinx.android.synthetic.main.activity_login.*

class LoginActivity : AppCompatActivity() {

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseAuthListener: FirebaseAuth.AuthStateListener

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        mAuth = FirebaseAuth.getInstance()

        firebaseAuthListener = FirebaseAuth.AuthStateListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) onLoginSuccess()
        }

        logInButton.setOnClickListener { attemptLogin() }
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

    private fun onLoginSuccess() {

        val userId = mAuth.currentUser!!.uid
        FirebaseHelper.getUserIsDriver(userId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {

                    val isDriver: Boolean = dataSnapshot.getValue().toString().toBoolean()

                    SaveSharedPreference.setLoggedIn(applicationContext, emailTextInputEdit.text.toString())
                    SaveSharedPreference.setUserType(applicationContext, isDriver)

                    if (isDriver)
                        startActivity(DriverActivity.getLaunchIntent(this@LoginActivity))
                    else
                        startActivity(CustomerActivity.getLaunchIntent(this@LoginActivity))
                } else
                    Toast.makeText(applicationContext, "There is a problem retrieving info", Toast.LENGTH_SHORT).show()
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun attemptLogin() {

        if (localLogin()) {
            val email = emailTextInputEdit.text.toString()
            val password = passwordTextInputEdit.text.toString()

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (!task.isSuccessful) Toast.makeText(this, "Couldn't Authenticate", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun localLogin(): Boolean {

        var success = true
        var failedFocusView: View? = null

        emailTextInput.error = null
        passwordTextInput.error = null

        if (!checkPassword(passwordTextInputEdit.text.toString())) {
            failedFocusView = passwordTextInput
            success = false
        }

        if (!checkEmail(emailTextInputEdit.text.toString())) {
            failedFocusView = emailTextInput
            success = false
        }

        if (!success) failedFocusView?.requestFocus()

        return success
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

}
