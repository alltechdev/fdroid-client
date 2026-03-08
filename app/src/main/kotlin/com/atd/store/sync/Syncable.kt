package com.atd.store.sync

import com.atd.store.data.model.Repo

interface Syncable<T> {
    suspend fun sync(repo: Repo, block: (SyncState) -> Unit)
}
