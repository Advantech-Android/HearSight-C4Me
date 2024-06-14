package com.codewithkael.firebasevideocall.firebaseClient

import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.adapters.MainRecyclerViewAdapter
import com.codewithkael.firebasevideocall.ui.LoginActivity
import com.codewithkael.firebasevideocall.ui.LoginActivity.Share.liveShare
import com.codewithkael.firebasevideocall.ui.MainActivity
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.LATEST_EVENT
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.PASSWORD
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.CALL_EVENT
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.LOGIN_STATUS
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.STATUS
import com.codewithkael.firebasevideocall.utils.MyChildEventListener
import com.codewithkael.firebasevideocall.utils.MyEventListener
import com.codewithkael.firebasevideocall.utils.UserStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.ValueEventListener
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "***>>FirebaseClient"


@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef: DatabaseReference,
    private val gson: Gson,
    private val context: Context
) : MainRecyclerViewAdapter.Listener {

    private var noAccountUserList: List<ContactInfo> = emptyList()
    var userContactStatus = MutableLiveData(UserStatus.OFFLINE.name)
    var myListener: ValueEventListener? = null
    private var currentUsername: String? = null
    private var currentUserPhonenumber: String? = null

    lateinit var sharedPref: SharedPreferences
    lateinit var shEdit: SharedPreferences.Editor


    companion object {
        var registerNumber = ""
    }

    fun setSharedPreference() {
        sharedPref = context.getSharedPreferences("see_for_me", MODE_PRIVATE)
        shEdit = sharedPref.edit()

        liveShare.value = sharedPref
    }



    fun setUsername(username: String, phonenumber: String)
    {
        setSharedPreference()
        if (!username.isNullOrEmpty()) {
            this.currentUsername = username
        } else {
            // Handle case when username is null or empty
            val userName = getData("user_name")
            this.currentUsername = userName
            Log.d(TAG, "setUsername: $currentUsername and $username")
        }

        if (!phonenumber.isNullOrEmpty()) {
            this.currentUserPhonenumber = phonenumber
        } else {
            // Handle case when phone number is null or empty
            val userPhoneNumber = getData("user_phone")
            this.currentUserPhonenumber = userPhoneNumber
            Log.d(TAG, "setUsername: $currentUserPhonenumber and $userPhoneNumber")
        }


    }

    public fun getUserName(): String {
        if (currentUsername.isNullOrEmpty()) {
            setSharedPreference()
            currentUsername=getData("user_name")

        }
        return currentUsername.toString()
    }


    public fun getUserPhone(): String//called in main repository
    {
        if (currentUserPhonenumber.isNullOrEmpty()) {
            setSharedPreference()
            currentUserPhonenumber=getData("user_phone")

        }
        return currentUserPhonenumber.toString()
    }



    fun getUserNameFB(phone: String, result: (String?) -> Unit)//Called in Main Repository
    {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {

            override fun onDataChange(snapshot: DataSnapshot) {
                result(snapshot.child(phone).child("user_name").value.toString())
            }
        })
    }

    fun login(
        username: String,
        phonenumber: String,
        status: String,
        isLogin: Boolean,
        done: (Boolean, String?) -> Unit
    ) {
        try {
            dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.hasChild(phonenumber)) {
                        val dbPhonenumber = snapshot.child(phonenumber).key.toString()
                        if (phonenumber == dbPhonenumber) {
                            registerNumber = phonenumber
                            val dbUsername = snapshot.child(phonenumber).child("user_name").value
                            val dbChild = dbRef.child(phonenumber)
                            dbChild.child(STATUS).setValue(status)
                                .continueWithTask { task ->
                                    dbChild.child("user_name").setValue(username)
                                    dbChild.child(LOGIN_STATUS).setValue(true)
                                    liveShare.value?.edit()?.putBoolean("is_login", true)?.apply()
                                    return@continueWithTask task
                                }
                                .addOnCompleteListener {
                                    setUsername(dbUsername.toString(), phonenumber)
                                    done(true, null)
                                }.addOnFailureListener {
                                    done(false, "${it.message}")
                                }
                        } else {
                            done(false, "Password is wrong")
                        }
                    } else {
                        dbRef.child(phonenumber).child("user_name").setValue(username)
                            .addOnCompleteListener {
                                dbRef.child(phonenumber).child(STATUS).setValue(status)
                                    .addOnCompleteListener {
                                        dbRef.child(phonenumber).child(LOGIN_STATUS).setValue(true)
                                        liveShare.value?.edit()?.putBoolean("is_login", true)?.apply()
                                        setUsername(username, phonenumber)
                                        done(true, null)
                                    }.addOnFailureListener {
                                        done(false, it.message)
                                    }
                            }.addOnFailureListener {
                                done(false, it.message)
                            }
                    }
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }



    fun getEndCallEvent(data: (DataModel, String) -> Unit) {
        try {
            Log.d(
                TAG,
                "getEndCallEvent-1: currentUsername =$currentUsername -> CALL_EVENT=$CALL_EVENT")

            if (currentUserPhonenumber.isNullOrEmpty()) {
                currentUserPhonenumber = getUserPhone()

            }

            Log.d(TAG, "getEndCallEvent-4:$currentUserPhonenumber ")

            if(currentUsername.isNullOrEmpty()) {
                currentUsername = getUserName()

            }
            Log.d(TAG, "getEndCallEvent-5:$currentUsername ")

            dbRef.child(currentUserPhonenumber!!).child(LATEST_EVENT)
                .addListenerForSingleValueEvent(object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)

                        val event = try {
                            gson.fromJson(snapshot.value.toString(), DataModel::class.java)
                        } catch (e: Exception) {
                            null
                        }
                        dbRef.child(currentUserPhonenumber!!).child(CALL_EVENT)
                            .addValueEventListener(
                                object : MyEventListener() {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        super.onDataChange(snapshot)
                                        Log.d(
                                            TAG,
                                            "getEndCallEvent: ${snapshot.value.toString() + ""}"
                                        )
                                        event?.let { dataModel ->
                                            data(dataModel, snapshot.value.toString())
                                        }
                                    }
                                }
                            )


                    }


                })
            dbRef.child(currentUserPhonenumber!!).child(LATEST_EVENT)
                .addChildEventListener(object:MyChildEventListener(){
                    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                        super.onChildAdded(snapshot, previousChildName)
                    }

                    override fun onChildChanged(
                        snapshot: DataSnapshot,
                        previousChildName: String?
                    ) {
                        super.onChildChanged(snapshot, previousChildName)
                    }

                    override fun onChildRemoved(snapshot: DataSnapshot) {
                        super.onChildRemoved(snapshot)
                    }
                })


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


    suspend fun observeContactDetails(
        ctx: MainActivity,
        status: (List<ContactInfo>) -> Unit,
        status2: (List<ContactInfo>) -> Unit,
        status3: (List<ContactInfo>) -> Unit,
    ) {
        val finalList = mutableListOf<ContactInfo>()

        val outerList = mutableListOf<ContactInfo>()
        val withAcc = mutableListOf<ContactInfo>()
        val withAcc1 = MutableLiveData<List<ContactInfo>>(mutableListOf<ContactInfo>())
        val innerList = mutableListOf<ContactInfo>()
        val unregisteredContacts = mutableListOf<ContactInfo>()

        myListener = dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                withAcc.clear()
                outerList.clear()
                innerList.clear()
                unregisteredContacts.clear()
                // Process all users except the current user
                snapshot.children.filter { it.key != currentUserPhonenumber }
                    .forEach { userSnapshot ->
                        val userInfo = ContactInfo(
                            userSnapshot.key.toString(),
                            userSnapshot.child("user_name").value.toString(),
                            userSnapshot.child(STATUS).value.toString(),
                            isCforMeAcc = true
                        )
                        outerList.add(userInfo)
                    }

                // Process current user's contacts
                snapshot.children.filter { it.key == currentUserPhonenumber }
                    .forEach { currentUserSnapshot ->
                        currentUserSnapshot.child("contacts")
                            .children.filter { it.key != currentUserPhonenumber }
                            .forEach { contactSnapshot ->
                                val contactInfo = ContactInfo(
                                    contactSnapshot.key.toString(),
                                    contactSnapshot.child("user_name").value.toString(),
                                    contactSnapshot.child(STATUS).value.toString(),
                                    isCforMeAcc = false
                                )
                                innerList.add(contactInfo)
                            }
                    }

                Log.d("***TAG", "All contacts: $outerList")
                Log.d("***TAG", "Current user contacts: $innerList")

                // Use a map for quick lookup of registered contacts
                val outerContactMap = outerList.associateBy { it.contactNumber.trim() }
                Log.d(TAG, "onDataChange: outerContactMap =$outerContactMap")

                innerList.forEach { innerContact ->
                    val matchedOuterContact = outerContactMap[innerContact.contactNumber.trim()]
                    matchedOuterContact?.let { outerContact ->
                        innerContact.isCforMeAcc = true
                        innerContact.status = outerContact.status
                    } ?: run {
                        innerContact.status = UserStatus.OFFLINE.name
                        unregisteredContacts.add(innerContact)
                    }
                    withAcc.add(innerContact)
                }

                withAcc1.value = withAcc.toSet().toList()
                if (withAcc1.value != null)
                    status3(withAcc1.value!!)
                else
                    status3(withAcc.toSet().toList())
                Log.d("***TAG", "Updated contacts with status: $withAcc")
                Log.d("***TAG", "Unregistered contacts: $unregisteredContacts")

                // Optionally, you can call a different status function for unregistered contacts
                status(unregisteredContacts)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("***TAG", "Error: $error")
            }
        })

        withAcc1.observe(ctx) { data ->
            data.forEach { innerContact ->
                val statusUpdateTask = dbRef.child(currentUserPhonenumber!!)
                    .child("contacts")
                    .child(innerContact.contactNumber)
                    .child(STATUS)
                    .setValue(innerContact.status)
                statusUpdateTask.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.d(
                            "***TAG",
                            "Successfully updated status for contact: ${innerContact.contactNumber}"
                        )
                    } else {
                        Log.e(
                            "***TAG",
                            "Failed to update status for contact: ${innerContact.contactNumber}",
                            task.exception
                        )
                    }
                }
                statusUpdateTask.addOnFailureListener { exception ->
                    Log.e(
                        "***TAG",
                        "Failed to update status for contact: ${innerContact.contactNumber}",
                        exception
                    )
                }
            }
        }
    }


    fun removeContactListener() {
        if (dbRef != null && myListener != null) {
            dbRef.child(currentUserPhonenumber!!)
                .removeEventListener(myListener!!)
        }

    }

    private fun setNoAccountUserList(noAccountUserList: List<ContactInfo>) {
        this.noAccountUserList = noAccountUserList
        Log.d(TAG, "setNoAccountUserList: ${noAccountUserList.get(0).contactNumber}")
    }

    fun subscribeForLatestEvent(listener: Listener) {
        try {
            Log.d(TAG, "subscribeForLatestEvent:currentUsername: $currentUserPhonenumber")
            dbRef.child(currentUserPhonenumber!!).child(LATEST_EVENT).addValueEventListener(
                object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)

                        val event = try {
                            Log.d(
                                TAG,
                                "subscribeForLatestEvent => onDataChange: ${snapshot.value.toString()}"
                            )
                            gson.fromJson(snapshot.value.toString(), DataModel::class.java)

                        } catch (e: Exception) {
                            e.printStackTrace()
                            null
                        }
                        event?.let {
                            listener.onLatestEventReceived(it)
                        }
                    }
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun sendMessageToOtherClient(message: DataModel, success: (Boolean) -> Unit) {
        try {
            Log.d(TAG, "sendMessageToOtherClient: target ${message.target} ")
            val convertedMessage = gson.toJson(message.copy(sender = currentUserPhonenumber))
            dbRef.child(message.target).child(LATEST_EVENT).setValue(convertedMessage)
                .addOnCompleteListener {
                    success(true)
                }.addOnFailureListener {
                    success(false)
                }
        } catch (e: Exception) {
            Log.d(TAG, "sendMessageToOtherClient: ${e.printStackTrace()}")
        }

    }

    fun changeMyStatus(status: UserStatus) {
        Log.d(
            TAG,
            "changeMyStatus() called with:currentUserPhonenumber =$currentUserPhonenumber status = $status"
        )
        dbRef.child(currentUserPhonenumber!!).child(STATUS).setValue(status.name)
    }

    fun clearLatestEvent() {
        Log.d(TAG, "clearLatestEvent: ")
        dbRef.child(currentUserPhonenumber!!).child(LATEST_EVENT).setValue(null)
    }


    fun sendCallStatusToOtherClient(
        target: String,
        sender: String,
        callLogs: String,
        callStatus: (String) -> Unit
    ) {
        //here sender act as caller change
        //here target act as receiver change
        Log.d(
            TAG,
            "Firebase: target = $target, sender = $sender, callLogs = $callLogs, callStatus = $callStatus"
        )
        dbRef.child(sender).child(CALL_EVENT).setValue(callLogs)
            .addOnCompleteListener {
                callStatus(callLogs)
            }.addOnFailureListener {
                callStatus(it.message.toString())
            }


    }


    fun logOff(function: () -> Unit) {
        dbRef.child(currentUserPhonenumber!!).child(STATUS).setValue(UserStatus.OFFLINE)
            .addOnCompleteListener {
                dbRef.child(currentUserPhonenumber!!).child(LOGIN_STATUS).setValue(false)
                    .addOnCompleteListener {
                        liveShare.value?.edit()?.putBoolean("is_login", false)?.apply()
                        function()
                    }
            }
    }


    fun addContacts(username: String, phone: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(currentUserPhonenumber!!)) {
                    dbRef.child(currentUserPhonenumber!!).child("contacts").child(phone)
                        .child("user_name").setValue(username)
                        .addOnCompleteListener {
                            dbRef.child(currentUserPhonenumber!!).child("contacts")
                                .child(phone).child("status")
                                .setValue(userContactStatus.value.toString())
                                .addOnCompleteListener {
                                    done(true, userContactStatus.value.toString())
                                }
                                .addOnFailureListener {
                                    done(false, null)
                                }
                        }.addOnFailureListener {
                            done(false, null)
                        }
                }
            }
        })
    }


    interface Listener {
        fun onLatestEventReceived(event: DataModel)
    }


    override fun onVideoCallClicked(username: String, user: ContactInfo) {

    }

    override fun onAudioCallClicked(username: String) {

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












