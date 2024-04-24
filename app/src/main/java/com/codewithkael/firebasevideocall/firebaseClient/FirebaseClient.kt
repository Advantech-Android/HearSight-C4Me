package com.codewithkael.firebasevideocall.firebaseClient

import android.util.Log
import com.codewithkael.firebasevideocall.utils.DataModel
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.CALL_EVENT
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.LATEST_EVENT
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.PASSWORD
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames.STATUS
import com.codewithkael.firebasevideocall.utils.MyEventListener
import com.codewithkael.firebasevideocall.utils.UserStatus
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.gson.Gson
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "***>>FirebaseClient"

@Singleton
class FirebaseClient @Inject constructor(
    private val dbRef: DatabaseReference,
    private val gson: Gson
) {

    private var currentUsername: String? = null
    private var currentUserPassword: String? = null
    private fun setUsername(username: String, password: String) {
        this.currentUsername = username
        this.currentUserPassword = password
    }

    public fun getUserName(): String {
        return currentUsername.toString()
    }


    fun login(username: String, password: String, done: (Boolean, String?) -> Unit) {
        dbRef.addListenerForSingleValueEvent(object : MyEventListener() {
            override fun onDataChange(snapshot: DataSnapshot) {
                //if the current user exists
                if (snapshot.hasChild(username)) {
                    //user exists , its time to check the password
                    val dbPassword = snapshot.child(username).child(PASSWORD).value
                    if (password == dbPassword) {
                        //password is correct and sign in
                        dbRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                            .addOnCompleteListener {
                                setUsername(username, password)
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
                    dbRef.child(username).child(PASSWORD).setValue(password).addOnCompleteListener {
                        dbRef.child(username).child(STATUS).setValue(UserStatus.ONLINE)
                            .addOnCompleteListener {
                                setUsername(username, password)
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
            dbRef.child(currentUsername!!).child(LATEST_EVENT)
                .addValueEventListener(object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)

                        val event=try {
                            gson.fromJson(snapshot.value.toString(), DataModel::class.java)
                        }catch (e:Exception){
                            null
                        }
                        dbRef.child(currentUsername!!).child(CALL_EVENT).addValueEventListener(
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

    fun subscribeForLatestEvent(listener: Listener)
    {
        try {

            dbRef.child(currentUsername!!).child(LATEST_EVENT).addValueEventListener(
                object : MyEventListener() {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        super.onDataChange(snapshot)
                        val event = try {
                            Log.d(
                                TAG,
                                "subscribeForLatestEvent:----------- ${snapshot.value.toString()}"
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
        val convertedMessage = gson.toJson(message.copy(sender = currentUsername))
        dbRef.child(message.target).child(LATEST_EVENT).setValue(convertedMessage)
            .addOnCompleteListener {

                success(true)
            }.addOnFailureListener {
                success(false)
            }
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

    fun changeMyStatus(status: UserStatus) {
        dbRef.child(currentUsername!!).child(STATUS).setValue(status.name)
    }

    fun clearLatestEvent() {
        dbRef.child(currentUsername!!).child(LATEST_EVENT).setValue(null)
    }

    fun logOff(function: () -> Unit) {
        dbRef.child(currentUsername!!).child(STATUS).setValue(UserStatus.OFFLINE)
            .addOnCompleteListener { function() }
    }


    interface Listener {
        fun onLatestEventReceived(event: DataModel)
    }

}