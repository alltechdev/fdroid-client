package com.atd.store.installer.installers

import com.atd.store.data.model.PackageName
import com.atd.store.installer.model.InstallItem
import com.atd.store.installer.model.InstallState

interface Installer : AutoCloseable {

    suspend fun install(installItem: InstallItem): InstallState

    suspend fun uninstall(packageName: PackageName)

}
