package cl.jacevedo.droidcontroller.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import cl.jacevedo.droidinterfaces.ButtonDroidEntity

class DroidActivityViewModel: ViewModel() {
    val viviMode: MutableLiveData<Boolean> = MutableLiveData(true)
    var supportsViviMode: Boolean = true
    val droidVolume : MutableLiveData<Float> by lazy{
        MutableLiveData<Float>(25f)
    }
    val macroButtonsList : MutableLiveData<MutableList<ButtonDroidEntity>> by lazy {
        MutableLiveData<MutableList<ButtonDroidEntity>>()
    }
    val audioButtonsList : MutableLiveData<MutableList<ButtonDroidEntity>> by lazy {
        MutableLiveData<MutableList<ButtonDroidEntity>>()
    }

    var testMode: Boolean = false
    val currentTestAction: MutableLiveData<ButtonDroidEntity?> = MutableLiveData(null)
    private val testQueue = ArrayDeque<ButtonDroidEntity>()

    fun startTestSequence(allButtons: List<ButtonDroidEntity>) {
        testQueue.clear()
        testQueue.addAll(allButtons)
        showNextTestAction()
    }

    fun showNextTestAction() {
        currentTestAction.value = testQueue.removeFirstOrNull()
    }

    fun cancelTestSequence() {
        testQueue.clear()
        currentTestAction.value = null
    }
}