package com.codewithkael.firebasevideocall.utils

import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

import com.google.firebase.database.ValueEventListener

open class MyEventListener : ValueEventListener {
    override fun onDataChange(snapshot: DataSnapshot) {

    }

    override fun onCancelled(error: DatabaseError) {
    }
}

open class MyChildEventListener : ChildEventListener {


    override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onChildRemoved(snapshot: DataSnapshot) {

    }

    override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {

    }

    override fun onCancelled(error: DatabaseError) {
    }
}