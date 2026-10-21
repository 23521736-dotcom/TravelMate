package com.example.travelmate.util

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import java.net.URLEncoder

object TravelokaNavigationHelper {

    fun openTravelokaGuide(context: Context, destinationName: String) {
        val encodedName = try {
            URLEncoder.encode(destinationName, "UTF-8")
        } catch (_: Exception) {
            destinationName
        }
        val url = "https://www.traveloka.com/vi-vn/explore/search?q=$encodedName"

        try {
            val customTabsIntent = CustomTabsIntent.Builder()
                .setToolbarColor(Color.parseColor("#0088FF"))
                .setShowTitle(true)
                .build()
            customTabsIntent.launchUrl(context, Uri.parse(url))
        } catch (_: Exception) {
            // Fallback to standard web browser
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            try {
                context.startActivity(browserIntent)
            } catch (_: Exception) {
                // Ignore if no browser available
            }
        }
    }
}
