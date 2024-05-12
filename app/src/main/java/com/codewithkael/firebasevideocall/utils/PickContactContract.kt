package com.codewithkael.firebasevideocall.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.provider.ContactsContract
import androidx.activity.result.contract.ActivityResultContract

class PickContactContract(private val context: Context) : ActivityResultContract<Intent, Pair<String, String>?>()
{
    override fun createIntent(context: Context, input: Intent): Intent {

        return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
    }


    override fun parseResult(resultCode: Int, intent: Intent?): Pair<String, String>? {
        if (resultCode == Activity.RESULT_OK && intent != null) {
            val cursor = context.contentResolver.query(
                intent.data!!,
                arrayOf(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME, ContactsContract.CommonDataKinds.Phone.NUMBER),
                null,
                null,
                null
            )
            cursor?.use {
                if (it.moveToFirst())
                {
                    val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                    val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                    val name = it.getString(nameIndex)
                    val number = it.getString(numberIndex)
                    return Pair(name, number)
                }
            }
        }
        return null
    }
}