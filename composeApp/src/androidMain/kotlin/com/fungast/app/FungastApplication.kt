package com.fungast.app

import android.app.Application
import androidx.room.Room
import com.fungast.app.data.DATABASE_FILE_NAME
import com.fungast.app.data.FungastDatabase
import com.fungast.app.data.buildFungastDatabase
import com.fungast.app.photos.AndroidPhotoStore
import org.maplibre.android.MapLibre

class FungastApplication : Application() {
    val database: FungastDatabase by lazy {
        Room.databaseBuilder<FungastDatabase>(
            context = this,
            name = getDatabasePath(DATABASE_FILE_NAME).absolutePath,
        ).buildFungastDatabase()
    }

    val photoStore: AndroidPhotoStore by lazy { AndroidPhotoStore(this) }

    override fun onCreate() {
        super.onCreate()
        // Must run before any MapView is created
        MapLibre.getInstance(this)
    }
}
