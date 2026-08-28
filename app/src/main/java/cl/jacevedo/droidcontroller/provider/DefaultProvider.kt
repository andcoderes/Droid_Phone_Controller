package cl.jacevedo.droidcontroller.provider

import cl.jacevedo.droidcontroller.R
import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import cl.jacevedo.droidinterfaces.DroidProvider


class DefaultProvider : DroidProvider {
    override fun provideDroidInformation() = DroidObject(
        name = "default",
        characteristic = "",
        type = DeviceType.NONE
    )
    override fun provideDeviceType() = DeviceType.NONE
    override fun provideButtonImage() = R.drawable.chopper_launcher
    override fun provideListMacros() = 0
    override fun provideListAudios() = 0
}