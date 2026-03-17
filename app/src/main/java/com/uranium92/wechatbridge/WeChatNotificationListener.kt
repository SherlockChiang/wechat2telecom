package com.uranium92.wechatbridge

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

class WeChatNotificationListener : NotificationListenerService() {

    private val TAG = "WeChatBridge"
    private var isCalling = false

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn?.packageName != "com.tencent.mm") return

        val notification = sbn.notification
        val extras = notification.extras
        val title = extras.getString(Notification.EXTRA_TITLE) ?: ""
        val text = extras.getString(Notification.EXTRA_TEXT) ?: ""
        val flags = notification.flags

        // 1. 识别呼叫 (触发系统响铃)
        if (text.contains("邀请你语音通话") || text.contains("邀请你视频通话")) {
            if (!isCalling) {
                isCalling = true
                val callerName = text.replace("邀请你语音通话", "").replace("邀请你视频通话", "")
                Log.d(TAG, "🔔 [触发响铃] 微信来电: $callerName")
                triggerTelecomCall(callerName)
            }
        }

        // 2. 识别“已在手机端接听” (停止手表响铃)
        // 解析 Flags: 如果包含 64 (FLAG_FOREGROUND_SERVICE)，说明微信已占用了麦克风，通话已经接通！
        if (isCalling && text == "语音通话中" && (flags and Notification.FLAG_FOREGROUND_SERVICE) != 0) {
            Log.d(TAG, "📞 [拦截成功] 识别到你在手机端接听了电话，强制停止系统响铃")
            endTelecomCall()
        }

        // 3. 识别未接来电消息 (停止手表响铃)
        // 比如收到 "[2条]姜宇涵: [语音通话]"
        if (isCalling && (text.contains("[语音通话]") || text.contains("[视频通话]"))) {
            Log.d(TAG, "📴 [识别到未接] 对方已挂断，强制停止系统响铃")
            endTelecomCall()
        }
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        super.onNotificationRemoved(sbn)
        if (sbn?.packageName != "com.tencent.mm") return

        val removedText = sbn.notification.extras.getString(Notification.EXTRA_TEXT) ?: ""

        // 4. 彻底挂断生命周期 (停止手表响铃)
        // 无论何种情况，只要这根“独苗”常驻通知消失了，就意味着一切结束
        if (removedText == "语音通话中" && isCalling) {
            Log.d(TAG, "📴 [通话彻底结束] 常驻通知消失，销毁系统来电")
            endTelecomCall()
        }
    }

    private fun endTelecomCall() {
        CallBridge.activeConnection?.let { conn ->
            conn.setDisconnected(android.telecom.DisconnectCause(android.telecom.DisconnectCause.REMOTE))
            conn.destroy()
        }
        isCalling = false
        CallBridge.activeConnection = null
    }

    private fun triggerTelecomCall(callerName: String) {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(this, WeChatConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, "WeChatBridgeAccount")

        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            val uri = Uri.fromParts("tel", "WeChat", null)
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)
            putString(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, "微信来电: $callerName")
        }

        try {
            telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
        } catch (e: Exception) {
            Log.e(TAG, "❌ 无法调起系统通话: ${e.message}")
            isCalling = false
        }
    }
}