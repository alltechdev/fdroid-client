package com.atd.store.datastore

import androidx.datastore.core.Serializer
import com.atd.store.datastore.model.InstallerType
import com.atd.store.datastore.model.LegacyInstallerComponent
import com.atd.store.datastore.model.SortOrder
import com.atd.store.datastore.model.Theme
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream

@Serializable
@OptIn(ExperimentalTime::class)
data class Settings(
    val language: String = "system",
    val notifyUpdate: Boolean = true,
    val theme: Theme = Theme.SYSTEM,
    val dynamicTheme: Boolean = false,
    val installerType: InstallerType = InstallerType.Default,
    val legacyInstallerComponent: LegacyInstallerComponent? = null,
    val autoUpdate: Boolean = false,
    val sortOrder: SortOrder = SortOrder.UPDATED,
    @Contextual
    val lastCleanup: Instant? = null,
    val lastRbLogFetch: Long? = null,
    val lastModifiedDownloadStats: Long? = null,
    val homeScreenSwiping: Boolean = true,
    val enabledRepoIds: Set<Int> = emptySet(),
)

@OptIn(ExperimentalSerializationApi::class)
object SettingsSerializer : Serializer<Settings> {

    private val json = Json { encodeDefaults = true }

    @OptIn(ExperimentalTime::class)
    override val defaultValue: Settings = Settings()

    override suspend fun readFrom(input: InputStream): Settings {
        return try {
            json.decodeFromStream(input)
        } catch (e: SerializationException) {
            e.printStackTrace()
            defaultValue
        }
    }

    override suspend fun writeTo(t: Settings, output: OutputStream) {
        try {
            json.encodeToStream(t, output)
        } catch (e: SerializationException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
}
