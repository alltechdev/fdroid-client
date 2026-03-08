package com.atd.store.installer.installers.root

import android.content.Context
import com.atd.store.data.model.PackageName
import com.atd.store.installer.installers.Installer
import com.atd.store.installer.installers.uninstallPackage
import com.atd.store.installer.model.InstallItem
import com.atd.store.installer.model.InstallState
import com.atd.store.utility.common.SdkCheck
import com.atd.store.utility.common.cache.Cache
import com.topjohnwu.superuser.Shell
import kotlin.coroutines.resume
import kotlinx.coroutines.suspendCancellableCoroutine

class RootInstaller(private val context: Context) : Installer {

    override suspend fun install(
        installItem: InstallItem,
    ): InstallState = suspendCancellableCoroutine { cont ->
        val releaseFile = Cache.getReleaseFile(context, installItem.installFileName)
        val installCommand = INSTALL_COMMAND.format(
            releaseFile.absolutePath,
            currentUser(),
            context.packageName,
            releaseFile.length(),
        )
        Shell.cmd(installCommand).submit { shellResult ->
            val result = if (shellResult.isSuccess) InstallState.Installed
            else InstallState.Failed
            cont.resume(result)
            val deleteCommand = DELETE_COMMAND.format(utilBox(), releaseFile.absolutePath)
            Shell.cmd(deleteCommand).submit()
        }
    }

    override suspend fun uninstall(packageName: PackageName) =
        context.uninstallPackage(packageName)

    override fun close() {}

}

private const val INSTALL_COMMAND = "cat %s | pm install --user %s -i %s -t -r -S %s"
private const val DELETE_COMMAND = "%s rm %s"

/** Returns the path of either toybox or busybox, or empty string if not found. */
private fun utilBox(): String {
    listOf("toybox", "busybox").forEach {
        // Returns the path of the requested [command], or empty string if not found
        val out = Shell.cmd("which $it").exec().out
        if (out.isEmpty()) return ""
        if (out.first().contains("not found")) return ""
        return out.first()
    }
    return ""
}

/** Returns the current user of the device. */
private fun currentUser() = if (SdkCheck.isOreo) {
    Shell.cmd("am get-current-user")
        .exec()
        .out[0]
} else {
    Shell.cmd("dumpsys activity | grep -E \"mUserLru\"")
        .exec()
        .out[0]
        .trim()
        .removePrefix("mUserLru: [")
        .removeSuffix("]")
}
