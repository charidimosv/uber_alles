package com.team.eddie.uber_alles.ui.generic

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.adapters.RequestAdapter
import com.team.eddie.uber_alles.databinding.FragmentGenericRequestListBinding
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.utils.firebase.Request
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*

class GenericRequestListFragment : Fragment() {

    private lateinit var binding: FragmentGenericRequestListBinding

    private lateinit var mAdapter: RequestAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var userId: String
    private var resultsRequestList = ArrayList<Request>()
    private var resultsRequestIdList = ArrayList<String>()

    private lateinit var mTotalTripsValue: TextView
    private lateinit var mTotalCashValue: TextView
    private lateinit var mTotalDistanceValue: TextView
    private lateinit var mSelectedDate: MaterialButton
    private lateinit var mDateOption: Spinner

    private var totalTripsValue: Int = 0
    private var totalCashValue: Double = 0.0
    private var totalDistanceValue: Double = 0.0

    private var formater: DateFormat = SimpleDateFormat("dd/MM/yyy", Locale("el"))

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        binding = FragmentGenericRequestListBinding.inflate(inflater, container, false)
        context ?: return binding.root
        setHasOptionsMenu(true)

        mAdapter = RequestAdapter()
        mAdapter.submitList(resultsRequestList)

        mTotalTripsValue = binding.totalTripsValue
        mTotalCashValue = binding.totalCashValue
        mTotalDistanceValue = binding.totalDistanceValue
        mSelectedDate = binding.selectedDate
        mDateOption = binding.dateOptions

        recyclerView = binding.requestRecyclerView
        recyclerView.adapter = mAdapter

        initValues()

        val adapter: ArrayAdapter<CharSequence> = ArrayAdapter.createFromResource(activity!!.applicationContext, R.array.date_options_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        mDateOption.adapter = adapter

        userId = FirebaseHelper.getUserId()

        //Init spinner to All option
        mDateOption.setSelection(0, false)
        getUserHistoryIds()

        mDateOption.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>) {}

            override fun onItemSelected(parent: AdapterView<*>, view: View, pos: Int, id: Long) {

                val option = parent.getItemAtPosition(pos).toString()
                mSelectedDate.isEnabled = false
                initValues()
                when (option) {
                    "All" -> { getUserHistoryIds() }
                    "Today" -> {
                        val currentDate = formater.format(Calendar.getInstance().time)
                        val currentDateFormated = formater.parse(currentDate)
                        getUserHistoryIdsByDate(currentDateFormated,currentDateFormated)
                    }
                    "Specific Date" -> {
                        mSelectedDate.isEnabled = true
                    }
                    "Week" -> {
                        val cal = Calendar.getInstance()
                        val currentDate = formater.format(cal.time)
                        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
                        val firstDayOfWeek = formater.format(cal.time)
                        getUserHistoryIdsByDate(formater.parse(firstDayOfWeek),formater.parse(currentDate))
                    }
                    "Month" -> {
                        val cal = Calendar.getInstance()
                        val actualLast = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                        cal.set(Calendar.DAY_OF_MONTH, 1)
                        val firstDayOfMonth = formater.format(cal.time)
                        cal.set(Calendar.DAY_OF_MONTH, actualLast)
                        val lastDayOfMonth = formater.format(cal.time)
                        getUserHistoryIdsByDate(formater.parse(firstDayOfMonth),formater.parse(lastDayOfMonth))
                    }
                    else -> { // Note the block
                        print("x is neither 1 nor 2")
                    }
                }
            }
        }

        val datePickerListener = DatePickerDialog.OnDateSetListener { view: DatePicker?, year: Int, month: Int, dayOfMonth: Int ->
            initValues()

            val newCalendar = Calendar.getInstance()
            newCalendar.set(year, month, dayOfMonth)
            val selectedDateString = formater.format(newCalendar.time)
            mSelectedDate.text = selectedDateString

            getUserHistoryIdsByDate(formater.parse(selectedDateString), formater.parse(selectedDateString))
        }
        mSelectedDate.setOnClickListener() {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(activity!!, datePickerListener, year, month, day)
            datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        return binding.root
    }

    private fun getUserHistoryIds() {
        val userHistoryDatabase = FirebaseHelper.getUserRequestList(userId)
        userHistoryDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0)
                    for (request in dataSnapshot.children) fetchRideInformation(request.key)
                else {
                    binding.layout.visibility = View.GONE
                    binding.noRequest.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun getUserHistoryIdsByDate(requestFromDate: Date, requestToDate: Date) {
        val userHistoryDatabase = FirebaseHelper.getUserRequestList(userId)
        userHistoryDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists() && dataSnapshot.childrenCount > 0)
                    for (request in dataSnapshot.children) fetchRideInformationByDate(request.key, requestFromDate, requestToDate)
                else {
                    binding.layout.visibility = View.GONE
                    binding.noRequest.visibility = View.VISIBLE
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun fetchRideInformation(requestId: String?) {
        val requestDatabase = FirebaseHelper.getRequestKey(requestId!!)
        requestDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val request = dataSnapshot.getValue(Request::class.java)
                    if (request != null && !resultsRequestIdList.contains(request.requestId)) {
                        loadData(request)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun fetchRideInformationByDate(requestId: String?, requestFromDate: Date, requestToDate: Date) {
        val requestDatabase = FirebaseHelper.getRequestKey(requestId!!)
        requestDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val request = dataSnapshot.getValue(Request::class.java)
                    val requestDate = formater.parse(request?.requestDate)
                    if (request != null && !resultsRequestIdList.contains(request.requestId)
                           && (requestFromDate.before(requestDate) ||  requestFromDate.equals(requestDate))
                            && (requestToDate.after(requestDate) ||  requestToDate.equals(requestDate))) {
                        loadData(request)
                    }
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun initValues(){
        totalTripsValue = 0
        mTotalTripsValue.text = "0"
        mTotalCashValue.text = "0 €"
        mTotalDistanceValue.text = "0 km"

        mSelectedDate.text = ""

        resultsRequestIdList.clear()
        resultsRequestList.clear()
        mAdapter.notifyDataSetChanged()

    }

    private fun loadData(request: Request){
        totalTripsValue++
        totalCashValue += request.amount
        totalDistanceValue += request.distance

        mTotalTripsValue.text = totalTripsValue.toString()
        mTotalCashValue.text = totalCashValue.toString() + "€"
        mTotalDistanceValue.text = totalDistanceValue.toString() + " km"

        resultsRequestIdList.add(request.requestId)
        resultsRequestList.add(request)
        mAdapter.notifyDataSetChanged()
    }


}