package cl.jacevedo.droidcontroller.droidController

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import cl.jacevedo.droidcontroller.AUDIO_BUTTONS
import cl.jacevedo.droidcontroller.DebugLog
import cl.jacevedo.droidcontroller.TAG
import cl.jacevedo.droidcontroller.communication.ButtonActions
import cl.jacevedo.droidcontroller.communication.ConnectionStatus
import cl.jacevedo.droidcontroller.communication.MovementObject
import cl.jacevedo.droidcontroller.communication.Settings
import cl.jacevedo.droidcontroller.connection.DroidBluetoothManager
import cl.jacevedo.droidinterfaces.ButtonDroidEntity
import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

val CONTROLLER_TO_DROID_ACTIONS = mapOf(
    KeyEvent.KEYCODE_BUTTON_Y to "y",
    KeyEvent.KEYCODE_BUTTON_X to "x",
    KeyEvent.KEYCODE_BUTTON_A to "a",
    KeyEvent.KEYCODE_BUTTON_B to "b",
    KeyEvent.KEYCODE_BUTTON_L1 to "l1",
    KeyEvent.KEYCODE_BUTTON_L2 to "l2",
    KeyEvent.KEYCODE_BUTTON_R1 to "r1",
    KeyEvent.KEYCODE_BUTTON_R2 to "r2",
    KeyEvent.KEYCODE_BUTTON_THUMBL to "al",
    KeyEvent.KEYCODE_BUTTON_THUMBR to "ar",
    KeyEvent.KEYCODE_BUTTON_START to "st",
    KeyEvent.KEYCODE_BUTTON_SELECT to "se",
    KeyEvent.KEYCODE_DPAD_UP to "du",
    KeyEvent.KEYCODE_DPAD_LEFT to "dl",
    KeyEvent.KEYCODE_DPAD_RIGHT to "dr",
    KeyEvent.KEYCODE_DPAD_DOWN to "dd",
    )

class DroidController {
    private val listButtons = mutableMapOf<String, Boolean>()
    private val lastButtonSentTime = mutableMapOf<String, Long>()
    private val BUTTON_DEBOUNCE_MS = 400L
    private var droidObject : DroidObject? = null
    private val dpadMotion = DpadMotion()
    private var previousButton: Int = KeyEvent.KEYCODE_DPAD_CENTER
    private var movementObject = MovementObject(0,0,0,0,0,0)
    private var isActive = false
    var droidBluetoothManager : DroidBluetoothManager ? = null
    var viviMode: Boolean = true
    private var sendingJob: Job? = null

    fun onPause(){
        DebugLog.e(TAG, "calling on pause")
        movementObject = MovementObject(0,0,0,0,0,0)
        droidObject?.let { setDroidInfo(it) }
        droidBluetoothManager?.sendDroidObjectMessage(movementObject.movementObject())
    }

    fun setDroidInfo(droidInfo: DroidObject){
        droidObject = droidInfo
        droidBluetoothManager?.droidObject = droidInfo
    }

    private fun startSendingWhileActive() {
        if(isActive){
            return
        }
        isActive = true
        sendingJob?.cancel()
        sendingJob = CoroutineScope(Dispatchers.IO).launch{
            while (isActive) {
                DebugLog.d(TAG, "Sending movement: $movementObject")
                droidBluetoothManager?.sendDroidObjectMessage(movementObject.movementObject())
                if (movementObject.isAllZero()) {
                    isActive = false
                    break
                }
                delay(100) // adjust frequency as needed
            }
        }
    }

