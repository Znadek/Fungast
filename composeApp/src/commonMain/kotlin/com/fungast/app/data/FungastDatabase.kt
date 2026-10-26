package com.fungast.app.data

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

@Database(entities = [Spot::class], version = 1)
@ConstructedBy(FungastDatabaseConstructor::class)
abstract class FungastDatabase : RoomDatabase() {
    abstract fun spotDao(): SpotDao
}

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
