package com.uranium92.wechatbridge

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat
import android.net.Uri

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 注册 Telecom 账户 (虚拟运营商牌照)
        registerPhoneAccount()

        // 2. 原有的强制唤醒逻辑
        try {
            val componentName = ComponentName(this, WeChatNotificationListener::class.java)
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
            packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        } catch (e: Exception) {}

        // 3. 权限检查
        checkPermissions()
    }

    private fun registerPhoneAccount() {
        val telecomManager = getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val componentName = ComponentName(this, WeChatConnectionService::class.java)
        
        // 创建一个唯一的账户 ID
        val phoneAccountHandle = PhoneAccountHandle(componentName, "WeChatBridgeAccount")

        val phoneAccount = PhoneAccount.builder(phoneAccountHandle, "WeChat Bridge")
            .setCapabilities(PhoneAccount.CAPABILITY_CALL_PROVIDER) // 声明它是通话提供者
            .setIcon(android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_call))
            .build()

        telecomManager.registerPhoneAccount(phoneAccount)

        // 检查账户是否已启用 (某些系统如小米需要用户手动在拨号设置里开启，但我们先尝试代码检查)
        val intent = Intent(TelecomManager.ACTION_CHANGE_PHONE_ACCOUNTS)
        Toast.makeText(this, "如果未激活，请在通话设置中开启 WeChat Bridge", Toast.LENGTH_LONG).show()
    }

    private fun checkPermissions() {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        if (!enabled) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
            Toast.makeText(this, "服务运行中", Toast.LENGTH_SHORT).show()
        }
    }
}