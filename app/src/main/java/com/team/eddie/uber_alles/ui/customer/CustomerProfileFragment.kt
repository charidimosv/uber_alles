package com.team.eddie.uber_alles.ui.customer

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.databinding.FragmentCustomerProfileBinding
import com.team.eddie.uber_alles.utils.FirebaseHelper
import com.team.eddie.uber_alles.utils.FirebaseHelper.NAME
import com.team.eddie.uber_alles.utils.FirebaseHelper.PHONE
import com.team.eddie.uber_alles.utils.FirebaseHelper.PROFILE_IMG_URL
import java.io.ByteArrayOutputStream

class CustomerProfileFragment : androidx.fragment.app.Fragment() {

    private lateinit var mCustomerDatabase: DatabaseReference

    private var userID: String? = null

    private lateinit var mProfileImage: ImageView
    private var resultUri: Uri? = null

    private lateinit var mNameField: EditText
    private lateinit var mPhoneField: EditText

    private lateinit var mConfirm: Button

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCustomerProfileBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        mNameField = binding.name
        mPhoneField = binding.phone

        mProfileImage = binding.profileImage

        mConfirm = binding.confirm

        userID = FirebaseAuth.getInstance().currentUser!!.uid
        mCustomerDatabase = FirebaseHelper.getCustomerInfo(userID!!)

        getUserInfo()

        mProfileImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        mConfirm.setOnClickListener { saveUserInformation() }

        return binding.root
    }

    private fun getUserInfo() {
        mCustomerDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any>

                    if (map[NAME] != null)
                        mNameField.setText(map[NAME].toString())

                    if (map[PHONE] != null)
                        mPhoneField.setText(map[PHONE].toString())

                    if (map[PROFILE_IMG_URL] != null)
                        Glide.with(activity?.application!!).load(map[PROFILE_IMG_URL].toString()).into(mProfileImage)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun saveUserInformation() {
        val mName = mNameField.text.toString()
        val mPhone = mPhoneField.text.toString()

        val userInfo: HashMap<String, *> = hashMapOf(NAME to mName, PHONE to mPhone)
        mCustomerDatabase.updateChildren(userInfo)

        if (resultUri != null) {

            val filePath = FirebaseHelper.getProfileImages(userID!!)
            val bitmap = MediaStore.Images.Media.getBitmap(activity?.application?.contentResolver, resultUri)

            val baos = ByteArrayOutputStream()
            bitmap!!.compress(Bitmap.CompressFormat.JPEG, 20, baos)
            val data = baos.toByteArray()
            val uploadTask = filePath.putBytes(data)

            uploadTask.addOnFailureListener(OnFailureListener {
                //activity?.finish()
                return@OnFailureListener
            })
            uploadTask.addOnSuccessListener(OnSuccessListener { taskSnapshot ->
                val downloadUrlTask = taskSnapshot.storage.downloadUrl
                downloadUrlTask.addOnFailureListener {
                    OnFailureListener {
                        //activity?.finish()
                        return@OnFailureListener
                    }
                }
                downloadUrlTask.addOnSuccessListener(OnSuccessListener { downloadUrl ->
                    val newImage: HashMap<String, *> = hashMapOf(PROFILE_IMG_URL to downloadUrl.toString())
                    mCustomerDatabase.updateChildren(newImage)

                    //activity?.finish()
                    return@OnSuccessListener
                })
                //activity?.finish()
                return@OnSuccessListener
            })
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            resultUri = data!!.data
            mProfileImage.setImageURI(resultUri)
        }
    }
}