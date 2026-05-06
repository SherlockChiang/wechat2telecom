package com.uranium92.wechatbridge

import android.content.ComponentName
import android.content.Context
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

object BridgeInitializer {
    private const val TAG = "WeChatBridge"
    const val ACCOUNT_ID = "WeChatBridgeAccount"

    fun initialize(context: Context) {
        registerSelfManagedAccount(context)
    }

    fun registerSelfManagedAccount(context: Context) {
        try {
            val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
            val componentName = ComponentName(context, WeChatConnectionService::class.java)
            val phoneAccountHandle = PhoneAccountHandle(componentName, ACCOUNT_ID)

            val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "WeChat Bridge")
                .setCapabilities(
                    PhoneAccount.CAPABILITY_SELF_MANAGED or
                        PhoneAccount.CAPABILITY_SUPPORTS_VIDEO_CALLING or
                        PhoneAccount.CAPABILITY_VIDEO_CALLING
                )
                .setIcon(android.graphics.drawable.Icon.createWithResource(context, android.R.drawable.ic_menu_call))
                .setShortDescription("微信通话桥接")
                .build()

            telecomManager.registerPhoneAccount(phoneAccount)
            Log.d(TAG, "Telecom 账户已注册")
        } catch (e: Exception) {
            Log.e(TAG, "注册 Telecom 账户失败: ${e.message}")
        }
    }
}
