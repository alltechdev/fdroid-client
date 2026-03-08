package com.atd.store.utility.extension

import android.content.pm.PackageInfo
import com.atd.store.utility.common.extension.calculateHash
import com.atd.store.utility.common.extension.singleSignature
import com.atd.store.utility.common.extension.versionCodeCompat
import com.atd.store.model.InstalledItem

fun PackageInfo.toInstalledItem(): InstalledItem {
    val signatureString = singleSignature?.calculateHash().orEmpty()
    return InstalledItem(
        packageName,
        versionName.orEmpty(),
        versionCodeCompat,
        signatureString
    )
}
