package com.codewithkael.firebasevideocall.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.databinding.ItemMainRecyclerViewBinding
import com.codewithkael.firebasevideocall.utils.ContactInfo
import com.codewithkael.firebasevideocall.utils.FirebaseFieldNames
import com.codewithkael.firebasevideocall.utils.UserStatus

private const val TAG = "====>>MainRecycViewAdap"

class MainRecyclerViewAdapter(private val listener: Listener) :
    RecyclerView.Adapter<MainRecyclerViewAdapter.MainRecyclerViewHolder>() {

    private var innerContactList: List<ContactInfo>? = null
    private var onlineContactList:List<ContactInfo>?=null
    private var finalModelLis:List<Pair<String,String>>?=null
    fun updateList(list:List<ContactInfo>, onlineList:List<ContactInfo> ) {
        this.innerContactList =list
        this.onlineContactList=onlineList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewHolder {
        val binding = ItemMainRecyclerViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MainRecyclerViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return onlineContactList?.size ?: 0
    }

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        Log.d(TAG, "Size\t: ${onlineContactList!!.size}")
        onlineContactList?.let { list ->
            val user = list[position]
            holder.bind(user, {
                Log.d(TAG, "onBindViewHolder: onVideoCallClicked")
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

    class MainRecyclerViewHolder(private val binding: ItemMainRecyclerViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context

        fun bind(
            user: ContactInfo,
            videoCallClicked: (String) -> Unit,
            audioCallClicked: (String) -> Unit
        ) {
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

                usernameTv.text = user.contactNumber
                userNumber.text = user.userName
            }


        }

    }
}