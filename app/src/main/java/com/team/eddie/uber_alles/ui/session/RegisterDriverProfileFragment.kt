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
import androidx.navigation.findNavController
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentDriverProfileBinding
import com.team.eddie.uber_alles.ui.ActivityHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.UserInfo
import java.io.ByteArrayOutputStream


class RegisterDriverProfileFragment : Fragment() {

    private lateinit var binding: FragmentDriverProfileBinding

    private lateinit var userDatabase: DatabaseReference

    private var userID: String? = null
    private var userInfo: UserInfo? = null

    private lateinit var mProfileImage: ImageView
    private var resultUri: Uri? = null

    private lateinit var mEmail: EditText
    private lateinit var mUsername: EditText
    private lateinit var mNameField: EditText
    private lateinit var mPhoneField: EditText

    private lateinit var mConfirm: MaterialButton

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDriverProfileBinding.inflate(inflater, container, false)
        setHasOptionsMenu(true)

        mEmail = binding.email
        mUsername = binding.username
        mNameField = binding.name
        mPhoneField = binding.phone
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

        mConfirm.text = getString(R.string.next)
        mConfirm.setOnClickListener { saveUserInformation() }

        return binding.root
    }

    private fun getUserInfo() {
        userDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    userInfo = dataSnapshot.getValue(UserInfo::class.java)

                    userInfo?.email?.let { mEmail.setText(it) }
                    userInfo?.username?.let { mUsername.setText(it) }
                    userInfo?.name?.let { mNameField.setText(it) }
                    userInfo?.phone.let { mPhoneField.setText(it.toString()) }
                    userInfo?.imageUrl?.let { ActivityHelper.bindImageFromUrl(mProfileImage, it) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun saveUserInformation() {
        val mName = mNameField.text.toString()
        val mPhone = mPhoneField.text.toString()

        userInfo?.name = mName
        userInfo?.phone = mPhone
        userInfo?.let { userDatabase.setValue(it) }
        userDatabase.setValue(userInfo).addOnCompleteListener {

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

                        moveNextStep()
                    }
                    return@OnSuccessListener
                })
            } else moveNextStep()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            resultUri = data!!.data
            mProfileImage.setImageURI(resultUri)
        }
    }

    private fun moveNextStep() {
        val direction = RegisterDriverProfileFragmentDirections.actionRegisterDriverProfileFragmentToRegisterDriverCarFragment()
        binding.root.findNavController().navigate(direction)
    }

}