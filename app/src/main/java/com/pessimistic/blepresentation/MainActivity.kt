package com.pessimistic.blepresentation

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.ParcelUuid
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import java.util.Arrays
import java.util.UUID


private const val SERVICE_UUID = "E6B81F14-F9E5-40C9-A739-4DE4564264D1"
private const val LEFT_TRACK_UUID = "82CA4F75-6FC8-48C0-8652-06F4595ADF20"
private const val RIGHT_TRACK_UUID = "82CA4F75-6FC8-48C0-8652-06F4595ADF21"
private const val LEFT_TRACK_DESCRIPTOR_UUID = "82CA4F75-6FC8-48C0-8652-06F4595ADF22"
private const val RIGHT_TRACK_DESCRIPTOR_UUID = "82CA4F75-6FC8-48C0-8652-06F4595ADF23"

@SuppressLint("MissingPermission")
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
class MainActivity : ComponentActivity() {
    private fun log(text: String) {
        Log.i("ble demo", text)
    }

    // system service and its kin
    private val btManager: BluetoothManager by lazy {
        ContextCompat.getSystemService(
            this.applicationContext,
            BluetoothManager::class.java
        )!!
    }
    private val btAdapter: BluetoothAdapter by lazy {
        btManager.adapter
    }
    private val bleAdvertiser by lazy {
        btAdapter.bluetoothLeAdvertiser
    }

    // setter for the status label on the activity
    private var setStatus: (String) -> Unit = { }

    // setup layout and gather status setter
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val setter = App(this)
                    setStatus = {
                        runOnUiThread {
                            setter(it)
                        }
                    }
                }
            }
        }
    }

    // permissions
    private fun requestBtPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADVERTISE
                )
            )
        } else {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            requestBluetooth.launch(enableBtIntent)
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                log("${it.key} = ${it.value}")
            }
        }
    private var requestBluetooth =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                log("bluetooth turned on")
            } else {
                log("bluetooth not started")
            }
        }

    // create gatt table
    private fun createTankControllerGattService(): BluetoothGattService {
        val tankControllerGattService = BluetoothGattService(
            UUID.fromString(SERVICE_UUID),
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )
        val leftTrackGattCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(LEFT_TRACK_UUID),
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        leftTrackGattCharacteristic.addDescriptor(
            BluetoothGattDescriptor(
                UUID.fromString(LEFT_TRACK_DESCRIPTOR_UUID),
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
        )
        tankControllerGattService.addCharacteristic(leftTrackGattCharacteristic)
        val rightTrackGattCharacteristic = BluetoothGattCharacteristic(
            UUID.fromString(RIGHT_TRACK_UUID),
            BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_READ
        )
        rightTrackGattCharacteristic.addDescriptor(
            BluetoothGattDescriptor(
                UUID.fromString(RIGHT_TRACK_DESCRIPTOR_UUID),
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
        )
        tankControllerGattService.addCharacteristic(rightTrackGattCharacteristic)

        return tankControllerGattService;
    }

    //advertising methods
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            setStatus("Advertising")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            log("Error starting to advertise: $errorCode")
        }
    }
    //settings for advertising
    private val advertiseSettings = AdvertiseSettings.Builder()
