package com.lucrasports.sdk.app.logger

import android.content.Context
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.lucrasports.logger.LucraLogger
import com.lucrasports.logger.impl.buildAnalyticsEvent
import com.lucrasports.logger.model.AnalyticEvent
import com.lucrasports.logger.model.AnalyticProperty
import com.lucrasports.logger.model.AnalyticsActions
import com.lucrasports.logger.model.AnalyticsPages

class FirebaseLogger(context: Context) : LucraLogger.Logger {

    private var firebaseAnalytics: FirebaseAnalytics? = null

    private var crashlytics: FirebaseCrashlytics? = null

    init {
        try {
            firebaseAnalytics =
                FirebaseAnalytics.getInstance(context).apply {
                    setAnalyticsCollectionEnabled(true)
                }
            crashlytics =
                FirebaseCrashlytics.getInstance()
        } catch (e: Exception) {
            Log.e(
                "FirebaseLogger",
                "Unable to initialize Firebase for the Sample",
                e
            )
        }
    }

    override fun logNonFatalException(exception: Throwable) {
        crashlytics?.recordException(exception)
    }

    override fun breadcrumb(event: String, postToLogs: Boolean) {
        crashlytics?.log(event)
    }

    override fun log(event: AnalyticEvent) {
        // 500 unique events
        //https://support.google.com/firebase/answer/9237506?hl=en
        firebaseAnalytics?.logEvent(event::class.simpleName ?: event.toString(), null)
    }

    override fun setUserId(userId: String, traitsMap: Map<String, String>) {
        firebaseAnalytics?.setUserId(userId)
        crashlytics?.setUserId(userId)
        traitsMap.setUserProperties()
    }

    override fun traitFlags(map: Map<String, Boolean>) {
        map.setUserProperties()
    }

    override fun traitKeys(map: Map<String, String?>) {
        map.setUserProperties()
    }

    override fun trackFromParts(
        page: AnalyticsPages,
        properties: List<AnalyticProperty>,
        target: String?,
        action: AnalyticsActions?
    ) {
        val eventName = buildAnalyticsEvent(page, target, action)
        firebaseAnalytics?.logEvent(eventName, null)
    }

    private fun <String, V> Map<String, V>.setUserProperties() {
        firebaseAnalytics?.apply {
            this@setUserProperties.forEach { (key, value) ->
                this.setUserProperty(key.toString(), value.toString())
            }
        }
        crashlytics?.apply {
            this@setUserProperties.forEach { (key, value) ->
                this.setCustomKey(key.toString(), value.toString())
            }
        }
    }
}

