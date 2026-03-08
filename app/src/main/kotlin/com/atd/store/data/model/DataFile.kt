package com.atd.store.data.model

import com.atd.store.network.DataSize

interface DataFile {
    val name: String
    val hash: String
    val size: DataSize
}
