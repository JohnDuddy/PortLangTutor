package com.duddy.portugues

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.duddy.portugues.data.local.DuddyDatabase
import com.duddy.portugues.data.local.dao.FavoritePhraseDao
import com.duddy.portugues.data.local.dao.PhraseReviewDao
import com.duddy.portugues.data.local.dao.ProgressStatsDao
import com.duddy.portugues.data.local.entity.FavoritePhraseEntity
import com.duddy.portugues.data.local.entity.PhraseReviewEntity
import com.duddy.portugues.data.local.entity.ProgressStatsEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DuddyDatabaseTest {
    private lateinit var db: DuddyDatabase
    private lateinit var reviewDao: PhraseReviewDao
    private lateinit var favoriteDao: FavoritePhraseDao
    private lateinit var progressDao: ProgressStatsDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, DuddyDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        reviewDao = db.reviewDao()
        favoriteDao = db.favoriteDao()
        progressDao = db.progressDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun phraseReview_upsertThenGetById_returnsEntity() = runBlocking {
        val entity = reviewEntity("phrase-1", intervalDays = 4, dueDate = "2026-05-20")

        reviewDao.upsert(entity)

        assertEquals(entity, reviewDao.getById("phrase-1"))
    }

    @Test
    fun phraseReview_upsertSameId_updatesExisting() = runBlocking {
        reviewDao.upsert(reviewEntity("phrase-1", intervalDays = 1))
        reviewDao.upsert(reviewEntity("phrase-1", intervalDays = 8))

        assertEquals(8, reviewDao.getById("phrase-1")?.intervalDays)
        assertEquals(listOf("phrase-1"), reviewDao.getAllReviewedIds())
    }

    @Test
    fun phraseReview_getDueIds_filtersCorrectly() = runBlocking {
        reviewDao.upsert(reviewEntity("due", dueDate = "2024-01-01"))
        reviewDao.upsert(reviewEntity("future", dueDate = "2099-01-01"))

        assertEquals(listOf("due"), reviewDao.getDueIds(today = "2025-01-01", limit = 10))
    }

    @Test
    fun phraseReview_getAllReviewedIds_returnsAllPhraseIds() = runBlocking {
        reviewDao.upsert(reviewEntity("one"))
        reviewDao.upsert(reviewEntity("two"))
        reviewDao.upsert(reviewEntity("three"))

        assertEquals(setOf("one", "two", "three"), reviewDao.getAllReviewedIds().toSet())
    }

    @Test
    fun phraseReview_clear_removesEverything() = runBlocking {
        reviewDao.upsert(reviewEntity("phrase-1"))

        reviewDao.clear()

        assertNull(reviewDao.getById("phrase-1"))
    }

    @Test
    fun favorite_addThenGetIds_containsId() = runBlocking {
        favoriteDao.add(FavoritePhraseEntity("phrase-1"))

        assertTrue(favoriteDao.getIds().contains("phrase-1"))
    }

    @Test
    fun favorite_removeThenGetIds_doesNotContainId() = runBlocking {
        favoriteDao.add(FavoritePhraseEntity("phrase-1"))
        favoriteDao.remove("phrase-1")

        assertFalse(favoriteDao.getIds().contains("phrase-1"))
    }

    @Test
    fun favorite_isFavorite_returnsTrueForExisting() = runBlocking {
        favoriteDao.add(FavoritePhraseEntity("phrase-1"))

        assertTrue(favoriteDao.isFavorite("phrase-1"))
    }

    @Test
    fun progress_upsertThenGet_returnsStats() = runBlocking {
        val stats = ProgressStatsEntity(
            lessonsStarted = 2,
            practicedPhrases = 7,
            sampleAudioPlays = 3,
            speakingAttempts = 4,
            aiCoachRequests = 1,
            streakDays = 5,
        )

        progressDao.upsert(stats)

        assertEquals(stats, progressDao.get())
    }

    @Test
    fun progress_observe_emitsLatestStats() = runBlocking {
        progressDao.upsert(ProgressStatsEntity(lessonsStarted = 1))
        progressDao.upsert(ProgressStatsEntity(lessonsStarted = 2))

        assertEquals(2, progressDao.observe().first()?.lessonsStarted)
    }

    private fun reviewEntity(
        phraseId: String,
        dueDate: String = "2025-01-01",
        intervalDays: Int = 1,
    ) = PhraseReviewEntity(
        phraseId = phraseId,
        dueDate = dueDate,
        intervalDays = intervalDays,
        easeFactor = 2.3,
        reviewCount = 1,
        correctStreak = 1,
        lastScore = 84,
        updatedAt = 1_700_000_000_000,
        synced = false,
    )
}
