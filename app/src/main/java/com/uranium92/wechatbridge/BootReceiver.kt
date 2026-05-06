package com.uranium92.wechatbridge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        Log.d("WeChatBridge", "收到系统启动事件: $action")
        BridgeInitializer.initialize(context.applicationContext)
    }
}
