package com.team.eddie.uber_alles.ui.session

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentLoginBinding
import com.team.eddie.uber_alles.ui.customer.CustomerActivity
import com.team.eddie.uber_alles.ui.driver.DriverActivity
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.isEmailValid
import com.team.eddie.uber_alles.utils.isPasswordValid

class LoginFragment : Fragment() {

    private lateinit var applicationContext: Context

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseAuthListener: FirebaseAuth.AuthStateListener

    private lateinit var emailTextInput: TextInputLayout
    private lateinit var emailTextInputEdit: TextInputEditText

    private lateinit var passwordTextInput: TextInputLayout
    private lateinit var passwordTextInputEdit: TextInputEditText

    private lateinit var logInButton: MaterialButton
    private lateinit var backButton: MaterialButton

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentLoginBinding.inflate(inflater, container, false)

        applicationContext = activity?.applicationContext!!

        emailTextInput = binding.emailTextInput
        emailTextInputEdit = binding.emailTextInputEdit

        passwordTextInput = binding.passwordTextInput
        passwordTextInputEdit = binding.passwordTextInputEdit

        logInButton = binding.logInButton
        backButton = binding.backButton

        mAuth = FirebaseAuth.getInstance()

        firebaseAuthListener = FirebaseAuth.AuthStateListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) onLoginSuccess()
        }

        logInButton.setOnClickListener { attemptLogin() }
        backButton.setOnClickListener { activity!!.supportFragmentManager.popBackStack() }

        return binding.root
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

                    val isDriver: Boolean = dataSnapshot.value.toString().toBoolean()

                    SaveSharedPreference.setLoggedIn(applicationContext, emailTextInputEdit.text.toString())
                    SaveSharedPreference.setUserType(applicationContext, isDriver)

                    if (isDriver)
                        startActivity(DriverActivity.getLaunchIntent(activity!!))
                    else
                        startActivity(CustomerActivity.getLaunchIntent(activity!!))
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

            mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                if (!task.isSuccessful) Toast.makeText(applicationContext, "Couldn't Authenticate", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun localLogin(): Boolean {

        var failedFocusView: View? = null

        emailTextInput.error = null
        passwordTextInput.error = null

        if (!checkPassword(passwordTextInputEdit.text.toString()))
            failedFocusView = passwordTextInput

        if (!checkEmail(emailTextInputEdit.text.toString()))
            failedFocusView = emailTextInput

        failedFocusView?.requestFocus()

        return failedFocusView == null
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

}
