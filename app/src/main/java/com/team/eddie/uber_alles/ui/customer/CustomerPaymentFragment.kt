package com.team.eddie.uber_alles.ui.customer

import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Switch
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentCustomerPaymentBinding
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.PaymentInfo

class CustomerPaymentFragment : Fragment() {

    private lateinit var paymentDatabase: DatabaseReference

    private var userID: String? = FirebaseHelper.getUserId()
    private var paymentInfo: PaymentInfo = PaymentInfo()


    private lateinit var mSwitch: Switch
    private lateinit var mCode: EditText
    private lateinit var mNumber: EditText
    private lateinit var mOwner: EditText

    private lateinit var mSave: MaterialButton
    private lateinit var mDelete: MaterialButton

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCustomerPaymentBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        mSwitch = binding.paymentSwitch
        mCode = binding.cardCode
        mNumber = binding.cardNumber
        mOwner = binding.cardOwner


        mSave = binding.save
        mDelete = binding.delete

        userID = FirebaseAuth.getInstance().currentUser!!.uid
        paymentDatabase = FirebaseHelper.getUserPaymentInfo(userID!!)

        getPaymentInfo()

        mSave.setOnClickListener { if (checkFields()) savePaymentInformation() }
        mDelete.setOnClickListener { deletePaymentInformation() }
        mSwitch.setOnClickListener {
            changeEditing(mSwitch.isChecked)
            if (!mSwitch.isChecked && checkFields())
                savePaymentInformation()
        }

        return binding.root
    }

    private fun getPaymentInfo() {
        mSwitch.isChecked = false
        paymentDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    paymentInfo = dataSnapshot.getValue(PaymentInfo::class.java)!!

                    paymentInfo?.cardNumber?.let { mNumber.setText(it) }
                    paymentInfo?.cardCode?.let { mCode.setText(it) }
                    paymentInfo?.cardOwner?.let { mOwner.setText(it) }
                    paymentInfo?.enableElectronicPayment?.let { mSwitch.isChecked = it.toBoolean() }

                    changeEditing(mSwitch.isChecked)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun changeEditing(value: Boolean) {
        mNumber.isEnabled = value
        mOwner.isEnabled = value
        mCode.isEnabled = value
        mSave.isEnabled = value
        mDelete.isEnabled = value
    }

    private fun savePaymentInformation() {
        paymentInfo?.cardNumber = mNumber.text.toString()
        paymentInfo?.cardOwner = mOwner.text.toString()
        paymentInfo?.cardCode = mCode.text.toString()
        paymentInfo?.enableElectronicPayment = mSwitch.isChecked.toString()

        paymentInfo?.let {
            paymentDatabase.setValue(it)
        }
    }

    private fun deletePaymentInformation() {
        if (!paymentInfo.cardNumber.isEmpty()) paymentDatabase.setValue(null)
        activity!!.supportFragmentManager.popBackStack()
    }

    private fun checkFields(): Boolean {
        var validationOk = true
        if (TextUtils.isEmpty(mCode.text.toString())) {
            mCode.error = getString(R.string.error_field_required)
            validationOk = false
        }
        if (TextUtils.isEmpty(mNumber.text.toString())) {
            mNumber.error = getString(R.string.error_field_required)
            validationOk = false
        }
        if (TextUtils.isEmpty(mOwner.text.toString())) {
            mOwner.error = getString(R.string.error_field_required)
            validationOk = false
        }

        return validationOk
    }
}