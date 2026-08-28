package cl.jacevedo.droidcontroller.connection

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Context
import android.content.IntentSender
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.startIntentSenderForResult
import androidx.core.content.getSystemService
import cl.jacevedo.droidcontroller.provider.Provider.Companion.getDroidObjects
import cl.jacevedo.droidcontroller.DebugLog
import cl.jacevedo.droidcontroller.TAG
import cl.jacevedo.droidcontroller.communication.ICommunication
import cl.jacevedo.droidcontroller.data.BluetoothDroidObject
import cl.jacevedo.droidcontroller.provider.DefaultProvider
import cl.jacevedo.droidinterfaces.DroidObject
import com.google.gson.Gson
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.regex.Pattern

const val SERVICE_UUID = "96e3f2cd-28cf-4d37-9b39-a291f917620e"
const val SELECT_DEVICE_REQUEST_CODE = 0
class DroidBluetoothManager {
    // Written on the GATT binder callback thread, read on the write-consumer
    // coroutine -- @Volatile so the consumer never sees a stale reference.
    @Volatile
    var bluetoothGatt : BluetoothGatt? = null
    var droidObject: DroidObject = DefaultProvider().provideDroidInformation()
    private val gson = Gson()

    // sendDroidObjectMessage() is called concurrently from different
    // threads (the movement loop on Dispatchers.IO, button press/release
    // handlers on whatever thread key events land on) -- writeCharacteristic()
    // calls racing each other on the same BluetoothGatt can silently drop
    // one (BLE stacks vary in how many outstanding WRITE_TYPE_NO_RESPONSE
    // writes they'll queue before returning an error that nothing here
    // checks for). Funneling every write through one channel + one
    // consumer coroutine makes them strictly sequential instead.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val writeChannel = Channel<String>(Channel.UNLIMITED)
    @Volatile
    private var writeLoopStarted = false

    // Started lazily on the first connection so a scan-only instance (see
    // MainActivity) never spins up an idle consumer. release() tears it down.
    private fun startWriteLoop() {
        if (writeLoopStarted) return
        writeLoopStarted = true
        scope.launch {
            for (json in writeChannel) {
                performWrite(json)
                delay(20)  // let the write actually flush before the next one
            }
        }
    }

     suspend fun scanningForDevice(context: Context, onDeviceConnected: (BluetoothDroidObject) -> Unit ):IntentSender?{
        // Name-pattern filter, not addServiceUuid(): CompanionDeviceManager's service-UUID
        // matching is unreliable for BLE-advertised (GAP scan record) UUIDs on many Android
        // versions, even when the UUID is confirmed correct in the advertisement (verified
        // with nRF Connect) — it was really built around classic Bluetooth SDP records.
        // All droid firmwares advertise with "Droid" somewhere in the name by convention
        // (e.g. "Droid babu", "Chopper Droid") — not necessarily as a prefix — so filter on
        // the word appearing anywhere instead of trying to match each individual registered
        // droid name.
        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder()
            .setNamePattern(Pattern.compile(".*\\bDroid\\b.*", Pattern.CASE_INSENSITIVE))
            .build()
        val deviceManager = context.getSystemService<CompanionDeviceManager>()
            ?: run {
                DebugLog.e(TAG, "CompanionDeviceManager not available")
                return null
            }
        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            .addDeviceFilter(deviceFilter)
            .setSingleDevice(false)
            .build()

        val result = CompletableDeferred<IntentSender>()
        val activity = context as? Activity
        val callback = object : CompanionDeviceManager.Callback() {
            // API 33+ path (this app's minSdk is 34). The pre-33 onDeviceFound()
            // override below is never invoked here, so the deferred must be
            // completed from this callback or scanningForDevice() would suspend
            // on result.await() forever and leak its calling coroutine.
            override fun onAssociationPending(intentSender: IntentSender) {
                val currentActivity = activity
                if (currentActivity == null) {
                    result.completeExceptionally(
                        IllegalStateException("scanningForDevice requires an Activity context")
                    )
                    return
                }
                startIntentSenderForResult(currentActivity, intentSender, SELECT_DEVICE_REQUEST_CODE, null, 0, 0, 0, null)
                result.complete(intentSender)
            }

            @Suppress("OVERRIDE_DEPRECATION")
            override fun onDeviceFound(intentSender: IntentSender) {
                result.complete(intentSender)
            }

            @SuppressLint("MissingPermission")
            override fun onAssociationCreated(associationInfo: AssociationInfo) {
                val associationId: Int = associationInfo.id
                val macAddress: String? = associationInfo.deviceMacAddress?.toString()
                val deviceName: String?= associationInfo.displayName?.toString()
                findCurrentDroid(deviceName)
                if(macAddress == null  || deviceName == null){
                    Toast.makeText(context, "Error getting mac address or device name", Toast.LENGTH_SHORT).show()
                } else {
                    associationInfo.associatedDevice?.bluetoothDevice?.createBond()
                    onDeviceConnected(BluetoothDroidObject(
                        associationId = associationId,
                        nameDevice = deviceName,
                        deviceType = droidObject.type,
                        macAddress = macAddress
                    ))
                }

            }

            override fun onFailure(errorMessage: CharSequence?) {
                DebugLog.e(TAG, "Device association failed: $errorMessage")
                result.completeExceptionally(RuntimeException(errorMessage?.toString() ?: "Unknown error"))
            }
        }
        deviceManager.associate(pairingRequest, callback, null)
        return result.await()
    }

