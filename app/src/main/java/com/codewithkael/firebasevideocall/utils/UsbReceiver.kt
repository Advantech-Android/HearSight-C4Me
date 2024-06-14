package com.codewithkael.firebasevideocall.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.util.Log
import android.widget.Toast

class UsbReceiver : BroadcastReceiver() {
    private  val TAG = "xxUsbReceiver"
    val intenAct by lazy{Intent()}
    override fun onReceive(context: Context, intent: Intent) {

        val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)

        if (device != null) {
            Log.d(TAG,"USB device attached:device= ${device?.deviceName} action=${intent.action}")
            when (intent.action) {

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    Log.d(TAG,"ACTION_USB_DEVICE_ATTACHED: ${device?.deviceName}")
                    intenAct.putExtra("connected", true)
                    launchCameraApp(context)

                }
                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device: UsbDevice? = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
                    Log.d(TAG,"ACTION_USB_DEVICE_DETACHED : ${device?.deviceName}")
                    intenAct.putExtra("connected", false)
                }
                "android.hardware.usb.action.USB_STATE" -> {

                    val connected = intenAct.getBooleanExtra("connected", false)
                    if (connected) {
                        launchCameraApp(context)
                        Log.d(TAG, "USB connected")
                        // Handle USB connected
                    } else {
                        Log.d(TAG, "USB disconnected")
                        // Handle USB disconnected
                    }
                }
            }
        }

    }

    private fun isUvcDevice(device: UsbDevice): Boolean {
        return device.deviceClass == UsbConstants.USB_CLASS_VIDEO
    }

    private fun launchCameraApp(context: Context) {
        val launchIntent = context.packageManager.getLaunchIntentForPackage("com.codewithkael.firebasevideocall")
        if (launchIntent != null) {

           /* launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_MULTIPLE_TASK)*/
            //launchIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(launchIntent)
        } else {
            Toast.makeText(context, "Camera app not found", Toast.LENGTH_SHORT).show()
        }
    }
}