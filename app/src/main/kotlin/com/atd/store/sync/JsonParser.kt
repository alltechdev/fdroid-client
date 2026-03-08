package com.atd.store.sync

import kotlinx.serialization.json.Json

val JsonParser = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    isLenient = true
}
