# Legacy Database System

SQLite database implementation for repositories, products, and installed apps.

## Overview

**File:** `app/src/main/kotlin/com/looker/droidify/database/Database.kt`

The legacy database uses raw SQLite with custom adapters. Being migrated to Room (see [Database](../data/database.md)).

## Schema

### Repository Table

```kotlin
object Repository : Table {
    const val ROW_ID = "_id"
    const val ROW_ENABLED = "enabled"
    const val ROW_DELETED = "deleted"
    const val ROW_DATA = "data"

    override val createTable = """
        $ROW_ID INTEGER PRIMARY KEY AUTOINCREMENT,
        $ROW_ENABLED INTEGER NOT NULL,
        $ROW_DELETED INTEGER NOT NULL,
        $ROW_DATA BLOB NOT NULL
    """
}
```

### Product Table

```kotlin
object Product : Table {
    const val ROW_REPOSITORY_ID = "repository_id"
    const val ROW_PACKAGE_NAME = "package_name"
    const val ROW_NAME = "name"
    const val ROW_SUMMARY = "summary"
    const val ROW_DESCRIPTION = "description"
    const val ROW_ADDED = "added"
    const val ROW_UPDATED = "updated"
    const val ROW_VERSION_CODE = "version_code"
    const val ROW_SIGNATURES = "signatures"
    const val ROW_COMPATIBLE = "compatible"
    const val ROW_DATA = "data"
    const val ROW_DATA_ITEM = "data_item"

    override val createTable = """
        PRIMARY KEY ($ROW_REPOSITORY_ID, $ROW_PACKAGE_NAME)
    """
    override val createIndex = ROW_PACKAGE_NAME
}
```

### Category Table

```kotlin
object Category : Table {
    const val ROW_REPOSITORY_ID = "repository_id"
    const val ROW_PACKAGE_NAME = "package_name"
    const val ROW_NAME = "name"

    override val createTable = """
        PRIMARY KEY ($ROW_REPOSITORY_ID, $ROW_PACKAGE_NAME, $ROW_NAME)
    """
    override val createIndex = "$ROW_PACKAGE_NAME, $ROW_NAME"
}
```

### Installed Table (In-Memory)

```kotlin
object Installed : Table {
    const val ROW_PACKAGE_NAME = "package_name"
    const val ROW_VERSION = "version"
    const val ROW_VERSION_CODE = "version_code"
    const val ROW_SIGNATURE = "signature"

    override val memory = true  // In-memory table
}
```

### Lock Table (In-Memory)

```kotlin
object Lock : Table {
    const val ROW_PACKAGE_NAME = "package_name"
    const val ROW_VERSION_CODE = "version_code"

    override val memory = true
}
```

## Adapters

### RepositoryAdapter

```kotlin
object RepositoryAdapter {
    fun put(repository: Repository, database: SQLiteDatabase = db): Repository
    fun get(id: Long): Repository?
    fun getAll(): List<Repository>
    fun getStream(id: Long): Flow<Repository?>
    fun getAllStream(): Flow<List<Repository>>
    fun getEnabledStream(): Flow<List<Repository>>
    fun markAsDeleted(id: Long)
    fun cleanup(removedRepos: Map<Long, Boolean>)
    fun importRepos(list: List<Repository>)
    fun query(signal: CancellationSignal?): Cursor
}
```

### ProductAdapter

```kotlin
object ProductAdapter {
    fun get(packageName: String, signal: CancellationSignal?): List<Product>
    fun getStream(packageName: String): Flow<List<Product>>
    fun getAll(): List<Product>
    suspend fun getUpdates(skipSignatureCheck: Boolean): List<ProductItem>
    fun getUpdatesStream(skipSignatureCheck: Boolean): Flow<List<ProductItem>>
    fun getCountStream(repositoryId: Long): Flow<Int>
    fun query(installed: Boolean, updates: Boolean, ...): Cursor
}
```

### CategoryAdapter

```kotlin
object CategoryAdapter {
    fun getAllStream(): Flow<Set<String>>
}
```

### InstalledAdapter

```kotlin
object InstalledAdapter {
    fun get(packageName: String, signal: CancellationSignal?): InstalledItem?
    fun getStream(packageName: String): Flow<InstalledItem?>
    fun put(installedItem: InstalledItem)
    fun putAll(installedItems: List<InstalledItem>)
    fun delete(packageName: String)
}
```

### LockAdapter

```kotlin
object LockAdapter {
    fun put(lock: Pair<String, Long>)
    fun putAll(locks: List<Pair<String, Long>>)
    fun delete(packageName: String)
}
```

### UpdaterAdapter

For bulk sync operations:

```kotlin
object UpdaterAdapter {
    fun createTemporaryTable()
    fun putTemporary(products: List<Product>)
    fun finishTemporary(repository: Repository, success: Boolean)
}
```

