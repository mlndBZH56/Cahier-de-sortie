package com.aca56.cahiersortiecodex.data.settings

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

class AppIconManager(
    context: Context,
) {
    private val appContext = context.applicationContext
    private val packageManager: PackageManager = appContext.packageManager

    fun applyLauncherIcon(option: LauncherIconOption) {
        LauncherIconOption.entries.forEach { launcherIcon ->
            val componentName = ComponentName(appContext, launcherIcon.aliasClassName)
            val newState = if (launcherIcon == option) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            packageManager.setComponentEnabledSetting(
                componentName,
                newState,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
