package cl.jacevedo.droidcontroller

import android.bluetooth.BluetoothGatt
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import cl.jacevedo.droidcontroller.connection.DroidBluetoothManager
import cl.jacevedo.droidcontroller.droidController.DroidController
import cl.jacevedo.droidcontroller.data.BluetoothDroidObject
import cl.jacevedo.droidcontroller.provider.Provider
import cl.jacevedo.droidinterfaces.ButtonDroidEntity
import cl.jacevedo.droidcontroller.ui.theme.ChopperControllerTheme
import cl.jacevedo.droidcontroller.view.DroidActivityView
import cl.jacevedo.droidcontroller.viewmodel.DroidActivityViewModel
import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidcontroller.TEST_MODE_EXTRA
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

const val MACRO_BUTTONS = 0
const val AUDIO_BUTTONS = 1

class DroidActivity : ComponentActivity() {
    private val droidActivityViewModel : DroidActivityViewModel by viewModels()
    private val droidController = DroidController()
    private val bluetoothDeviceManager = DroidBluetoothManager()
    private val droidObject by lazy {
        intent.getParcelableExtra(SELECTED_DROID, BluetoothDroidObject::class.java)
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        droidActivityViewModel.testMode = intent.getBooleanExtra(TEST_MODE_EXTRA, false)
        getDataFromDatabase(droidType = droidObject?.deviceType ?: DeviceType.CHOPPER)
        droidActivityViewModel.supportsViviMode = Provider.getDroidObjects()
            .firstOrNull { it.type == droidObject?.deviceType }
            ?.supportsViviMode ?: true
        droidActivityViewModel.viviMode.observe(this) { isViviMode ->
            droidController.viviMode = isViviMode
        }
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        setContent {
            ChopperControllerTheme {
                DroidActivityView(droidActivityViewModel, droidObject,{
                    droidController.appButtonPress(it)
                }, {
                    droidController.setVolume(it)
                })
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        if(!hasFocus){
            droidController.onPause()
        }
        super.onWindowFocusChanged(hasFocus)
    }

    override fun onPause() {
        super.onPause()
        droidController.onPause()
    }

    override fun onStop() {
        droidController.onPause()
        bluetoothDeviceManager.disconnect()
        super.onStop()
    }

    override fun onDestroy() {
        // Releases the GATT client and stops the write-consumer coroutine so
        // neither leaks past the Activity.
        bluetoothDeviceManager.release()
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        bluetoothDeviceManager.connectBluetoothDroid(this, droidObject?.macAddress,{state ->
            val textToast = when(state){
                BluetoothGatt.STATE_CONNECTED -> {
                    "Droid Connected"
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    "Droid Disconnected"
                }
                BluetoothGatt.STATE_CONNECTING -> {
                    "STATE_CONNECTING"
                }
                BluetoothGatt.STATE_DISCONNECTING ->{
                    "Disconnect"
                }
                else -> {
                    ""
                }
            }
            runOnUiThread {
                Toast.makeText(baseContext, textToast, Toast.LENGTH_SHORT).show()
                if(state == BluetoothGatt.STATE_DISCONNECTED) {
                    droidController.onPause()
                    bluetoothDeviceManager.disconnect()
                    finish()
                }
            }
            if(state == BluetoothGatt.STATE_CONNECTED) {
                droidController.droidBluetoothManager = bluetoothDeviceManager
                Provider.getDroidObjects().firstOrNull { it.type == droidObject?.deviceType }?.let {
                    droidController.setDroidInfo(it)
                }
                if (droidActivityViewModel.testMode) {
                    val controllerButtons = Provider.getTestButtons(droidObject?.deviceType ?: DeviceType.CHOPPER)
                    if (controllerButtons.isNullOrEmpty()) {
                        runOnUiThread {
                            Toast.makeText(baseContext, "no test mode set up", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        val firstAudio = listOfNotNull(droidActivityViewModel.audioButtonsList.value?.firstOrNull())
                        runOnUiThread {
                            droidActivityViewModel.startTestSequence(controllerButtons + firstAudio)
                        }
                    }
                }
            } else {
                droidController.droidBluetoothManager = null
            }
        }, { message ->
            DebugLog.e(TAG, "Message from device $message")
        })
    }

    override fun onGenericMotionEvent(event: MotionEvent?): Boolean {
        return droidController.motionEvent(event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if(event?.keyCode == KeyEvent.KEYCODE_BACK){
            return super.onKeyUp(keyCode, event)
        }
        return droidController.buttonRelease(keyCode, event)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(event?.keyCode == KeyEvent.KEYCODE_BACK){
            return super.onKeyDown(keyCode, event)
        }
        return droidController.buttonPress(keyCode, event)
    }

    private fun getDataFromDatabase(droidType: DeviceType){
        droidObject?.let{ currentDroidObject ->
            droidActivityViewModel.macroButtonsList.value =
                readButtonsFromXml(currentDroidObject, droidType, MACRO_BUTTONS)
            droidActivityViewModel.audioButtonsList.value =
                readButtonsFromXml(currentDroidObject, droidType, AUDIO_BUTTONS)
        }
    }

    private fun readButtonsFromXml(
        bluetoothDroidObject: BluetoothDroidObject,
        droidType: DeviceType,
        macroType: Int
    ):MutableList<ButtonDroidEntity>{
        val droidTypeResource = if(macroType == MACRO_BUTTONS){
            Provider.getMacroFile(droidType)
        } else {
            Provider.getAudioFile(droidType)
        }
        val str = resources.openRawResource(droidTypeResource).bufferedReader().use { it.readText() }
        val itemType = object : TypeToken<List<ButtonDroidEntity>>() {}.type
        val listItems = Gson().fromJson(str, itemType) ?: emptyList<ButtonDroidEntity>()
        listItems.forEach { it.buttonType = macroType }
        listItems.forEach { it.deviceId = bluetoothDroidObject.associationId }
        return listItems.toMutableList()
    }
}
