package com.atd.store.utility.extension

import com.atd.store.model.InstalledItem
import com.atd.store.model.Product
import com.atd.store.model.Repository
import com.atd.store.model.findSuggested
import com.atd.store.service.Connection
import com.atd.store.service.DownloadService
import com.atd.store.utility.extension.android.Android

fun Connection<DownloadService.Binder, DownloadService>.startUpdate(
    packageName: String,
    installedItem: InstalledItem?,
    products: List<Pair<Product, Repository>>
) {
    if (binder == null || products.isEmpty()) return

    val (product, repository) = products.findSuggested(installedItem) ?: return

    val compatibleReleases = product.selectedReleases
        .filter { installedItem == null || installedItem.signature == it.signature }
        .ifEmpty { return }

    val selectedRelease = compatibleReleases.singleOrNull() ?: compatibleReleases.run {
        filter { Android.primaryPlatform in it.platforms }.minByOrNull { it.platforms.size }
            ?: minByOrNull { it.platforms.size }
            ?: firstOrNull()
    } ?: return

    requireNotNull(binder).enqueue(
        packageName = packageName,
        name = product.name,
        repository = repository,
        release = selectedRelease,
        isUpdate = installedItem != null
    )
}
