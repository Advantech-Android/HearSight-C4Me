package com.codewithkael.firebasevideocall.ui
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager

import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView

import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.adapters.MainRecyclerViewAdapter
import com.codewithkael.firebasevideocall.databinding.ActivityMainBinding
import com.codewithkael.firebasevideocall.repository.MainRepository
import com.codewithkael.firebasevideocall.service.MainService
import com.codewithkael.firebasevideocall.service.MainServiceActions
import com.codewithkael.firebasevideocall.service.MainServiceRepository
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.DataModelType
import com.codewithkael.firebasevideocall.utils.ProgressBarUtil
import com.codewithkael.firebasevideocall.utils.getCameraAndMicPermission
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.log


@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainRecyclerViewAdapter.Listener, MainService.Listener {
    private val TAG = "***>>MainActivity"

    private lateinit var views: ActivityMainBinding
    private var username: String? = null

    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var mainServiceRepository: MainServiceRepository
    @Inject
    lateinit var serviceRepository: MainServiceRepository
    private var mainAdapter: MainRecyclerViewAdapter? = null

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var mp3Player: Mp3Ring

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)
        init()
    }

    private fun init() {
        username = intent.getStringExtra("username")
        if (username == null) finish()
        views.addContact.setOnClickListener {
            addContacts()
        }
        //1. observe other users status
        subscribeObservers()
        //2. start foreground service to listen negotiations and calls.
        startMyService()
    }

    private fun addContacts()
    {
        val mDialog= Dialog(this)
        mDialog.setContentView(R.layout.addcontacts)
        mDialog.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog.show()
        val addName=mDialog.findViewById<TextInputEditText>(R.id.addNameid)
        val addPhoneNumber=mDialog.findViewById<TextInputEditText>(R.id.addPhoneNumberid)
        val registerBtn=mDialog.findViewById<CardView>(R.id.registerbutton)
        registerBtn.setOnClickListener {
            if (addName==null||addPhoneNumber==null)
            {
                Toast.makeText(this, "Field empty", Toast.LENGTH_SHORT).show()
            }
            else {
                mainRepository.addContacts(addName.text.toString(),addPhoneNumber.text.toString()){ isDone, reason ->
                    if (!isDone){
                        Toast.makeText(this, reason, Toast.LENGTH_SHORT).show()
                        mDialog.dismiss()
                    }else{
                        Toast.makeText(this,"Success" , Toast.LENGTH_SHORT).show()
                        mDialog.dismiss()
                    }
                }
            }
        }
    }


    private fun subscribeObservers(){
        setupRecyclerView()
        MainService.listener = this
        var outerList:List<ContactInfo> ?=null
        mainRepository.observeUsersStatus({ userList->
                                          outerList=userList

//            if (userList.isEmpty())
//            {
//                views.noContactTV.visibility=View.VISIBLE
//            }
//            else{
//                views.noContactTV.visibility=View.GONE
//                mainAdapter!!.updateList(userList,innerList?: emptyList())
//            }
        },{ userList1->
            Toast.makeText(this, "${userList1}", Toast.LENGTH_SHORT).show()
            if (userList1.isNullOrEmpty()){
                views.noContactTV.visibility=View.VISIBLE
                mainAdapter!!.updateList(outerList ?: emptyList(),emptyList())
            }else{
                views.noContactTV.visibility=View.GONE
                mainAdapter!!.updateList(outerList ?: emptyList(),userList1)
            }
        })

        mainRepository.onObserveEndCall() { data,callStatus ->
            Log.d(TAG, "subscribeObservers: target: ${data.target} ,sender:${data.sender} , message:${"empty"} callstatus:$callStatus")
            if (callStatus == "EndCall"||callStatus=="AcceptCall")
            {        mp3Player.stopMP3()
                decline(target = data.target, sender = data.sender, message = "")
            }
        }
    }


    private fun setupRecyclerView() {
        mainAdapter = MainRecyclerViewAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        views.mainRecyclerView.apply {
            setLayoutManager(layoutManager)
            adapter = mainAdapter
        }
    }

    private fun startMyService() {

        mainServiceRepository.startService(username!!, MainServiceActions.START_SERVICE.name)

    }

    override fun onVideoCallClicked(username: String) {
        //check if permission of mic and camera is taken
        Log.d(TAG, "onVideoCallClicked: =====================$username")
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, true) {
                if (it) {
                    //we have to start video call
                    //we wanna create an intent to move to call activity
                    startActivity(Intent(this, CallActivity::class.java).apply {
                        putExtra("target", username)
                        putExtra("isVideoCall", true)
                        putExtra("isCaller", true)
                        putExtra("isIncoming", "Out")
                        putExtra("timer", false)
                    })


                }
            }

        }
    }
    override fun onAudioCallClicked(username: String) {
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, false) {
                if (it) {
                    //we have to start audio call
                    //we wanna create an intent to move to call activity
                    startActivity(Intent(this, CallActivity::class.java).apply {
                        putExtra("target", username)
                        putExtra("isVideoCall", false)
                        putExtra("isCaller", true)


                    })
                }
            }
        }
    }
    override fun onPause() {
        super.onPause()

    }

    override fun onDestroy() {
        super.onDestroy()
        mp3Player.stopMP3()
    }


    override fun onBackPressed() {
        super.onBackPressed()
        mainServiceRepository.stopService()
    }

    override fun onCallReceived(model: DataModel) {
        mp3Player.startMP3(isIncoming = true)
        runOnUiThread {
            views.apply {
                Log.d(TAG, "onCallReceived: sender:${model.sender}, target: ${model.target}")

                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                contactLayout.isVisible=false
                incomingCallLayout.isVisible = true
                acceptButton.setOnClickListener {
                    mp3Player.stopMP3()
                    getCameraAndMicPermission {
                        incomingCallLayout.isVisible = false
                        contactLayout.isVisible=true
                        mp3Player.stopMP3()
                        //create an intent to go to video call activity
                        startActivity(Intent(this@MainActivity,CallActivity::class.java).apply {
                            putExtra("target",model.sender)
                            putExtra("isVideoCall",isVideoCall)
                            putExtra("isCaller",false)
                            putExtra("timer", true)
                        })
                    }

                    mainRepository.setCallStatus(model.target!!,model.sender!!,"AcceptCall"){
                        mp3Player.stopMP3()
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                    contactLayout.isVisible=true
                    mp3Player.stopMP3()

//                    Toast.makeText(this@MainActivity, "You declined the call", Toast.LENGTH_LONG).show()
                    decline(model.target,model.sender,"EndCall")

                }
            }
        }
    }

    fun decline(target: String?,sender:String?,message:String?) {
        Log.d(TAG, "decline: target = $target, sender = $sender, message = $message")
        mp3Player.stopMP3()
        runOnUiThread {
          //  mainRepository.setTarget(target!!)
           // serviceRepository.sendEndCall()

            views.apply {
                //Toast.makeText(this@MainActivity, "$sender is cut the call", Toast.LENGTH_LONG).show()

                incomingCallLayout.isVisible = false
                contactLayout.isVisible = true
            }
            if (message!="")
            {

                mainRepository.setCallStatus(target= target!!,sender=sender!!,message!!){}
            }
            else
            {
                Log.d(TAG, "decline: Else part $message")
                mainRepository.setCallStatus(target= sender!!,sender=target!!,message+""){}
            }
        }
    }

}


