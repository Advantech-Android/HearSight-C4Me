package com.codewithkael.firebasevideocall.QRCode

import android.app.ActionBar
import android.app.Dialog
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import com.codewithkael.firebasevideocall.R
import com.google.android.material.textfield.TextInputEditText
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.journeyapps.barcodescanner.BarcodeEncoder



class WifiPasswordGenerated(private val context:android.content.Context)
{


    fun showWifi()
    {
        val wifiDialog = Dialog(context)
        wifiDialog.setContentView(R.layout.wifi_ui)
        wifiDialog.window!!.setLayout(ActionBar.LayoutParams.WRAP_CONTENT, ActionBar.LayoutParams.WRAP_CONTENT)
        wifiDialog.show()
        val wifieEditxt=wifiDialog.findViewById(R.id.wifiname) as TextInputEditText
        val wifiePassword=wifiDialog.findViewById(R.id.wifipassword) as TextInputEditText
        val mImage=wifiDialog.findViewById(R.id.idIVQrcode) as ImageView
        val createBtn=wifiDialog.findViewById(R.id.generateBtn) as Button
        val editextFieldContainer=wifiDialog.findViewById<ImageView>(R.id.editextFieldContainer) as LinearLayout
        createBtn.setOnClickListener {
            var wname=wifieEditxt.text.toString()
            var wpass=wifiePassword.text.toString()
            if (wname.isNotEmpty()&&wpass.isNotEmpty())
            {
                val combinedData: String = wname +"|"+wpass
                generateQRCodeBitmap(combinedData,mImage,wifiDialog,editextFieldContainer)
            }
            else
                Toast.makeText(context, "Field is missing", Toast.LENGTH_SHORT).show()
        }

    }


    private fun generateQRCodeBitmap(data: String, mImage: ImageView, wifiDialog: Dialog, editextFieldContainer: LinearLayout) {
        val multiFormatWriter = MultiFormatWriter()
        try {
            val bitMatrix = multiFormatWriter.encode(data, BarcodeFormat.QR_CODE, 500, 500)
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.createBitmap(bitMatrix)
            mImage.setImageBitmap(bitmap)

        } catch (e: WriterException) {
            e.printStackTrace()
        }finally {
            // wifiDialog.dismiss()
        }
    }}