## Observable Pattern

### Subject System

```kotlin
sealed class Subject {
    data object Repositories : Subject()
    data class Repository(val id: Long) : Subject()
    data object Products : Subject()
}

private val observers = mutableMapOf<Subject, MutableSet<() -> Unit>>()

fun flowCollection(subject: Subject): Flow<Unit> = callbackFlow {
    val callback: () -> Unit = { trySend(Unit) }
    dataObservable(subject)(true, callback)
    awaitClose { dataObservable(subject)(false, callback) }
}.flowOn(Dispatchers.IO)
```

### Notification

```kotlin
private fun notifyChanged(vararg subjects: Subject) {
    synchronized(observers) {
        subjects.asSequence()
            .mapNotNull { observers[it] }
            .flatten()
            .forEach { it() }
    }
}
```

## Query Building

### QueryBuilder

**File:** `database/QueryBuilder.kt`

```kotlin
class QueryBuilder {
    operator fun plusAssign(sql: String)
    operator fun remAssign(arg: String)
    operator fun remAssign(args: List<String>)
    fun query(db: SQLiteDatabase, signal: CancellationSignal?): Cursor
}
```

**Usage:**
```kotlin
val builder = QueryBuilder()
builder += "SELECT * FROM product WHERE name LIKE ?"
builder %= "%search%"
builder.query(db, null)
```

## JSON Serialization

### Parse JSON Blob

```kotlin
fun <T> ByteArray.jsonParse(callback: (JsonParser) -> T): T {
    return Json.factory.createParser(this).use { it.parseDictionary(callback) }
}
```

### Generate JSON Blob

```kotlin
fun jsonGenerate(callback: (JsonGenerator) -> Unit): ByteArray {
    val outputStream = ByteArrayOutputStream()
    Json.factory.createGenerator(outputStream).use { it.writeDictionary(callback) }
    return outputStream.toByteArray()
}
```

## Product Query

Complex query for app list with updates detection:

```kotlin
fun query(
    installed: Boolean,
    updates: Boolean,
    skipSignatureCheck: Boolean,
    searchQuery: String,
    order: SortOrder,
    signal: CancellationSignal?,
): Cursor {
    val builder = QueryBuilder()

    // Signature matching clause
    val signatureMatches = if (skipSignatureCheck) "1"
    else """installed.signature IS NOT NULL AND
        product.signatures LIKE ('%.' || installed.signature || '.%')"""

    // Can update clause
    builder += """
        (lock.version_code NOT IN (0, product.version_code) AND
        product.compatible != 0 AND
        product.version_code > COALESCE(installed.version_code, 0xffffffff) AND
        $signatureMatches) AS can_update
    """

    // Search ranking
    if (searchQuery.isNotEmpty()) {
        builder += """
            (((product.name LIKE ? OR product.summary LIKE ?) * 7) |
            ((product.package_name LIKE ?) * 3) |
            (product.description LIKE ?)) AS match_rank
        """
        builder %= List(4) { "%$searchQuery%" }
    }

    // Joins
    builder += "JOIN repository ON product.repository_id = repository._id"
    builder += "LEFT JOIN lock ON product.package_name = lock.package_name"
    builder += "JOIN installed ON product.package_name = installed.package_name"

    // Filters
    builder += "WHERE repository.enabled != 0 AND repository.deleted == 0"

    // Grouping and ordering
    builder += "GROUP BY product.package_name"
    builder += "ORDER BY match_rank DESC, product.name ASC"

    return builder.query(db, signal).observable(Subject.Products)
}
```

## Temporary Tables for Sync

During sync, products are written to temporary tables:

```kotlin
fun createTemporaryTable() {
    db.execSQL("DROP TABLE IF EXISTS ${Schema.Product.temporaryName}")
    db.execSQL(Schema.Product.formatCreateTable(Schema.Product.temporaryName))
}

fun putTemporary(products: List<Product>) {
    db.transaction {
        for (product in products) {
            db.insertOrReplace(true, Schema.Product.temporaryName, ...)
        }
    }
}

fun finishTemporary(repository: Repository, success: Boolean) {
    if (success) {
        db.transaction {
            // Delete old products
            db.delete(Schema.Product.name, "repository_id = ?", arrayOf(repository.id))
            // Copy from temporary
            db.execSQL("INSERT INTO product SELECT * FROM product_temporary")
            // Drop temporary
            db.execSQL("DROP TABLE product_temporary")
        }
        notifyChanged(Subject.Products)
    }
}
```

## Migration Status

This legacy database is being replaced by Room:
- New screens use Room DAOs
- Sync still uses legacy UpdaterAdapter
- Complete migration in progress

See [Database](../data/database.md) for the new Room-based system.

## Removed

| Feature | Removal Doc |
|---------|-------------|
| `section` parameter in `query()` | [category-filtering.md](../removal/category-filtering.md) |
