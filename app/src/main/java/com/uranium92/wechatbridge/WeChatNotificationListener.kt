package com.uranium92.wechatbridge

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

class WeChatNotificationListener : NotificationListenerService() {

    private val TAG = "WeChatBridge"
    private var isCalling = false
    private var lastCallId: String? = null

    private val mHandler = Handler(Looper.getMainLooper())
    private var disconnectRunnable: Runnable? = null

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.packageName != "com.tencent.mm") return

        // 核心：一旦有微信通知更新，立即撤销“挂断预警”
        disconnectRunnable?.let {
            mHandler.removeCallbacks(it)
            disconnectRunnable = null
        }

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: "微信通话"
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""

        // 判定来电特征
        val isInsistent = (notification.flags and Notification.FLAG_INSISTENT) != 0
        val isVoipText = text.contains("语音通话") || text.contains("视频通话") || text.contains("邀请你")

        if (isInsistent || isVoipText) {
            // 防抖逻辑
            if (isCalling && lastCallId == title) return
            
            isCalling = true
            lastCallId = title
            
            Log.d(TAG, "🔔 [同步开始] 微信来电: $title")
            triggerTelecomCall(title)
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName == "com.tencent.mm") {
            // 设置 1.5 秒缓冲，防止微信通知在切换（如显示时长）时导致手表误挂断
            disconnectRunnable = Runnable {
                Log.d(TAG, "📴 [同步结束] 微信通知已清理，释放系统电话")
                CallBridge.activeConnection?.let { conn ->
                    conn.setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.REMOTE))
                    conn.destroy()
                }
                isCalling = false
                lastCallId = null
                CallBridge.activeConnection = null
            }
            mHandler.postDelayed(disconnectRunnable!!, 1500)
        }
    }

    private fun triggerTelecomCall(callerName: String) {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(this, WeChatConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, "WeChatBridgeAccount")

        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            val uri = Uri.fromParts("tel", "WeChat", null) // 统一标识
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)
            // 额外信息，有些系统会在 UI 上显示
            putString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, "微信来电: $callerName")
        }

        try {
            telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
        } catch (e: Exception) {
            Log.e(TAG, "无法调起系统通话: ${e.message}")
            isCalling = false
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "WeChat Bridge 已就绪")
    }
}