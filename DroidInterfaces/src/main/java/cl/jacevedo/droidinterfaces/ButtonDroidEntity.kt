package cl.jacevedo.droidinterfaces

data class ButtonDroidEntity(
    val uid: Int,
    var deviceId: Int?,
    var buttonType: Int?,
    val label: String?,
    val macro: String?
)