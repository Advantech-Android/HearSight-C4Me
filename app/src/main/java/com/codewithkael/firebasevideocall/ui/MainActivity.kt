package com.codewithkael.firebasevideocall.ui

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.ContactsContract
import android.provider.Settings
import com.codewithkael.firebasevideocall.utils.SnackBarUtils
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.util.TypedValue
import android.widget.Button

import android.widget.EditText
import android.widget.Toast
import androidx.activity.OnBackPressedCallback

import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ContextThemeWrapper
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.appcompat.widget.SearchView

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

import com.codewithkael.firebasevideocall.utils.getCameraAndMicPermission
import com.codewithkael.firebasevideocall.utils.LoginActivityFields
import com.codewithkael.firebasevideocall.utils.MainActivityFields

import com.codewithkael.firebasevideocall.utils.PickContactContract
import com.codewithkael.firebasevideocall.utils.setViewFields
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import com.codewithkael.firebasevideocall.ui.LoginActivity.Share.liveShare
import com.codewithkael.firebasevideocall.utils.UserStatus
import kotlinx.coroutines.launch

import javax.inject.Inject


private const val STORAGE_PERMISSION_CODE = 123

@AndroidEntryPoint
class MainActivity : AppCompatActivity(), MainRecyclerViewAdapter.Listener, MainService.Listener {

    private lateinit var recentContactName: String
    private var recentContactPhone: String = ""
    private lateinit var contactBind: AddcontactsBinding

    private val MAX_LENGTH_PHONE=5
    private lateinit var mDialog: Dialog
    private val TAG = "***>>MainActivity"

    private var views: ActivityMainBinding? = null
    private var username: String? = null
    private var userphone: String? = null

    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var mainServiceRepository: MainServiceRepository

    @Inject
    lateinit var serviceRepository: MainServiceRepository
    private var mainAdapter: MainRecyclerViewAdapter? = null

    lateinit var sharedPref: SharedPreferences
    lateinit var shEdit: SharedPreferences.Editor

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var mp3Player: Mp3Ring
    lateinit var wifiManager: WifiManager

