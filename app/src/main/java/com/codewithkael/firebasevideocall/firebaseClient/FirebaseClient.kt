package com.codewithkael.firebasevideocall.firebaseClient

import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.codewithkael.firebasevideocall.adapters.MainRecyclerViewAdapter
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.LATEST_EVENT
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

private const val TAG = "===>>FirebaseClient"

@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef: DatabaseReference,
    private val gson: Gson
) : MainRecyclerViewAdapter.Listener {

    var userContactStatus = MutableLiveData(UserStatus.OFFLINE.name)
    val registerContactKeysArrayList = ArrayList<registerContactKeys>()
    private var currentUsername: String? = null
    private var currentUserPhonenumber: String? = null
    private fun setUsername(username: String, phonenumber: String) {
        this.currentUsername = username
        this.currentUserPhonenumber = phonenumber

    }


    fun login(username: String, phonenumber: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                //if the current user exists
                if (snapshot.hasChild(phonenumber)) {
                    //user exists , its time to check the phone_number
                    val dbUsername = snapshot.child(phonenumber).child("user_name").value
                    if (username == dbUsername) {
                        //username is correct and sign in
                        dbRef.child(phonenumber).child(STATUS).setValue(UserStatus.ONLINE)
                            .addOnCompleteListener {
                                setUsername(username, phonenumber)
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


//    fun observeUsersStatus(
//        status: (List<Pair<String, String>>) -> Unit,
//        status1: (List<Pair<String, String>>) -> Unit
//    ) {
//       * dbRef.addValueEventListener(object : MyEventListener() {
//             override fun onDataChange(snapshot: DataSnapshot) {
//
//                 val list = snapshot.children.filter {
//
//                     it.key !=currentUserPhonenumber
//                 }.map {
//                     it.key!! to it.child(STATUS).value.toString()
//                 }
//                 status(list)
//
//                 val list1 = snapshot.children.filter {
//                     it.key !=currentUsername
//                 }.map {
//                     Log.d(TAG, "onDataChange: ")
//
//                     it.key!! to it.child(PHONE_NUMBER).value.toString()
//                 }
//                 status1(list1)
//
//             }
//         })
//        observeContactDetails(status,status1)
//    }


    fun observeContactDetails(
        status: (List<ContactInfo>) -> Unit,
        status2: (List<ContactInfo>) -> Unit
    ) {
        val finalList = mutableListOf<ContactInfo>()
        val innerList = mutableListOf<ContactInfo>()
        val outerList = mutableListOf<ContactInfo>()
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d(TAG, "observeContactDetails:-> onDataChange: ")
                finalList.clear()
                innerList.clear()
                outerList.clear()
                 snapshot.children
                    .filter { it.key != currentUserPhonenumber }
                    .map {
                        outerList.add(
                            ContactInfo(
                                it.key.toString(),
                                it.child("user_name").value.toString(),
                                it.child(STATUS).value.toString()
                            )
                        )
                        it.key!! to it.child(STATUS).value.toString()
                    }
                status(outerList)

                snapshot.children
                    .filter { it.key == currentUserPhonenumber }
                    .forEach { currentUser ->
                         currentUser.child("contacts").children
                            .filter { it.key != currentUserPhonenumber }
                            .map {
                                innerList.add(
                                    ContactInfo(
                                        it.key.toString(),
                                        it.child("user_name").value.toString(),
                                        it.child(STATUS).value.toString()
                                    )
                                )
                                it.key!! to it.child("user_name").value.toString()
                            }


                        outerList.forEach { outerContact ->
                            innerList.filter { innerContact ->
                                if (innerContact.contactNumber==outerContact.contactNumber){

                                    innerContact.status=outerContact.status

                                    dbRef.child(currentUserPhonenumber!!)
                                        .child("contacts")
                                        .child(innerContact.contactNumber)
                                        .child("status").setValue(outerContact.status.toString())
                                        .addOnCompleteListener {

                                        }
                                        .addOnFailureListener {

                                        }
                                }
                               innerContact.contactNumber==outerContact.contactNumber
                            }.map {
                                finalList.add(it)
                            }
                        }
                    }
                Log.d(TAG, "observeContactDetails ==> onDataChange: ${finalList.size}")
                status2(finalList)

            }

            override fun onCancelled(error: DatabaseError) {
                // Handle onCancelled event if needed
            }
        })
    }


    fun subscribeForLatestEvent(listener: Listener) {
        try {
            Log.d(TAG, "subscribeForLatestEvent: ")
            dbRef.child(currentUsername!!).child(LATEST_EVENT).addValueEventListener(
                object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)
                        val event = try { gson.fromJson(snapshot.value.toString(), DataModel::class.java)
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
        val convertedMessage = gson.toJson(message.copy(sender = currentUsername))
        dbRef.child(message.target).child(LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener {
                success(true)
            }.addOnFailureListener {
                success(false)
            }
    }

    fun changeMyStatus(status: UserStatus) {
        Log.d(TAG, "changeMyStatus() called with:currentUserPhonenumber =$currentUserPhonenumber status = $status")
        dbRef.child(currentUserPhonenumber!!).child(STATUS).setValue(status.name)
    }

    fun clearLatestEvent() {
        Log.d(TAG, "clearLatestEvent: ")
        dbRef.child(currentUsername!!).child(LATEST_EVENT).setValue(null)
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
                            dbRef.child(currentUserPhonenumber!!)
                                .child("contacts")
                                .child(phone)
                                .child("status").setValue(userContactStatus.value.toString())
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