package cl.jacevedo.droidcontroller.provider

import cl.jacevedo.babu.BabuProvider
import cl.jacevedo.chopper.ChopperProvider
import cl.jacevedo.droidinterfaces.DeviceType
import cl.jacevedo.droidinterfaces.DroidObject
import cl.jacevedo.droidinterfaces.DroidProvider
import cl.jacevedo.grogu.GroguProvider
import cl.jacevedo.mouse.MouseProvider
import cl.jacevedo.pit.PitProvider
import cl.jacevedo.rogerroger.RogerRogerProvider


class Provider {
    companion object{
        private val mapProviders : Map<DeviceType, DroidProvider> = mapOf(
            DeviceType.NONE to DefaultProvider(),
            DeviceType.CHOPPER to ChopperProvider(),
            DeviceType.BABU to BabuProvider(),
            DeviceType.MOUSE to MouseProvider(),
            DeviceType.PIT to PitProvider(),
            DeviceType.ROGER_ROGER to RogerRogerProvider(),
            DeviceType.GROGU to GroguProvider()
        )
        private fun getProvider(deviceType: DeviceType) = mapProviders[deviceType] ?: DefaultProvider()


        fun getDroidObjects():List<DroidObject> =
            mapProviders.values.map { it.provideDroidInformation() }

        fun getImages(deviceType: DeviceType) = getProvider(deviceType).provideButtonImage()

        fun getAudioFile(deviceType: DeviceType) = getProvider(deviceType).provideListAudios()

        fun getMacroFile(deviceType: DeviceType) = getProvider(deviceType).provideListMacros()

        fun getTestButtons(deviceType: DeviceType) = getProvider(deviceType).provideTestButtons()

    }
}