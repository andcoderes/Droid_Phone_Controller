package cl.jacevedo.droidcontroller.storage

import android.content.Context
import androidx.preference.PreferenceManager
import cl.jacevedo.droidcontroller.data.BluetoothDroidObject
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

private const val DROID_JSON_KEY = "droid_json"
class BluetoothDroidStorage(private val context: Context) {
    private val gson = Gson()

    fun getDroids():List<BluetoothDroidObject>{
        val stringJson = PreferenceManager.getDefaultSharedPreferences(context).getString(DROID_JSON_KEY, "")
        return if(!stringJson.isNullOrEmpty()){
            val type = object : TypeToken<List<BluetoothDroidObject>>() {}.type
            gson.fromJson(stringJson, type)
        } else {
            listOf()
        }
    }

    fun storeDroids(listBluetooth : List<BluetoothDroidObject>){
        val jsonString = gson.toJson(listBluetooth)
        PreferenceManager
            .getDefaultSharedPreferences(context)
            .edit()
            .putString(DROID_JSON_KEY, jsonString)
            .apply()
    }

}