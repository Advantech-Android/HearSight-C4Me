package com.codewithkael.firebasevideocall.ui
import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.ContactsContract.Contacts
import android.text.InputFilter
import android.text.method.DigitsKeyListener
import com.codewithkael.firebasevideocall.utils.SnackBarUtils
import android.util.Log

import android.view.Menu
import android.view.MenuItem

import android.util.TypedValue
import android.view.LayoutInflater

import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText


import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codewithkael.firebasevideocall.QRCode.WifiPasswordGenerated

import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.adapters.MainRecyclerViewAdapter
import com.codewithkael.firebasevideocall.databinding.ActivityMainBinding
import com.codewithkael.firebasevideocall.databinding.AddcontactsBinding
import com.codewithkael.firebasevideocall.repository.MainRepository
import com.codewithkael.firebasevideocall.service.MainService
import com.codewithkael.firebasevideocall.service.MainServiceActions
import com.codewithkael.firebasevideocall.service.MainServiceRepository
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.DataModelType
import com.codewithkael.firebasevideocall.utils.MainActivityFields
import com.codewithkael.firebasevideocall.utils.PickContactContract
import com.codewithkael.firebasevideocall.utils.ProgressBarUtil
import com.codewithkael.firebasevideocall.utils.RecyclerViewFields

import com.codewithkael.firebasevideocall.utils.WifiPassWordGeneratedField
import com.codewithkael.firebasevideocall.utils.getCameraAndMicPermission
import com.codewithkael.firebasevideocall.utils.setViewFields
import com.google.android.material.textfield.TextInputEditText
import dagger.hilt.android.AndroidEntryPoint
import java.nio.channels.AlreadyBoundException
import javax.inject.Inject
import kotlin.math.log



