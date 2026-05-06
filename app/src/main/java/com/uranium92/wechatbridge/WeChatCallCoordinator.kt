package com.uranium92.wechatbridge

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

object WeChatCallCoordinator {
    private const val TAG = "WeChatBridge"
    private const val RINGING_TIMEOUT_MS = 75_000L

    private enum class State { IDLE, RINGING }

    @Volatile private var state: State = State.IDLE
    @Volatile private var generation = 0
    @Volatile private var lastCallerName = "微信来电"
    private val mainHandler = Handler(Looper.getMainLooper())

    fun isRinging(): Boolean = state == State.RINGING

    fun lastCallerName(): String = lastCallerName

    fun beginRinging(context: Context, callerName: String, source: String) {
        if (state == State.RINGING) {
            Log.d(TAG, "🔔 [$source] 已在响铃中，忽略重复来电: $callerName")
            return
        }

        state = State.RINGING
        lastCallerName = callerName.ifBlank { "微信来电" }
        scheduleRingingTimeout()
        triggerTelecomCall(context.applicationContext, lastCallerName, source)
    }

    fun endCall(cause: Int, source: String) {
        if (state == State.IDLE && CallBridge.activeConnection == null) return

        generation++
        state = State.IDLE
        Log.d(TAG, "📴 [$source] 结束本地 Telecom 提醒")
        CallBridge.markEnded(cause)
    }

    private fun scheduleRingingTimeout() {
        val currentGeneration = ++generation
        mainHandler.postDelayed({
            if (state == State.RINGING && generation == currentGeneration) {
                endCall(DisconnectCause.MISSED, "timeout")
            }
        }, RINGING_TIMEOUT_MS)
    }

    private fun triggerTelecomCall(context: Context, callerName: String, source: String) {
        BridgeInitializer.registerSelfManagedAccount(context)

        val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(context, WeChatConnectionService::class.java)
        val phoneAccountHandle = PhoneAccountHandle(componentName, BridgeInitializer.ACCOUNT_ID)
        val uri = Uri.fromParts("tel", "10000", null)

        val customExtras = Bundle().apply {
            putString("WX_CALLER_NAME", callerName)
        }

        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle)
            putParcelable(TelecomManager.EXTRA_INCOMING_CALL_ADDRESS, uri)
            putBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS, customExtras)
        }

        try {
            Log.d(TAG, "🔔 [$source] 调起 Telecom 来电: $callerName")
            telecomManager.addNewIncomingCall(phoneAccountHandle, extras)
        } catch (e: SecurityException) {
            Log.e(TAG, "❌ 缺少 MANAGE_OWN_CALLS 权限或账户未注册: ${e.message}")
            state = State.IDLE
        } catch (e: Exception) {
            Log.e(TAG, "❌ 无法调起系统通话: ${e.message}")
            state = State.IDLE
        }
    }
}
