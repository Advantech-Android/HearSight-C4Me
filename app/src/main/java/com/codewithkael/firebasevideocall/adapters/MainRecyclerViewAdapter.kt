package com.codewithkael.firebasevideocall.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.codewithkael.firebasevideocall.R
import com.codewithkael.firebasevideocall.databinding.ItemMainRecyclerViewBinding
import com.codewithkael.firebasevideocall.utils.UserStatus

private const val TAG = "====>>MainRecycViewAdap"

class MainRecyclerViewAdapter(private val listener: Listener) :
    RecyclerView.Adapter<MainRecyclerViewAdapter.MainRecyclerViewHolder>() {

    private var usersList: List<Pair<String, String>>? = null
    private var usersList1: List<Pair<String, String>>? = null
    private var password = ""
    fun updateList(list: List<Pair<String, String>>, list1: List<Pair<String, String>>) {
        this.usersList = list
        this.usersList1 = list1
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewHolder {
        val binding = ItemMainRecyclerViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MainRecyclerViewHolder(binding)
    }

    override fun getItemCount(): Int {
        return usersList?.size ?: 0
    }

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        usersList?.let { list ->
            usersList1?.let { l1 ->
                val user = list[position]
                val user1 = l1[position]
                holder.bind(user, user1, {
                    Log.d(TAG, "onBindViewHolder: onVideoCallClicked")
                    listener.onVideoCallClicked(it)
                }, {
                    Log.d(TAG, "onBindViewHolder: onAudioCallClicked")
                    listener.onAudioCallClicked(it)
                })
            }
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
            user: Pair<String, String>,
            user1: Pair<String, String>,
            videoCallClicked: (String) -> Unit,
            audioCallClicked: (String) -> Unit
        ) {
            binding.apply {
                when (user.second) {
                    "ONLINE" -> {
                        videoCallBtn.isVisible = true
                        audioCallBtn.isVisible = true

                        videoCallBtn.setOnClickListener {
                            videoCallClicked.invoke(user.first)
                        }
                        audioCallBtn.setOnClickListener {
                            audioCallClicked.invoke(user.first)
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

                usernameTv.text = user.first
                userNumber.text = user1.second
            }


        }

    }
}