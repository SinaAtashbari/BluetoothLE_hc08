package com.example.bluetoothle_hc08

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import com.example.bluetoothle_hc08.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    var m_BluetoothAdapter: BluetoothAdapter?=null
    val BLUETOOTHCONNECT_REQUEST_CODE = 1
    val LOCATION_REQUEST_CODE = 2
    val BLUETOOTH_ENABLE_REQUEST = 3
    val BLUETOOTHSCANN_REQUEST_CODE = 4

    private val scanSettings = ScanSettings.Builder()
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
        .build()

    private val SCAN_PERIOD: Long = 10000 // Scan for 10 seconds
    private var scanning = false

    val foundDeviceNames: ArrayList<String> = ArrayList()
    val foundDeviceAddress: ArrayList<BluetoothDevice> = ArrayList()

    lateinit var le_list:ListView


    companion object {
        val EXTRA_ADDRESS:String = "Device_Address"
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        le_list = findViewById(R.id.device_list);


        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        m_BluetoothAdapter = bluetoothManager.adapter
        btConnect()

        binding.ref.setOnClickListener(){
            if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED)
                if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED )
                    if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
                        if(m_BluetoothAdapter!!.isEnabled)
                            startScan()

        }



    }

    private val scanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            with(result.device) {
                if(checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    if( name!=null && name.contains("KF")) ///filtering LE_Devises by name
                    {
                        Log.i("ScanCallback", "Found BLE device!,Name: ${name ?: "Unnamed"} ,address: $address")
                        if(!foundDeviceNames.contains(name)){
                            foundDeviceAddress.add(result.device)
                            foundDeviceNames.add(name);
                            updateList()
                            le_list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                                val device: BluetoothDevice = foundDeviceAddress[position]
                                val address: String = device.address
                                stopScan()
                                val intent = Intent(this@MainActivity , MainActivity2::class.java)
                                intent.putExtra(EXTRA_ADDRESS,address)
                                startActivity(intent)

                            }
                        }
                    }
                }
            }

        }

    }

    private fun updateList(){
        val adapter = ArrayAdapter(this,android.R.layout.simple_list_item_1,foundDeviceNames)
        le_list.adapter = adapter
    }

    private fun startScan() {

        // Start LE scan
        scanning = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), BLUETOOTHSCANN_REQUEST_CODE)
            }else{
                m_BluetoothAdapter?.bluetoothLeScanner?.startScan(null,scanSettings,scanCallback)
            }
        }
        else{
            m_BluetoothAdapter?.bluetoothLeScanner?.startScan(null,scanSettings,scanCallback)
        }

        // Stop scan after a specified period
        Handler().postDelayed({
            if (scanning) {
                stopScan()
            }
        }, SCAN_PERIOD)
    }

    private fun stopScan() {

        // Stop LE scan
        scanning = false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_SCAN), BLUETOOTHSCANN_REQUEST_CODE)
            }else{
                m_BluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            }
        }
        else{
            m_BluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    private fun turnOn_BT(){
        if(!m_BluetoothAdapter!!.isEnabled) {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                val enableBluetoothIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBluetoothIntent,BLUETOOTH_ENABLE_REQUEST)
            }

        }else {
            startScan()
            return
        }
    }

    private fun btConnect() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        {
            if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_CONNECT), BLUETOOTHCONNECT_REQUEST_CODE)
            else
            {
                location()
                return
            }

        }
        else
        {
            location()
            return
        }

    }

    private fun location() {

        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_REQUEST_CODE)
        else
        {
            turnOn_BT()
            return
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode==BLUETOOTH_ENABLE_REQUEST){
            if(resultCode== Activity.RESULT_OK){
                if(m_BluetoothAdapter!!.isEnabled){
                    Toast.makeText(this, "Bluetooth has been Enabled", Toast.LENGTH_SHORT).show()
                    startScan()
                }else {
                    Toast.makeText(this, "Bluetooth has been Disabled", Toast.LENGTH_SHORT).show()
                }
            }else if (resultCode== Activity.RESULT_CANCELED){
                Toast.makeText(this, "Bluetooth Enable has been Canceled", Toast.LENGTH_SHORT).show()

            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == BLUETOOTHCONNECT_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "permission granted", Toast.LENGTH_SHORT).show()
                location()
            }
        }

        if(requestCode == LOCATION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Location permission granted", Toast.LENGTH_SHORT).show()
                turnOn_BT()
            }
        }

        if(requestCode == BLUETOOTHSCANN_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Scan permission granted", Toast.LENGTH_SHORT).show()
            }
        }


    }

}