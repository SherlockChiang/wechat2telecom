package com.uranium92.wechatbridge

import android.telecom.Connection
import android.os.Handler
import android.os.Looper
import android.telecom.CallAudioState
import android.telecom.DisconnectCause
import android.util.Log

object CallBridge {
    @Volatile
    var activeConnection: WeChatConnection? = null

    /** 微信端通话结束或手机端接通时调用：只清理本地 Telecom 提醒，不控制微信。 */
    fun markEnded(cause: Int = DisconnectCause.REMOTE) {
        val conn = activeConnection ?: return
        Handler(Looper.getMainLooper()).post {
            try {
                Log.d("WeChatBridge", "🔴 切换 Telecom -> DISCONNECTED")
                conn.setDisconnected(DisconnectCause(cause))
                conn.destroy()
            } catch (e: Exception) {
                Log.e("WeChatBridge", "markEnded 异常: ${e.message}")
            } finally {
                activeConnection = null
            }
        }
    }
}

class WeChatConnection : Connection() {

    init {
        CallBridge.activeConnection = this
        // 自管理：必须显式声明
        connectionProperties = PROPERTY_SELF_MANAGED
        // 不主动抢音频路由，让微信继续用它的 AudioRecord/AudioTrack
        audioModeIsVoip = false
        connectionCapabilities = (CAPABILITY_MUTE or CAPABILITY_HOLD or CAPABILITY_SUPPORT_HOLD)
    }

    override fun onShowIncomingCallUi() {
        // 系统已经在通过通知栏弹微信原生界面了，这里不主动弹 UI，但要确保进入 RINGING
        Log.d("WeChatBridge", "Telecom: onShowIncomingCallUi")
        setRinging()
    }

    override fun onAnswer() {
        Log.d("WeChatBridge", "Telecom: 手表/系统点击接听")
        Log.d("WeChatBridge", "Telecom: 只监听模式，接听动作只关闭本地提醒")
        closeConnection(DisconnectCause.LOCAL)
    }

    override fun onReject() {
        Log.d("WeChatBridge", "Telecom: 手表/系统拒接")
        Log.d("WeChatBridge", "Telecom: 只监听模式，拒接动作不转发给微信")
        closeConnection(DisconnectCause.REJECTED)
    }

    override fun onDisconnect() {
        Log.d("WeChatBridge", "Telecom: 手表/系统挂断")
        Log.d("WeChatBridge", "Telecom: 只监听模式，挂断动作不转发给微信")
        closeConnection(DisconnectCause.LOCAL)
    }

    override fun onAbort() {
        Log.d("WeChatBridge", "Telecom: 通话中止")
        closeConnection(DisconnectCause.CANCELED)
    }

    override fun onCallAudioStateChanged(state: CallAudioState?) {
        // 仅记录，不主动操作音频路由，避免与微信争抢
        Log.d("WeChatBridge", "Telecom: 音频路由 -> ${state?.route}")
    }

    private fun closeConnection(cause: Int) {
        try {
            setDisconnected(DisconnectCause(cause))
        } catch (e: Exception) {}
        if (CallBridge.activeConnection == this) {
            CallBridge.activeConnection = null
        }
        destroy()
    }
}
