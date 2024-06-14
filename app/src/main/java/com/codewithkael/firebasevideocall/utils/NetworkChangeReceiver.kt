package com.codewithkael.firebasevideocall.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.SharedPreferences.Editor
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkInfo
import android.net.NetworkRequest
import android.os.Build
import android.util.Log
import android.widget.Toast
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader

class NetworkChangeReceiver constructor() :
    BroadcastReceiver() {

    private lateinit var connectivityManager: ConnectivityManager
    private lateinit var networkCallback: ConnectivityManager.NetworkCallback

    var isClose = false

    interface InetWorkChange {
        fun onNetworkAvailable(isAvailable: Boolean, type: String)
    }


    override fun onReceive(context: Context?, intent: Intent?) {
        connectivityManager =
            context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        sharePref(context)
        newNetActivity(context)

    }


    @OptIn(DelicateCoroutinesApi::class)
    fun isNetworkConnected(callback: (Any) -> Unit) {
        GlobalScope.launch(Dispatchers.IO) {

            try {
                val process = Runtime.getRuntime().exec("/system/bin/ping -c 1 8.8.8.8")
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                val output = StringBuffer()
                var line: String?

                while (reader.readLine().also { line = it } != null) {
                    output.append(line)
                }

                reader.close()
                process.waitFor()
               // process.exitValue() == 0
                callback(process.exitValue() == 0)
            } catch (e: Exception) {
                e.printStackTrace()
                //false
                callback(false)
            }

        }

    }

    private fun newNetActivity(context: Context) {
        var i = 0
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
            .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
            .build()
        networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                super.onAvailable(network)

                isNetworkConnected {
                    Log.d("NetworkChangeReceiver", "onReceive: ping =$it isClose=${getData("isClose", Boolean::class.java) as Boolean}")
                    if (it == true) {
                        if (getData("isClose", Boolean::class.java) as Boolean){

                            isClose = false
                            inetWorkChange?.onNetworkAvailable(
                                true,
                                "Internet_Available"
                            )
                            putData("isClose", false)
                        }

                    } else {

                        putData("isClose", true)
                        isClose = true
                        Log.d("NetworkChangeReceiver", "else---Network is lost isclose =$isClose")

                    }

                }

            }

            override fun onLost(network: Network) {

                //onNetworkAvailable(false)
                Log.d("NetworkChangeReceiver", "Network is lost")
                putData("isClose", true)
                isClose = true

            }

            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                super.onCapabilitiesChanged(network, networkCapabilities)

                val hasInternetCapability = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                Log.d("NetworkChangeReceiver", "onCapabilitiesChanged: $hasInternetCapability")
            }
        }
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
    }




    companion object {
        var inetWorkChange: InetWorkChange? = null
        lateinit var sharedPref: SharedPreferences
        lateinit var shEdit: Editor
        fun sharePref(context: Context) {
            sharedPref = context.getSharedPreferences("see_for_me", Context.MODE_PRIVATE)
            shEdit = sharedPref.edit()

        }

        fun <T> getData(key: String, type: Class<T>): Any? {

            return when (type) {
                Boolean::class.java -> sharedPref.getBoolean(key, false)
                String::class.java -> sharedPref.getString(key, "")
                Float::class.java -> sharedPref.getFloat(key, 0.0f)
                else -> null
            }

        }

        fun putData(key: String, value: Any) {
            when (value) {
                is Int -> {
                    shEdit.putInt(key, value)
                }

                is Boolean -> {
                    shEdit.putBoolean(key, value)
                }

                is String -> {
                    shEdit.putString(key, value)
                }
            }

            shEdit.apply()
        }

        fun scheduleNetworkCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(context, NetworkChangeReceiver::class.java).let { intent ->
                PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
            }

            // Schedule the alarm to trigger on network change
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis(),
                1000L,
                alarmIntent
            )
        }

        fun register(
            context: Context,
            onNetworkAvailable: (Boolean) -> Unit
        ): NetworkChangeReceiver {
            val receiver = NetworkChangeReceiver()
            val filter = IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION)
            context.registerReceiver(receiver, filter)
            return receiver
        }

        fun unregister(context: Context, receiver: NetworkChangeReceiver) {
            context.unregisterReceiver(receiver)
        }

        fun registerNetworkCallback(
            context: Context,
            networkCallback: ConnectivityManager.NetworkCallback
        ) {

            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        }

        fun unregisterNetworkCallback(
            context: Context,
            networkCallback: ConnectivityManager.NetworkCallback,
            connectivityManager: ConnectivityManager
        ) {
            // connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }

    }
}



/*    private fun isConnectedToHotspotWithoutInternet(
        context: Context,
        network: Network
    ): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            // val network = connectivityManager.activeNetwork ?: return false
            val networkCapabilities =
                connectivityManager.getNetworkCapabilities(network) ?: return false
            val isWifiConnected =
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val hasInternet =
                networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            Log.d(
                "NetworkChangeReceiver",
                "isConnectedToHotspotWithoutInternet: isWifiConnected = $isWifiConnected || " +
                        " hasInternet =$hasInternet"
            )
            return when {
                isWifiConnected && hasInternet -> {
                    Log.d(
                        "NetworkChangeReceiver",
                        "isConnectedToHotspotWithoutInternet:\"Connected to Wi-Fi with internet access\" "
                    )
                    return true
                }

                isWifiConnected && !hasInternet -> {

                    Log.d(
                        "NetworkChangeReceiver",
                        "isConnectedToHotspotWithoutInternet: \"Connected to Wi-Fi without internet access\" "
                    )
                    return false
                }

                else -> {
                    Log.d(
                        "NetworkChangeReceiver",
                        "isConnectedToHotspotWithoutInternet:  \"Not connected to Wi-Fi\""
                    )
                    return false
                }
            }
        } else {

            @Suppress("DEPRECATION")
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.type == ConnectivityManager.TYPE_WIFI && !networkInfo.isConnectedOrConnecting
        }
    }*/