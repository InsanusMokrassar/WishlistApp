# Pattern: CRUD Repository (Exposed + cache)

> Read together with the hard rules in `agents/CODING.md` (value-class property naming, Exposed `initTable()` rule).

This is the canonical pattern for adding a persistent, cache-backed CRUD repository to a feature. Names use a generic `Item` entity — replace `Item`/`ItemId`/`ItemName` and `FEATURE_NAME` with your feature's own types.

> **Reference implementation:** `features/users` (`User`/`UserId`/`Username`) follows this exact pattern end to end.

## Model layer (`features/FEATURE_NAME/common/commonMain`)

Define three model types:

- **`Id`** — inline value class wrapping a primitive (used as the primary key):
  ```kotlin
  @Serializable @JvmInline value class ItemId(val long: Long)
  ```
- **`NewObject`** — data sent on create (no id yet):
  ```kotlin
  @Serializable data class NewItem(override val name: ItemName) : Item
  ```
- **`RegisteredObject`** — stored entity returned after create/read (carries the id):
  ```kotlin
  @Serializable data class RegisteredItem(val id: ItemId, override val name: ItemName) : Item
  ```

A sealed `Item` interface must be used as the shared base for `NewItem` and `RegisteredItem`; fields common to both variants are declared there.

Auxiliary value types (e.g. `ItemName`) should also be inline value classes so they carry type-safety with zero runtime overhead:

```kotlin
@Serializable @JvmInline value class ItemName(val string: String)
```

Wrapped-property naming follows the **value-class property naming rule** in `agents/CODING.md` (`String` → `string`, `Long` → `long`, …).

## Repository interfaces (`features/FEATURE_NAME/common/commonMain`)

Split the repository into three interfaces:

```kotlin
// ReadItemsRepo.kt
interface ReadItemsRepo : ReadCRUDRepo<RegisteredItem, ItemId>

// WriteItemsRepo.kt
interface WriteItemsRepo : WriteCRUDRepo<RegisteredItem, ItemId, NewItem>

// ItemsRepo.kt
interface ItemsRepo : ReadItemsRepo, WriteItemsRepo, CRUDRepo<RegisteredItem, ItemId, NewItem>
```

- `ReadCRUDRepo`, `WriteCRUDRepo`, and `CRUDRepo` are from `dev.inmo.micro_utils.repos`.
- Splitting read and write allows consumers that only need read access to depend only on `ReadItemsRepo`.

## Cache repository (`features/FEATURE_NAME/common/commonMain`)

Wrap the real repo with `FullCRUDCacheRepo` from `dev.inmo.micro_utils.repos.cache.full`:

```kotlin
class CacheItemsRepo(
    parentRepo: ItemsRepo,
    scope: CoroutineScope,
    kvCache: KeyValueRepo<ItemId, RegisteredItem> = MapKeyValueRepo(),
    locker: SmartRWLocker = SmartRWLocker()
) : ItemsRepo, FullCRUDCacheRepo<RegisteredItem, ItemId, NewItem>(
    crudRepo = parentRepo,
    kvCache = kvCache,
    scope = scope,
    skipStartInvalidate = false,
    locker = locker,
    idGetter = RegisteredItem::id
)
```

- `kvCache` defaults to an in-memory `MapKeyValueRepo`; swap for a persistent implementation if needed.
- `skipStartInvalidate = false` causes the cache to pre-fill from the DB on startup.
- `idGetter` is a function reference pointing to the id property of the registered type.

## Exposed (JVM) implementation (`features/FEATURE_NAME/common/jvmMain`)

Extend `AbstractExposedCRUDRepo` from `dev.inmo.micro_utils.repos.exposed`:

```kotlin
class ExposedItemsRepo(
    override val database: Database
) : ItemsRepo, AbstractExposedCRUDRepo<RegisteredItem, ItemId, NewItem>(tableName = "items") {

    private val idColumn = long("id").autoIncrement()
    private val nameColumn = text("name").uniqueIndex()

    override val primaryKey = PrimaryKey(idColumn)

    override val ResultRow.asObject: RegisteredItem
        get() = RegisteredItem(
            id = ItemId(get(idColumn)),
            name = ItemName(get(nameColumn))
        )

    override val ResultRow.asId: ItemId
        get() = ItemId(get(idColumn))

    override val selectById: (ItemId) -> Op<Boolean> = { idColumn.eq(it.long) }

    override fun update(id: ItemId?, value: NewItem, it: UpdateBuilder<Int>) {
        it[nameColumn] = value.name.string
    }

    override fun InsertStatement<Number>.asObject(value: NewItem): RegisteredItem =
        RegisteredItem(
            id = ItemId(this[idColumn]),
            name = value.name
        )

    init { initTable() }
}
```

- `initTable()` (from `dev.inmo.micro_utils.repos.exposed`) runs `SchemaUtils.createMissingTablesAndColumns` inside a transaction on `init`.
- The `update` function is called for both insert-fill and explicit update paths; `id` is `null` during insert.
- `InsertStatement<Number>.asObject` constructs the registered object from the auto-generated id returned by the insert statement.

## DI wiring (`features/FEATURE_NAME/common/jvmMain — JVMPlugin`)

Register `ExposedItemsRepo` as a plain `single`, then wrap it in `CacheItemsRepo` and bind both `ReadItemsRepo` and `WriteItemsRepo` via `singleWithBinds`:

```kotlin
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(Plugin) { setupDI(config) }

        single { ExposedItemsRepo(get()) }           // raw DB repo; only needed for cache wiring
        singleWithBinds<ItemsRepo> {
            CacheItemsRepo(parentRepo = get<ExposedItemsRepo>(), scope = get())
        }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

- `singleWithBinds` registers the `CacheItemsRepo` as `ItemsRepo`, `ReadItemsRepo`, and `WriteItemsRepo` simultaneously — any consumer that injects any of those three interfaces gets the cache-backed instance.
- `ExposedItemsRepo` is registered separately so it can be retrieved by type when constructing `CacheItemsRepo`; consumers should never inject it directly.

## Server plugin wiring (`features/FEATURE_NAME/server/jvmMain — JVMPlugin`)

The server plugin delegates to the common JVM plugin so the repo is available in the DI graph:

```kotlin
object JVMPlugin : StartPlugin {
    override fun Module.setupDI(config: JsonObject) {
        with(features.FEATURE_NAME.common.JVMPlugin) { setupDI(config) }
        with(Plugin) { setupDI(config) }
    }

    override suspend fun startPlugin(koin: Koin) {
        super.startPlugin(koin)
        features.FEATURE_NAME.common.JVMPlugin.startPlugin(koin)
        Plugin.startPlugin(koin)
    }
}
```

The `Database` singleton required by `ExposedItemsRepo` is provided by `features.common.server.JVMPlugin` (which connects to some database), so `features/FEATURE_NAME/server` must be loaded after (or alongside) `features/common/server` in the plugin list.
