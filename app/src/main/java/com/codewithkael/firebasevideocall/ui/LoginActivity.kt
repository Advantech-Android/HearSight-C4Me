package com.codewithkael.firebasevideocall.ui


import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import WebQ
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.codewithkael.firebasevideocall.databinding.ActivityLoginBinding
import com.codewithkael.firebasevideocall.repository.MainRepository
import com.codewithkael.firebasevideocall.service.MainServiceRepository
import com.codewithkael.firebasevideocall.utils.MySMSBroadcastReceiver
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.astra.AstraDemo
import com.codewithkael.firebasevideocall.service.MainServiceActions
import com.codewithkael.firebasevideocall.utils.LoginActivityFields
import com.codewithkael.firebasevideocall.utils.LoginActivityFields.USERNAME_INVALID
import com.codewithkael.firebasevideocall.utils.LoginActivityFields.PASWORD_INVALID
import com.codewithkael.firebasevideocall.utils.ProgressBarUtil
import com.codewithkael.firebasevideocall.utils.SnackBarUtils
import com.codewithkael.firebasevideocall.utils.UserStatus
import com.codewithkael.firebasevideocall.utils.setViewFields
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.jiangdg.usb.USBMonitor
import dagger.hilt.android.AndroidEntryPoint
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.Locale
import java.util.regex.Pattern
import javax.inject.Inject


