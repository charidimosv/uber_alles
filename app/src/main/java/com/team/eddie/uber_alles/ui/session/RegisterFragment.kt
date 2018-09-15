package com.team.eddie.uber_alles.ui.session

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavDirections
import androidx.navigation.findNavController
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentRegisterBinding
import com.team.eddie.uber_alles.utils.*
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.EMAIL
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.IS_DRIVER
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.PASSWORD
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.USERNAME
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class RegisterFragment : Fragment() {

    private lateinit var binding: FragmentRegisterBinding
    private lateinit var applicationContext: Context

    private lateinit var mAuth: FirebaseAuth
    private lateinit var firebaseAuthListener: FirebaseAuth.AuthStateListener

    private lateinit var usernameTextInput: TextInputLayout
    private lateinit var usernameTextInputEdit: TextInputEditText

    private lateinit var emailTextInput: TextInputLayout
    private lateinit var emailTextInputEdit: TextInputEditText

    private lateinit var passwordTextInput: TextInputLayout
    private lateinit var passwordTextInputEdit: TextInputEditText

    private lateinit var repeatPasswordTextInput: TextInputLayout
    private lateinit var repeatPasswordTextInputEdit: TextInputEditText

    private lateinit var registerButton: MaterialButton
    private lateinit var backButton: MaterialButton

    private lateinit var driverSwitch: Switch


    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentRegisterBinding.inflate(inflater, container, false)
        applicationContext = activity?.applicationContext!!

        usernameTextInput = binding.usernameTextInput
        usernameTextInputEdit = binding.usernameTextInputEdit

        emailTextInput = binding.emailTextInput
        emailTextInputEdit = binding.emailTextInputEdit

        passwordTextInput = binding.passwordTextInput
        passwordTextInputEdit = binding.passwordTextInputEdit

        repeatPasswordTextInput = binding.repeatPasswordTextInput
        repeatPasswordTextInputEdit = binding.repeatPasswordTextInputEdit

        registerButton = binding.registerButton
        backButton = binding.backButton

        driverSwitch = binding.driverSwitch


        mAuth = FirebaseAuth.getInstance()
        firebaseAuthListener = FirebaseAuth.AuthStateListener {
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) onRegisterSuccess()
        }

        registerButton.setOnClickListener { attemptRegister() }
        backButton.setOnClickListener { activity!!.supportFragmentManager.popBackStack() }

        return binding.root
    }

    override fun onStart() {
        super.onStart()

        if (SaveSharedPreference.isLoggedIn(applicationContext)) {
            FirebaseHelper.cleanUser(FirebaseHelper.getUserId())
            mAuth.currentUser?.delete()?.addOnCompleteListener {
                SaveSharedPreference.cleanAll(applicationContext)
                mAuth.addAuthStateListener(firebaseAuthListener)
            }
        } else mAuth.addAuthStateListener(firebaseAuthListener)
    }

    override fun onStop() {
        super.onStop()
        mAuth.removeAuthStateListener(firebaseAuthListener)
    }

    private fun onRegisterSuccess() {
        SaveSharedPreference.setLoggedIn(applicationContext, emailTextInputEdit.text.toString())
        SaveSharedPreference.setUserType(applicationContext, driverSwitch.isChecked)

        val direction: NavDirections =
                if (driverSwitch.isChecked) RegisterFragmentDirections.actionRegisterFragmentToRegisterDriverProfileFragment()
                else RegisterFragmentDirections.actionRegisterFragmentToRegisterCustomerProfileFragment()
        binding.root.findNavController().navigate(direction)
    }

    private fun attemptRegister() {

        if (localRegister()) {
            val email = emailTextInputEdit.text.toString()
            val password = passwordTextInputEdit.text.toString()
            val username = usernameTextInputEdit.text.toString()

            val retrofit = RetrofitClient.getClient(applicationContext)
            val sessionServices = retrofit!!.create(SessionServices::class.java)

            val params: HashMap<String, String> = hashMapOf("email" to email, "password" to password)
            val call = sessionServices.registerCheck(params)
            call.enqueue(object : Callback<Boolean> {
                override fun onFailure(call: Call<Boolean>?, t: Throwable?) {
                    Toast.makeText(applicationContext, "Service is unavailable right now", Toast.LENGTH_SHORT).show()
                }

                override fun onResponse(call: Call<Boolean>?, response: Response<Boolean>?) {
                    val userExists = response!!.body()
                    if (userExists!!)
                        Toast.makeText(applicationContext, "Couldn't Sign Up", Toast.LENGTH_SHORT).show()
                    else {
                        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                            if (!task.isSuccessful)
                                Toast.makeText(applicationContext, "Couldn't Sign Up", Toast.LENGTH_SHORT).show()
                            else {
                                val userId = mAuth.currentUser!!.uid

                                val userInfo = hashMapOf(
                                        EMAIL to email,
                                        USERNAME to username,
                                        PASSWORD to password,
                                        IS_DRIVER to driverSwitch.isChecked)

                                val registerCall = sessionServices.register(userId, userInfo)
                                registerCall.enqueue(object : Callback<Void> {
                                    override fun onFailure(call: Call<Void>, t: Throwable) {
                                        Toast.makeText(applicationContext, "Couldn't Save Info", Toast.LENGTH_SHORT).show()
                                    }

                                    override fun onResponse(call: Call<Void>, response: Response<Void>) {}
                                })
                            }
                        }

                    }
                }
            })
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