//        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
//        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .setConnectable(true)
        .build()
    private val advertiseData = AdvertiseData.Builder()
        .setIncludeDeviceName(false) // very limited data size, don't include anything you don't have to
        .addServiceUuid(ParcelUuid(UUID.fromString(SERVICE_UUID)))
        .build()
    private fun bleStartAdvertising() {
        bleStartGattServer()
        bleAdvertiser.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
    }
    private fun bleStopAdvertising() {
        bleStopGattServer()
        bleAdvertiser.stopAdvertising(advertiseCallback)
    }

    //gatt server stuff
    private var gattServer: BluetoothGattServer? = null
    private val leftTrackChar
        get() = gattServer
            ?.getService(UUID.fromString(SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(LEFT_TRACK_UUID))
    private val rightTrackChar
        get() = gattServer
            ?.getService(UUID.fromString(SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(RIGHT_TRACK_UUID))
    private val leftTrackSubscribedDevices = mutableSetOf<BluetoothDevice>()
    private val rightTrackSubscribedDevices = mutableSetOf<BluetoothDevice>()

    private fun bleStartGattServer() {
        val gattServer = btManager.openGattServer(this, btGattServerCallback)
        setStatus("gattServer open")
        val result = gattServer.addService(createTankControllerGattService())
        this.gattServer = gattServer
        val ret = when (result) {
            true -> "OK"
            false -> "fail"
        }
        log("addService $ret")
    }

    private fun bleStopGattServer() {
        for (device in btManager.getDevicesMatchingConnectionStates(
            BluetoothProfile.GATT,
            intArrayOf(BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_CONNECTING)
        )) {
            gattServer?.cancelConnection(device)
        }
        gattServer?.close()
        gattServer = null
        log("gattServer closed")
        setStatus("gattServer closed")
    }

    //handle all the gatt server interactions
    private val btGattServerCallback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                log("Central did connect: ${device.address}")
                setStatus("central connected")
                gattServer?.connect(device, false)// required if you want to cancel the connection later.
            } else {
                log("Central did disconnect: ${device.address}")
                leftTrackSubscribedDevices.remove(device)
                rightTrackSubscribedDevices.remove(device)
            }
        }

        override fun onNotificationSent(device: BluetoothDevice, status: Int) {
            log("notification sent. Status: $status")
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            log("Characteristic Read. offset: $offset. uuid: ${characteristic.uuid}")
            when (characteristic.uuid) {
                UUID.fromString(LEFT_TRACK_UUID) -> {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        leftPower.toByteArray()
                    )
                    log("Characteristic Read - left track. response value=${leftPower.toByteArray()}")
                }

                UUID.fromString(RIGHT_TRACK_UUID) -> {
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        leftPower.toByteArray()
                    )
                    log("Characteristic Read - right track. response value=${leftPower.toByteArray()}")
                }

                else -> {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                    log("Characteristic Read - unknown uuid")
                }
            }
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray?
        ) {
            log("Characteristic Write. offset: $offset. uuid: ${characteristic.uuid}")
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
        }

        override fun onDescriptorReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            descriptor: BluetoothGattDescriptor
        ) {
            log("Descriptor Read. offset: $offset. uuid: ${descriptor.uuid}")
            when (descriptor.uuid) {
                UUID.fromString(LEFT_TRACK_DESCRIPTOR_UUID) -> {
                    val returnValue = if (leftTrackSubscribedDevices.contains(device)) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }
                    log("Descriptor Read. toggling notifications on left")
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue
                    )
                }

                UUID.fromString(RIGHT_TRACK_DESCRIPTOR_UUID) -> {
                    val returnValue = if (rightTrackSubscribedDevices.contains(device)) {
                        BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    } else {
                        BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                    }
                    log("Descriptor Read. toggling notifications on right")
                    gattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        returnValue
                    )
                }

                else -> {
                    gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, 0, null)
                }
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            log("Descriptor Write. offset: $offset. uuid: ${descriptor.uuid}")
            when (descriptor.uuid) {
                UUID.fromString(LEFT_TRACK_DESCRIPTOR_UUID) -> {
                    if (Arrays.equals(
                            value,
                            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    )) {
                        leftTrackSubscribedDevices.add(device)
                        log("Descriptor Write. added left subscriber")
                    } else if (Arrays.equals(
                            value,
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        )
                    ) {
                        leftTrackSubscribedDevices.remove(device)
                        log("Descriptor Write. added left subscriber")
                    }
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null
                        )
                    }
                }

                UUID.fromString(RIGHT_TRACK_DESCRIPTOR_UUID) -> {
                    if (Arrays.equals(
                            value,
                            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                    )) {
                        rightTrackSubscribedDevices.add(device)
                        log("Descriptor Write. added right subscriber")
                    } else if (Arrays.equals(
                            value,
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        )
                    ) {
                        rightTrackSubscribedDevices.remove(device)
                        log("Descriptor Write. added right subscriber")
                    }
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null
                        )
                    }
                }

                else -> {
                    log("Descriptor Write. unknown descriptor")
                    if (responseNeeded) {
                        gattServer?.sendResponse(
                            device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null
                        )
                    }
                }
            }
        }
    }

    fun connect() {
        requestBtPermissions()
        bleStartAdvertising()
    }

    fun closeAll() {
        bleStopAdvertising()
    }

    var leftPower: Int = 0
        set(value) {
            val oldValue = field
            field = value
            if (oldValue != value) {
                sendCharacteristicChangedIndicator(
                    value,
                    leftTrackChar,
                    leftTrackSubscribedDevices
                )
            }
        }
    var rightPower: Int = 0
        set(value) {
            val oldValue = field
            field = value
            if (oldValue != value) {
                sendCharacteristicChangedIndicator(
                    value,
                    rightTrackChar,
                    rightTrackSubscribedDevices
                )
            }
        }

    fun setTracks(l: Int, r: Int) {
        leftPower = l
        rightPower = r
    }

    private fun sendCharacteristicChangedIndicator(
        value: Int,
        characteristic: BluetoothGattCharacteristic?,
        devices: Set<BluetoothDevice>
    ) {
        val data = value.toByteArray()
        log("sending indication for new value: $data")
        for (device in devices) {
            log("sending indication to ${device.address}")
            characteristic?.let {
                gattServer?.notifyCharacteristicChanged(device, it, true, data)
            }
        }
    }
}

private fun Int.toByteArray(): ByteArray {
    val buffer = ByteArray(4)
    buffer[0] = (this shr 0).toByte()
    buffer[1] = (this shr 8).toByte()
    buffer[2] = (this shr 16).toByte()
    buffer[3] = (this shr 24).toByte()
    return buffer;
}