private const val TAG = "***LoginActivity"
private const val STORAGE_PERMISSION_CODE = 123
private const val FILECHOOSER_RESULTCODE = 1
private const val MAX_LENGTH_PHONE = 5

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {
    object uvc {
        var isUvc = MutableLiveData(false)
    }

    private lateinit var countryCode: String
    private var number: String? = null
    private var views: ActivityLoginBinding? = null

    @Inject
    lateinit var mainRepository: MainRepository


    lateinit var webQ: WebQ
    lateinit var wifiManager: WifiManager
    lateinit var sharedPref: SharedPreferences
    lateinit var shEdit: SharedPreferences.Editor
    var ACTION_USB_PERMISSION = "com.serenegiant.USB_PERMISSION."
    private var fetchedPhoneNumber:String?=null

    companion object Share {
        var liveShare =
            MutableLiveData<SharedPreferences>()//is used to provide a reactive and centralized way to manage and observe changes to the SharedPreferences instance across the app.
        val tempLiveData = MutableLiveData<String>("-0.0f")
        private const val REQUEST_READ_PHONE_NUMBERS = 1001//permission request code
        private const val MAX_USERNAME_LENGTH = 15
    }

    var sms_otp = ""

    lateinit var mainServiceRepository: MainServiceRepository
    var mySMSBroadcastReceiver: MySMSBroadcastReceiver = MySMSBroadcastReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        views = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(views!!.root)

        sharedPref = getSharedPreferences(setViewFields.PREF_NAME, MODE_PRIVATE)
        shEdit = sharedPref.edit()
        putData(setViewFields.KEY_IS_LOGIN, false) //Setting "is_login" to false
        liveShare.value = sharedPref

        //Request sim number know permission from the user at run time
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.READ_PHONE_NUMBERS), REQUEST_READ_PHONE_NUMBERS
            )
        }


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerReceiver(
                mySMSBroadcastReceiver,
                IntentFilter("com.google.android.gms.auth.api.phone.SMS_RETRIEVED"),
                RECEIVER_VISIBLE_TO_INSTANT_APPS
            )
        }

        astra()
        wifiManager = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        getIPAdd()
        countryCode = Locale.getDefault().country
        Log.d(TAG, "init: CountryCode:\t$countryCode")
        init()
        // modelDebug()ḍ
        clearData()

    }


    private fun modelDebug() {
        views?.apply {
            if (Build.BRAND!!.equals("samsung", true)) {
                usernameEt.setText("Divya")
                passwordEt.setText("9843716886")
            } else {
                usernameEt.setText("Pooja")
                passwordEt.setText("9994639839")
            }
        }
    }

    private fun astra() {
        views!!.astra.setOnClickListener {
            startActivity(Intent(this, AstraDemo::class.java))
        }
    }

    private fun isFirebaseVerificationTrue(callback: (Boolean) -> Unit) {
        val firebaseUser = FirebaseAuth.getInstance().currentUser
        val firebaseDatabase = FirebaseDatabase.getInstance()
        firebaseUser?.uid?.let { uid ->
            firebaseDatabase.reference.child("users").child(uid).child("verified")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    @SuppressLint("SuspiciousIndentation")
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val isVerified = snapshot.getValue(Boolean::class.java) ?: false
                        callback(isVerified)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        callback(false)
                    }
                })
        }
    }

    private fun getIPAdd() {
        views?.apply {
            ip.setOnClickListener {
                val connectivityManager =
                    baseContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val activeNetwork = connectivityManager.activeNetwork
                val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
                val ipV4Address =
                    linkProperties?.linkAddresses?.firstOrNull { it.address.isAnyLocalAddress }?.address?.hostAddress

                if (ipV4Address != null && ipV4Address != "0.0.0.0") {
                    // Use ipV4Address here
                    ip.text = "ip-" + ipV4Address
                } else {
                    // No IP address found
                    ip.text =
                        "ip-" + android.text.format.Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
                }

            }
        }
    }

    fun getBatteryTemprature(): Float {
        val intent = registerReceiver(
            null,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        )
        val temprature = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0
        return temprature / 10F

    }

    //app can  handle both granted and denied permissions to fetch the sim number
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_READ_PHONE_NUMBERS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                setPhoneNumber()
            } else {
                // Permission denied, handle accordingly
                views?.setmobileno?.text = "Permission denied"
            }
        }
    }

    private fun setPhoneNumber() {
        // Check if the required permissions are granted
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_NUMBERS) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED) {

            // Get the TelephonyManager instance
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

            // Initialize the SIM number variable
            var simNumber: String? = null

            // Check the SIM state
            val simState = telephonyManager.simState
            if (simState == TelephonyManager.SIM_STATE_READY)
            {
                // For Android TIRAMISU and above, use SubscriptionManager to get the phone number
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val subscriptionManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    simNumber = subscriptionManager.getPhoneNumber(SubscriptionManager.DEFAULT_SUBSCRIPTION_ID).trim()
                }


                // Fallback to TelephonyManager for older Android versions or if SubscriptionManager returns null/empty
                if (simNumber.isNullOrEmpty()) {
                    simNumber = telephonyManager.line1Number
                }


                // Double-check by accessing subscription info directly
                if (simNumber.isNullOrEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val subscriptionManager = getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
                    val activeSubscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                    if (!activeSubscriptionInfoList.isNullOrEmpty()) {
                        simNumber = activeSubscriptionInfoList[0].number
                    }
                }
            }


            // Check if the SIM number is not null or empty
            if (!simNumber.isNullOrEmpty()) {
                // If SIM number is available, set it to the TextView
                iterateSIM_Number(simNumber)
            }
            else
            {
                // If SIM number is null or empty, clear the TextView and show a toast message
                Toast.makeText(this@LoginActivity, "Please insert the SIM card first", Toast.LENGTH_SHORT).show()
            }
        }
        else
        {
            // If permissions are not granted, show a toast message
            Toast.makeText(this@LoginActivity, "Please allow all permifbackssions in your mobile", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setSIM(simNumber: String?) {
        // Check if the SIM number is not null or empty
        if (!simNumber.isNullOrEmpty())
        {
            copyToClipboard(simNumber)
            // If SIM number is available, set it
            views?.setmobileno?.text = simNumber
            fetchedPhoneNumber = simNumber

        }
        else
        {
            // If SIM number is null or empty, clear the text view and reset fetchedPhoneNumber
            views?.setmobileno?.text = ""
            fetchedPhoneNumber = null
            Toast.makeText(this@LoginActivity, "Please insert the SIM card first", Toast.LENGTH_SHORT).show()
        }
    }

    private fun iterateSIM_Number(sim_number: String) {
        var i = ""
        var j = 0
        sim_number.reversed().toCharArray().iterator().forEach {
            Log.d(TAG, "setPhoneNumber: ${it}")
            if (sim_number.length >= 10) {
                j++
                if (j <= 10) {
                    i = it + i
                }
            }
        }
        Log.d(TAG, "-----setPhoneNumber: -->${"+91" + i}")
        i = "+91" + i
        setSIM(i)
    }

    private fun init() {
        setUserData()
        handleButtonClick()
        uvcVI_Control()
        restrictUsernameInput(views!!.usernameEt)
    }

    fun setUserData() {
        views?.apply {

            usernameEt.setText(getData(setViewFields.KEY_USER_NAME, String::class.java) as String)
            passwordEt.setText(getData(setViewFields.KEY_USER_PHONE, String::class.java) as String)
            Log.d(TAG, "***LoginActivity-setUserData: $usernameEt and $passwordEt")
        }
    }
    fun clearData() {

        views?.apply {
            clearData.setOnCheckedChangeListener() { buttonView, isChecked ->
                if (isChecked) {
                    Log.d("Checkbox", "Checkbox is checked!")
                    // ProgressBarUtil.showProgressBar(this@LoginActivity)
                   // clearCacheAndRestartApp(applicationContext)
                  Handler(Looper.getMainLooper()).postDelayed({ clearCacheAndRestartApp(this@LoginActivity)},1000)

                    triggerAppRestart()

                } else {
                    Log.d("Checkbox", "Checkbox is unchecked!")
                }

            }
        }

    }

    private fun clearCacheAndRestartApp(context: Context) {
        clearCache(context)
        restartApp(context)
    }
    private fun clearCache(context: Context) {
        try {
            val cacheDir = context.cacheDir
            val dataDir=context.filesDir.parentFile
            if (cacheDir.isDirectory) {
                cacheDir.deleteRecursively()
            }else{
                cacheDir.delete()
            }
            if (dataDir != null) {
                if (dataDir.isDirectory){
                   if(dataDir.deleteRecursively()) {

                   }
                    dataDir.listFiles()?.forEach { file ->
                        if (file.name!="lib") {
                            file.deleteRecursively()
                        }
                    }
                }


            }
            // clear app data
        } catch (e: Exception) {
            Toast.makeText(context, "error in clearCache${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    private fun restartApp(context: Context) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        context.startActivity(intent)
        (context as? Activity)?.finish()
        Runtime.getRuntime().exit(0)
    }
    private fun clearAppData() {
        try {
            // clearing app data
            (getSystemService(ACTIVITY_SERVICE) as ActivityManager).clearApplicationUserData() // note: it has a return value!
        } catch (e: Exception) {
            Log.d(TAG, "clearAppData: ${e.message}")
        }
    }



    fun triggerRebirth(context: Context, nextIntent: Intent?) {
        val intent = Intent(this@LoginActivity, SplashAcitivity::class.java)
      //  intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP)
        intent.putExtra("Restart", Intent(this@LoginActivity, LoginActivity::class.java))
        context.startActivity(intent)
        if (context is Activity) {
            context.finish()
        }
        Runtime.getRuntime().exit(0)

    }


    private fun triggerAppRestart() {
        // Set up the pending intent to restart the app
        val restartIntent = Intent(applicationContext, SplashAcitivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            applicationContext, 0, restartIntent, PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Schedule the restart using AlarmManager
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.set(
            AlarmManager.ELAPSED_REALTIME,
            SystemClock.elapsedRealtime() + 1000, // Delay of 1 second
            pendingIntent
        )

        // Clear the app data
        clearAppData()
    }



    fun uvcVI_Control() {
        views?.apply {
            checkVi.setOnCheckedChangeListener() { buttonView, isChecked ->
                if (isChecked) {
                    Log.d("Checkbox", "Checkbox is checked!")
                    uvc.isUvc.value = true
                } else {
                    Log.d("Checkbox", "Checkbox is unchecked!")
                    uvc.isUvc.value = false
                }

            }

        }
    }

    private fun ActivityLoginBinding.loginBtnUI(isEnabled: Boolean, delay: Long = 0L) {
        if (isEnabled) {
            ProgressBarUtil.hideProgressBar(this@LoginActivity)
            btn.isEnabled = true
            passwordEt.isEnabled = true
            usernameEt.isEnabled = true
        } else {
            ProgressBarUtil.showProgressBar(this@LoginActivity)
            btn.isEnabled = false
            passwordEt.isEnabled = false
            usernameEt.isEnabled = false
            if (delay > 0) {
                Handler(Looper.getMainLooper()).postDelayed({
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                    btn.isEnabled = true
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                }, delay)
            }
        }
    }

    private fun handleButtonClick() {
        views?.apply {

            btn.isEnabled = true
            btn.setOnClickListener {
                btn.isEnabled = false
                Log.d(TAG, "handleButtonClick: ${uvc.isUvc.value}")
                loginBtnUI(false)// This will show the progress bar and disable the login button, password field, and username field.
                val usernameText =
                    usernameEt.text.toString().trim().lowercase().replace(" ", "")
                var passwordText = passwordEt.text.toString().trim()

                if (!passwordText.startsWith("+91"))
                {
                    passwordText = "+91${passwordText}"
                }
                usernameEt.setText(usernameText)
                passwordEt.setText(passwordText)

                if (usernameText.isEmpty()) {
                    SnackBarUtils.showSnackBar(views!!.root, USERNAME_INVALID)
                    loginBtnUI(false, 5000)
                    return@setOnClickListener
                }
                if (passwordText.isEmpty()) {
                    SnackBarUtils.showSnackBar(views!!.root, PASWORD_INVALID)
                    loginBtnUI(false, 5000)
                    return@setOnClickListener
                }
                else if (fetchedPhoneNumber != null && passwordText != fetchedPhoneNumber)
                {
                    SnackBarUtils.showSnackBar(
                        views!!.root,
                        "Please enter the correct mobile number"
                    )
                    loginBtnUI(false, 5000)
                    return@setOnClickListener
                } else if (fetchedPhoneNumber == null) {
                    Toast.makeText(this@LoginActivity, "Please insert the SIM card first", Toast.LENGTH_SHORT).show()
                    loginBtnUI(false, 5000)
                    return@setOnClickListener
                }

                val run = {
                    loginBtnUI(true)
                }
                var hand = Handler(Looper.getMainLooper())
                hand.postDelayed(run, 5000)


                passwordEt.isEnabled = false
                usernameEt.isEnabled = false

                if (!ProgressBarUtil.checkInternetConnection(this@LoginActivity)) {
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled = true
                    SnackBarUtils.showSnackBar(btn, LoginActivityFields.CHECK_NET_CONNECTION)
                    hand.removeCallbacksAndMessages(null)
                    ProgressBarUtil.hideProgressBar(this@LoginActivity)
                }
                else
                {
                 //   performLogin(usernameText, passwordText)
                  loginReactive(usernameText,passwordText)
                }
            }

        }
    }

    private fun loginReactive(usernameText: String,  phoneNumber: String) {
        views?.apply {
            if (fetchedPhoneNumber == phoneNumber)
            {
                startActivityForResult(Intent(this@LoginActivity, RandomOTPGenerate::class.java).apply {
                    putExtra(setViewFields.EXTRA_USER_NAME, usernameEt.text.toString().trim())
                    putExtra(setViewFields.EXTRA_USER_PHONE, passwordEt.text.toString().trim()) },111)
            }
            else
            {
                views?.apply {
                    SnackBarUtils.showSnackBar(btn,"please check your phone number")
                }
            }
        }
    }
    // Inside your LoginActivity class
    private fun restrictUsernameInput(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                Log.d(TAG, "beforeTextChanged: $s")
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                Log.d(TAG, "beforeTextChanged: $s")
            }

            override fun afterTextChanged(s: Editable?) {
                s?.let {
                    val inputUserName = it.toString()
                    if (inputUserName.length > MAX_USERNAME_LENGTH) {
                        // Limit the input to 10 characters
                        editText.setText(inputUserName.substring(0, MAX_USERNAME_LENGTH))
                        editText.setSelection(MAX_USERNAME_LENGTH) // Stop cursor movement
                        return
                    }

                    // Disallow special characters and convert to lowercase
                    val filteredInput = inputUserName.filter { char ->
                        char.isLetter() && char.isLowerCase()
                    }

                    if (inputUserName != filteredInput) {
                        editText.setText(filteredInput)
                        editText.setSelection(filteredInput.length) // Stop cursor movement
                    }
                }
            }
        })
    }

    private fun performLogin(usernameText: String, passwordText: String) {
        views?.apply {
            mainRepository.login(
                usernameText, passwordText, UserStatus.ONLINE.name, true
            ) { isDone, reason ->
                Log.d(TAG, "Login attempt result: $isDone, Reason: $reason")
                ProgressBarUtil.hideProgressBar(this@LoginActivity)

                if (!isDone) {
                    passwordEt.isEnabled = true
                    usernameEt.isEnabled = true
                    btn.isEnabled = true

                    Log.d(TAG, "Login failed: $reason")
                    SnackBarUtils.showSnackBar(root, LoginActivityFields.UN_PW_INCORRECT)
                } else {
                    putData(setViewFields.KEY_USER_NAME, usernameText)
                    putData(setViewFields.KEY_USER_PHONE, passwordText)
                    Log.d(TAG, "!!>>performLogin:$usernameText and $passwordText ")
                    putData(setViewFields.KEY_IS_LOGIN, true)

                    startActivity(Intent(this@LoginActivity, MainActivity::class.java).apply {
                        putExtra(setViewFields.EXTRA_USER_NAME, usernameText)
                        putExtra(setViewFields.EXTRA_USER_PHONE, passwordText)
                    })

                }

            }
        }
    }

//
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//        if (requestCode == STORAGE_PERMISSION_CODE) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "scanSuccess:onRequestPermissionsResult")
//            }
//        } else if (requestCode == 1000) {
//            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                Log.d(TAG, "scanSuccess:onRequestPermissionsResult")
//            }
//        }
//    }
fun Context.copyToClipboard(text: CharSequence){
    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("label",text)
    clipboard.setPrimaryClip(clip)
}
    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (requestCode == FILECHOOSER_RESULTCODE) {
            // Handle file chooser result
        }
        if(requestCode==111){
            val resultData = data?.getStringExtra(setViewFields.EXTRA_RESULT)
            views?.apply {
                SnackBarUtils.showSnackBar(btn,resultData.toString())
                val uName=usernameEt.text.toString().trim()
                val uPhone=passwordEt.text.toString().trim()
                mainRepository.login(
                    uName,
                    uPhone,
                    UserStatus.ONLINE.name, true
                ) { isDone, reason ->
                    isLoginDebug(uName, uPhone)
                }
               // isLoginDebug(uName, uPhone)

            }
        }
    }

    private fun isLoginDebug(uName: String, uPhone: String) {
        putData(setViewFields.KEY_USER_NAME, uName)
        putData(setViewFields.KEY_USER_PHONE, uPhone)
        Log.d("***LoginActivity", "uName and pass:$uName and $uPhone ")
        putData(setViewFields.KEY_IS_LOGIN, true)
        val intent = Intent(this@LoginActivity, MainActivity::class.java)
            .apply {
                putExtra(setViewFields.EXTRA_USER_NAME, uName)
                putExtra(setViewFields.EXTRA_USER_PHONE, uPhone)
            }
        startActivity(intent)
        finish()
    }


    @SuppressLint("MissingPermission")
    private fun TelephonyManager() // previous function name init()
    {
        val tm = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        if (ActivityCompat.checkSelfPermission(
                this@LoginActivity,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {

            requestPermissions(
                arrayOf(Manifest.permission.READ_PHONE_STATE),
                1000
            )
        }
        try {
            number = tm.line1Number
            Log.d(TAG, "init: ${tm.simCountryIso} $number")
        } catch (e: Exception) {

        }
    }


    private fun startMyService() {
        mainServiceRepository.startService("wifi", MainServiceActions.START_WIFI_SCAN.name)
    }

    private fun openFileExplorer() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            setType("image/*")
        }
        startActivityForResult(Intent.createChooser(intent, "File Chooser"), FILECHOOSER_RESULTCODE)
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


    override fun onResume() {
        super.onResume()
        if (tempLiveData != null && !tempLiveData.value.equals("-0.0f")) {
            views?.temperatureTextView?.text = "Mobile Temprature:${tempLiveData.value} °C"
        } else {
            val batteryTemprature = getBatteryTemprature()
            tempLiveData.value = batteryTemprature.toString()
            views?.temperatureTextView?.text = "Mobile Temperature: $batteryTemprature °C"
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mySMSBroadcastReceiver)
        views = null
    }


    fun isValidAndAddCountryCode(phoneNumber: String): String {
        val pattern = "^\\+(?:\\d{1,3})?[789]{1}\\d{9}$"
        val regex = Pattern.compile(pattern)
        val matcher = regex.matcher(phoneNumber)

        return if (matcher.matches()) {
            phoneNumber
        } else {

            val formattedNumber = if (phoneNumber.length == 10) {
                "+91$phoneNumber"
            } else {

                "Invalid"
            }
            formattedNumber
        }
    }


    fun <T> getData(key: String, type: Class<T>): Any? {
        return when (type) {
            Boolean::class.java -> sharedPref.getBoolean(key, false)
            String::class.java -> sharedPref.getString(key, "")
            Float::class.java -> "sharedPref.getFloat(key,0.0f)"
            else -> null
        }

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

