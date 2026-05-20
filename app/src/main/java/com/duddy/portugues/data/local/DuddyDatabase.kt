package com.duddy.portugues.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.migration.Migration
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 2,
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
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration()
                .build()

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN hearts INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN max_hearts INTEGER NOT NULL DEFAULT 5")
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN last_heart_refill_at INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN daily_xp_goal INTEGER NOT NULL DEFAULT 50")
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN today_xp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN today_xp_date TEXT")
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN weekly_league_xp INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN league_name TEXT NOT NULL DEFAULT 'Bronze'")
                db.execSQL("ALTER TABLE progress_stats ADD COLUMN league_week_id TEXT")
            }
        }
    }
}