    private fun findCurrentDroid(deviceName: String?) {
        deviceName?.let {
            val normalizedDeviceName = normalizeName(deviceName)
            getDroidObjects().forEach { currentDroid ->
                if(normalizedDeviceName.contains(normalizeName(currentDroid.name))){
                    droidObject = currentDroid
                }
            }
        }
    }

    // Strips separators so names compare equal regardless of spacing/casing style
    // (e.g. "Droid roger_roger" vs a registered name of "roger_roger" still matches
    // even if separator style ever drifts between the two).
    private fun normalizeName(name: String) = name.lowercase().replace(Regex("[\\s_-]+"), "")

    fun connectBluetoothDroid(context: Context,
                              bluetoothAddress : String?,
                              gattCallback: (newState: Int) -> Unit,
                              onCharacteristicMessage: (value:String) -> Unit){
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        startWriteLoop()
        // Release any previous client first: a pause/resume that skips onStop
        // would otherwise open a second GATT connection and leak the old one.
        closeGatt()
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        val device = bluetoothManager.adapter.bondedDevices.firstOrNull{bluetoothAddress != null && it.address.equals(bluetoothAddress, ignoreCase = true)}
        device?.connectGatt(context, false, object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                super.onServicesDiscovered(gatt, status)
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    DebugLog.e(TAG, "Service discovery failed: status=$status")
                    return
                }
                gatt?.readRemoteRssi()
                bluetoothGatt = gatt
                DebugLog.e(TAG, "characteristic ${droidObject.characteristic}")
                bluetoothGatt?.getService(UUID.fromString(SERVICE_UUID))?.getCharacteristic(UUID.fromString(droidObject.characteristic))?.let{
                    DebugLog.e(TAG, "characteristic found")
                    bluetoothGatt?.setCharacteristicNotification(it, true)
                }

            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                onCharacteristicMessage(value.decodeToString())
                super.onCharacteristicChanged(gatt, characteristic, value)
            }

            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                DebugLog.d(TAG, "Gatt status: $status, newState: $newState")
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    DebugLog.e(TAG, "GATT error status=$status; closing connection")
                    gatt?.close()
                    bluetoothGatt = null
                    gattCallback(BluetoothGatt.STATE_DISCONNECTED)
                    super.onConnectionStateChange(gatt, status, newState)
                    return
                }
                if (newState == BluetoothGatt.STATE_CONNECTED) {
                    DebugLog.d(TAG, "Connected")
                    bluetoothGatt = gatt
                    bluetoothGatt?.requestMtu(128)
                    bluetoothGatt?.discoverServices()
                } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                    DebugLog.d(TAG, "Disconnected")
                    gatt?.close()
                    bluetoothGatt = null
                }
                gattCallback(newState)
                super.onConnectionStateChange(gatt, status, newState)
            }

            override fun onCharacteristicRead(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray,
                status: Int
            ) {
               DebugLog.e(TAG, "Read" + value.decodeToString())
            }

        })
    }

    fun sendDroidObjectMessage(buttonActions : ICommunication){
        writeChannel.trySend(gson.toJson(buttonActions))
    }

    @SuppressLint("MissingPermission")
    private fun performWrite(jsonMessage: String) {
        bluetoothGatt?.run {
            val service : BluetoothGattService? = getService(UUID.fromString(SERVICE_UUID))
            val characteristicString = droidObject.characteristic
            val characteristic : BluetoothGattCharacteristic? =
                service?.getCharacteristic(UUID.fromString(characteristicString))
            if (characteristic == null) {
                DebugLog.e(TAG, "characteristic $characteristicString not found on service $SERVICE_UUID; dropping write")
                return@run
            }
            writeCharacteristic(characteristic, jsonMessage.toByteArray(), BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE)
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
    }

    // BluetoothGatt.close() carries no permission requirement, unlike disconnect().
    private fun closeGatt() {
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    // Call when the owner is done with this manager (e.g. Activity.onDestroy):
    // closes the GATT client and stops the write-consumer coroutine so neither
    // outlives the caller.
    fun release() {
        closeGatt()
        writeChannel.close()
        scope.cancel()
    }
}
