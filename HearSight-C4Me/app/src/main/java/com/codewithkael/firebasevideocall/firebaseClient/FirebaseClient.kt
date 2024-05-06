package com.codewithkael.firebasevideocall.firebaseClient

import android.app.Activity
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.adapters.MainRecyclerViewAdapter
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
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "***>>FirebaseClient"


@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef: DatabaseReference,
    private val gson: Gson

) : MainRecyclerViewAdapter.Listener {

    var userContactStatus = MutableLiveData(UserStatus.OFFLINE.name)
    val registerContactKeysArrayList = ArrayList<registerContactKeys>()
    private var currentUsername: String? = null
    private var currentUserPhonenumber: String? = null
    private var currentUserPassword: String? = null
    companion object{
        var registerNumber=""
    }
    private fun setUsername(username: String, phonenumber: String) {
        this.currentUsername = username
        this.currentUserPhonenumber = phonenumber

    }
    public fun getUserName(): String {
        return currentUsername.toString()
    }
    public fun getUserPhone(): String {
        return currentUserPhonenumber.toString()
    }

    fun login(username: String, phonenumber: String, done: (Boolean, String?) -> Unit) {

        try {
            dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var childKey = ""
                    if (snapshot.hasChild(phonenumber)) {
                        for (childSnapshot in snapshot.children) {
                            childKey = childSnapshot.key.toString()
                            if (phonenumber == childKey) {
                                // Username is correct, sign in
                                registerNumber = phonenumber
                                dbRef.child(phonenumber).child(STATUS).setValue(UserStatus.ONLINE)
                                    .addOnCompleteListener {
                                        setUsername(username, registerNumber)
                                        done(true, null)
                                    }.addOnFailureListener {
                                        Log.d(TAG, "onDataChange: ${it.message}")
                                        done(false, "${it.message}")
                                    }
                                return // Exit the loop once the user is found
                            }
                        }
                        // If loop completes without finding the user, notify that the password is wrong
                        done(false, "Password is wrong")
                    } else {
                        // User doesn't exist, register the user
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


    fun observeUsersStatus(
        status: (List<Pair<String, String>>) -> Unit,
        status1: (List<Pair<String, String>>) -> Unit
    ) {
        dbRef.addValueEventListener(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {

                Log.d(TAG, "observeUsersStatus => onDataChange: ")

                val list = snapshot.children.filter {
                    it.key != currentUsername
                }.map {
                    // Log.d(TAG, "onDataChange: ")

                    it.key!! to it.child(STATUS).value.toString()
                }
                status(list)

                val list1 = snapshot.children.filter {
                    it.key != currentUsername
                }.map {


                    it.key!! to it.child(PASSWORD).value.toString()
                }
                status1(list1)

            }
        })
    }

    fun getEndCallEvent(data: (DataModel,String) -> Unit) {
        try {
            Log.d(TAG, "getEndCallEvent: currentUsername =$currentUsername -> CALL_EVENT=$CALL_EVENT")
            dbRef.child(currentUserPhonenumber!!).child(LATEST_EVENT)
                .addValueEventListener(object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)

                        val event=try {
                            gson.fromJson(snapshot.value.toString(), DataModel::class.java)
                        }catch (e:Exception){
                            null
                        }
                        dbRef.child(currentUserPhonenumber!!).child(CALL_EVENT).addValueEventListener(
                            object : MyEventListener() {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    super.onDataChange(snapshot)
                                    Log.d(TAG, "getEndCallEvent: ${snapshot.value.toString()+""}")
                                    event?.let { dataModel->
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

    fun getUserContactList() {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.child(currentUsername!!).child("contacts").children.filter { data ->
                    Log.d(
                        "***contactlist",
                        "getUserContactList => onDataChange: ${data.key}:${data.value}:${data.children}"
                    )

                    true
                }
            }
        })
    }
    fun observeContactDetails(status: (List<ContactInfo>) -> Unit, status2: (List<ContactInfo>) -> Unit) {
        val finalList = mutableListOf<ContactInfo>()
        val outerList = mutableListOf<ContactInfo>()

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                finalList.clear()
                outerList.clear()

                // Extract outer contacts
                snapshot.children.filter { it.key != currentUserPhonenumber }.forEach { currentUser ->
                    val currentUserInfo = ContactInfo(currentUser.key.toString(), currentUser.child("user_name").value.toString(), currentUser.child(STATUS).value.toString())
                    outerList.add(currentUserInfo)
                }
                status(outerList)
                snapshot.children.filter { it.key == currentUserPhonenumber }.forEach { currentUser ->
                    currentUser.child("contacts").children.filter { it.key != currentUserPhonenumber }.forEach { innerContact ->
                        val innerContactInfo = ContactInfo(innerContact.key.toString(), innerContact.child("user_name").value.toString(), innerContact.child(STATUS).value.toString())
                        val matchingContact = outerList.find { it.contactNumber == innerContactInfo.contactNumber }
                        if (matchingContact != null) {
                            finalList.add(innerContactInfo.copy(status = matchingContact.status))
                            val statusUpdateTask = dbRef.child(currentUserPhonenumber!!)
                                .child("contacts")
                                .child(innerContactInfo.contactNumber)
                                .child(STATUS)
                                .setValue(matchingContact.status)

                            statusUpdateTask.addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    Log.d(TAG, "Successfully updated status for contact: ${innerContactInfo.contactNumber}")
                                } else {
                                    Log.e(TAG, "Failed to update status for contact: ${innerContactInfo.contactNumber}", task.exception)
                                }
                            }
                            statusUpdateTask.addOnFailureListener { exception ->
                                Log.e(TAG, "Failed to update status for contact: ${innerContactInfo.contactNumber}", exception)
                            }
                        }
                    }
                }
                status2(finalList)
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event if needed
            }
        })
    }
    fun subscribeForLatestEvent(listener: Listener) {
        try {
            Log.d(TAG, "subscribeForLatestEvent:currentUsername: $currentUserPhonenumber")
            dbRef.child(currentUserPhonenumber!!).child(LATEST_EVENT).addValueEventListener(
                object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)

                        val event = try {
                            Log.d(TAG, "subscribeForLatestEvent => onDataChange: ${snapshot.value.toString()}")
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
        }catch (e:Exception)
        {
            Log.d(TAG, "sendMessageToOtherClient: ${e.printStackTrace()}")
        }

    }
    fun changeMyStatus(status: UserStatus) {
        Log.d(TAG, "changeMyStatus() called with:currentUserPhonenumber =$currentUserPhonenumber status = $status")
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
                function() }
    }

    fun addContacts(username: String, phone: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(currentUserPhonenumber!!)) {
                    dbRef.child(currentUserPhonenumber!!).child("contacts").child(phone)
                        .child("user_name").setValue(username)
                        .addOnCompleteListener {
                            dbRef.child(currentUserPhonenumber!!).child("contacts")
                                .child(phone).child("status").setValue(userContactStatus.value.toString())
                                .addOnCompleteListener {
                                    done(true, null)
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


    fun getUserContactList(callback: (List<Pair<String, String>>) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.hasChild(currentUserPhonenumber!!)) {
                    if (currentUserPhonenumber!!.isNotEmpty()) {
                        snapshot.child(currentUserPhonenumber!!.filter { data ->
                            true
                        })
                    }
                }
            }
        })
    }


    interface Listener {
        fun onLatestEventReceived(event: DataModel)
    }

    data class registerContactKeys(val mcontact: String)

    override fun onVideoCallClicked(username: String) {
    }

    override fun onAudioCallClicked(username: String) {

    }

}