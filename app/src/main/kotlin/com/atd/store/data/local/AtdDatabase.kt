package com.atd.store.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.atd.store.data.local.converters.Converters
import com.atd.store.data.local.converters.PermissionConverter
import com.atd.store.data.local.dao.AppDao
import com.atd.store.data.local.dao.AuthDao
import com.atd.store.data.local.dao.DownloadStatsDao
import com.atd.store.data.local.dao.IndexDao
import com.atd.store.data.local.dao.InstalledDao
import com.atd.store.data.local.dao.RBLogDao
import com.atd.store.data.local.dao.RepoDao
import com.atd.store.data.local.model.AppEntity
import com.atd.store.data.local.model.AuthenticationEntity
import com.atd.store.data.local.model.AuthorEntity
import com.atd.store.data.local.model.CategoryAppRelation
import com.atd.store.data.local.model.CategoryEntity
import com.atd.store.data.local.model.CategoryRepoRelation
import com.atd.store.data.local.model.DonateEntity
import com.atd.store.data.local.model.DownloadStats
import com.atd.store.data.local.model.GraphicEntity
import com.atd.store.data.local.model.InstalledEntity
import com.atd.store.data.local.model.LinksEntity
import com.atd.store.data.local.model.LocalizedAppDescriptionEntity
import com.atd.store.data.local.model.LocalizedAppIconEntity
import com.atd.store.data.local.model.LocalizedAppNameEntity
import com.atd.store.data.local.model.LocalizedAppSummaryEntity
import com.atd.store.data.local.model.LocalizedRepoDescriptionEntity
import com.atd.store.data.local.model.LocalizedRepoIconEntity
import com.atd.store.data.local.model.LocalizedRepoNameEntity
import com.atd.store.data.local.model.MirrorEntity
import com.atd.store.data.local.model.RBLogEntity
import com.atd.store.data.local.model.RepoEntity
import com.atd.store.data.local.model.ScreenshotEntity
import com.atd.store.data.local.model.VersionEntity

@Database(
    version = 1,
    exportSchema = true,
    entities = [
        AuthenticationEntity::class,
        AuthorEntity::class,
        AppEntity::class,
        CategoryEntity::class,
        CategoryAppRelation::class,
        CategoryRepoRelation::class,
        DonateEntity::class,
        GraphicEntity::class,
        InstalledEntity::class,
        LinksEntity::class,
        MirrorEntity::class,
        RepoEntity::class,
        ScreenshotEntity::class,
        VersionEntity::class,
        RBLogEntity::class,
        DownloadStats::class,
        // Localized Data
        LocalizedAppNameEntity::class,
        LocalizedAppSummaryEntity::class,
        LocalizedAppDescriptionEntity::class,
        LocalizedAppIconEntity::class,
        LocalizedRepoNameEntity::class,
        LocalizedRepoDescriptionEntity::class,
        LocalizedRepoIconEntity::class,
    ],
)
@TypeConverters(
    PermissionConverter::class,
    Converters::class,
)
abstract class AtdDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao
    abstract fun repoDao(): RepoDao
    abstract fun authDao(): AuthDao
    abstract fun indexDao(): IndexDao
    abstract fun rbLogDao(): RBLogDao
    abstract fun downloadStatsDao(): DownloadStatsDao
    abstract fun installedDao(): InstalledDao
}

fun atdDatabase(context: Context): AtdDatabase = Room
    .databaseBuilder(
        context = context,
        klass = AtdDatabase::class.java,
        name = "atd_room",
    )
    .fallbackToDestructiveMigration(true)
    .addCallback(
        object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.query("PRAGMA synchronous = OFF")
                db.query("PRAGMA journal_mode = WAL")
            }
        },
    )
    .build()
