package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Telephony
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private val smsList = ArrayList<String>()
    private var totalAmount = 0
    private lateinit var smsListView: ListView
    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 1000L // Refresh interval in milliseconds (e.g., 30 seconds)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        smsListView = findViewById(R.id.smsListView)
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, smsList)
        smsListView.adapter = adapter

        if (checkSmsPermission()) {
            readSms()
            startRefreshTimer()
        } else {
            requestSmsPermission()
        }
    }

    private fun checkSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestSmsPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_SMS), 1)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            readSms()
            startRefreshTimer()
        }
    }

    private fun readSms() {
        smsList.clear() // Clear the previous data
        totalAmount = 0 // Reset the total amount

        val cursor = contentResolver.query(Telephony.Sms.Inbox.CONTENT_URI, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                do {
                    val sender = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.ADDRESS))
                    val body = it.getString(it.getColumnIndexOrThrow(Telephony.Sms.Inbox.BODY))

                    val senderRegex = "Kotak Bank|HDFC Bank"
                    val amountRegex = "Rs\\. (\\d+)"

                    val senderPattern = Pattern.compile(senderRegex)
                    val amountPattern = Pattern.compile(amountRegex)

                    if (senderPattern.matcher(sender).matches()) {
                        val amountMatcher = amountPattern.matcher(body)
                        if (amountMatcher.find()) {
                            val amount = amountMatcher.group(1).toInt()
                            totalAmount += amount
                            val messageInfo = "Sender: $sender\nAmount: $amount"
                            smsList.add(messageInfo)
                        }
                    }
                } while (it.moveToNext())
            }
        }

        val totalMessage = "Total Amount: Rs. $totalAmount"
        smsList.add(totalMessage)
        (smsListView.adapter as ArrayAdapter<*>).notifyDataSetChanged()
    }

    private fun startRefreshTimer() {
        handler.postDelayed({
            readSms()
            startRefreshTimer() // Start the timer again for the next refresh
        }, refreshInterval)
    }
}
