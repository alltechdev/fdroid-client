package com.atd.store.model

import android.view.View
import com.atd.store.utility.common.extension.dpi

data class ProductItem(
    var repositoryId: Long,
    var packageName: String,
    var name: String,
    var summary: String,
    val icon: String,
    val metadataIcon: String,
    val version: String,
    var installedVersion: String,
    var compatible: Boolean,
    var canUpdate: Boolean,
    var matchRank: Int
) {
    private val supportedDpi = intArrayOf(120, 160, 240, 320, 480, 640)
    private var deviceDpi: Int = -1

    fun icon(
        view: View,
        repository: Repository
    ): String? {
        if (packageName.isBlank()) return null
        if (icon.isBlank() && metadataIcon.isBlank()) return null
        if (repository.version < 11 && icon.isNotBlank()) {
            return "${repository.address}/icons/$icon"
        }
        if (icon.isNotBlank()) {
            if (deviceDpi == -1) {
                deviceDpi = supportedDpi.find { it >= view.dpi } ?: supportedDpi.last()
            }
            return "${repository.address}/icons-$deviceDpi/$icon"
        }
        if (metadataIcon.isNotBlank()) {
            return "${repository.address}/$packageName/$metadataIcon"
        }
        return null
    }
}
