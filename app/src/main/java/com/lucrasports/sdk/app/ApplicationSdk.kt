package com.lucrasports.sdk.app

import android.app.Application
import android.os.StrictMode
import coil.ImageLoaderFactory
import com.lucrasports.apphost.LucraCoilImageLoader
import io.branch.referral.Branch

class ApplicationSdk : Application(), ImageLoaderFactory {
    override fun newImageLoader() = LucraCoilImageLoader.get(this)
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.ENABLE_STRICT_MODE) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectNetwork() // Detect network calls on main thread
                    .detectCustomSlowCalls() // Detect other potentially slow calls
                    .penaltyLog() // Log violations to logcat
                    .penaltyFlashScreen() // Flash screen on violation (visual indicator)
                    .build()
            )
        }

        // Branch logging for debugging
        Branch.enableLogging()

        // Branch object initialization
        Branch.getAutoInstance(this)
    }
}