package com.team.eddie.uber_alles.ui.session

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentCustomerProfileBinding
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.ui.customer.CustomerActivity
import com.team.eddie.uber_alles.utils.RetrofitClient
import com.team.eddie.uber_alles.utils.SaveSharedPreference
import com.team.eddie.uber_alles.utils.SessionServices
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.UserInfo
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream

class RegisterCustomerProfileFragment : Fragment() {

    private lateinit var userDatabase: DatabaseReference

    private var userID: String? = null
    private var userInfo: UserInfo? = null

    private lateinit var mProfileImage: ImageView
    private var resultUri: Uri? = null

    private lateinit var mEmail: EditText
    private lateinit var mUsername: EditText
    private lateinit var mNameField: EditText
    private lateinit var mPhoneField: EditText
    private lateinit var mAddressField: EditText

    private lateinit var mConfirm: MaterialButton

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCustomerProfileBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        mEmail = binding.email
        mUsername = binding.username
        mNameField = binding.name
        mPhoneField = binding.phone
        mAddressField = binding.address

        mProfileImage = binding.profileImage

        mConfirm = binding.confirm

        userID = FirebaseAuth.getInstance().currentUser!!.uid
        userDatabase = FirebaseHelper.getUserInfo(userID!!)

        getUserInfo()

        mProfileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        mConfirm.text = getString(R.string.complete)
        mConfirm.setOnClickListener { saveUserInformation() }

        return binding.root
    }

    private fun getUserInfo() {
        userInfo = SaveSharedPreference.getUserInfo(activity?.applicationContext!!)
        userInfo?.email?.let { mEmail.setText(it) }
        userInfo?.username?.let { mUsername.setText(it) }
        userInfo?.name?.let { mNameField.setText(it) }
        userInfo?.address?.let { mAddressField.setText(it) }
        userInfo?.phone.let { mPhoneField.setText(it.toString()) }
        userInfo?.imageUrl?.let { ActivityHelper.bindImageFromUrl(mProfileImage, it) }
    }

    private fun saveUserInformation() {
        val mName = mNameField.text.toString()
        val mPhone = mPhoneField.text.toString()
        val mAddress = mAddressField.text.toString()

        userInfo?.name = mName
        userInfo?.phone = mPhone
        userInfo?.address = mAddress

        val retrofit = RetrofitClient.getClient(activity?.applicationContext!!)
        val sessionServices = retrofit!!.create(SessionServices::class.java)

        val registerCall = sessionServices.saveUserInfo(userInfo!!)
        registerCall.enqueue(object : Callback<Void> {
            override fun onFailure(call: Call<Void>, t: Throwable) {
                Toast.makeText(activity?.applicationContext!!, "Couldn't Save Info", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                SaveSharedPreference.setUserInfo(activity?.applicationContext!!, userInfo!!)
                if (resultUri != null) {

                    val filePath = FirebaseHelper.getProfileImages(userID!!)
                    val bitmap = MediaStore.Images.Media.getBitmap(activity?.application?.contentResolver, resultUri)

                    val baos = ByteArrayOutputStream()
                    bitmap!!.compress(Bitmap.CompressFormat.JPEG, 20, baos)
                    val data = baos.toByteArray()
                    val uploadTask = filePath.putBytes(data)

                    uploadTask.addOnFailureListener(OnFailureListener {
                        Toast.makeText(activity!!, getString(R.string.problem_saving_photo), Toast.LENGTH_SHORT).show()
                        return@OnFailureListener
                    })

                    uploadTask.addOnSuccessListener(OnSuccessListener { taskSnapshot ->
                        val downloadUrlTask = taskSnapshot.storage.downloadUrl
                        downloadUrlTask.addOnFailureListener {
                            OnFailureListener {
                                Toast.makeText(activity!!, getString(R.string.problem_saving_photo), Toast.LENGTH_SHORT).show()
                                return@OnFailureListener
                            }
                        }
                        downloadUrlTask.addOnSuccessListener { downloadUrl ->
                            val newImage: HashMap<String, *> = hashMapOf(FirebaseHelper.IMG_URL to downloadUrl.toString())
                            userDatabase.updateChildren(newImage)

                            startActivity(CustomerActivity.getLaunchIntent(activity!!))
                        }
                        return@OnSuccessListener
                    })

                } else startActivity(CustomerActivity.getLaunchIntent(activity!!))
            }
        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            resultUri = data!!.data
            mProfileImage.setImageURI(resultUri)
        }
    }

}