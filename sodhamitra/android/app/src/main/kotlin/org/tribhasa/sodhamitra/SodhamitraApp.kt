package org.tribhasa.sodhamitra

import android.app.Application
import java.io.File
import org.tribhasa.sodhamitra.data.AppDatabase
import org.tribhasa.sodhamitra.data.Repository

/** Creates the local database once. There is no networking code anywhere in this application. */
class SodhamitraApp : Application() {
    val repository: Repository by lazy {
        val db = databaseOverride ?: AppDatabase.open(this)
        Repository(db, File(filesDir, "profile").apply { mkdirs() })
    }

    /** set by automated tests before the first screen opens, to use an in-memory database */
    var databaseOverride: AppDatabase? = null
}
