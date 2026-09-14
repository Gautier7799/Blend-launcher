package com.blend.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.blend.launcher.data.model.AppModel

class AppRepository(private val context: Context) {

    fun getInstalledApps(): List<AppModel> {
        val packageManager = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        return packageManager.queryIntentActivities(intent, 0).map { resolveInfo ->
            AppModel(
                label = resolveInfo.loadLabel(packageManager).toString(),
                packageName = resolveInfo.activityInfo.packageName,
                icon = resolveInfo.loadIcon(packageManager)
            )
        }.sortedBy { it.label }
    }
}
