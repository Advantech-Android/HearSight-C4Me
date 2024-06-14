package com.codewithkael.firebasevideocall.adapters

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codewithkael.firebasevideocall.BuildConfig
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.databinding.ItemMainRecyclerViewBinding
import com.codewithkael.firebasevideocall.firebaseClient.FirebaseClient
import com.codewithkael.firebasevideocall.ui.Mp3Ring
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.utils.NetworkChangeReceiver
import com.google.firebase.database.FirebaseDatabase
import javax.inject.Inject


private const val TAG = "====>>MainRecycViewAdap"

class MainRecyclerViewAdapter(private val listener: Listener) :

    RecyclerView.Adapter<MainRecyclerViewAdapter.MainRecyclerViewHolder>() {
    @Inject
    lateinit var mp3Player: Mp3Ring
    private var innerContactList: List<ContactInfo>? = null
    private var onlineContactList: List<ContactInfo>? = null
    var filteredContactList: List<ContactInfo>? = null
    fun updateList(list: List<ContactInfo>, onlineList: List<ContactInfo>) {
        this.innerContactList = list
        this.onlineContactList = onlineList
        filteredContactList = onlineList
        notifyDataSetChanged()

    }

    fun filterList(query: String) {
        filteredContactList = onlineContactList?.filter {
            it.userName.contains(query, ignoreCase = true) || it.contactNumber.contains(query, ignoreCase = true)

        }
        filteredContactList?.filter {
            filteredContactList?.indexOf(it)?.let { it1 -> notifyItemChanged(it1) }
            return
        }
        notifyDataSetChanged()

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewHolder {
        val binding = ItemMainRecyclerViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MainRecyclerViewHolder(binding, listener)
    }

    override fun getItemCount(): Int {
        return onlineContactList?.size ?: 0
    }

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        onlineContactList?.let { list ->
            val user = list[position]
            holder.bind(user, position, {
                Log.d(TAG, "onBindViewHolder: onVideoCallClicked $it")
                listener.onVideoCallClicked(it, user)
            }, {
                Log.d(TAG, "onBindViewHolder: onAudioCallClicked")
                listener.onAudioCallClicked(it)
            })
        }
    }


    interface Listener {
        fun onVideoCallClicked(username: String, user: ContactInfo)
        fun onAudioCallClicked(username: String)
    }


    class MainRecyclerViewHolder(private val binding: ItemMainRecyclerViewBinding, private val listener: Listener) :
        RecyclerView.ViewHolder(binding.root) {
        val handler = android.os.Handler(Looper.getMainLooper())
        private val context = binding.root.context
        @Inject
        lateinit var mp3Player: Mp3Ring
        fun bind(
            user: ContactInfo,
            pos: Int,
            videoCallClicked: (String) -> Unit,
            audioCallClicked: (String) -> Unit
        ) {
            Log.d(
                TAG,
                "bind: user:${user.userName} ,phone:${user.contactNumber},status:${user.status}"
            )
            binding.apply {

                when (user.status) {
                    "ONLINE" -> {
                        videoCallBtn.isVisible = true
                        audioCallBtn.isVisible = false
                        cardViewVideoCallClick.isEnabled = true
                        /*videoCallBtn.setOnClickListener {
                            Toast.makeText(context, "Please wait while your call is being connected", Toast.LENGTH_LONG).show()

                            videoCallClicked.invoke(user.first)//represents username
                        }*/
                        handler.postDelayed(kotlinx.coroutines.Runnable {
                            cardViewVideoCallClick.isEnabled = true
                        }, 3000)
                        cardViewVideoCallClick.setOnClickListener {

                            cardViewVideoCallClick.isEnabled = false
                            Toast.makeText(
                                context,
                                "Please wait while your call is being connected",
                                Toast.LENGTH_LONG
                            ).show()
                            videoCallClicked.invoke(user.contactNumber)//represents username
                        }

                        audioCallBtn.setOnClickListener {
                            audioCallClicked.invoke(user.contactNumber)
                        }
                        statusTv.setTextColor(context.resources.getColor(R.color.light_green, null))
                        statusTv.text = "${user.userName} is Online"
                        usernameTv.text = "Call ${user.userName}"
                        userNumber.text = ": ${user.contactNumber}"
                    }


                    "OFFLINE" -> {

                        if (user.isCforMeAcc) {
                            videoCallBtn.isVisible = true
                            audioCallBtn.isVisible = false
                            cardViewVideoCallClick.isEnabled = false
                            statusTv.setTextColor(context.resources.getColor(R.color.red, null))
                            statusTv.text = "${user.userName} is Offline"
                            usernameTv.text = "You cant connect ${user.userName}"
                            userNumber.text = ": ${user.contactNumber}"
                        }else{

                            videoCallBtn.isVisible = true
                            audioCallBtn.isVisible = false
                            cardViewVideoCallClick.isEnabled = false
                            statusTv.setTextColor(context.resources.getColor(R.color.blue_dark, null))
                            statusTv.text = "${user.userName} is Unregister"
                            usernameTv.text = "Invite ${user.userName}"
                            usernameTv.setTextColor(Color.parseColor("#bdbdbd"))
                            usernameTv.setOnClickListener {
                                inviteGuideUser(user)
                            }
                            userNumber.text = ": ${user.contactNumber}"
                        }
                    }

                    "IN_CALL" -> {
                       //00.................................................................................................................................................. mp3Player.stopMP3()
                        videoCallBtn.isVisible = true
                        audioCallBtn.isVisible = false
                        statusTv.setTextColor(context.resources.getColor(R.color.yellow, null))
                        statusTv.text = "${user.userName} is In call"
                        usernameTv.text = "Unable to call at the moment ${user.userName}"
                        userNumber.text = ": ${user.contactNumber}"
                    }
                }



                deleteContactBtn.setOnClickListener {
                    val deleteDialog = Dialog(context)
                    deleteDialog.setContentView(R.layout.deletelayout)
                    //deleteDialog.window!!.setLayout(WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT)
                    val noBtn = deleteDialog.findViewById<Button>(R.id.noId)
                    val yesBtn = deleteDialog.findViewById<Button>(R.id.yesId)
                    deleteDialog.show()
                    yesBtn.setOnClickListener {
                        Log.d(TAG, "deleteContacts:ContactNumber: ${user.contactNumber.toString()}\t,UserName${user.userName.toString()}")
                        deleteContacts(
                            deleteDialog,
                            user.contactNumber,
                            user.userName,
                            listener,
                            pos
                        )
                    }
                    noBtn.setOnClickListener { deleteDialog.dismiss() }

                }

            }
        }






        private fun inviteGuideUser(user: ContactInfo) {
            try {
                val shareIntent = Intent(Intent.ACTION_SEND)
                shareIntent.setType("text/plain")
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "HearSight App")
                var shareMessage = "\n${user.userName} recommend you this application\n\n"
                shareMessage = shareMessage + "https://drive.google.com/file/d/1VoXKYUIVVPHr-VZGbQaXUyr721VN1TXk/view?usp=sharing" + BuildConfig.APPLICATION_ID + "\n\n"
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage)
                context.startActivity(Intent.createChooser(shareIntent, "choose one"))
            } catch (e: Exception) {
                //e.toString();
            }
        }



        private fun deleteContacts(
            deleteDialog: Dialog,
            contactNumber: String,
            userName: String,
            listener: Listener,
            pos: Int
        ) {
            val registerNumber = FirebaseClient.registerNumber
            Log.d(
                TAG,
                "deleteContacts:ContactNumber: ${contactNumber}\t,UserName${userName.toString()}\t\tRegisterNumber${registerNumber}"
            )
            val ref = FirebaseDatabase.getInstance().reference
            val hastContactRef =
                ref.child(registerNumber).child("contacts").child(contactNumber)
            hastContactRef.removeValue().addOnSuccessListener {

                Toast.makeText(
                    context,
                    "Contact ${userName} deleted Successfully",
                    Toast.LENGTH_SHORT
                ).show()
                val mainRecyclerViewAdapter = MainRecyclerViewAdapter(listener)
                mainRecyclerViewAdapter.notifyItemChanged(pos)
                deleteDialog.dismiss()

            }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        context,
                        "Failed to remove host contact: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }


    }

}







