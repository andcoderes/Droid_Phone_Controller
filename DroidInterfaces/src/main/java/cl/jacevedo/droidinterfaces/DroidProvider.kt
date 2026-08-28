package cl.jacevedo.droidinterfaces

interface DroidProvider {
    fun provideDroidInformation(): DroidObject
    fun provideDeviceType(): DeviceType
    fun provideButtonImage(): Int
    fun provideListMacros(): Int
    fun provideListAudios(): Int
    fun provideTestButtons(): List<ButtonDroidEntity>? = null
}