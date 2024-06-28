package com.codewithkael.firebasevideocall.utils

import android.app.Application
import android.content.IntentFilter
import android.hardware.usb.UsbManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application(){
    lateinit var usbReceiver: UsbReceiver
    override fun onCreate() {
        super.onCreate()
      /*  usbReceiver = UsbReceiver()
        val filter = IntentFilter()
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        filter.addAction("android.hardware.usb.action.USB_STATE")
        registerReceiver(usbReceiver, filter)*/
    }

    override fun onTerminate() {
        super.onTerminate()
      //  unregisterReceiver(usbReceiver)
    }
}