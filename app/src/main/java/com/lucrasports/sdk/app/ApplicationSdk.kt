package com.lucrasports.sdk.app


import android.app.Application
import coil.ImageLoaderFactory
import com.lucrasports.apphost.LucraCoilImageLoader


class ApplicationSdk : Application(), ImageLoaderFactory {

    override fun newImageLoader() = LucraCoilImageLoader.get(this)

}