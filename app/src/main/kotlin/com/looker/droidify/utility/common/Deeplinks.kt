package com.looker.droidify.utility.common

import android.content.Intent

val Intent.getInstallPackageName: String?
    get() = if (data?.scheme == "package") data?.schemeSpecificPart?.nullIfEmpty() else null
