package com.uranium92.wechatbridge

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.os.Handler
import android.os.Looper
import android.telecom.DisconnectCause
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class WeChatAccessibilityService : AccessibilityService() {
    private val mainHandler = Handler(Looper.getMainLooper())
    @Volatile private var windowGeneration = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED or
                AccessibilityEvent.TYPE_WINDOWS_CHANGED
            packageNames = arrayOf("com.tencent.mm")
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 150
            flags = AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS
        }
        Log.d(TAG, "✅ 微信浮窗监听已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.packageName?.toString() != "com.tencent.mm") return

        val root = rootInActiveWindow ?: return
        val visibleText = collectVisibleText(root)
        if (visibleText.isBlank()) return

        Log.d(TAG, "🪟 微信窗口: ${visibleText.take(180)}")

        when {
            looksLikeIncomingWindow(visibleText) -> {
                windowGeneration++
                WeChatCallCoordinator.beginRinging(
                    this,
                    extractCallerName(visibleText),
                    "accessibility"
                )
            }
            WeChatCallCoordinator.isRinging() && looksLikeEndedWindow(visibleText) -> {
                windowGeneration++
                WeChatCallCoordinator.endCall(DisconnectCause.REMOTE, "accessibility-ended")
            }
            WeChatCallCoordinator.isRinging() && looksLikeActiveCallWindow(visibleText) -> {
                windowGeneration++
                WeChatCallCoordinator.endCall(DisconnectCause.LOCAL, "accessibility-active")
            }
            WeChatCallCoordinator.isRinging() -> {
                scheduleWindowGoneConfirmation()
            }
        }
    }

    override fun onInterrupt() {
        Log.w(TAG, "微信浮窗监听被中断")
    }

    private fun scheduleWindowGoneConfirmation() {
        val generation = ++windowGeneration
        mainHandler.postDelayed({
            if (!WeChatCallCoordinator.isRinging() || windowGeneration != generation) return@postDelayed

            val text = rootInActiveWindow?.let { collectVisibleText(it) }.orEmpty()
            if (!looksLikeIncomingWindow(text) && !looksLikeActiveCallWindow(text)) {
                WeChatCallCoordinator.endCall(DisconnectCause.MISSED, "accessibility-window-gone")
            }
        }, WINDOW_GONE_CONFIRMATION_MS)
    }

    private fun collectVisibleText(root: AccessibilityNodeInfo): String {
        val result = StringBuilder()
        fun visit(node: AccessibilityNodeInfo?) {
            if (node == null || !node.isVisibleToUser || result.length > MAX_TEXT_CHARS) return

            node.text?.toString()?.takeIf { it.isNotBlank() }?.let {
                result.append(it).append(' ')
            }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let {
                result.append(it).append(' ')
            }

            for (i in 0 until node.childCount) {
                visit(node.getChild(i))
            }
        }
        visit(root)
        return result.toString()
    }

    private fun looksLikeIncomingWindow(text: String): Boolean {
        val hasInviteText = text.contains("邀请你语音通话") ||
            text.contains("邀请你视频通话") ||
            text.contains("邀请你通话") ||
            text.contains("微信电话")
        val hasCallButtons = (text.contains("接听") || text.contains("接受")) &&
            (text.contains("拒绝") || text.contains("挂断")) &&
            (text.contains("语音") || text.contains("视频") || text.contains("通话"))
        return hasInviteText || hasCallButtons
    }

    private fun looksLikeActiveCallWindow(text: String): Boolean {
        return text.contains("语音通话中") ||
            text.contains("视频通话中") ||
            text.contains("通话中")
    }

    private fun looksLikeEndedWindow(text: String): Boolean {
        return text.contains("未接") ||
            text.contains("已取消") ||
            text.contains("已拒绝") ||
            text.contains("通话时长") ||
            text.contains("[语音通话]") ||
            text.contains("[视频通话]")
    }

    private fun extractCallerName(text: String): String {
        return text
            .replace("邀请你语音通话", "")
            .replace("邀请你视频通话", "")
            .replace("邀请你通话", "")
            .replace("微信电话", "")
            .replace("接听", "")
            .replace("接受", "")
            .replace("拒绝", "")
            .replace("挂断", "")
            .trim()
            .split(Regex("\\s+"))
            .firstOrNull { it.isNotBlank() && it.length <= 24 }
            ?: "微信来电"
    }

    companion object {
        private const val TAG = "WeChatBridge"
        private const val MAX_TEXT_CHARS = 1200
        private const val WINDOW_GONE_CONFIRMATION_MS = 2_500L
    }
}
