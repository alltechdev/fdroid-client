package com.atd.store.service

import android.content.Context
import androidx.annotation.StringRes
import com.atd.store.data.encryption.sha256
import com.atd.store.data.model.hex
import com.atd.store.model.Release
import com.atd.store.network.validation.FileValidator
import com.atd.store.network.validation.invalid
import com.atd.store.utility.common.extension.calculateHash
import com.atd.store.utility.common.extension.getPackageArchiveInfoCompat
import com.atd.store.utility.common.extension.singleSignature
import com.atd.store.utility.common.extension.versionCodeCompat
import java.io.File
import com.atd.store.R.string as strings

class ReleaseFileValidator(
    private val context: Context,
    private val packageName: String,
    private val release: Release,
) : FileValidator {

    override suspend fun validate(file: File) {
        val checksum = sha256(file).hex()
        if (!checksum.equals(release.hash, ignoreCase = true)) {
            invalid(getString(strings.integrity_check_error_DESC))
        }
        val packageInfo = context.packageManager.getPackageArchiveInfoCompat(file.path)
            ?: invalid(getString(strings.file_format_error_DESC))
        if (packageInfo.packageName != packageName ||
            packageInfo.versionCodeCompat != release.versionCode
        ) {
            invalid(getString(strings.invalid_metadata_error_DESC))
        }

        packageInfo.singleSignature
            ?.calculateHash()
            ?.takeIf { it.isNotBlank() || it == release.signature }
            ?: invalid(getString(strings.invalid_signature_error_DESC))

        packageInfo.permissions
            ?.map { it.name }
            ?.toSet()
            .orEmpty()
            .takeIf { release.permissions.containsAll(it) }
            ?: invalid(getString(strings.invalid_permissions_error_DESC))
    }

    private fun getString(@StringRes id: Int): String = context.getString(id)
}
