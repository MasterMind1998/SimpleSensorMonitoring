package com.example.simplesensormonitoring

import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.os.AsyncTask
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlinx.android.synthetic.main.control_activity_layout.*
import java.io.IOException
import java.util.*


class ControlActivity : AppCompatActivity() {

    companion object {
        var m_myUUID : UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb")
        var m_bluetoothSocket : BluetoothSocket? = null
        lateinit var m_progress : ProgressDialog
        lateinit var m_bluetoothAdapter : BluetoothAdapter
        var m_isConnect : Boolean = false
        lateinit var m_address : String


        private var mTxtReceive : TextView? = null
        lateinit var scrollView : ScrollView


    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.control_activity_layout)
        title = "Sensor Monitoring"

        m_address = intent.getStringExtra(SelectDeviceActivity.EXTRA_ADDRESS)!!
        ConnectToDevice(this).execute()

        mTxtReceive = findViewById(R.id.txtReceive)

        scrollView = findViewById(R.id.viewScroll)


        btn_startMonitoring.setOnClickListener { receiveData() }
        btn_clearInput.setOnClickListener { with(mTxtReceive) { this!!.text = "" } }
        btn_disconnect.setOnClickListener { disconnect() }



    }



    // Receive data from remote bluetooth device
     private fun receiveData() {

         val handler = Handler()  /*
         A Handler gives you a mechanism to push tasks into this queue from
         any other threads thus allowing other threads to communicate with the UI thread.
         However, it doesn't mean that the Handler supports only the main thread.
         It can push tasks to any thread's queue that has a Looper.
         */
         var stopWorker = false
         var readMessage = ""
         val buffer = ByteArray(1024)

         val workerThread = Thread {
             while (!Thread.currentThread().isInterrupted && !stopWorker) {
                 try {
                     val bytes = m_bluetoothSocket!!.inputStream.read(buffer)
                     if (bytes > 0) {
                         readMessage += String(buffer, 0, bytes)
                         handler.post {
                             mTxtReceive!!.append(readMessage)
                             scrollView.post(Runnable
                             { scrollView.fullScroll(View.FOCUS_DOWN) })
                         }
                     }
                     else {
                         Toast.makeText(this , "bytes is less than zero" , Toast.LENGTH_SHORT).show()
                     }
                 } catch (ex: IOException) {
                     stopWorker = true

                 }
             }
         }
         workerThread.start()

     }

    /*
    /* With this function(sendCommand) we can send command to the Bluetooth
    *and turn light on or off
    * or any command for the arduino control.
    * in this project I don't use this function but I leave this here for
    * anyone that want to learn how to send command to the bluetooth*/

    private fun sendCommand(input : String){
        if (m_bluetoothSocket != null){
            try {
                m_bluetoothSocket!!.outputStream.write(input.toByteArray())

            } catch (ex :IOException) {
                ex.printStackTrace()
            }
        }
    }

     */

    private fun disconnect(){
        if (m_bluetoothSocket!=null){
            try {
                m_bluetoothSocket!!.close()
                m_bluetoothSocket = null
                m_isConnect = false
                super.onBackPressed()
            } catch (ex : IOException){
                ex.printStackTrace()
            }
        }
    }

    private class ConnectToDevice(c : Context): AsyncTask<Void, Void, String>() {

        private var connectSuccess : Boolean = true
        private val context : Context

        init {
            this.context = c

        }

        override fun onPreExecute() {
            super.onPreExecute()
            m_progress = ProgressDialog.show(context ,"Connecting...", "Please wait")
        }

        override fun doInBackground(vararg p0: Void?): String? {
            try {
                if (m_bluetoothSocket == null || !m_isConnect){
                    m_bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
                    val device : BluetoothDevice = m_bluetoothAdapter.getRemoteDevice(m_address)
                    m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery()
                    m_bluetoothSocket!!.connect()
                }
            }catch (ex : IOException){
                connectSuccess = false
                ex.printStackTrace()
            }
            return null
        }

        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (!connectSuccess){
                Log.i("data" , "Couldn't Connect")
            } else {
                m_isConnect = true
            }
            m_progress.dismiss()
        }
    }
}


