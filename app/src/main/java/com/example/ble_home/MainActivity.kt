package com.example.ble_home

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import io.objectbox.BoxStore
import java.util.*

class MainActivity : AppCompatActivity() {

    private val REQUEST_ENABLE_BT = 1
    private val PERMISSIONS_REQUEST_CODE = 100
    private val SCAN_PERIOD: Long = 10000 // 10 seconds

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private var scanning = false
    private val handler = Handler(Looper.getMainLooper())

    private lateinit var listDevices: ListView
    private lateinit var deviceListAdapter: ArrayAdapter<String>
    private val devices = mutableListOf<BluetoothDevice>()

    private var bluetoothGatt: BluetoothGatt? = null
    private lateinit var tvData: TextView
    private lateinit var tvStatus: TextView // TextView для отображения статуса
    private lateinit var btnSend: Button

    //lateinit var boxStore: BoxStore

    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI
        val btnScan: Button = findViewById(R.id.btnScan)
        listDevices = findViewById(R.id.listDevices)
        tvData = findViewById(R.id.tvData)
        tvStatus = findViewById(R.id.tvStatus) // Инициализация TextView для статуса
        btnSend = findViewById(R.id.btnSend)

        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listDevices.adapter = deviceListAdapter

        btnSend.setOnClickListener { sendMessage() }

        // Проверка поддержки BLE
        if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show()
            finish()
        }

        // Инициализация BluetoothAdapter
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        // Проверка включенного Bluetooth
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Запрос разрешений
        checkPermissions()

        // Сканирование устройств
        btnScan.setOnClickListener {
            if (!scanning) {
                startScan()
            } else {
                stopScan()
            }
        }

        // Подключение к устройству
        listDevices.setOnItemClickListener { _, _, position, _ ->
            val device = devices[position]
            connectToDevice(device)
        }

    }

    private fun checkPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun startScan() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BLE", "BLUETOOTH_SCAN permission not granted")
            return
        }

        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        val scanFilter = ScanFilter.Builder().build()
        val scanFilters = listOf(scanFilter)

        bluetoothLeScanner.startScan(scanFilters, scanSettings, scanCallback)
        scanning = true
        Log.d("BLE", "Scanning started")

        handler.postDelayed({ stopScan() }, SCAN_PERIOD)
    }

    private fun stopScan() {
        if (scanning) {
            try {
                bluetoothLeScanner.stopScan(scanCallback)
                scanning = false
                Log.d("BLE", "Scanning stopped")
            } catch (e: SecurityException) {
                Log.e("BLE", "SecurityException: ${e.message}")
            }
        } else {
            Log.d("BLE", "No active scan to stop")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val device = result.device
            if (!devices.contains(device)) {
                devices.add(device)
                deviceListAdapter.add("${device.name ?: "Unknown Device"}\n${device.address}")
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("BLE", "All permissions granted")
                startScan()
            } else {
                Log.e("BLE", "Permissions denied")
                Toast.makeText(this, "Permissions are required for Bluetooth scanning", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            runOnUiThread {
                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        tvStatus.text = "Status: Connected"
                        Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show()
                        bluetoothGatt?.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        tvStatus.text = "Status: Disconnected"
                        Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                if (service != null) {
                    val characteristic = service.getCharacteristic(CHARACTERISTIC_UUID)
                    if (characteristic != null) {
                        gatt.setCharacteristicNotification(characteristic, true)
                        readCharacteristic(characteristic)
                    }
                }
            }
        }

        override fun onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val data = characteristic.value.toString(Charsets.UTF_8)
                runOnUiThread { tvData.text = "Received Data: $data" }
            }
        }
    }

    private fun sendMessage() {
        if (bluetoothGatt == null) {
            Toast.makeText(this, "Not connected!", Toast.LENGTH_SHORT).show()
            return
        }

        val service = bluetoothGatt?.getService(SERVICE_UUID)
        val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)

        if (characteristic != null) {
            val message = "Hello from Android!".toByteArray(Charsets.UTF_8)
            characteristic.value = message
            bluetoothGatt?.writeCharacteristic(characteristic)
            Toast.makeText(this, "Message sent!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Characteristic not found!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        bluetoothGatt?.readCharacteristic(characteristic)
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }
}