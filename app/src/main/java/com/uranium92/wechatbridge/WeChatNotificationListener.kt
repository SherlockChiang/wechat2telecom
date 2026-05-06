package com.uranium92.wechatbridge

import android.app.Notification
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.DisconnectCause
import android.util.Log

class WeChatNotificationListener : NotificationListenerService() {
    private val mainHandler = Handler(Looper.getMainLooper())

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn?.packageName != "com.tencent.mm") return

        val extras = sbn.notification.extras
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""

        Log.d(TAG, "📩 通知: title=$title text=$text")

        when {
            isIncomingInvite(text) -> {
                WeChatCallCoordinator.beginRinging(
                    this,
                    extractCallerName(title, text),
                    "notification"
                )
            }
            WeChatCallCoordinator.isRinging() && isActiveCall(text) -> {
                WeChatCallCoordinator.endCall(DisconnectCause.LOCAL, "notification-active")
            }
            WeChatCallCoordinator.isRinging() && isEndedMessage(text) -> {
                WeChatCallCoordinator.endCall(DisconnectCause.REMOTE, "notification-ended")
            }
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName != "com.tencent.mm") return

        val text = sbn.notification.extras.getString(Notification.EXTRA_TEXT) ?: ""
        if (WeChatCallCoordinator.isRinging() && isIncomingInvite(text)) {
            Log.d(TAG, "↩️ 响铃通知移除，延迟确认是否为来电取消")
            scheduleRemovedConfirmation()
        }
    }

    private fun scheduleRemovedConfirmation() {
        mainHandler.postDelayed({
            if (!WeChatCallCoordinator.isRinging()) return@postDelayed

            val callerName = WeChatCallCoordinator.lastCallerName()
            val hasRelatedNotification = activeNotifications
                .filter { it.packageName == "com.tencent.mm" }
                .any {
                    val extras = it.notification.extras
                    val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
                    val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
                    isIncomingInvite(text) || isActiveCall(text) ||
                        title.contains(callerName) || text.contains(callerName)
                }

            if (hasRelatedNotification) {
                Log.d(TAG, "↩️ 微信仍有相关通知，继续保持手表端响铃")
            } else {
                Log.d(TAG, "📴 [ENDED] 未发现相关活跃通知，判定来电已取消")
                WeChatCallCoordinator.endCall(DisconnectCause.MISSED, "notification-removed")
            }
        }, REMOVED_CONFIRMATION_DELAY_MS)
    }

    private fun isIncomingInvite(text: String): Boolean {
        return text.contains("邀请你语音通话") || text.contains("邀请你视频通话") ||
            text.contains("邀请你通话") || text.contains("微信电话")
    }

    private fun isActiveCall(text: String): Boolean {
        return text.contains("语音通话中") || text.contains("视频通话中") ||
            text.contains("通话中")
    }

    private fun isEndedMessage(text: String): Boolean {
        return text.contains("[语音通话]") || text.contains("[视频通话]") ||
            text.contains("未接") || text.contains("已取消") ||
            text.contains("已拒绝") || text.contains("通话时长")
    }

    private fun extractCallerName(title: String, text: String): String {
        return title.takeIf { it.isNotBlank() }
            ?: text.replace("邀请你语音通话", "")
                .replace("邀请你视频通话", "")
                .replace("邀请你通话", "")
                .replace("微信电话", "")
                .trim()
                .ifBlank { "微信来电" }
    }

    companion object {
        private const val TAG = "WeChatBridge"
        private const val REMOVED_CONFIRMATION_DELAY_MS = 3_500L
    }
}
