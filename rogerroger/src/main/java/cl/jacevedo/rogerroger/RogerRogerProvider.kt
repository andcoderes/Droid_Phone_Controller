package cl.jacevedo.rogerroger

import cl.jacevedo.droidinterfaces.ButtonDroidEntity
import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import cl.jacevedo.droidinterfaces.DroidProvider

class RogerRogerProvider : DroidProvider {
    override fun provideDroidInformation() = DroidObject(
        name = droidName,
        characteristic = CHARACTERISTIC,
        type = DeviceType.ROGER_ROGER,
        supportsViviMode = false
    )
    override fun provideDeviceType() = DeviceType.ROGER_ROGER
    override fun provideButtonImage() = R.drawable.roger_roger
    override fun provideListMacros() = R.raw.macro_rogerroger
    override fun provideListAudios() = R.raw.audio_rogerroger

    // buttonType 0 = MACRO_BUTTONS (app module's constant - can't import it here
    // since app depends on this module, not the other way around).
    // Keys match roger_roger firmware's PololuController placeholder table
    // (src/controllers/PololuController.cpp in the roger_roger project) —
    // replace both sides together once real Maestro scripts are authored.
    override fun provideTestButtons(): List<ButtonDroidEntity> = listOf(
        ButtonDroidEntity(0, null, 0, "L1", "l1"),
        ButtonDroidEntity(0, null, 0, "R1", "r1"),
        ButtonDroidEntity(0, null, 0, "A", "a"),
        ButtonDroidEntity(0, null, 0, "B", "b"),
        ButtonDroidEntity(0, null, 0, "X", "x"),
    )
}
