package cl.jacevedo.mouse

import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import cl.jacevedo.droidinterfaces.DroidProvider

class MouseProvider : DroidProvider {
    override fun provideDroidInformation() = DroidObject(
        name = "mouse",
        characteristic = CHARACTERISTIC,
        type = DeviceType.MOUSE
    )
    override fun provideDeviceType() = DeviceType.MOUSE
    override fun provideButtonImage() = R.drawable.mouse
    override fun provideListMacros() = R.raw.macro_mouse
    override fun provideListAudios() = R.raw.audio_mouse
}