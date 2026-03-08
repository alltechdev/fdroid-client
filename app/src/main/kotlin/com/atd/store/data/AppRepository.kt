package com.atd.store.data

import com.atd.store.data.local.dao.AppDao
import com.atd.store.data.local.dao.RepoDao
import com.atd.store.data.local.model.toApp
import com.atd.store.data.model.App
import com.atd.store.data.model.AppMinimal
import com.atd.store.data.model.PackageName
import com.atd.store.datastore.SettingsRepository
import com.atd.store.datastore.get
import com.atd.store.datastore.model.SortOrder
import com.atd.store.sync.v2.model.DefaultName
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class AppRepository @Inject constructor(
    private val appDao: AppDao,
    private val repoDao: RepoDao,
    private val settingsRepository: SettingsRepository,
) {

    private val localeStream = settingsRepository.get { language }

    suspend fun apps(
        sortOrder: SortOrder,
        searchQuery: String? = null,
        repoId: Int? = null,
        categoriesToInclude: List<DefaultName>? = null,
        categoriesToExclude: List<DefaultName>? = null,
    ): List<AppMinimal> = withContext(Dispatchers.Default) {
        val currentLocale = localeStream.first()
        appDao.query(
            sortOrder = sortOrder,
            searchQuery = searchQuery?.ifEmpty { null },
            repoId = repoId,
            categoriesToInclude = categoriesToInclude?.ifEmpty { null },
            categoriesToExclude = categoriesToExclude?.ifEmpty { null },
            locale = currentLocale,
        )
    }

    val categories: Flow<List<DefaultName>>
        get() = repoDao.categories().map { it.map { category -> category.defaultName } }

    fun getApp(packageName: PackageName): Flow<List<App>> = combine(
        appDao.queryAppEntity(packageName.name),
        localeStream,
    ) { appEntityRelations, locale ->
        appEntityRelations.map {
            val repo = repoDao.getRepo(it.app.repoId)!!
            it.toApp(locale, repo)
        }
    }
}
