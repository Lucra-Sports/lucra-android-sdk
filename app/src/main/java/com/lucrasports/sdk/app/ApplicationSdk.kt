package com.lucrasports.sdk.app

import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.ThreadPolicy.Builder
import coil.ImageLoaderFactory
import com.lucrasports.apphost.LucraCoilImageLoader
import io.branch.referral.Branch

class ApplicationSdk : Application(), ImageLoaderFactory {
    override fun newImageLoader() = LucraCoilImageLoader.get(this)
    override fun onCreate() {
        super.onCreate()

        if (BuildConfig.ENABLE_STRICT_MODE) {
            /**
             * Set a thread policy that detects all potential problems on the main thread, such as network
             * and disk access.
             *
             * If a problem is found, the offending call will be logged and the application will be killed.
             */
            StrictMode.setThreadPolicy(
                Builder().detectAll().penaltyLog().build(),
            )
        }

        // Branch logging for debugging
        Branch.enableLogging()

        // Branch object initialization
        Branch.getAutoInstance(this)
    }
}
