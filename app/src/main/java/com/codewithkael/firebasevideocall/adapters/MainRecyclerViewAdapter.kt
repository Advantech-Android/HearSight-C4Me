package com.codewithkael.firebasevideocall.adapters

import android.app.Dialog
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.databinding.ItemMainRecyclerViewBinding
import com.codewithkael.firebasevideocall.firebaseClient.FirebaseClient
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.google.firebase.database.FirebaseDatabase


private const val TAG = "====>>MainRecycViewAdap"

class MainRecyclerViewAdapter(private val listener: Listener) :
    RecyclerView.Adapter<MainRecyclerViewAdapter.MainRecyclerViewHolder>() {

    private var innerContactList: List<ContactInfo>? = null
    private var onlineContactList:List<ContactInfo>?=null
    fun updateList(list:List<ContactInfo>, onlineList:List<ContactInfo> ) {
        this.innerContactList =list
        this.onlineContactList=onlineList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewHolder {
        val binding = ItemMainRecyclerViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MainRecyclerViewHolder(binding,listener)
    }

    override fun getItemCount(): Int {
        return onlineContactList?.size ?: 0
    }

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        onlineContactList?.let { list ->
            val user = list[position]
            holder.bind(user,position,{
                Log.d(TAG, "onBindViewHolder: onVideoCallClicked $it")
                listener.onVideoCallClicked(it)
            }, {
                Log.d(TAG, "onBindViewHolder: onAudioCallClicked")
                listener.onAudioCallClicked(it)
            })
        }
    }

    interface Listener {
        fun onVideoCallClicked(username: String)
        fun onAudioCallClicked(username: String)
    }

    class MainRecyclerViewHolder(
        private val binding: ItemMainRecyclerViewBinding,private val listener: Listener) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context
        fun bind(user: ContactInfo,pos:Int, videoCallClicked: (String) -> Unit, audioCallClicked: (String) -> Unit)
        {
            Log.d(TAG, "bind: user:${user.userName} ,phone:${user.contactNumber},status:${user.status}")
            binding.apply {

                when (user.status) {
                    "ONLINE" -> {
                        videoCallBtn.isVisible = true
                        audioCallBtn.isVisible = true

                        videoCallBtn.setOnClickListener {
                            videoCallClicked.invoke(user.contactNumber)
                        }
                        audioCallBtn.setOnClickListener {
                            audioCallClicked.invoke(user.contactNumber)
                        }
                        statusTv.setTextColor(context.resources.getColor(R.color.light_green, null))
                        statusTv.text = "Online"
                    }

                    "OFFLINE" -> {
                        videoCallBtn.isVisible = false
                        audioCallBtn.isVisible = false
                        statusTv.setTextColor(context.resources.getColor(R.color.red, null))
                        statusTv.text = "Offline"
                    }

                    "IN_CALL" -> {
                        videoCallBtn.isVisible = false
                        audioCallBtn.isVisible = false
                        statusTv.setTextColor(context.resources.getColor(R.color.yellow, null))
                        statusTv.text = "In Call"
                    }
                }

                usernameTv.text = user.userName
                userNumber.text = user.contactNumber
            }


            binding.apply {
                statusTv.setOnClickListener {
                    val deleteDialog=Dialog(context)
                    deleteDialog.setContentView(R.layout.deletelayout)
                    deleteDialog.window!!.setLayout(WindowManager.LayoutParams.MATCH_PARENT,WindowManager.LayoutParams.WRAP_CONTENT)
                    val noBtn=deleteDialog.findViewById<Button>(R.id.noId)
                    val yesBtn=deleteDialog.findViewById<Button>(R.id.yesId)
                    deleteDialog.show()
                    yesBtn.setOnClickListener {
                        Log.d(TAG, "deleteContacts:ContactNumber: ${user.contactNumber.toString()}\t,UserName${user.userName.toString()}")
                        deleteContacts(deleteDialog,user.contactNumber,user.userName,listener,pos) }
                    noBtn.setOnClickListener { deleteDialog.dismiss() }

                }
            }
        }

        private fun deleteContacts(deleteDialog: Dialog, contactNumber: String, userName: String, listener: Listener,pos:Int) {
            val registerNumber=FirebaseClient.registerNumber
            Log.d(TAG, "deleteContacts:ContactNumber: ${contactNumber}\t,UserName${userName.toString()}\t\tRegisterNumber${registerNumber}")
            val ref = FirebaseDatabase.getInstance().reference
            val hastContactRef=ref.child(registerNumber).child("contacts").child(contactNumber)
            hastContactRef.removeValue().addOnSuccessListener {
                Toast.makeText(context, "Successfully removed ${userName} contact", Toast.LENGTH_SHORT).show()
                val mainRecyclerViewAdapter=MainRecyclerViewAdapter(listener)
                mainRecyclerViewAdapter.notifyItemChanged(pos)
                deleteDialog.dismiss()

            }
                .addOnFailureListener {exception->
                    Toast.makeText(context, "Failed to remove host contact: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
        }

    }

}