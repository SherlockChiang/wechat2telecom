package com.uranium92.wechatbridge

import android.telecom.Connection
import android.telecom.DisconnectCause
import android.util.Log

object CallBridge {
    var activeConnection: WeChatConnection? = null

    // 纯日志模式，引导用户手动操作
    fun answer() {
        Log.d("WeChatBridge", "📢 用户在外部设备点击了[接听]，请在手机上开始通话")
    }

    fun reject() {
        Log.d("WeChatBridge", "📢 用户在外部设备点击了[拒绝/挂断]")
    }
}

class WeChatConnection : Connection() {

    init {
        // 绑定引用
        CallBridge.activeConnection = this
    }

    override fun onAnswer() {
        Log.d("WeChatBridge", "Telecom: 系统接听信号")
        CallBridge.answer() 
        setActive() // 必须调用，否则手表会认为没接通而继续震动
    }

    // 处理用户在手表/系统界面点“拒绝”的情况
    override fun onReject() {
        Log.d("WeChatBridge", "Telecom: 系统拒绝信号")
        CallBridge.reject()
        closeConnection(DisconnectCause.REJECTED)
    }

    // 处理通话结束后的销毁
    override fun onDisconnect() {
        Log.d("WeChatBridge", "Telecom: 系统断开信号")
        CallBridge.reject()
        closeConnection(DisconnectCause.LOCAL)
    }

    // 统一清理逻辑
    private fun closeConnection(cause: Int) {
        setDisconnected(DisconnectCause(cause))
        if (CallBridge.activeConnection == this) {
            CallBridge.activeConnection = null
        }
        destroy()
    }
    
    override fun onAbort() {
        super.onAbort()
        Log.d("WeChatBridge", "Telecom: 通话中止")
        if (CallBridge.activeConnection == this) {
            CallBridge.activeConnection = null
        }
        destroy()
    }
}