    companion object Share {
        var liveShare = MutableLiveData<SharedPreferences>()//is used to provide a reactive and centralized way to manage and observe changes to the SharedPreferences instance across the app.

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityMainBinding.inflate(layoutInflater)
        setContentView(views?.root)


        sharedPref = this.getSharedPreferences("see_for_me", MODE_PRIVATE)
        shEdit = sharedPref.edit()

        // Fetch data from shared preferences
        username = sharedPref.getString("user_name", null)
        userphone = sharedPref.getString("user_phone", null)



        Log.d(TAG, "onCreate==??:$username and $userphone")


      //Setting "is_login" to false

        wifiManager = getSystemService(Context.WIFI_SERVICE) as WifiManager

        onBackPressedDispatcher.addCallback(back)

        lifecycleScope.launch { init() }

        searchQuery()
    }
    private suspend fun init() {

        //Extras from Main Activity
        username = intent.getStringExtra(setViewFields.USER_NAME)
        userphone = intent.getStringExtra(setViewFields.USER_PHONE)

        Log.d(TAG, "init==??:$username and $userphone ")

        if(username.isNullOrEmpty()||userphone.isNullOrEmpty())
        {

            username = sharedPref.getString("user_name", null)
            userphone = sharedPref.getString("user_phone", null)
        }
        if (username == null) finish()

        views?.addContact?.setOnClickListener {
            addContacts()
        }
        //1. observe other users status
        subscribeObservers()
        //2. start foreground service to listen negotiations and calls.
        startMyService()
    }
    val back=object: OnBackPressedCallback(true) {
        /* override back pressing */
        override fun handleOnBackPressed() {
            //Your code here
            showCloseAppDialog()
        }
    }
    fun showCloseAppDialog(){
        // Inflate the dialog layout
        val dialogView = layoutInflater.inflate(R.layout.closeapp_dialog, null)

        // Initialize the AlertDialog Builder
        val builder = AlertDialog.Builder(this)
        builder.setView(dialogView)

        val dialog = builder.create()


        val closeAppOkbtn=dialogView.findViewById<Button>(R.id.closeAppOkbtn)
        val closeAppbtn=dialogView.findViewById<Button>(R.id.closeAppbtn)//
        builder.setView(dialogView)
            .setPositiveButton(null, null) // Setting null for positive button for custom handling
            .setNegativeButton(null, null) // Setting null for negative button for custom handling

        closeAppOkbtn.setOnClickListener {

            mainServiceRepository.stopService()
            finishAffinity()
            dialog.dismiss()
        }
        closeAppbtn.setOnClickListener {
            dialog.dismiss()
        }
        dialog.show()

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
            liveShare.value?.edit()?.putBoolean("is_login",false)
            mainRepository.login(username!!,userphone!!,UserStatus.OFFLINE.name,false){ischeck ,status->

            }
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




    private fun searchQuery() {
        views?.searchView?.queryHint = "Search Contacts"

        // Find the EditText inside the SearchView
        val searchEditText = views?.searchView?.findViewById<EditText>(
            androidx.appcompat.R.id.search_src_text
        )

        // Set the text size of the query hint
        searchEditText?.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15f)


        views?.searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
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

            var ph = phoneNumber
            if (!phoneNumber.startsWith("+91")) {
                ph = "+91${phoneNumber}"
            }
            contactBind.addNameid.setText(name.trim().lowercase())
            contactBind.addPhoneNumberid.setText(ph.trim().replace(" ", ""))
        }
    }

    private fun addContacts() {
        mDialog = Dialog(this)
        contactBind = AddcontactsBinding.inflate(layoutInflater)
        mDialog.setContentView(contactBind.root)
        mDialog.window!!.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT
        )
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
            val name = contactBind.addNameid.text.toString().trim().lowercase()
            var phoneNumber = contactBind.addPhoneNumberid.text.toString().trim().replace(" ", "")
            if (!phoneNumber.startsWith("+91")) {
                phoneNumber = "+91$phoneNumber"
            }
            if (name.isEmpty()) {
                SnackBarUtils.showSnackBar(views!!.root, LoginActivityFields.USERNAME_INVALID)
                return@setOnClickListener
            }
            if (phoneNumber.length < MAX_LENGTH_PHONE || phoneNumber.isEmpty()) {
                SnackBarUtils.showSnackBar(views!!.root, LoginActivityFields.PASWORD_INVALID)
                return@setOnClickListener
            }
            recentContactPhone = phoneNumber
            recentContactName = name
            contactBind.addNameid.setText(name)
            contactBind.addPhoneNumberid.setText(phoneNumber)
            mainRepository.addContacts(name, phoneNumber) { isDone, reason ->
                if (!isDone) {
                    SnackBarUtils.showSnackBar(
                        contactBind.root,
                        LoginActivityFields.UN_PW_INCORRECT
                    )
                    mDialog.dismiss()
                } else {
                    SnackBarUtils.showSnackBar(
                        contactBind.root,
                        LoginActivityFields.UN_PW_INCORRECT
                    )

                    Toast.makeText(
                        this@MainActivity,
                        "Contact added Successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                    //SnackBarUtils.showSnackBar(contactBind.root, MainActivityFields.CONTACT_ADD_SUCCESS)
                    mDialog.dismiss()
                }

            }
        }
    }

    private suspend fun subscribeObservers() {
        setupRecyclerView()
        MainService.listener = this
        var allContact: List<ContactInfo>? = null
        var registerCommonList: List<ContactInfo>? = null
        var unregisterList: List<ContactInfo>? = null
        mainRepository.observeUsersStatus(this,
            { registerContact ->
                Log.d(TAG, "All_Contacts: ${registerContact.filter {
                    it.userName
                    false
                }}")
                allContact = registerContact
            },
            { onlineContactList ->
                if (onlineContactList.isEmpty()) {
                    Log.d(TAG, "Available Contact: ${onlineContactList.filter {
                        it.userName
                        false
                    }}")
                    views?.noContactTV?.visibility = View.VISIBLE
                    //mainAdapter!!.updateList(allContact ?: emptyList(), emptyList())
                } else {
                    views?.noContactTV?.visibility = View.GONE
                    // mainAdapter!!.updateList(allContact ?: emptyList(), onlineContactList!!)
                    registerCommonList = onlineContactList
                }
            }
        ) { newContact ->
            Log.d(TAG, "subscribeObservers: ${newContact.size}")


            if (newContact.isEmpty()) {
                views?.noContactTV?.visibility = View.VISIBLE
                //mainAdapter!!.updateList(allContact ?: emptyList(), emptyList())
            } else {
                views?.noContactTV?.visibility = View.GONE
                mainAdapter!!.updateList(allContact ?: emptyList(), newContact!!)
            }
//            newContact.filter {
//                it.contactNumber == recentContactPhone
//            }.map {
//                if (!recentContactPhone.isNullOrEmpty())
//                    Log.d(TAG, "--->unregisterContact_two:${it.userName} ")
//                 SnackBarUtils.showSnackBar(views!!.root, "No See for me account to ${it.userName}")
//                recentContactPhone=""
//            }
//            mainRepository.removeContactListener()
        }

        mainRepository.onObserveEndCall() { data, callStatus ->
            Log.d(
                TAG,
                "subscribeObservers: target: ${data.target} ,sender:${data.sender} , message:${"empty"} callstatus:$callStatus"
            )
            if (callStatus == "EndCall" || callStatus == "AcceptCall") {
                mp3Player.stopMP3()
                decline(target = data.target, sender = data.sender, message = "")
            }
        }
    }


    private fun setupRecyclerView() {
        mainAdapter = MainRecyclerViewAdapter(this)
        val layoutManager = LinearLayoutManager(this)
        views!!.mainRecyclerView.apply {
            setLayoutManager(layoutManager)
            adapter = mainAdapter
        }
    }

    private fun startMyService() {
        if (username.isNullOrEmpty())
            username=liveShare.value?.getString("user_name","")

        mainServiceRepository.startService(username!!, MainServiceActions.START_SERVICE.name)
    }

    override fun onVideoCallClicked(username: String, user: ContactInfo) {
        //check if permission of mic and camera is taken
        Log.d(TAG, "onVideoCallClicked: =====================$username")
        getCameraAndMicPermission {
            mainRepository.sendConnectionRequest(username, true) {
                if (it) {
                    //we have to start video call
                    //we wanna create an intent to move to call activity
                    //here username is phone number

                    startActivity(Intent(this, CallActivity::class.java).apply {
                        putExtra(setViewFields.TARGET, username)
                        putExtra(setViewFields.IS_VIDEO_CALL, true)
                        putExtra(setViewFields.IS_CALLER, true)
                        putExtra(setViewFields.IS_INCOMING, "Out")
                        putExtra(setViewFields.TIMER, false)
                        putExtra(setViewFields.CALLER_NAME, user.userName)
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
                        putExtra(setViewFields.TARGET, username)
                        putExtra(setViewFields.IS_VIDEO_CALL, false)
                        putExtra(setViewFields.IS_CALLER, true)

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
        wifiManager.disconnect()
        views = null
    }


//    override fun onBackPressed() {
//        super.onBackPressed()
//        mainServiceRepository.stopService()
//    }


    override fun onCallReceived(model: DataModel) {
        mp3Player.startMP3(isIncoming = true)
        runOnUiThread {
            views?.apply {
                Log.d(TAG, "onCallReceived: sender:${model.sender}, target: ${model.target}")
                var username = ""
                val isVideoCall = model.type == DataModelType.StartVideoCall
                val isVideoCallText = if (isVideoCall) "Video" else "Audio"
                incomingCallTitleTv.text = "${model.sender} is $isVideoCallText Calling you"
                contactLayout.isVisible = false
                incomingCallLayout.isVisible = true
                mainRepository.getUserNameFB(model.sender!!) { user_name ->
                    Log.d(TAG, "onCallReceived: $user_name")
                    username = user_name + ""
                }
                acceptButton.setOnClickListener {
                    getCameraAndMicPermission {

                        mp3Player.stopMP3()
                        mainRepository.setCallStatus(model.target, model.sender!!, "AcceptCall") {
                            mp3Player.stopMP3()

                            incomingCallLayout.isVisible = false
                            contactLayout.isVisible = true


                            //create an intent to go to video call activity
                            startActivity(
                                Intent(
                                    this@MainActivity,
                                    CallActivity::class.java
                                ).apply {
                                    putExtra(setViewFields.TARGET, model.sender)
                                    putExtra(setViewFields.IS_VIDEO_CALL, isVideoCall)
                                    putExtra(setViewFields.IS_CALLER, false)
                                    putExtra(setViewFields.TIMER, true)
                                    putExtra(setViewFields.CALLER_NAME, username)
                                })
                        }
                    }
                }
                declineButton.setOnClickListener {
                    incomingCallLayout.isVisible = false
                    contactLayout.isVisible = true
                    mp3Player.stopMP3()
                    decline(model.target, model.sender, "EndCall")
                }
                Handler(Looper.getMainLooper()).postDelayed({
                    mp3Player.stopMP3()
                    incomingCallLayout.isVisible = false
                    contactLayout.isVisible = true

                }, 20000)
            }
        }
    }

    fun decline(target: String?, sender: String?, message: String?) {
        Log.d(TAG, "decline: target = $target, sender = $sender, message = $message")

        mp3Player.stopMP3()
        runOnUiThread {
            views?.apply {
                incomingCallLayout.isVisible = false
                contactLayout.isVisible = true
            }
            if (message != "") {
                mainRepository.setCallStatus(target = target!!, sender = sender!!, message!!) {}
            } else {
                Log.d(TAG, "decline: Else part $message")
                mainRepository.setCallStatus(target = sender!!, sender = target!!, message + "") {}
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
                showEnableDeveloperModeDialog()
                true
            }

            R.id.logOut -> {
                showLogoutDialog()
                true
            }

            else -> super.onOptionsItemSelected(item)
        }


    }

    private fun showEnableDeveloperModeDialog(){
        AlertDialog.Builder(ContextThemeWrapper(this, androidx.appcompat.R.style.Theme_AppCompat))
            .setTitle("Enable Developer Mode/Wi-Fi Throttling")
            .setMessage("To access available network settings, please follow these steps:\n\n1. Go to Settings.\n2. Scroll down and tap on 'About phone'.\n3. Find 'Build number' and tap it seven times to enable Developer Mode.\n4. Go back to the main Settings menu.\n5. Tap on 'System' and then 'Developer options'.\n6. Find and enable 'Wi-Fi throttling'.\n\nIf you have already enabled Developer Mode, you can ignore this and click Cancel.\n\n*Note:Options name may differ in models but procedures are same")
            .setPositiveButton("Ok"){dialog,which->
                dialog.dismiss()
            }
            .setNegativeButton("Cancel"){dialog,which->
                isPermissionGrand()
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    555
                )
                isCheckHotspot()
                if (wifiManager.isWifiEnabled) {
                    val wifiReceiver = WifiPasswordGenerated(this)
                    wifiReceiver.showQRDialog()
                } else {
                    SnackBarUtils.showSnackBar(views!!.root, MainActivityFields.TURN_ON_WIFI)
                }
            }.show()
    }




    private fun isCheckHotspot(): Boolean {
        try {
            val npm = Class.forName("android.net.NetworkPolicyManager")
                .getDeclaredMethod("from", Context::class.java).invoke(null, this)
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
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_WIFI_STATE
            ) == PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            // Permissions granted, do nothing
        } else {
            val permissions = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
            ActivityCompat.requestPermissions(this, permissions, STORAGE_PERMISSION_CODE)
        }
    }

    fun getData(key: String): String {
        return sharedPref.getString(key, "").toString()
    }

    fun putData(key: String, value: Any) {
        when (value) {
            is Int -> {
                shEdit.putInt(key, value)
            }

            is Boolean -> {
                shEdit.putBoolean(key, value)
            }

            is String -> {
                shEdit.putString(key, value)
            }
        }

        shEdit.apply()
    }


}
