package com.duddy.portugues.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.duddy.portugues.data.local.dao.FavoritePhraseDao
import com.duddy.portugues.data.local.dao.PhraseReviewDao
import com.duddy.portugues.data.local.dao.ProgressStatsDao
import com.duddy.portugues.data.local.entity.FavoritePhraseEntity
import com.duddy.portugues.data.local.entity.PhraseReviewEntity
import com.duddy.portugues.data.local.entity.ProgressStatsEntity

@Database(
    entities = [
        PhraseReviewEntity::class,
        FavoritePhraseEntity::class,
        ProgressStatsEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class DuddyDatabase : RoomDatabase() {

    abstract fun reviewDao():   PhraseReviewDao
    abstract fun favoriteDao(): FavoritePhraseDao
    abstract fun progressDao(): ProgressStatsDao

    companion object {
        private const val DB_NAME = "duddy.db"

        @Volatile
        private var instance: DuddyDatabase? = null

        fun getInstance(context: Context): DuddyDatabase =
            instance ?: synchronized(this) {
                instance ?: build(context).also { instance = it }
            }

        private fun build(context: Context): DuddyDatabase =
            Room.databaseBuilder(
                context.applicationContext,
                DuddyDatabase::class.java,
                DB_NAME,
            )
                // Future versions: add .addMigrations(MIGRATION_1_2) etc.
                .fallbackToDestructiveMigration()
                .build()
    }
}
