package com.example.bluetoothle_hc08

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import java.util.UUID

class MainActivity2 : AppCompatActivity() {


    var m_address:String ? =null
    var bluetoothAdapter: BluetoothAdapter? = null
    var bluetoothGatt: BluetoothGatt? = null
    var characteristic: BluetoothGattCharacteristic? = null
    val SERIAL_SERVICE_UUID = UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB")
    val CHAR_UUID = UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB")
    val CLIENT_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)


        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        m_address = intent.getStringExtra(MainActivity.EXTRA_ADDRESS)
        val device = bluetoothAdapter?.getRemoteDevice(m_address)

        if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
            bluetoothGatt = device?.connectGatt(this, true, bluetoothGattCallback)  ///Connect to Selected Device
        }


    }

    private val bluetoothGattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                    gatt.discoverServices()   ///after establishing connection we have to perform a serviceDiscovery to find the required Services and characteristics
                    gatt.requestMtu(200) ///Mtu stands for Maximum transmission unit amd it defines the largest size of the packet that can be transmitted
                }

            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            characteristic = gatt.getService(SERIAL_SERVICE_UUID).getCharacteristic(CHAR_UUID)
            enableNotification(characteristic) ///we have to enable characteristic Notification inorder to Received data from it
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            val buffer: ByteArray = characteristic.value    ///read the Received data

            if (buffer[0]==0xaa.toByte() && buffer[1]==0xbb.toByte() ){
                characteristic.setValue("Data received") /// preparing data to send
                writeCharacteristic(characteristic)      ///send data
            }
        }
    }

        fun writeCharacteristic(characteristic: BluetoothGattCharacteristic?) {
        if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED){
            bluetoothGatt!!.writeCharacteristic(characteristic)

        }
    }

    fun enableNotification(characteristic: BluetoothGattCharacteristic?){

        if(checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED){
            if (bluetoothGatt!!.setCharacteristicNotification(characteristic, true)) {
                val descriptor = characteristic!!.getDescriptor(CLIENT_UUID)
                val data = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                if (descriptor.setValue(data)) {
                    bluetoothGatt!!.writeDescriptor(descriptor)
                }
            }
        }

    }
}