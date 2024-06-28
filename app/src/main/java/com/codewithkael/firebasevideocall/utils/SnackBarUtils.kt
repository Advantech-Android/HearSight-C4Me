package com.codewithkael.firebasevideocall.utils

import android.view.View
import com.google.android.material.snackbar.Snackbar

object SnackBarUtils
{
    fun showSnackBar(view:View,message:String){
        Snackbar.make(view,message,Snackbar.LENGTH_LONG).show()
    }
}
object RecyclerViewFields//Snack bar not display
{
    const val CALL_CONNECTED = "Please wait while your call is being connected"
    const val REMOVED_CONTACT = "Successfully removed - contact"
    const val REMOVE_HOST_CONTACT = "Failed to removed host contact: -"
}
object WifiPassWordGeneratedField
{
    const val SCANNING="Scanning..."
    const val FILED_MIS="Field missing"
    const val WIFI_PW_MIS="Wifi password missing"
    const val WIFI_NAME_MIS="Wifi name missing"
}
object LoginActivityFields{
    val PASWORD_INVALID: String="Enter valid password"
    const val USERNAME_INVALID="Enter valid username "
    const val USERNAME_NOT_EMPTY="Contact name should not be empty"
    const val CHECK_NET_CONNECTION="Check your Internet Connection"
    const val UN_PW_INCORRECT="Username and Password incorrect.."
    const val PICKED_CONTACTNO_WRONG="The Picked contact number is wrong"
}
object MainActivityFields{
    const val AC_BOTH_USERNAME_PW="Enter both username and password"
    const val CONTACT_ADD_SUCCESS="Contact added Successfully"
    const val TURN_ON_WIFI="Please turn on Wifi"
}
object OTPFields{
    const val AUTH_FAIL="Authentication failed."
}
object WebQFields{
    const val PERMISSION_GRANTED="Permission granted to read storage"
    const val PERMISSION_DENIED="Permission denied to read storage"
}