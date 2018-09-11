package com.team.eddie.uber_alles.ui.driver

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.team.eddie.uber_alles.adapters.CarAdapter
import com.team.eddie.uber_alles.databinding.FragmentDriverCarListBinding
import com.team.eddie.uber_alles.utils.firebase.FirebaseHelper
import com.team.eddie.uber_alles.view.CarItem
import java.util.*

class DriverCarListFragment : Fragment() {

    private lateinit var mAdapter: CarAdapter
    private lateinit var recyclerView: RecyclerView

    private lateinit var fab: FloatingActionButton

    private lateinit var userId: String
    private var resultsCarList = ArrayList<CarItem>()

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentDriverCarListBinding.inflate(inflater, container, false)
        context ?: return binding.root
        setHasOptionsMenu(true)

        mAdapter = CarAdapter()
        mAdapter.submitList(resultsCarList)

        recyclerView = binding.carRecyclerView
        recyclerView.adapter = mAdapter

        userId = FirebaseHelper.getUserId()
        getUserCarIds()

        fab = binding.fab
        fab.setOnClickListener {
            val direction = DriverCarListFragmentDirections.ActionDriverCarListFragmentToDriverCarSingleFragment("")
            it.findNavController().navigate(direction)
        }

        return binding.root
    }

    private fun getUserCarIds() {
        val userCarDatabase = FirebaseHelper.getUserCar(userId)
        userCarDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                resultsCarList.clear()
                if (dataSnapshot.exists())
                    for (car in dataSnapshot.children)
                        car.key?.let { fetchCarInformation(it) }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

    private fun fetchCarInformation(carKey: String) {
        val carDatabase = FirebaseHelper.getCarKey(carKey)
        carDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                if (dataSnapshot.exists()) {
                    val carId = dataSnapshot.key
                    var brand: String? = ""
                    var model: String? = ""
                    var plate: String? = ""
                    var year: String? = ""
                    var image: String? = ""

                    dataSnapshot.child(FirebaseHelper.CAR_BRAND).value?.let { brand = it.toString() }
                    dataSnapshot.child(FirebaseHelper.CAR_MODEL).value?.let { model = it.toString() }
                    dataSnapshot.child(FirebaseHelper.CAR_PLATE).value?.let { plate = it.toString() }
                    dataSnapshot.child(FirebaseHelper.CAR_YEAR).value?.let { year = it.toString() }
                    dataSnapshot.child(FirebaseHelper.CAR_IMG_URL).value?.let { image = it.toString() }

                    resultsCarList.add(CarItem(carId!!, brand!!, model!!, year!!, plate!!, image!!))
                    mAdapter.notifyDataSetChanged()
                }
            }

            override fun onCancelled(databaseError: DatabaseError) {}
        })
    }

}