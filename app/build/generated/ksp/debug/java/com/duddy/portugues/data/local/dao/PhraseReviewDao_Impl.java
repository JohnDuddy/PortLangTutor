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
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.room.util.StringUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.duddy.portugues.data.local.entity.PhraseReviewEntity;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.StringBuilder;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class PhraseReviewDao_Impl implements PhraseReviewDao {
  private final RoomDatabase __db;

  private final SharedSQLiteStatement __preparedStmtOfClear;

  private final EntityUpsertionAdapter<PhraseReviewEntity> __upsertionAdapterOfPhraseReviewEntity;

  public PhraseReviewDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__preparedStmtOfClear = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM phrase_review";
        return _query;
      }
    };
    this.__upsertionAdapterOfPhraseReviewEntity = new EntityUpsertionAdapter<PhraseReviewEntity>(new EntityInsertionAdapter<PhraseReviewEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT INTO `phrase_review` (`phrase_id`,`due_date`,`interval_days`,`ease_factor`,`review_count`,`correct_streak`,`last_score`,`updated_at`,`synced`) VALUES (?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PhraseReviewEntity entity) {
        statement.bindString(1, entity.getPhraseId());
        statement.bindString(2, entity.getDueDate());
        statement.bindLong(3, entity.getIntervalDays());
        statement.bindDouble(4, entity.getEaseFactor());
        statement.bindLong(5, entity.getReviewCount());
        statement.bindLong(6, entity.getCorrectStreak());
        if (entity.getLastScore() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getLastScore());
        }
        statement.bindLong(8, entity.getUpdatedAt());
        final int _tmp = entity.getSynced() ? 1 : 0;
        statement.bindLong(9, _tmp);
      }
    }, new EntityDeletionOrUpdateAdapter<PhraseReviewEntity>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE `phrase_review` SET `phrase_id` = ?,`due_date` = ?,`interval_days` = ?,`ease_factor` = ?,`review_count` = ?,`correct_streak` = ?,`last_score` = ?,`updated_at` = ?,`synced` = ? WHERE `phrase_id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final PhraseReviewEntity entity) {
        statement.bindString(1, entity.getPhraseId());
        statement.bindString(2, entity.getDueDate());
        statement.bindLong(3, entity.getIntervalDays());
        statement.bindDouble(4, entity.getEaseFactor());
        statement.bindLong(5, entity.getReviewCount());
        statement.bindLong(6, entity.getCorrectStreak());
        if (entity.getLastScore() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getLastScore());
        }
        statement.bindLong(8, entity.getUpdatedAt());
        final int _tmp = entity.getSynced() ? 1 : 0;
        statement.bindLong(9, _tmp);
        statement.bindString(10, entity.getPhraseId());
      }
    });
  }

  @Override
  public Object clear(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClear.acquire();
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfClear.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object upsert(final PhraseReviewEntity review,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __upsertionAdapterOfPhraseReviewEntity.upsert(review);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object getById(final String phraseId,
      final Continuation<? super PhraseReviewEntity> $completion) {
    final String _sql = "SELECT * FROM phrase_review WHERE phrase_id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, phraseId);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<PhraseReviewEntity>() {
      @Override
      @Nullable
      public PhraseReviewEntity call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPhraseId = CursorUtil.getColumnIndexOrThrow(_cursor, "phrase_id");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "due_date");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "interval_days");
          final int _cursorIndexOfEaseFactor = CursorUtil.getColumnIndexOrThrow(_cursor, "ease_factor");
          final int _cursorIndexOfReviewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "review_count");
          final int _cursorIndexOfCorrectStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "correct_streak");
          final int _cursorIndexOfLastScore = CursorUtil.getColumnIndexOrThrow(_cursor, "last_score");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final PhraseReviewEntity _result;
          if (_cursor.moveToFirst()) {
            final String _tmpPhraseId;
            _tmpPhraseId = _cursor.getString(_cursorIndexOfPhraseId);
            final String _tmpDueDate;
            _tmpDueDate = _cursor.getString(_cursorIndexOfDueDate);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final double _tmpEaseFactor;
            _tmpEaseFactor = _cursor.getDouble(_cursorIndexOfEaseFactor);
            final int _tmpReviewCount;
            _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount);
            final int _tmpCorrectStreak;
            _tmpCorrectStreak = _cursor.getInt(_cursorIndexOfCorrectStreak);
            final Integer _tmpLastScore;
            if (_cursor.isNull(_cursorIndexOfLastScore)) {
              _tmpLastScore = null;
            } else {
              _tmpLastScore = _cursor.getInt(_cursorIndexOfLastScore);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp != 0;
            _result = new PhraseReviewEntity(_tmpPhraseId,_tmpDueDate,_tmpIntervalDays,_tmpEaseFactor,_tmpReviewCount,_tmpCorrectStreak,_tmpLastScore,_tmpUpdatedAt,_tmpSynced);
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

  @Override
  public Object getByIds(final List<String> ids,
      final Continuation<? super List<PhraseReviewEntity>> $completion) {
    final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
    _stringBuilder.append("SELECT * FROM phrase_review WHERE phrase_id IN (");
    final int _inputSize = ids.size();
    StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
    _stringBuilder.append(")");
    final String _sql = _stringBuilder.toString();
    final int _argCount = 0 + _inputSize;
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, _argCount);
    int _argIndex = 1;
    for (String _item : ids) {
      _statement.bindString(_argIndex, _item);
      _argIndex++;
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PhraseReviewEntity>>() {
      @Override
      @NonNull
      public List<PhraseReviewEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPhraseId = CursorUtil.getColumnIndexOrThrow(_cursor, "phrase_id");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "due_date");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "interval_days");
          final int _cursorIndexOfEaseFactor = CursorUtil.getColumnIndexOrThrow(_cursor, "ease_factor");
          final int _cursorIndexOfReviewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "review_count");
          final int _cursorIndexOfCorrectStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "correct_streak");
          final int _cursorIndexOfLastScore = CursorUtil.getColumnIndexOrThrow(_cursor, "last_score");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final List<PhraseReviewEntity> _result = new ArrayList<PhraseReviewEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhraseReviewEntity _item_1;
            final String _tmpPhraseId;
            _tmpPhraseId = _cursor.getString(_cursorIndexOfPhraseId);
            final String _tmpDueDate;
            _tmpDueDate = _cursor.getString(_cursorIndexOfDueDate);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final double _tmpEaseFactor;
            _tmpEaseFactor = _cursor.getDouble(_cursorIndexOfEaseFactor);
            final int _tmpReviewCount;
            _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount);
            final int _tmpCorrectStreak;
            _tmpCorrectStreak = _cursor.getInt(_cursorIndexOfCorrectStreak);
            final Integer _tmpLastScore;
            if (_cursor.isNull(_cursorIndexOfLastScore)) {
              _tmpLastScore = null;
            } else {
              _tmpLastScore = _cursor.getInt(_cursorIndexOfLastScore);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp != 0;
            _item_1 = new PhraseReviewEntity(_tmpPhraseId,_tmpDueDate,_tmpIntervalDays,_tmpEaseFactor,_tmpReviewCount,_tmpCorrectStreak,_tmpLastScore,_tmpUpdatedAt,_tmpSynced);
            _result.add(_item_1);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getDueIds(final String today, final int limit,
      final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT phrase_id FROM phrase_review WHERE due_date <= ? ORDER BY due_date ASC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    _statement.bindString(_argIndex, today);
    _argIndex = 2;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object getAllReviewedIds(final Continuation<? super List<String>> $completion) {
    final String _sql = "SELECT phrase_id FROM phrase_review";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<String>>() {
      @Override
      @NonNull
      public List<String> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final List<String> _result = new ArrayList<String>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final String _item;
            _item = _cursor.getString(0);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<Integer> observeDueCount(final String today) {
    final String _sql = "SELECT COUNT(*) FROM phrase_review WHERE due_date <= ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, today);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"phrase_review"}, new Callable<Integer>() {
      @Override
      @NonNull
      public Integer call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final Integer _result;
          if (_cursor.moveToFirst()) {
            final int _tmp;
            _tmp = _cursor.getInt(0);
            _result = _tmp;
          } else {
            _result = 0;
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
  public Object getUnsynced(final int limit,
      final Continuation<? super List<PhraseReviewEntity>> $completion) {
    final String _sql = "SELECT * FROM phrase_review WHERE synced = 0 ORDER BY updated_at ASC LIMIT ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, limit);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<PhraseReviewEntity>>() {
      @Override
      @NonNull
      public List<PhraseReviewEntity> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfPhraseId = CursorUtil.getColumnIndexOrThrow(_cursor, "phrase_id");
          final int _cursorIndexOfDueDate = CursorUtil.getColumnIndexOrThrow(_cursor, "due_date");
          final int _cursorIndexOfIntervalDays = CursorUtil.getColumnIndexOrThrow(_cursor, "interval_days");
          final int _cursorIndexOfEaseFactor = CursorUtil.getColumnIndexOrThrow(_cursor, "ease_factor");
          final int _cursorIndexOfReviewCount = CursorUtil.getColumnIndexOrThrow(_cursor, "review_count");
          final int _cursorIndexOfCorrectStreak = CursorUtil.getColumnIndexOrThrow(_cursor, "correct_streak");
          final int _cursorIndexOfLastScore = CursorUtil.getColumnIndexOrThrow(_cursor, "last_score");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updated_at");
          final int _cursorIndexOfSynced = CursorUtil.getColumnIndexOrThrow(_cursor, "synced");
          final List<PhraseReviewEntity> _result = new ArrayList<PhraseReviewEntity>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final PhraseReviewEntity _item;
            final String _tmpPhraseId;
            _tmpPhraseId = _cursor.getString(_cursorIndexOfPhraseId);
            final String _tmpDueDate;
            _tmpDueDate = _cursor.getString(_cursorIndexOfDueDate);
            final int _tmpIntervalDays;
            _tmpIntervalDays = _cursor.getInt(_cursorIndexOfIntervalDays);
            final double _tmpEaseFactor;
            _tmpEaseFactor = _cursor.getDouble(_cursorIndexOfEaseFactor);
            final int _tmpReviewCount;
            _tmpReviewCount = _cursor.getInt(_cursorIndexOfReviewCount);
            final int _tmpCorrectStreak;
            _tmpCorrectStreak = _cursor.getInt(_cursorIndexOfCorrectStreak);
            final Integer _tmpLastScore;
            if (_cursor.isNull(_cursorIndexOfLastScore)) {
              _tmpLastScore = null;
            } else {
              _tmpLastScore = _cursor.getInt(_cursorIndexOfLastScore);
            }
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final boolean _tmpSynced;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfSynced);
            _tmpSynced = _tmp != 0;
            _item = new PhraseReviewEntity(_tmpPhraseId,_tmpDueDate,_tmpIntervalDays,_tmpEaseFactor,_tmpReviewCount,_tmpCorrectStreak,_tmpLastScore,_tmpUpdatedAt,_tmpSynced);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Object markSynced(final List<String> ids, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final StringBuilder _stringBuilder = StringUtil.newStringBuilder();
        _stringBuilder.append("UPDATE phrase_review SET synced = 1 WHERE phrase_id IN (");
        final int _inputSize = ids.size();
        StringUtil.appendPlaceholders(_stringBuilder, _inputSize);
        _stringBuilder.append(")");
        final String _sql = _stringBuilder.toString();
        final SupportSQLiteStatement _stmt = __db.compileStatement(_sql);
        int _argIndex = 1;
        for (String _item : ids) {
          _stmt.bindString(_argIndex, _item);
          _argIndex++;
        }
        __db.beginTransaction();
        try {
          _stmt.executeUpdateDelete();
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
