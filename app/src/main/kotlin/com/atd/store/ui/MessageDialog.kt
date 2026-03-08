package com.atd.store.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.atd.store.utility.common.SdkCheck
import com.atd.store.model.Release
import com.atd.store.utility.extension.android.Android
import kotlinx.parcelize.Parceler
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.TypeParceler
import com.atd.store.R.string as stringRes

class MessageDialog() : DialogFragment() {
    companion object {
        private const val EXTRA_MESSAGE = "message"
    }

    constructor(message: Message) : this() {
        arguments = bundleOf(EXTRA_MESSAGE to message)
    }

    fun show(fragmentManager: FragmentManager) {
        show(fragmentManager, this::class.java.name)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): AlertDialog {
        val dialog = MaterialAlertDialogBuilder(requireContext())
        val message = arguments?.let { BundleCompat.getParcelable(it, EXTRA_MESSAGE, Message::class.java) }!!
        when (message) {
            is Message.Link -> {
                dialog.setTitle(stringRes.confirmation)
                dialog.setMessage(getString(stringRes.open_DESC_FORMAT, message.uri.toString()))
                dialog.setPositiveButton(stringRes.ok) { _, _ ->
                    try {
                        startActivity(Intent(Intent.ACTION_VIEW, message.uri))
                    } catch (e: ActivityNotFoundException) {
                        e.printStackTrace()
                    }
                }
                dialog.setNegativeButton(stringRes.cancel, null)
            }

            is Message.ReleaseIncompatible -> {
                val builder = StringBuilder()
                val minSdkVersion =
                    if (Release.Incompatibility.MinSdk in message.incompatibilities) {
                        message.minSdkVersion
                    } else {
                        null
                    }
                val maxSdkVersion =
                    if (Release.Incompatibility.MaxSdk in message.incompatibilities) {
                        message.maxSdkVersion
                    } else {
                        null
                    }
                if (minSdkVersion != null || maxSdkVersion != null) {
                    val versionMessage = minSdkVersion?.let {
                        getString(
                            stringRes.incompatible_api_min_DESC_FORMAT,
                            it
                        )
                    }
                        ?: maxSdkVersion?.let {
                            getString(
                                stringRes.incompatible_api_max_DESC_FORMAT,
                                it
                            )
                        }
                    builder.append(
                        getString(
                            stringRes.incompatible_api_DESC_FORMAT,
                            Android.name,
                            SdkCheck.sdk,
                            versionMessage.orEmpty()
                        )
                    ).append("\n\n")
                }
                if (Release.Incompatibility.Platform in message.incompatibilities) {
                    builder.append(
                        getString(
                            stringRes.incompatible_platforms_DESC_FORMAT,
                            Android.primaryPlatform ?: getString(stringRes.unknown),
                            message.platforms.joinToString(separator = ", ")
                        )
                    ).append("\n\n")
                }
                val features =
                    message.incompatibilities.mapNotNull { it as? Release.Incompatibility.Feature }
                if (features.isNotEmpty()) {
                    builder.append(getString(stringRes.incompatible_features_DESC))
                    for (feature in features) {
                        builder.append("\n\u2022 ").append(feature.feature)
                    }
                    builder.append("\n\n")
                }
                if (builder.isNotEmpty()) {
                    builder.delete(builder.length - 2, builder.length)
                }
                dialog.setTitle(stringRes.incompatible_version)
                dialog.setMessage(builder)
                dialog.setPositiveButton(stringRes.ok, null)
            }

            is Message.ReleaseOlder -> {
                dialog.setTitle(stringRes.incompatible_version)
                dialog.setMessage(stringRes.incompatible_older_DESC)
                dialog.setPositiveButton(stringRes.ok, null)
            }

            is Message.ReleaseSignatureMismatch -> {
                dialog.setTitle(stringRes.incompatible_version)
                dialog.setMessage(stringRes.incompatible_signature_DESC)
                dialog.setPositiveButton(stringRes.ok, null)
            }

            is Message.InsufficientStorage -> {
                dialog.setTitle(stringRes.insufficient_storage)
                dialog.setMessage(stringRes.insufficient_storage_DESC)
                dialog.setPositiveButton(stringRes.ok, null)
            }
        }::class
        return dialog.create()
    }
}

@Parcelize
sealed interface Message : Parcelable {
    @Parcelize
    class Link(val uri: Uri) : Message

    @Parcelize
    @TypeParceler<Release.Incompatibility, ReleaseIncompatibilityParceler>
    class ReleaseIncompatible(
        val incompatibilities: List<Release.Incompatibility>,
        val platforms: List<String>,
        val minSdkVersion: Int,
        val maxSdkVersion: Int
    ) : Message

    @Parcelize
    data object ReleaseOlder : Message

    @Parcelize
    data object ReleaseSignatureMismatch : Message

    @Parcelize
    data object InsufficientStorage : Message
}

class ReleaseIncompatibilityParceler : Parceler<Release.Incompatibility> {

    private companion object {
        // Incompatibility indices in `Parcel`
        const val MIN_SDK_INDEX = 0
        const val MAX_SDK_INDEX = 1
        const val PLATFORM_INDEX = 2
        const val FEATURE_INDEX = 3
    }

    override fun create(parcel: Parcel): Release.Incompatibility {
        return when (parcel.readInt()) {
            MIN_SDK_INDEX -> Release.Incompatibility.MinSdk
            MAX_SDK_INDEX -> Release.Incompatibility.MaxSdk
            PLATFORM_INDEX -> Release.Incompatibility.Platform
            FEATURE_INDEX -> Release.Incompatibility.Feature(requireNotNull(parcel.readString()))
            else -> error("Invalid Index for Incompatibility")
        }
    }

    override fun Release.Incompatibility.write(parcel: Parcel, flags: Int) {
        when (this) {
            is Release.Incompatibility.MinSdk -> {
                parcel.writeInt(MIN_SDK_INDEX)
            }

            is Release.Incompatibility.MaxSdk -> {
                parcel.writeInt(MAX_SDK_INDEX)
            }

            is Release.Incompatibility.Platform -> {
                parcel.writeInt(PLATFORM_INDEX)
            }

            is Release.Incompatibility.Feature -> {
                parcel.writeInt(FEATURE_INDEX)
                parcel.writeString(feature)
            }
        }
    }
}
