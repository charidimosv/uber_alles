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
import com.google.firebase.storage.FirebaseStorage
import com.team.eddie.uber_alles.databinding.FragmentCustomerProfileBinding
import com.team.eddie.uber_alles.utils.FirebaseHelper
import java.io.ByteArrayOutputStream

class CustomerProfileFragment : androidx.fragment.app.Fragment() {

    private var mNameField: EditText? = null
    private var mPhoneField: EditText? = null

    private var mConfirm: Button? = null

    private var mProfileImage: ImageView? = null

    private var mCustomerDatabase: DatabaseReference? = null

    private var userID: String? = null

    private var resultUri: Uri? = null

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentCustomerProfileBinding.inflate(inflater, container, false)
        //context ?: return binding.root

        setHasOptionsMenu(true)

        mNameField = binding.name
        mPhoneField = binding.phone

        mProfileImage = binding.profileImage

        mConfirm = binding.confirm

        userID = FirebaseAuth.getInstance().currentUser!!.uid
        mCustomerDatabase = FirebaseHelper.getCustomer(userID!!)

        getUserInfo()

        mProfileImage!!.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        mConfirm!!.setOnClickListener { saveUserInformation() }


        return binding.root
    }

    private fun getUserInfo() {
        mCustomerDatabase?.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any>?
                    if (map!!["name"] != null)
                        mNameField?.setText(map["name"].toString())

                    if (map["phone"] != null)
                        mPhoneField?.setText(map["phone"].toString())

                    if (map["profileImageUrl"] != null)
                        Glide.with(activity?.application!!).load(map["profileImageUrl"].toString()).into(mProfileImage!!)
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun saveUserInformation() {
        val mName = mNameField?.text.toString()
        val mPhone = mPhoneField?.text.toString()


        val userInfo: HashMap<String, String?> = hashMapOf("name" to mName, "phone" to mPhone)
        mCustomerDatabase!!.updateChildren(userInfo as Map<String, Any>)

        if (resultUri != null) {

            val filePath = FirebaseStorage.getInstance().reference.child("profile_images").child(userID!!)
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
                    val newImage: HashMap<String, String> = hashMapOf("profileImageUrl" to downloadUrl.toString())
                    mCustomerDatabase!!.updateChildren(newImage as Map<String, Any>)

                    //activity?.finish()
                    return@OnSuccessListener
                })
                //activity?.finish()
                return@OnSuccessListener
            })
        } //else {
        //  activity?.finish()
        //}

        //todo navigate back

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            val imageUri = data!!.data
            resultUri = imageUri
            mProfileImage?.setImageURI(resultUri)
        }
    }
}