    fun buttonPress(keyCode: Int, event: KeyEvent?) : Boolean{
        if (event != null && event.source and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD ) {
            return pressButtonAction(keyCode)
        }
        return true
    }
    fun buttonRelease(keyCode: Int, event: KeyEvent?) : Boolean{
        releaseButtonAction(keyCode)
        return true
    }
    fun motionEvent(event: MotionEvent?) : Boolean{
        val press: Int = dpadMotion.getDirectionPressed(event)
        if(press != DpadMotion.CENTER){
            pressButtonAction(press)
            previousButton = press
            return false
        }
        if(previousButton == DpadMotion.CENTER){
            genericMotionEvent(event)
            return true
        }
        // Hat-switch dpad returned to center -- the direction that was
        // held has no separate key-up event, so release it here.
        releaseButtonAction(previousButton)
        previousButton = press
        return true
    }
    fun appButtonPress(buttonDroidEntity: ButtonDroidEntity) {
        val connectionStatus = if(buttonDroidEntity.buttonType == AUDIO_BUTTONS){
            ConnectionStatus.AUDIO.value
        } else {
            ConnectionStatus.BUTTONS.value
        }
        buttonDroidEntity.macro?.let {
            if(connectionStatus == ConnectionStatus.AUDIO.value){
                droidBluetoothManager?.sendDroidObjectMessage(ButtonActions(connectionStatus, arrayOf(), arrayOf(it.toInt())))
            } else {
                droidBluetoothManager?.sendDroidObjectMessage(ButtonActions(connectionStatus, arrayOf(it), arrayOf()))
            }
        }
    }

    private fun setPressButton(keyCode:Int, state:Boolean) : String {
        val buttonCode = getButtonString(keyCode)
        if(buttonCode != "") {
            listButtons[buttonCode] = state
        }
        return buttonCode
    }

    private fun pressButtonAction(keyCode:Int) : Boolean {
        val buttonString = getButtonString(keyCode)
        if(buttonString != "" && listButtons[buttonString] != true) {
            val now = System.currentTimeMillis()
            if (now - (lastButtonSentTime[buttonString] ?: 0L) < BUTTON_DEBOUNCE_MS) return false
            setPressButton(keyCode, true)
            lastButtonSentTime[buttonString] = now
            val arrayButtons = listButtons.filterValues { it }.keys.toTypedArray()
            val buttonActions = ButtonActions(ConnectionStatus.BUTTONS.value, arrayButtons, arrayOf())
            droidBluetoothManager?.sendDroidObjectMessage(buttonActions)
            return true
        }
        return false
    }

    // Grogu-specific: key up needs its own signal so the receiver can stop
    // continuous joint movement the instant a button is released, not just
    // ignore repeats while held (other droids don't use hold-to-move, so
    // this is scoped to grogu to avoid changing their existing behavior).
    private fun releaseButtonAction(keyCode: Int) {
        val buttonString = setPressButton(keyCode, false)
        if (buttonString == "" || droidObject?.type != DeviceType.GROGU) return
        val arrayButtons = listButtons.filterValues { it }.keys.toTypedArray()
        droidBluetoothManager?.sendDroidObjectMessage(
            ButtonActions(ConnectionStatus.BUTTONS.value, arrayButtons, arrayOf())
        )
    }
    private fun genericMotionEvent(event: MotionEvent?) {
        event?.let{
            if (event.source and InputDevice.SOURCE_GAMEPAD != InputDevice.SOURCE_GAMEPAD) {
                val leftMultiplier = if (viviMode) 50 else 100
                val lxAxis: Int = deltaValueOfAnalog((event.getAxisValue(MotionEvent.AXIS_X) * leftMultiplier).roundToInt())
                val lyAxis: Int = deltaValueOfAnalog((event.getAxisValue(MotionEvent.AXIS_Y) * leftMultiplier).roundToInt())
                val rxAxis: Int = deltaValueOfAnalog((event.getAxisValue(MotionEvent.AXIS_Z) * 100).roundToInt())
                val ryAxis: Int = deltaValueOfAnalog((event.getAxisValue(MotionEvent.AXIS_RZ) * 100).roundToInt())
                val buttonR2 = (event.getAxisValue(MotionEvent.AXIS_RTRIGGER) * 100).roundToInt()
                val buttonL2 = (event.getAxisValue(MotionEvent.AXIS_LTRIGGER) * 100).roundToInt()

                movementObject = MovementObject(lxAxis, lyAxis, rxAxis, ryAxis, buttonR2, buttonL2)
                startSendingWhileActive()
            }
        }
    }

    private fun deltaValueOfAnalog(value:Int) : Int {
        if(abs(value) <= 3){
            return 0
        }
        return value
    }

    private fun getButtonString(keyCode: Int) =
        CONTROLLER_TO_DROID_ACTIONS.getOrDefault(keyCode, "")

    fun setVolume(it: Float) {
        droidBluetoothManager?.sendDroidObjectMessage(Settings(ConnectionStatus.SETTINGS.value, it.toInt()))
    }

}