package cl.jacevedo.droidinterfaces

data class DroidObject(
    val name:String,
    val characteristic:String,
    val type : DeviceType,
    val supportsViviMode: Boolean = true
)