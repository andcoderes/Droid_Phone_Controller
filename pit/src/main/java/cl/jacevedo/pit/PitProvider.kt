package cl.jacevedo.pit

import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import cl.jacevedo.droidinterfaces.DroidProvider

class PitProvider : DroidProvider {
    override fun provideDroidInformation() = DroidObject(
        name = "pit",
        characteristic = CHARACTERISTIC,
        type = DeviceType.PIT
    )
    override fun provideDeviceType() = DeviceType.PIT
    override fun provideButtonImage() = R.drawable.pit
    override fun provideListMacros() = R.raw.macro_pit
    override fun provideListAudios() = R.raw.audio_pit

}