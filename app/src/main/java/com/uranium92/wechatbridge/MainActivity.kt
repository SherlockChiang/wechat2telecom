package com.uranium92.wechatbridge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationManagerCompat

class MainActivity : Activity() {
    private lateinit var statusView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        BridgeInitializer.initialize(this)

        buildUi()
        checkPermissions()
    }

    private fun checkPermissions() {
        val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
        updateStatus(enabled)
        if (!enabled) {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        } else {
            Toast.makeText(this, "服务运行中", Toast.LENGTH_SHORT).show()
        }
    }

    private fun buildUi() {
        val padding = (20 * resources.displayMetrics.density).toInt()
        val gap = (12 * resources.displayMetrics.density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(padding, padding, padding, padding)
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        }

        val title = TextView(this).apply {
            text = "WeChat Telecom Bridge"
            textSize = 22f
        }
        root.addView(title)

        statusView = TextView(this).apply {
            textSize = 15f
            setPadding(0, gap, 0, gap)
        }
        root.addView(statusView)

        val modeView = TextView(this).apply {
            text = "模式：只监听微信通知并同步响铃，不控制微信接听或挂断。"
            textSize = 16f
            setPadding(0, 0, 0, gap)
        }
        root.addView(modeView)

        val notificationButton = Button(this).apply {
            text = "打开通知监听设置"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
        root.addView(notificationButton)

        val accessibilityButton = Button(this).apply {
            text = "打开无障碍浮窗监听设置"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
            }
        }
        root.addView(accessibilityButton)

        setContentView(root)
    }

    private fun updateStatus(notificationListenerEnabled: Boolean) {
        statusView.text = if (notificationListenerEnabled) {
            "通知监听已开启。当前模式：只监听同步响铃。"
        } else {
            "通知监听未开启，请先授权后再测试微信来电。"
        }
    }

    override fun onResume() {
        super.onResume()
        if (::statusView.isInitialized) {
            val enabled = NotificationManagerCompat.getEnabledListenerPackages(this).contains(packageName)
            updateStatus(enabled)
        }
    }
}
