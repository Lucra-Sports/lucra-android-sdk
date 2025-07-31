package com.lucrasports.sdk.app


import android.app.Application
import coil.ImageLoaderFactory
import com.lucrasports.apphost.LucraCoilImageLoader
import io.branch.referral.Branch


class ApplicationSdk : Application(), ImageLoaderFactory {

    override fun newImageLoader() = LucraCoilImageLoader.get(this)
    override fun onCreate() {
        super.onCreate()

        // Branch logging for debugging
        Branch.enableLogging()

        // Branch object initialization
        Branch.getAutoInstance(this)
    }

}