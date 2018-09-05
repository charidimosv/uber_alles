package com.team.eddie.uber_alles.ui.generic

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.ScrollView
import android.widget.TextView
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.team.eddie.uber_alles.R
import com.team.eddie.uber_alles.databinding.FragmentChatBinding
import com.team.eddie.uber_alles.utils.FirebaseHelper
import com.team.eddie.uber_alles.utils.SaveSharedPreference


class GenericChatFragment : androidx.fragment.app.Fragment(){
    var layout: LinearLayout? = null
    var layout_2:  RelativeLayout? = null
    var sendButton: ImageView? = null
    var messageArea: EditText? = null
    var scrollView: ScrollView? = null
    var reference1: DatabaseReference? = null
    var reference2: DatabaseReference? = null

    var addListener: ChildEventListener? = null

    var NEW_MESSAGE_PUSHED: String = "newMessagePushed"

    override fun onCreateView(
            inflater: LayoutInflater,
            container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        val binding = FragmentChatBinding.inflate(inflater, container, false)

        setHasOptionsMenu(true)

        layout = binding.layout1
        layout_2 = binding.layout2
        sendButton = binding.sendButton
        messageArea = binding.messageArea
        scrollView = binding.scrollView

        val senderName =  SaveSharedPreference.getChatSender(activity!!.applicationContext)
        val receiverName =  SaveSharedPreference.getChatReceiver(activity!!.applicationContext)
        reference1 = FirebaseHelper.getMessage().child(senderName+"_to_"+receiverName)
        reference2 = FirebaseHelper.getMessage().child(receiverName+"_to_"+senderName)

        sendButton?.setOnClickListener{
            val messageText = messageArea?.text.toString()
            if(messageText != ""){
                val map = hashMapOf("message" to messageText, "user" to senderName)
                reference1?.push()?.setValue(map)
                reference2?.child("newMessagePushed")?.setValue(true)
                reference2?.push()?.setValue(map)

            }
        }

        addListener = reference1?.addChildEventListener(object : ChildEventListener{
            override fun onCancelled(p0: DatabaseError) {}

            override fun onChildMoved(p0: DataSnapshot, p1: String?) {}

            override fun onChildChanged(p0: DataSnapshot, p1: String?) {}

            override fun onChildRemoved(p0: DataSnapshot) {}

            override fun onChildAdded(dataSnapshot: DataSnapshot, p1: String?) {
                if(dataSnapshot.exists()){
                    if(NEW_MESSAGE_PUSHED != dataSnapshot.key) {
                        val map = dataSnapshot.value as Map<*, *>
                        val message = map["message"].toString()
                        val userName = map["user"].toString()

                        if (userName == senderName)
                            addMessageBox("You:-\n" + message, 1)
                        else
                            addMessageBox(receiverName + ":-\n" + message, 2)
                    }
                    else
                        reference1!!.child("newMessagePushed").removeValue()
                }
            }

        })


        return binding.root
    }


    private fun addMessageBox(message: String, type: Int){
        val textView  = TextView(this.context)
        textView.text = message

        val lp2 = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        lp2.weight = 1.0f

        if(type == 1) {
            lp2.gravity = Gravity.RIGHT
            textView.setBackgroundResource(R.drawable.bubble_out)
        }
        else{
            lp2.gravity = Gravity.LEFT
            textView.setBackgroundResource(R.drawable.bubble_in)
        }
        textView.layoutParams = lp2
        layout?.addView(textView)
        scrollView?.fullScroll(View.FOCUS_DOWN)

        //TODO clean form
    }

    override fun onStop() {
        super.onStop()
        reference1?.removeEventListener(addListener!!)
    }
}