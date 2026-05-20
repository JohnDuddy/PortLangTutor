package com.duddy.portugues.data.local.dao;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.EntityUpsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.duddy.portugues.data.local.entity.ProgressStatsEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class ProgressStatsDao_Impl implements ProgressStatsDao {
  private final RoomDatabase __db;

  private final EntityUpsertionAdapter<ProgressStatsEntity> __upsertionAdapterOfProgressStatsEntity;

  public ProgressStatsDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__upsertionAdapterOfProgressStatsEntity = new EntityUpsertionAdapter<ProgressStatsEntity>(new EntityInsertionAdapter<ProgressStatsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `progress_stats` (`id`,`lessons_started`,`practiced_phrases`,`sample_audio_plays`,`speaking_attempts`,`ai_coach_requests`,`streak_days`,`last_active_date`,`total_xp`,`longest_streak`,`updated_at`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProgressStatsEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getLessonsStarted());
        statement.bindLong(3, entity.getPracticedPhrases());
        statement.bindLong(4, entity.getSampleAudioPlays());
        statement.bindLong(5, entity.getSpeakingAttempts());
        statement.bindLong(6, entity.getAiCoachRequests());
        statement.bindLong(7, entity.getStreakDays());
        if (entity.getLastActiveDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getLastActiveDate());
        }
        statement.bindLong(9, entity.getTotalXp());
        statement.bindLong(10, entity.getLongestStreak());
        statement.bindLong(11, entity.getUpdatedAt());
      }
    }, new EntityDeletionOrUpdateAdapter<ProgressStatsEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `progress_stats` SET `id` = ?,`lessons_started` = ?,`practiced_phrases` = ?,`sample_audio_plays` = ?,`speaking_attempts` = ?,`ai_coach_requests` = ?,`streak_days` = ?,`last_active_date` = ?,`total_xp` = ?,`longest_streak` = ?,`updated_at` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final ProgressStatsEntity entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getLessonsStarted());
        statement.bindLong(3, entity.getPracticedPhrases());
        statement.bindLong(4, entity.getSampleAudioPlays());
        statement.bindLong(5, entity.getSpeakingAttempts());
        statement.bindLong(6, entity.getAiCoachRequests());
        statement.bindLong(7, entity.getStreakDays());
        if (entity.getLastActiveDate() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getLastActiveDate());
        }
        statement.bindLong(9, entity.getTotalXp());
        statement.bindLong(10, entity.getLongestStreak());
        statement.bindLong(11, entity.getUpdatedAt());
        statement.bindLong(12, entity.getId());
      }
    });
  }

  @Override
  public Object upsert(final ProgressStatsEntity stats,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfProgressStatsEntity.upsert(stats);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<ProgressStatsEntity> observe() {
    final String _sql = "SELECT * FROM progress_stats WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"progress_stats"}, new Callable<ProgressStatsEntity>() {
      @Override
      @Nullable
      public ProgressStatsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLessonsStarted = CursorUtil.getColumnIndexOrThrow(_cursor, "lessons_started");
          final int _cursorIndexOfPracticedPhrases = CursorUtil.getColumnIndexOrThrow(_cursor, "practiced_phrases");
          final int _cursorIndexOfSampleAudioPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "sample_audio_plays");
          final int _cursorIndexOfSpeakingAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "speaking_attempts");
          final int _cursorIndexOfAiCoachRequests = CursorUtil.getColumnIndexOrThrow(_cursor, "ai_coach_requests");
          final int _cursorIndexOfStreakDays = CursorUtil.getColumnIndexOrThrow(_cursor, "streak_days");
          final int _cursorIndexOfLastActiveDate = CursorUtil.getColumnIndexOrThrow(_cursor, "last_active_date");
          final int _cursorIndexOfTotalXp = CursorUtil.getColumnIndexOrThrow(_cursor, "total_xp");
          final int _cursorIndexOfLongestStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "longest_streak");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final ProgressStatsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpLessonsStarted;
            _tmpLessonsStarted = _cursor.getInt(_cursorIndexOfLessonsStarted);
            final int _tmpPracticedPhrases;
            _tmpPracticedPhrases = _cursor.getInt(_cursorIndexOfPracticedPhrases);
            final int _tmpSampleAudioPlays;
            _tmpSampleAudioPlays = _cursor.getInt(_cursorIndexOfSampleAudioPlays);
            final int _tmpSpeakingAttempts;
            _tmpSpeakingAttempts = _cursor.getInt(_cursorIndexOfSpeakingAttempts);
            final int _tmpAiCoachRequests;
            _tmpAiCoachRequests = _cursor.getInt(_cursorIndexOfAiCoachRequests);
            final int _tmpStreakDays;
            _tmpStreakDays = _cursor.getInt(_cursorIndexOfStreakDays);
            final String _tmpLastActiveDate;
            if (_cursor.isNull(_cursorIndexOfLastActiveDate)) {
              _tmpLastActiveDate = null;
            } else {
              _tmpLastActiveDate = _cursor.getString(_cursorIndexOfLastActiveDate);
            }
            final int _tmpTotalXp;
            _tmpTotalXp = _cursor.getInt(_cursorIndexOfTotalXp);
            final int _tmpLongestStreak;
            _tmpLongestStreak = _cursor.getInt(_cursorIndexOfLongestStreak);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new ProgressStatsEntity(_tmpId,_tmpLessonsStarted,_tmpPracticedPhrases,_tmpSampleAudioPlays,_tmpSpeakingAttempts,_tmpAiCoachRequests,_tmpStreakDays,_tmpLastActiveDate,_tmpTotalXp,_tmpLongestStreak,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object get(final Continuation<? super ProgressStatsEntity> $completion) {
    final String _sql = "SELECT * FROM progress_stats WHERE id = 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<ProgressStatsEntity>() {
      @Override
      @Nullable
      public ProgressStatsEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfLessonsStarted = CursorUtil.getColumnIndexOrThrow(_cursor, "lessons_started");
          final int _cursorIndexOfPracticedPhrases = CursorUtil.getColumnIndexOrThrow(_cursor, "practiced_phrases");
          final int _cursorIndexOfSampleAudioPlays = CursorUtil.getColumnIndexOrThrow(_cursor, "sample_audio_plays");
          final int _cursorIndexOfSpeakingAttempts = CursorUtil.getColumnIndexOrThrow(_cursor, "speaking_attempts");
          final int _cursorIndexOfAiCoachRequests = CursorUtil.getColumnIndexOrThrow(_cursor, "ai_coach_requests");
          final int _cursorIndexOfStreakDays = CursorUtil.getColumnIndexOrThrow(_cursor, "streak_days");
          final int _cursorIndexOfLastActiveDate = CursorUtil.getColumnIndexOrThrow(_cursor, "last_active_date");
          final int _cursorIndexOfTotalXp = CursorUtil.getColumnIndexOrThrow(_cursor, "total_xp");
          final int _cursorIndexOfLongestStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "longest_streak");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final ProgressStatsEntity _result;
          if (_cursor.moveToFirst()) {
            final int _tmpId;
            _tmpId = _cursor.getInt(_cursorIndexOfId);
            final int _tmpLessonsStarted;
            _tmpLessonsStarted = _cursor.getInt(_cursorIndexOfLessonsStarted);
            final int _tmpPracticedPhrases;
            _tmpPracticedPhrases = _cursor.getInt(_cursorIndexOfPracticedPhrases);
            final int _tmpSampleAudioPlays;
            _tmpSampleAudioPlays = _cursor.getInt(_cursorIndexOfSampleAudioPlays);
            final int _tmpSpeakingAttempts;
            _tmpSpeakingAttempts = _cursor.getInt(_cursorIndexOfSpeakingAttempts);
            final int _tmpAiCoachRequests;
            _tmpAiCoachRequests = _cursor.getInt(_cursorIndexOfAiCoachRequests);
            final int _tmpStreakDays;
            _tmpStreakDays = _cursor.getInt(_cursorIndexOfStreakDays);
            final String _tmpLastActiveDate;
            if (_cursor.isNull(_cursorIndexOfLastActiveDate)) {
              _tmpLastActiveDate = null;
            } else {
              _tmpLastActiveDate = _cursor.getString(_cursorIndexOfLastActiveDate);
            }
            final int _tmpTotalXp;
            _tmpTotalXp = _cursor.getInt(_cursorIndexOfTotalXp);
            final int _tmpLongestStreak;
            _tmpLongestStreak = _cursor.getInt(_cursorIndexOfLongestStreak);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new ProgressStatsEntity(_tmpId,_tmpLessonsStarted,_tmpPracticedPhrases,_tmpSampleAudioPlays,_tmpSpeakingAttempts,_tmpAiCoachRequests,_tmpStreakDays,_tmpLastActiveDate,_tmpTotalXp,_tmpLongestStreak,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
