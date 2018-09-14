package com.team.eddie.uber_alles.ui.driver

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentDriverCarSingleBinding
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import java.io.ByteArrayOutputStream

class DriverCarSingleFragment : Fragment() {

    private lateinit var binding: FragmentDriverCarSingleBinding
    private lateinit var applicationContext: Context

    private lateinit var carDatabase: DatabaseReference
    private lateinit var carId: String

    private val userId: String = FirebaseHelper.getUserId()

    private var resultUri: Uri? = null
    private lateinit var mCarImage: ImageView

    private lateinit var mBrandField: EditText
    private lateinit var mModelField: EditText
    private lateinit var mPlateField: EditText
    private lateinit var mYearField: EditText

    private lateinit var spOption: Spinner

    private lateinit var mSave: MaterialButton
    private lateinit var mDelete: MaterialButton

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?

    ): View? {
        binding = FragmentDriverCarSingleBinding.inflate(inflater, container, false)
        applicationContext = activity?.applicationContext!!

        mCarImage = binding.carImage
        mBrandField = binding.brand
        mModelField = binding.model
        mPlateField = binding.plate
        mYearField = binding.year

        spOption = binding.spOption

        mSave = binding.save
        mDelete = binding.delete

        carId = DriverCarSingleFragmentArgs.fromBundle(arguments).carId
        if (!carId.isBlank()) syncCarInfo()

        mCarImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        mSave.setOnClickListener { saveCarInfo() }
        mDelete.setOnClickListener { deleteCarInfo() }

        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(applicationContext, R.array.car_brands_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spOption.adapter = adapter;
        spOption.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>) {}

            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {
                Toast.makeText(applicationContext, parent.getItemAtPosition(pos).toString(), Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun syncCarInfo() {
        carDatabase = FirebaseHelper.getCarKey(carId)
        carDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0) {
                    val map = dataSnapshot.value as Map<String, Any?>

                    map[FirebaseHelper.CAR_BRAND]?.let { mBrandField.setText(it.toString()) }
                    map[FirebaseHelper.CAR_MODEL]?.let { mModelField.setText(it.toString()) }
                    map[FirebaseHelper.CAR_PLATE]?.let { mPlateField.setText(it.toString()) }
                    map[FirebaseHelper.CAR_YEAR]?.let { mYearField.setText(it.toString()) }
                    map[FirebaseHelper.CAR_IMG_URL]?.let { Glide.with(activity?.application!!).load(it.toString()).into(mCarImage) }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun saveCarInfo() {

        if (carId.isBlank()) {
            carId = FirebaseHelper.createCarForDriver(userId)
            syncCarInfo()
        }

        val mBrand = mBrandField.text.toString()
        val mModel = mModelField.text.toString()
        val mPlate = mPlateField.text.toString()
        val mYear = mYearField.text.toString()

        val userInfo: HashMap<String, *> = hashMapOf(
                FirebaseHelper.CAR_BRAND to mBrand,
                FirebaseHelper.CAR_MODEL to mModel,
                FirebaseHelper.CAR_PLATE to mPlate,
                FirebaseHelper.CAR_YEAR to mYear)
        carDatabase.updateChildren(userInfo)

        if (resultUri != null) {

            val filePath = FirebaseHelper.getCarImages(carId)
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
                downloadUrlTask.addOnSuccessListener(OnSuccessListener { downloadUrl ->
                    val newImage: HashMap<String, *> = hashMapOf(FirebaseHelper.CAR_IMG_URL to downloadUrl.toString())
                    carDatabase.updateChildren(newImage)
                    Toast.makeText(activity!!, getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show()
                    return@OnSuccessListener
                })
                return@OnSuccessListener
            })
        } else Toast.makeText(activity!!, getString(R.string.saved_successfully), Toast.LENGTH_SHORT).show()
    }

    private fun deleteCarInfo() {
        if (!carId.isBlank()) FirebaseHelper.deleteCar(carId, userId)
        activity!!.supportFragmentManager.popBackStack()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            resultUri = data!!.data
            mCarImage.setImageURI(resultUri)
        }
    }

}
