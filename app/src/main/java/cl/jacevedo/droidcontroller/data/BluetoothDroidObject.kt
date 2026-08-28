package cl.jacevedo.droidcontroller.data

import android.os.Parcelable
import cl.jacevedo.droidinterfaces.DeviceType
import kotlinx.parcelize.Parcelize


@Parcelize
data class BluetoothDroidObject(val nameDevice : String,
                                val macAddress: String,
                                val associationId : Int,
                                val deviceType : DeviceType
) : Parcelable
