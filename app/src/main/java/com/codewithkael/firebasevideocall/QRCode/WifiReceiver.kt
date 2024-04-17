package com.codewithkael.firebasevideocall.QRCode

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.core.app.ActivityCompat

private const val TAG = "==>>WifiReceiver"
class WifiReceiver(private val wifiManager:WifiManager,private val wifiDevicelist:ListView):BroadcastReceiver() {


    var stringBuilder: StringBuilder? =null
    var wifideviceList: ListView? =null
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: ")
        val action =intent!!.action
        try {
            if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION==action)
            {
                stringBuilder= java.lang.StringBuilder()
                if (ActivityCompat.checkSelfPermission(
                        context!!,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    Toast.makeText(context!!, "fine location not granted", Toast.LENGTH_SHORT).show()
                }
                var wifiList:List<ScanResult> = wifiManager.scanResults
                Log.d(TAG, "onReceive: $wifiList")
                var deviceList=ArrayList<String>()
                for (scanResults in wifiList)
                {
                    Toast.makeText(context!!.applicationContext, "${wifiList.size.toString()}", Toast.LENGTH_SHORT).show()
                    Log.d("__devicelist", "onReceive: ${wifiList.size}")
                    stringBuilder!!.append("\n").append(scanResults.SSID).append("_").append(scanResults.capabilities)
                    deviceList.add(scanResults.SSID.toString()+"-"+scanResults.capabilities)
                    val arrayAdapter:ArrayAdapter<*> = ArrayAdapter(context!!.applicationContext,android.R.layout.simple_list_item_1,deviceList.toArray())
                    wifideviceList!!.adapter=arrayAdapter
                }
            }
        }catch (e:Exception)
        {
            e.printStackTrace()
        }
    }

    init {
        this.wifideviceList=wifiDevicelist
    }
}