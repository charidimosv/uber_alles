package com.team.eddie.uber_alles.ui.session

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.Switch
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.gms.tasks.OnFailureListener
import com.google.android.gms.tasks.OnSuccessListener
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DatabaseReference
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentDriverCarSingleBinding
import com.team.eddie.uber_alles.ui.driver.DriverActivity
import com.team.eddie.uber_alles.utils.RetrofitClient
import com.team.eddie.uber_alles.utils.SessionServices
import com.team.eddie.uber_alles.utils.firebase.Car
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*

class RegisterDriverCarFragment : Fragment() {

    private lateinit var binding: FragmentDriverCarSingleBinding

    private lateinit var carDatabase: DatabaseReference
    private var carId: String? = null

    private val userId: String = FirebaseHelper.getUserId()

    private var resultUri: Uri? = null
    private lateinit var mCarImage: ImageView

    private lateinit var mBrandField: EditText
    private lateinit var mModelField: EditText
    private lateinit var mColorField: EditText
    private lateinit var mPlateField: EditText
    private lateinit var mYearField: EditText
    private lateinit var mDefault: Switch

    private lateinit var mSave: MaterialButton
    private lateinit var mDelete: MaterialButton

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentDriverCarSingleBinding.inflate(inflater, container, false)

        mCarImage = binding.carImage
        mBrandField = binding.brand
        mModelField = binding.model
        mColorField = binding.color
        mPlateField = binding.plate
        mYearField = binding.year
        mDefault = binding.defaultSwitch
        mDefault.isEnabled = false

        mSave = binding.save
        mDelete = binding.delete

        mCarImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 1)
        }

        mYearField.setOnClickListener() {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerListener = DatePickerDialog.OnDateSetListener { datePicker, i, j, k ->
                val day = datePicker.dayOfMonth
                val month = datePicker.month
                val year = datePicker.year

                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, day)
                var carDate = Editable.Factory.getInstance().newEditable(SimpleDateFormat("dd/MM/yyy").format(newCalendar.time))
                mYearField.text = carDate

            }
            val datePickerDialog = DatePickerDialog(activity!!, datePickerListener, year, month, day)
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        mSave.text = getString(R.string.complete)
        mSave.setOnClickListener { saveCarInfo() }
        mDelete.visibility = View.GONE

        return binding.root
    }

    private fun saveCarInfo() {

        val mBrand = mBrandField.text.toString()
        val mModel = mModelField.text.toString()
        val mColor = mColorField.text.toString()
        val mPlate = mPlateField.text.toString()
        val mYear = mYearField.text.toString()


        val currentCar = Car(carId, mBrand, mModel, mColor, mPlate, mYear, "true")

        val retrofit = RetrofitClient.getClient(activity?.applicationContext!!)
        val sessionServices = retrofit!!.create(SessionServices::class.java)

        val registerCall = sessionServices.saveCarInfo(userId, currentCar)
        registerCall.enqueue(object : Callback<Car> {
            override fun onFailure(call: Call<Car>, t: Throwable) {
                Toast.makeText(activity?.applicationContext!!, "Couldn't Save Info", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call<Car>, response: Response<Car>) {
                if (resultUri != null) {
                    carId = response.body()?.carId
                    carDatabase = FirebaseHelper.getCarKey(carId!!)

                    val filePath = FirebaseHelper.getCarImages(carId!!)
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
                            carDatabase.updateChildren(newImage)

                            startActivity(DriverActivity.getLaunchIntent(activity!!))
                        }
                        return@OnSuccessListener
                    })
                } else startActivity(DriverActivity.getLaunchIntent(activity!!))
            }

        })

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1 && resultCode == Activity.RESULT_OK) {
            resultUri = data!!.data
            mCarImage.setImageURI(resultUri)
        }
    }

}