private const val STORAGE_PERMISSION_CODE = 123
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainRecyclerViewAdapter.Listener, MainService.Listener
{
    private lateinit var contactBind: AddcontactsBinding

    private lateinit var mDialog: Dialog
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
    private var callerName:String?=""
    @Inject
    lateinit var context: Context

    @Inject
    lateinit var mp3Player: Mp3Ring
    lateinit var wifiManager: WifiManager

    private val PICK_CONTACT_REQUEST = 123 // Request code for picking contacts
    private var selectedContactName:String?=null
    private var selectedContactNumber:String?=null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views.root)

        wifiManager=getSystemService(Context.WIFI_SERVICE) as WifiManager

        searchQuery()
        init()



    }
    private fun showLogoutDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_logout, null)

        val logoutOkBtn = dialogView.findViewById<Button>(R.id.logoutOkBtn)
        val logoutCancelBtn = dialogView.findViewById<Button>(R.id.logoutCancleBtn)

        builder.setView(dialogView)
            .setPositiveButton(null, null) // Setting null for positive button for custom handling
            .setNegativeButton(null, null) // Setting null for negative button for custom handling

        val dialog = builder.create()
        dialog.show()

        logoutOkBtn.setOnClickListener {
            // Perform logout action
            // For example, navigate to login screen or clear session
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            dialog.dismiss()
        }

        logoutCancelBtn.setOnClickListener {
            dialog.dismiss()
        }

    }





    private fun init() {
        username = intent.getStringExtra(setViewFields.USER_NAME)
        if (username == null) finish()
        views.addContact.setOnClickListener {

            addContacts()
        }
        //1. observe other users status
        subscribeObservers()
        //2. start foreground service to listen negotiations and calls.
        startMyService()
    }

    private fun searchQuery(){
        views.searchView.queryHint="Search Contacts"

        // Find the EditText inside the SearchView
        val searchEditText = views.searchView.findViewById<EditText>(
            androidx.appcompat.R.id.search_src_text
        )

        // Set the text size of the query hint
        searchEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)


        views.searchView.setOnQueryTextListener(object :SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean// if the text is available in search query box
            {
                newText?.let {
                    mainAdapter?.filterList(newText)
                }
                return true
            }

        })
    }
    private val pickContactLauncher = registerForActivityResult(PickContactContract(this@MainActivity)) { result ->
        result?.let { (name, phoneNumber) ->
            Log.d(TAG, "name:$name = phoneNumber:$phoneNumber")

            var ph=phoneNumber
            if(!phoneNumber.startsWith("+91")){
                ph="+91${phoneNumber}"
            }
            contactBind.addNameid.setText(name.trim().lowercase())
            contactBind.addPhoneNumberid.setText(ph.trim().replace(" ",""))
        }
    }
    private fun addContacts()
    {
        mDialog= Dialog(this)
        contactBind=AddcontactsBinding.inflate(layoutInflater)
        mDialog.setContentView(contactBind.root)
        mDialog.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT,WindowManager.LayoutParams.WRAP_CONTENT)
        mDialog.show()
        contactBind.pickBtn.setOnClickListener {

            /* val intent = Intent(Intent.ACTION_PICK)
             intent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
             startActivityForResult(intent, PICK_CONTACT_REQUEST)*/
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE
            pickContactLauncher.launch(intent)

        }

        contactBind.cancelBtn.setOnClickListener {
            mDialog.dismiss()
        }

        contactBind.saveBtn.setOnClickListener {
            mainRepository.addContacts(
                contactBind.addNameid.text.toString().trim(),
                contactBind.addPhoneNumberid.text.toString().trim().replace(" ",""))
            {isDone,reason->

                if(!isDone){

                    SnackBarUtils.showSnackBar(contactBind.root,"$reason")
                    mDialog.dismiss()
                }
                else{

                    SnackBarUtils.showSnackBar(contactBind.root,MainActivityFields.CONTACT_ADD_SUCCESS)
                    mDialog.dismiss()

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

        },{ userList1->

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

    override fun onVideoCallClicked(username: String,user:ContactInfo) {
        //check if permission of mic and camera is taken
        Log.d(TAG, "onVideoCallClicked: =====================$username")
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, true) {
                if (it) {
                    //we have to start video call
                    //we wanna create an intent to move to call activity
                    //here username is phone number
                    //  mainRepository.getUserNameFB(username){ user_name->
                    startActivity(Intent(this, CallActivity::class.java).apply {
                        putExtra(setViewFields.TARGET, username)
                        putExtra(setViewFields.IS_VIDEO_CALL, true)
                        putExtra(setViewFields.IS_CALLER, true)
                        putExtra(setViewFields.IS_INCOMING, "Out")
                        putExtra(setViewFields.TIMER, false)
                        putExtra(setViewFields.CALLER_NAME, user.userName)
                    })
                    //  }

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
                        putExtra(setViewFields.TARGET, username)
                        putExtra(setViewFields.IS_VIDEO_CALL, false)
                        putExtra(setViewFields.IS_CALLER, true)

                    })
                }
            }
        }
    }
    override fun onPause()
    {
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

    @SuppressLint("SuspiciousIndentation")
    override fun onCallReceived(model: DataModel) {
        mp3Player.startMP3(isIncoming = true)
        runOnUiThread {
            views.apply {
                Log.d(TAG, "onCallReceived: sender:${model.sender}, target: ${model.target}")
                var username=""
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                contactLayout.isVisible=false
                incomingCallLayout.isVisible = true
                mainRepository.getUserNameFB(model.sender!!) { user_name ->
                    Log.d(TAG, "onCallReceived: $user_name")
                    username=user_name+""
                }
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {

                        mp3Player.stopMP3()
                        mainRepository.setCallStatus(model.target,model.sender!!,"AcceptCall"){
                            mp3Player.stopMP3()
                            incomingCallLayout.isVisible = false
                            contactLayout.isVisible=true


                            //create an intent to go to video call activity
                            startActivity(Intent(this@MainActivity,CallActivity::class.java).apply {
                                putExtra(setViewFields.TARGET,model.sender)
                                putExtra(setViewFields.IS_VIDEO_CALL,isVideoCall)
                                putExtra(setViewFields.IS_CALLER,false)
                                putExtra(setViewFields.TIMER, true)
                                putExtra(setViewFields.CALLER_NAME, username)
                            })
                        }
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                    contactLayout.isVisible=true
                    mp3Player.stopMP3()
                    decline(model.target,model.sender,"EndCall")
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    mp3Player.stopMP3()
                    incomingCallLayout.isVisible = false
                    contactLayout.isVisible=true

                }, 20000)
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


    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.qr_codeid -> {
                Log.d(TAG, "onOptionsItemSelected: ===>>>")
                isPermissionGrand()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    555
                )
                isCheckHotspot()

                if (wifiManager.isWifiEnabled )
                {
                    val wifiReceiver= WifiPasswordGenerated(this)
                    wifiReceiver.showQRDialog()
                }else
                {
                    SnackBarUtils.showSnackBar(views.root,MainActivityFields.TURN_ON_WIFI)
                }

                true
            }
            R.id.logOut->{
             showLogoutDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }


    }


    private fun isCheckHotspot(): Boolean {
        try {
            val npm = Class.forName("android.net.NetworkPolicyManager").getDeclaredMethod("from", Context::class.java).invoke(null, this)
            val policies = npm.javaClass.getDeclaredMethod("getNetworkPolicies").invoke(npm)

            if (policies != null) {
                val policyArray = policies as Array<Any>
                for (policy in policyArray) {
                    val isHotspotEnabled =
                        policy.javaClass.getDeclaredMethod("isMetered", *arrayOfNulls(0))
                            .invoke(policy) as Boolean
                    if (isHotspotEnabled) {
                        return true
                    } else {
                        return false
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }

    private fun isPermissionGrand() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_COARSE_LOCATION)== PackageManager.PERMISSION_GRANTED){
            // Permissions granted, do nothing
        } else {
            val permissions = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_WIFI_STATE,Manifest.permission.ACCESS_COARSE_LOCATION)
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
        }
    }


}