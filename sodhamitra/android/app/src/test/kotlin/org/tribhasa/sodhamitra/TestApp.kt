package org.tribhasa.sodhamitra

import androidx.room.Room
import org.tribhasa.sodhamitra.data.AppDatabase

/**
 * The application used by the Robolectric UI tests: the real app code over a real (temporary) database file, as on a
 * phone. A file is used because an in-memory SQLite database is private to one connection.
 */
class TestApp : SodhamitraApp() {
    override fun onCreate() {
        super.onCreate()
        databaseOverride = Room.databaseBuilder(this, AppDatabase::class.java, "test-${System.nanoTime()}.db")
            .allowMainThreadQueries().build()
    }
}
