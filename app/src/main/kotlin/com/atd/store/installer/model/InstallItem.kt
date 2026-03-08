package com.atd.store.installer.model

import com.atd.store.data.model.PackageName
import com.atd.store.data.model.toPackageName

class InstallItem(
    val packageName: PackageName,
    val installFileName: String,
    val unarchiveId: Int? = null
)

infix fun String.installFrom(fileName: String) = InstallItem(this.toPackageName(), fileName)
