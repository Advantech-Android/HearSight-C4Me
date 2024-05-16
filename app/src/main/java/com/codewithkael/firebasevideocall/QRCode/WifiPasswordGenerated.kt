package com.codewithkael.firebasevideocall.QRCode

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActionBar
import android.app.Activity
import android.app.Dialog
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.codewithkael.firebasevideocall.R
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder


private const val TAG = "==>>WifiPassGenerat"

class WifiPasswordGenerated(private val context: Context)
{


    private var wifiManager: WifiManager? =null
    fun showQRDialog()
    {
        wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val wifiDialog = Dialog(context)
        wifiDialog.setContentView(R.layout.wifi_ui)
        wifiDialog.window!!.setLayout(ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT)
        val wifieEditxt=wifiDialog.findViewById(R.id.wifiname) as TextInputEditText
        val wifiePassword=wifiDialog.findViewById(R.id.wifipassword) as TextInputEditText
        val mImage=wifiDialog.findViewById(R.id.idIVQrcode) as ImageView
        val createBtn=wifiDialog.findViewById(R.id.generateBtn) as Button
        val wifiListView=wifiDialog.findViewById<ListView>(R.id.wifiLV)
        val refresh=wifiDialog.findViewById<TextView>(R.id.refresh)
        val createNewid=wifiDialog.findViewById<TextView>(R.id.createNewid)

        wifiDialog.show()
        val availableWifilistContainer=wifiDialog.findViewById<ImageView>(R.id.availableWifilistContainer) as LinearLayout
        val qrCodeGenerateContainer=wifiDialog.findViewById<ImageView>(R.id.qrCodeGenerateContainer) as LinearLayout

        wifiListShow(wifiListView,createBtn,wifieEditxt,wifiePassword,availableWifilistContainer,qrCodeGenerateContainer,mImage)
        refresh.setOnClickListener {
            Toast.makeText(context, "Scanning...", Toast.LENGTH_SHORT).show()
           wifiListShow(wifiListView, createBtn, wifieEditxt, wifiePassword, availableWifilistContainer, qrCodeGenerateContainer, mImage)
        }

        createNewid.setOnClickListener {
            availableWifilistContainer.visibility=View.GONE
            qrCodeGenerateContainer.visibility=View.VISIBLE
            createBtn.setOnClickListener {
                val wifiName=wifiePassword.text.toString()
                val mpassword=wifiePassword.text.toString()
                if (wifiName.isNotEmpty()&&mpassword.isNotEmpty())
                {
                        val combinedData: String = wifieEditxt.text.toString()+"|"+mpassword
                        generateQRCodeBitmap(combinedData,mImage)
                }
                else{
                    Toast.makeText(context, "Field missing", Toast.LENGTH_SHORT).show()
                }
            }

        }
    }

    private fun wifiListShow(
        wifiListView: ListView,
        createBtn: Button,
        wifieEditxt: TextInputEditText,
        wifiePassword: TextInputEditText,
        availableWifilistContainer: LinearLayout,
        qrCodeGenerateContainer: LinearLayout,
        mImage: ImageView
    ) {

        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1022)
            return
        }
        val wifiScanReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                try {
                    val success = intent.getBooleanExtra(WifiManager.EXTRA_RESULTS_UPDATED, false)
                    if (success) {
                        scanSuccess(wifiListView,createBtn,wifieEditxt,wifiePassword,availableWifilistContainer,qrCodeGenerateContainer,mImage)

                    } else {
                        scanFailure(wifiListView,createBtn,wifieEditxt,wifiePassword,availableWifilistContainer,qrCodeGenerateContainer,mImage)
                    }
                }catch (e:WriterException){
                    e.printStackTrace()
                }

            }
        }
        val intentFilter = IntentFilter()
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        context.registerReceiver(wifiScanReceiver, intentFilter)

        // Start Wi-Fi scan
        wifiManager?.startScan()

    }



    @SuppressLint("MissingPermission")
    private fun scanSuccess(
        wifiListView: ListView,
        createBtn: Button,
        wifieEditxt: TextInputEditText,
        wifiePassword: TextInputEditText,
        availableWifilistContainer: LinearLayout,
        qrCodeGenerateContainer: LinearLayout,
        mImage: ImageView
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(context as Activity, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 555)
            } else {
                var message="wifi not available"
                val ssidList = mutableListOf<String>()
                for (i in wifiManager!!.scanResults.indices) {
                    //ssidList.clear()
                    val ssid = wifiManager!!.scanResults[i].SSID
                    ssidList.add(ssid?:message)
                }

                val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, ssidList)
                wifiListView.adapter = adapter
                wifiListView.setOnItemClickListener { parent, view, position, id ->
                    availableWifilistContainer.visibility=View.GONE
                    qrCodeGenerateContainer.visibility=View.VISIBLE
                    wifieEditxt.setText(ssidList[position])

                    createBtn.setOnClickListener {
                        if (wifieEditxt.text.toString().isNotEmpty())
                        {
                            val mpassword=wifiePassword.text.toString()
                            if (mpassword.isNotEmpty())
                            {
                                val combinedData: String = wifieEditxt.text.toString()+"|"+mpassword
                                generateQRCodeBitmap(combinedData,mImage)
                            }else{
                                Toast.makeText(context, "Wifi password missing", Toast.LENGTH_SHORT).show()
                            }

                        }
                        else{
                            Toast.makeText(context, "Wifi name missing", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
                Log.d(TAG, "scanSuccess: SSID $ssidList")
                ssidList.toTypedArray()
            }
        }
    }



    @SuppressLint("MissingPermission")
    private fun scanFailure(
        wifiListView: ListView,
        createBtn: Button,
        wifieEditxt: TextInputEditText,
        wifiePassword: TextInputEditText,
        availableWifilistContainer: LinearLayout,
        qrCodeGenerateContainer: LinearLayout,
        mImage: ImageView
    ){
       if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)

       else {
           var message="Wifi not available"
           val ssidList = mutableListOf<String>()
           for (i in wifiManager!!.scanResults.indices) {
               //ssidList.clear()
               val ssid = wifiManager!!.scanResults[i].SSID
               ssidList.add(ssid?:message)
           }
           val adapter = ArrayAdapter(context, android.R.layout.simple_list_item_1, ssidList)
           wifiListView.adapter = adapter
           wifiListView.setOnItemClickListener { parent, view, position, id ->
               availableWifilistContainer.visibility=View.GONE
               qrCodeGenerateContainer.visibility=View.VISIBLE
               wifieEditxt.setText(ssidList[position])
               createBtn.setOnClickListener {
                   if (wifieEditxt.text.toString().isNotEmpty())
                   {
                       val mpassword=wifiePassword.text.toString()
                       if (mpassword.isNotEmpty())
                       {
                           val combinedData: String = wifieEditxt.text.toString()+"|"+mpassword
                           generateQRCodeBitmap(combinedData,mImage)
                       }else{
                           Toast.makeText(context, "Wifi password missing", Toast.LENGTH_SHORT).show()
                       }
                   }
                   else{
                       Toast.makeText(context, "Wifi name missing", Toast.LENGTH_SHORT).show()
                   }
               }
           }
           Log.d(TAG, "scanSuccess: SSID $ssidList")
           ssidList.toTypedArray()
       }

    }

    private fun generateQRCodeBitmap(data: String, mImage: ImageView) {
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 500, 500)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            mImage.setImageBitmap(bitmap)

        } catch (e: WriterException) {
            e.printStackTrace()
        }finally {

        }
    }}