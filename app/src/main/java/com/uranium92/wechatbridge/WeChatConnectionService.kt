package com.uranium92.wechatbridge

import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.util.Log

class WeChatConnectionService : ConnectionService() {
    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d("WeChatBridge", "Telecom: 系统正在请求创建来电实例")
        val connection = WeChatConnection()
        // 设置联系人名称
        connection.setAddress(request?.address, android.telecom.TelecomManager.PRESENTATION_ALLOWED)
        connection.setCallerDisplayName("微信来电", android.telecom.TelecomManager.PRESENTATION_ALLOWED)
        return connection
    }
}