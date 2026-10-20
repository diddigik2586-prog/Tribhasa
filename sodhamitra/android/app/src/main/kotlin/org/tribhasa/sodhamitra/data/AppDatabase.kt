package org.tribhasa.sodhamitra.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        Project::class, Chapter::class, Point::class, PointDescriptionChunk::class, Note::class, NoteDescriptionChunk::class,
        BibBook::class, BibArticle::class, Manuscript::class, Citation::class, PlannerBook::class, Profile::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        const val FILE_NAME = "sodhamitra.db"

        /** The real, file-backed database. There is deliberately no destructive migration: user data is never wiped. */
        fun open(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, FILE_NAME).build()
    }
}
