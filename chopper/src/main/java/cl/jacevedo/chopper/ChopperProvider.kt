package cl.jacevedo.chopper

import cl.jacevedo.droidinterfaces.ButtonDroidEntity
import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import cl.jacevedo.droidinterfaces.DroidProvider

class ChopperProvider : DroidProvider {
    override fun provideDroidInformation() = DroidObject(
        name = "chopper",
        characteristic = CHARACTERISTIC,
        type = DeviceType.CHOPPER
    )
    override fun provideDeviceType() = DeviceType.CHOPPER
    override fun provideButtonImage() = R.drawable.chopper
    override fun provideListMacros() = R.raw.macro_chopper
    override fun provideListAudios() = R.raw.audio_chopper

    // buttonType 0 = MACRO_BUTTONS (app module's constant - can't import it here
    // since app depends on this module, not the other way around).
    override fun provideTestButtons(): List<ButtonDroidEntity> = listOf(
        ButtonDroidEntity(0, null, 0, "L1 (1/3)", "l1"),
        ButtonDroidEntity(0, null, 0, "L1 (2/3)", "l1"),
        ButtonDroidEntity(0, null, 0, "L1 (3/3)", "l1"),
        ButtonDroidEntity(0, null, 0, "R1 (1/3)", "r1"),
        ButtonDroidEntity(0, null, 0, "R1 (2/3)", "r1"),
        ButtonDroidEntity(0, null, 0, "R1 (3/3)", "r1"),
        ButtonDroidEntity(0, null, 0, "Y", "y"),
        ButtonDroidEntity(0, null, 0, "X", "x"),
        ButtonDroidEntity(0, null, 0, "B", "b"),
        ButtonDroidEntity(0, null, 0, "Y", "y"),
        ButtonDroidEntity(0, null, 0, "A (1/2)", "a"),
        ButtonDroidEntity(0, null, 0, "A (2/2)", "a"),
        ButtonDroidEntity(0, null, 0, "Bubble (1/2)", "99"),
        ButtonDroidEntity(0, null, 0, "Bubble (2/2)", "99"),
        ButtonDroidEntity(0, null, 0, "Head Left", "100"),
        ButtonDroidEntity(0, null, 0, "Head Right", "101"),
        ButtonDroidEntity(0, null, 0, "Arm body", "102"),
        ButtonDroidEntity(0, null, 0, "Bubble head", "107"),
    )
}
