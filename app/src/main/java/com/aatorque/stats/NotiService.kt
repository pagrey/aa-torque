package com.aatorque.stats

import android.service.notification.NotificationListenerService

class NotiService : NotificationListenerService() {
    companion object {
        var isNotificationAccessEnabled = false
    }

    override fun onListenerConnected() {
        isNotificationAccessEnabled = true
    }

    override fun onListenerDisconnected() {
        isNotificationAccessEnabled = false
    }
}