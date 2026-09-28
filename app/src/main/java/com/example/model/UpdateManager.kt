package com.example.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Url
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

sealed class UpdateState {
    object Loading : UpdateState()
    object UpToDate : UpdateState()
    data class ForceUpdateRequired(
        val minVersionCode: Int,
        val currentVersionCode: Int,
        val updateUrl: String,
        val messageUrdu: String,
        val messageEnglish: String
    ) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

data class UpdateConfig(
    val minVersionCode: Int = 1,
    val updateUrl: String = "https://play.google.com/store/apps/details?id=com.dr.yy.y",
    val messageUrdu: String = "نیا اپڈیٹ دستیاب ہے! براہ کرم جاری رکھنے کے لیے ایپ کو اپڈیٹ کریں۔",
    val messageEnglish: String = "A new update is available! Please update the app to continue.",
    val forceUpdate: Boolean = false
)

interface UpdateApi {
    @GET
    suspend fun fetchUpdateConfig(@Url url: String): UpdateConfig
}

class UpdateManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("force_update_prefs", Context.MODE_PRIVATE)
    
    companion object {
        const val KEY_API_URL = "update_api_url"
        const val KEY_SIMULATE_FORCE = "simulate_force_update"
        const val DEFAULT_API_URL = "https://raw.githubusercontent.com/nature4024/droid-toolkit-update/main/update.json"
    }

    private val _updateState = MutableStateFlow<UpdateState>(UpdateState.Loading)
    val updateState: StateFlow<UpdateState> = _updateState.asStateFlow()

    fun getApiUrl(): String {
        return prefs.getString(KEY_API_URL, DEFAULT_API_URL) ?: DEFAULT_API_URL
    }

    fun setApiUrl(url: String) {
        prefs.edit().putString(KEY_API_URL, url).apply()
        // Re-evaluate
    }

    fun isSimulateForceUpdateEnabled(): Boolean {
        return prefs.getBoolean(KEY_SIMULATE_FORCE, false)
    }

    fun setSimulateForceUpdate(enable: Boolean) {
        prefs.edit().putBoolean(KEY_SIMULATE_FORCE, enable).apply()
        if (enable) {
            _updateState.value = UpdateState.ForceUpdateRequired(
                minVersionCode = 2,
                currentVersionCode = 1,
                updateUrl = "https://play.google.com/store/apps/details?id=com.dr.yy.y",
                messageUrdu = "یہ ایک تجرباتی فورس اپڈیٹ ہے۔ آپ ڈویلپر سیٹنگز سے اسے بند کر سکتے ہیں۔",
                messageEnglish = "This is a simulated Force Update. You can disable this in Developer Settings."
            )
        } else {
            // Recheck real update
            _updateState.value = UpdateState.Loading
        }
    }

    suspend fun checkUpdate() {
        if (isSimulateForceUpdateEnabled()) {
            _updateState.value = UpdateState.ForceUpdateRequired(
                minVersionCode = 2,
                currentVersionCode = 1,
                updateUrl = "https://play.google.com/store/apps/details?id=com.dr.yy.y",
                messageUrdu = "یہ ایک تجرباتی فورس اپڈیٹ ہے۔ آپ ڈویلپر سیٹنگز سے اسے بند کر سکتے ہیں۔",
                messageEnglish = "This is a simulated Force Update. You can disable this in Developer Settings."
            )
            return
        }

        _updateState.value = UpdateState.Loading

        withContext(Dispatchers.IO) {
            try {
                val currentVersionCode = try {
                    val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        packageInfo.longVersionCode.toInt()
                    } else {
                        packageInfo.versionCode
                    }
                } catch (e: Exception) {
                    1
                }

                val url = getApiUrl()
                
                // Initialize Retrofit with a generic base URL because we use dynamic @Url parameter
                val moshi = Moshi.Builder()
                    .add(KotlinJsonAdapterFactory())
                    .build()
                
                val retrofit = Retrofit.Builder()
                    .baseUrl("https://localhost/") // placeholder base URL
                    .addConverterFactory(MoshiConverterFactory.create(moshi))
                    .build()

                val api = retrofit.create(UpdateApi::class.java)
                val config = api.fetchUpdateConfig(url)

                if (config.forceUpdate && config.minVersionCode > currentVersionCode) {
                    _updateState.value = UpdateState.ForceUpdateRequired(
                        minVersionCode = config.minVersionCode,
                        currentVersionCode = currentVersionCode,
                        updateUrl = config.updateUrl,
                        messageUrdu = config.messageUrdu,
                        messageEnglish = config.messageEnglish
                    )
                } else {
                    _updateState.value = UpdateState.UpToDate
                }
            } catch (e: retrofit2.HttpException) {
                Log.d("UpdateManager", "Update API HTTP response code: ${e.code()}. Treating as up to date.")
                _updateState.value = UpdateState.UpToDate
            } catch (e: Exception) {
                Log.d("UpdateManager", "Update check skipped/offline: ${e.message}")
                _updateState.value = UpdateState.UpToDate
            }
        }
    }
}
