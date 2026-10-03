package com.ltebandslock.ui.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.ltebandslock.MainActivity
import com.ltebandslock.R
import com.ltebandslock.data.model.DeviceInfo
import com.ltebandslock.data.model.SignalInfo
import com.ltebandslock.data.model.TrafficInfo

class LteWidgetProvider : AppWidgetProvider() {

    companion object {
        const val ACTION_REFRESH_WIDGET = "com.ltebandslock.ACTION_REFRESH_WIDGET"

        private var lastSignal: SignalInfo? = null
        private var lastDevice: DeviceInfo? = null
        private var lastTraffic: TrafficInfo? = null

        fun updateAllWidgets(
            context: Context,
            signal: SignalInfo? = null,
            device: DeviceInfo? = null,
            traffic: TrafficInfo? = null
        ) {
            signal?.let { lastSignal = it }
            device?.let { lastDevice = it }
            traffic?.let { lastTraffic = it }

            val appWidgetManager = AppWidgetManager.getInstance(context)
            val thisWidget = ComponentName(context, LteWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(thisWidget)

            for (widgetId in appWidgetIds) {
                updateSingleWidget(context, appWidgetManager, widgetId, lastSignal, lastDevice, lastTraffic)
            }
        }

        private fun updateSingleWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int,
            signal: SignalInfo?,
            device: DeviceInfo?,
            traffic: TrafficInfo?
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_lte_status)

            // Setup Click to Open App
            val openAppIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val openAppPendingIntent = PendingIntent.getActivity(
                context,
                0,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_root, openAppPendingIntent)

            // Setup Refresh Click
            val refreshIntent = Intent(context, LteWidgetProvider::class.java).apply {
                action = ACTION_REFRESH_WIDGET
            }
            val refreshPendingIntent = PendingIntent.getBroadcast(
                context,
                1,
                refreshIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            views.setOnClickPendingIntent(R.id.widget_btn_refresh, refreshPendingIntent)

            // Populate Signal Info
            if (signal != null) {
                views.setTextViewText(R.id.widget_ca_badge, signal.caLabel.ifEmpty { "4G" })
                views.setTextViewText(R.id.widget_bands, signal.activeBands.ifEmpty { signal.primaryBand.ifEmpty { "-" } })
                views.setTextViewText(R.id.widget_rsrp, signal.rsrp?.let { "$it dBm" } ?: "-")
                views.setTextViewText(R.id.widget_sinr, signal.sinr?.let { "$it dB" } ?: "-")
            } else {
                views.setTextViewText(R.id.widget_ca_badge, "4G")
                views.setTextViewText(R.id.widget_bands, "-")
                views.setTextViewText(R.id.widget_rsrp, "-")
                views.setTextViewText(R.id.widget_sinr, "-")
            }

            // Populate Device Info & Signal Strength
            if (device != null) {
                val barsText = if (signal != null && signal.signalBars > 0) " [${signal.signalBars}/5格]" else ""
                views.setTextViewText(R.id.widget_carrier, (device.carrier.ifEmpty { "4G LTE" }) + barsText)
                views.setTextViewText(R.id.widget_model, device.model.ifEmpty { "-" })
            } else {
                views.setTextViewText(R.id.widget_carrier, "未連線")
                views.setTextViewText(R.id.widget_model, "-")
            }

            // Populate Traffic Info
            if (traffic != null) {
                views.setTextViewText(R.id.widget_speed, traffic.formattedDownloadSpeed)
            } else {
                views.setTextViewText(R.id.widget_speed, "0 bps")
            }

            appWidgetManager.updateAppWidget(appWidgetId, views)
        }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateSingleWidget(context, appWidgetManager, appWidgetId, lastSignal, lastDevice, lastTraffic)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_REFRESH_WIDGET) {
            // Trigger refresh broadcast back to active app or refresh widgets
            updateAllWidgets(context)
        }
    }
}
