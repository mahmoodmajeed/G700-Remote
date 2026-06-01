package com.mmy.g700remote

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.mmy.g700remote.data.AppIconTheme

object LauncherIconManager {
    private val aliases = mapOf(
        AppIconTheme.GtBlack to "LauncherGtBlack",
        AppIconTheme.GtHorizon to "LauncherGtHorizon",
        AppIconTheme.GtDune to "LauncherGtDune",
        AppIconTheme.DisplayMirror to "LauncherDisplayMirror",
    )

    fun applyIcon(context: Context, theme: AppIconTheme) {
        val appContext = context.applicationContext
        val packageManager = appContext.packageManager
        val selectedAlias = aliases.getValue(theme)
        setAliasState(packageManager, appContext, selectedAlias, PackageManager.COMPONENT_ENABLED_STATE_ENABLED)
        aliases.values.filterNot { it == selectedAlias }.forEach { alias ->
            setAliasState(packageManager, appContext, alias, PackageManager.COMPONENT_ENABLED_STATE_DISABLED)
        }
    }

    private fun setAliasState(
        packageManager: PackageManager,
        context: Context,
        alias: String,
        state: Int,
    ) {
        runCatching {
            packageManager.setComponentEnabledSetting(
                ComponentName(context, "${context.packageName}.$alias"),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
    }
}
