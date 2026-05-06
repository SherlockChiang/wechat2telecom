package com.uranium92.wechatbridge

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

class WeChatConnectionService : ConnectionService() {

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ): Connection {
        Log.d("WeChatBridge", "Telecom: onCreateIncomingConnection")
        val connection = WeChatConnection()

        val incomingExtras = request?.extras?.getBundle(TelecomManager.EXTRA_INCOMING_CALL_EXTRAS)
        val callerName = incomingExtras?.getString("WX_CALLER_NAME") ?: "未知"
        Log.d("WeChatBridge", "Telecom: 来电人 -> $callerName")

        connection.setAddress(
            Uri.fromParts("tel", "10000", null),
            TelecomManager.PRESENTATION_ALLOWED
        )
        connection.setCallerDisplayName("微信: $callerName", TelecomManager.PRESENTATION_ALLOWED)
        // Self-managed 必须主动 setRinging，否则手表不会显示来电
        connection.setRinging()
        return connection
    }

    override fun onCreateIncomingConnectionFailed(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest?
    ) {
        super.onCreateIncomingConnectionFailed(connectionManagerPhoneAccount, request)
        Log.e("WeChatBridge", "Telecom: 来电创建失败 -> ${request?.extras}")
    }
}
