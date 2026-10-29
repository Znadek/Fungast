package com.fungast.app.data

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RenameColumn
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(
    entities = [Spot::class, Route::class, RoutePoint::class],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 1, to = 2, spec = MultipleCategoriesMigration::class),
        // Version 3: photos
        AutoMigration(from = 2, to = 3),
        // Version 4: recorded routes
        AutoMigration(from = 3, to = 4),
    ],
)
@TypeConverters(Converters::class)
@ConstructedBy(FungastDatabaseConstructor::class)
abstract class FungastDatabase : RoomDatabase() {
    abstract fun spotDao(): SpotDao
    abstract fun routeDao(): RouteDao
}

/** Version 2: several categories per spot and optional find dates. A version 1 category name is a valid one-item list. */
@RenameColumn(tableName = "spots", fromColumnName = "category", toColumnName = "categories")
class MultipleCategoriesMigration : AutoMigrationSpec

// Implemented by the Room compiler for each platform
@Suppress("NO_ACTUAL_FOR_EXPECT", "KotlinNoActualForExpect")
expect object FungastDatabaseConstructor : RoomDatabaseConstructor<FungastDatabase> {
    override fun initialize(): FungastDatabase
}

const val DATABASE_FILE_NAME = "fungast.db"

/** Finishes a platform-specific builder with the settings shared by all platforms. */
fun RoomDatabase.Builder<FungastDatabase>.buildFungastDatabase(): FungastDatabase =
    setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
