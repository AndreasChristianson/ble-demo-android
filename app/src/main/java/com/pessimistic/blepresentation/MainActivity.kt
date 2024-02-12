package com.pessimistic.blepresentation

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothAdapter.LeScanCallback
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.security.Permission
import java.security.Permissions
import java.util.UUID

class MainActivity : ComponentActivity() {
    val BL_PERMISSION_REQUEST = 1
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    App()
                }
            }
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("ble demo", "${it.key} = ${it.value}")
            }
        }
    private var requestBluetooth = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            Log.i("ble demo", "bluetooth turned on")
        }else{
            Log.w("ble demo", "bluetooth not started")
        }
    }

    @Composable
    fun App() {
        var status by remember { mutableStateOf("Disconnected") }
        var tankControllerService = BluetoothGattService(UUID.fromString("E6B81F14-F9E5-40C9-A739-4DE4564264D1"), BluetoothGattService.SERVICE_TYPE_PRIMARY)
//        tankControllerService.addCharacteristic(BluetoothGattCharacteristic())
        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                super.onScanResult(callbackType, result)
                Log.i("ble demo", "result: ${result?.device?.address}")
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                status= "Scan failed"
            }
        }

        fun connect() {
            status = "starting"
            // make sure we have ble
            if (!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
                Toast.makeText(this, "BLE not supported", Toast.LENGTH_SHORT).show()
                finish()
            }

            //make sure the bluetooth manager is non-null
            val manager = ContextCompat.getSystemService(
                this.applicationContext,
                BluetoothManager::class.java
            )!!


            // request bluetooth permissions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                requestMultiplePermissions.launch(arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.ACCESS_FINE_LOCATION
                    ))
            } else{
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                requestBluetooth.launch(enableBtIntent)
            }

            manager.adapter.bluetoothLeScanner.startScan(callback)
            status = "Scanning"
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row {
                Button(onClick = { connect() }) {
                    Text(text = "Connect")
                }
                Text(text = "status: $status")
            }
            Row(modifier = Modifier
                    .fillMaxSize()
                    .wrapContentSize(Alignment.Center)
            ) {
                Column {
                    Button(onClick = {
                        Log.i("ble demo", "left")
                    }) {
                        Text("Left")
                    }
                }
                Column {
                    Button(onClick = {
                        Log.i("ble demo", "forwards")
                    }) {
                        Text("Forwards")
                    }
                    Button(onClick = {
                        Log.i("ble demo", "stop")
                    }) {
                        Text("Stop")
                    }
                    Button(onClick = {
                        Log.i("ble demo", "backwards")
                    }) {
                        Text("Backwards")
                    }
                }
                Column {
                    Button(onClick = {
                        Log.i("ble demo", "right")
                    }) {
                        Text("Right")
                    }
                }
            }
        }
    }
}


//@Preview(showBackground = true)
//@Composable
//fun Preview() {
//    MaterialTheme {
//        App()
//    }
//}