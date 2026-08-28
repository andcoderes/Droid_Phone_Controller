package cl.jacevedo.droidinterfaces

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
enum class DeviceType : Parcelable {
    CHOPPER,
    BABU,
    MOUSE,
    PIT,
    ROGER_ROGER,
    GROGU,
    NONE
}