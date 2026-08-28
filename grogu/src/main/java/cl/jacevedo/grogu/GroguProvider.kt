package cl.jacevedo.grogu

import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import cl.jacevedo.droidinterfaces.DroidProvider

// Grogu has no audio hardware, so audio_grogu.json is repurposed to hold
// motor_controller's (Bottango Impulse) animation triggers instead of
// real audio tracks -- ids there map 1:1 to Studio's exported animation
// indices, plus a reserved 999 = STOP. See motor_controller's README.
// macro_grogu.json stays empty for now.
class GroguProvider : DroidProvider {
    override fun provideDroidInformation() = DroidObject(
        name = droidName,
        characteristic = CHARACTERISTIC,
        type = DeviceType.GROGU
    )
    override fun provideDeviceType() = DeviceType.GROGU
    override fun provideButtonImage() = R.drawable.grogu
    override fun provideListMacros() = R.raw.macro_grogu
    override fun provideListAudios() = R.raw.audio_grogu
}
