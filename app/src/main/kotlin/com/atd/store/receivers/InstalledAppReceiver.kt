package com.atd.store.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import com.atd.store.database.Database
import com.atd.store.utility.common.extension.getPackageInfoCompat
import com.atd.store.utility.extension.toInstalledItem

class InstalledAppReceiver(
    private val packageManager: PackageManager,
) : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val packageName =
            intent.data?.let { if (it.scheme == "package") it.schemeSpecificPart else null }
        if (packageName != null) {
            when (intent.action.orEmpty()) {
                Intent.ACTION_PACKAGE_ADDED,
                Intent.ACTION_PACKAGE_REMOVED,
                    -> {
                    val packageInfo = packageManager.getPackageInfoCompat(packageName)
                    if (packageInfo != null) {
                        Database.InstalledAdapter.put(packageInfo.toInstalledItem())
                    } else {
                        Database.InstalledAdapter.delete(packageName)
                    }
                }
            }
        }
    }
}
