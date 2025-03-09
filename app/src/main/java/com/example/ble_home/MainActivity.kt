package com.example.ble_home

import android.Manifest
import android.bluetooth.*
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
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

    private val SERVICE_UUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val CHARACTERISTIC_UUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Инициализация UI
        val btnScan: Button = findViewById(R.id.btnScan)
        listDevices = findViewById(R.id.listDevices)
        tvData = findViewById(R.id.tvData)

        deviceListAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, mutableListOf())
        listDevices.adapter = deviceListAdapter

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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSIONS_REQUEST_CODE)
        }
    }

    private fun startScan() {
        Log.d("BLE", "Attempting to start scan")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                Log.e("BLE", "BLUETOOTH_SCAN permission not granted")
                return
            }
        }

        try {
            bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
            bluetoothLeScanner.startScan(scanCallback)
            scanning = true
            Log.d("BLE", "Scanning started")

            // Остановка сканирования через SCAN_PERIOD
            handler.postDelayed({ stopScan() }, SCAN_PERIOD)
        } catch (e: SecurityException) {
            Log.e("BLE", "SecurityException: ${e.message}")
        }
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

    private fun connectToDevice(device: BluetoothDevice) {
        bluetoothGatt = device.connectGatt(this, false, gattCallback)
    }

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Connected", Toast.LENGTH_SHORT).show() }
                bluetoothGatt?.discoverServices()
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                runOnUiThread { Toast.makeText(this@MainActivity, "Disconnected", Toast.LENGTH_SHORT).show() }
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