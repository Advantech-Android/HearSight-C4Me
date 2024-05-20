package com.codewithkael.firebasevideocall.firebaseClient

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.adapters.MainRecyclerViewAdapter
import com.codewithkael.firebasevideocall.ui.LoginActivity
import com.codewithkael.firebasevideocall.ui.MainActivity
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.LATEST_EVENT
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.PASSWORD
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.CALL_EVENT
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.STATUS
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

    companion object {
        var registerNumber = ""
    }


    fun setUsername(username: String, phonenumber: String) {
        if (!username.isNullOrEmpty()) {
            this.currentUsername = username
            this.currentUserPhonenumber = phonenumber
        } else {
            val userName = LoginActivity.Share.liveShare.value?.getString("user_name", "")
            this.currentUsername = userName
        }
        if (!phonenumber.isNullOrEmpty()) {
            this.currentUserPhonenumber = phonenumber

        } else {
            val userPhoneNUmber = LoginActivity.Share.liveShare.value?.getString("user_phone", "")
            this.currentUserPhonenumber = userPhoneNUmber
        }


    }

    public fun getUserName(): String {
        return currentUsername.toString()
    }


    public fun getUserPhone(): String//called in main repository
    {
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

    fun login(username: String, phonenumber: String, done: (Boolean, String?) -> Unit) {

        try {
            dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
                override fun onDataChange(snapshot: DataSnapshot) {
                    //if the current user exists
                    if (snapshot.hasChild(phonenumber)) {
                        //user exists , its time to check the phone_number
                        // val dbUsername = snapshot.child(phonenumber).child("user_name").value
                        val dbPhonenumber = snapshot.child(phonenumber).key.toString()
                        if (phonenumber == dbPhonenumber) {
                            //username is correct and sign in
                            registerNumber = phonenumber
                            val dbUsername = snapshot.child(phonenumber).child("user_name").value
                            dbRef.child(phonenumber).child(STATUS).setValue(UserStatus.ONLINE)
                                .addOnCompleteListener {
                                    setUsername(dbUsername.toString(), phonenumber)

                                    done(true, null)
                                }.addOnFailureListener {
                                    done(false, "${it.message}")
                                }
                        } else {
                            //password is wrong, notify user
                            done(false, "Password is wrong")
                        }

                    } else {
                        //user doesnt exist, register the user
                        dbRef.child(phonenumber).child("user_name").setValue(username)
                            .addOnCompleteListener {
                                dbRef.child(phonenumber).child(STATUS).setValue(UserStatus.ONLINE)
                                    .addOnCompleteListener {
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
                "getEndCallEvent: currentUsername =$currentUsername -> CALL_EVENT=$CALL_EVENT"
            )
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

                // Extract outer contacts
                snapshot.children.filter { it.key != currentUserPhonenumber }
                    .forEach { currentUser ->
                        val currentUserInfo = ContactInfo(
                            currentUser.key.toString(),
                            currentUser.child("user_name").value.toString(),
                            currentUser.child(STATUS).value.toString(),false
                        )
                        outerList.add(currentUserInfo)
                    }
                status(outerList)
                snapshot.children.filter { it.key == currentUserPhonenumber }
                    .forEach { currentUser ->
                        currentUser.child("contacts").children.filter { it.key != currentUserPhonenumber }
                            .forEach { innerContact ->
                                val innerContactInfo = ContactInfo(
                                    innerContact.key.toString(),
                                    innerContact.child("user_name").value.toString(),
                                    innerContact.child(STATUS).value.toString(),false
                                )
                                val matchingContact =
                                    outerList.find { it.contactNumber == innerContactInfo.contactNumber }
                                if (matchingContact != null) {
                                    finalList.add(innerContactInfo.copy(status = matchingContact.status))
                                    val statusUpdateTask = dbRef.child(currentUserPhonenumber!!)
                                        .child("contacts")
                                        .child(innerContactInfo.contactNumber)
                                        .child(STATUS)
                                        .setValue(matchingContact.status)

                                    statusUpdateTask.addOnCompleteListener { task ->
                                        if (task.isSuccessful) {
                                            Log.d(
                                                TAG,
                                                "Successfully updated status for contact: ${innerContactInfo.contactNumber}"
                                            )
                                        } else {
                                            Log.e(
                                                TAG,
                                                "Failed to update status for contact: ${innerContactInfo.contactNumber}",
                                                task.exception
                                            )
                                        }
                                    }
                                    statusUpdateTask.addOnFailureListener { exception ->
                                        Log.e(
                                            TAG,
                                            "Failed to update status for contact: ${innerContactInfo.contactNumber}",
                                            exception
                                        )
                                    }
                                }
                            }
                    }
                status2(finalList)
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
                function()
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

}












