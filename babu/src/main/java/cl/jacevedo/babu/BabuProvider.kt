package cl.jacevedo.babu

import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import cl.jacevedo.droidinterfaces.DroidProvider

class BabuProvider : DroidProvider {
    override fun provideDroidInformation() = DroidObject(
        name = "Babu",
        characteristic = CHARACTERISTIC,
        type = DeviceType.BABU
    )
    override fun provideDeviceType() = DeviceType.BABU
    override fun provideButtonImage() = R.drawable.babu
    override fun provideListMacros() = R.raw.macro_babu
    override fun provideListAudios() = R.raw.audio_babu
}
