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
import com.bumptech.glide.Glide
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
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.NAME
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.PHONE
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper.PROFILE_IMG_URL
import java.io.ByteArrayOutputStream


class RegisterDriverProfileFragment : Fragment() {

    private lateinit var binding: FragmentDriverProfileBinding

    private lateinit var mDriverDatabase: DatabaseReference

    private var userID: String? = null

    private lateinit var mProfileImage: ImageView
    private var resultUri: Uri? = null

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

        mNameField = binding.name
        mPhoneField = binding.phone
        mProfileImage = binding.profileImage

        mConfirm = binding.confirm

        userID = FirebaseAuth.getInstance().currentUser!!.uid
        mDriverDatabase = FirebaseHelper.getUserInfo(userID!!)

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
        mDriverDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any?>

                    map[NAME]?.let { mNameField.setText(it.toString()) }
                    map[PHONE]?.let { mPhoneField.setText(it.toString()) }
                    map[PROFILE_IMG_URL]?.let { Glide.with(activity?.application!!).load(it.toString()).into(mProfileImage) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun saveUserInformation() {
        val mName = mNameField.text.toString()
        val mPhone = mPhoneField.text.toString()

        val userInfo: HashMap<String, *> = hashMapOf(NAME to mName, PHONE to mPhone)
        mDriverDatabase.updateChildren(userInfo)

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
                    val newImage: HashMap<String, *> = hashMapOf(PROFILE_IMG_URL to downloadUrl.toString())
                    mDriverDatabase.updateChildren(newImage)

                    moveNextStep()
                }
                return@OnSuccessListener
            })
        } else moveNextStep()
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