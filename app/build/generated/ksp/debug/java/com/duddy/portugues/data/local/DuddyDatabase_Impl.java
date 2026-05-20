package com.duddy.portugues.data.local;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import com.duddy.portugues.data.local.dao.FavoritePhraseDao;
import com.duddy.portugues.data.local.dao.FavoritePhraseDao_Impl;
import com.duddy.portugues.data.local.dao.PhraseReviewDao;
import com.duddy.portugues.data.local.dao.PhraseReviewDao_Impl;
import com.duddy.portugues.data.local.dao.ProgressStatsDao;
import com.duddy.portugues.data.local.dao.ProgressStatsDao_Impl;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class DuddyDatabase_Impl extends DuddyDatabase {
  private volatile PhraseReviewDao _phraseReviewDao;

  private volatile FavoritePhraseDao _favoritePhraseDao;

  private volatile ProgressStatsDao _progressStatsDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(2) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `phrase_review` (`phrase_id` TEXT NOT NULL, `due_date` TEXT NOT NULL, `interval_days` INTEGER NOT NULL, `ease_factor` REAL NOT NULL, `review_count` INTEGER NOT NULL, `correct_streak` INTEGER NOT NULL, `last_score` INTEGER, `updated_at` INTEGER NOT NULL, `synced` INTEGER NOT NULL, PRIMARY KEY(`phrase_id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `favorite_phrase` (`phrase_id` TEXT NOT NULL, `created_at` INTEGER NOT NULL, `synced` INTEGER NOT NULL, PRIMARY KEY(`phrase_id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `progress_stats` (`id` INTEGER NOT NULL, `lessons_started` INTEGER NOT NULL, `practiced_phrases` INTEGER NOT NULL, `sample_audio_plays` INTEGER NOT NULL, `speaking_attempts` INTEGER NOT NULL, `ai_coach_requests` INTEGER NOT NULL, `streak_days` INTEGER NOT NULL, `last_active_date` TEXT, `total_xp` INTEGER NOT NULL, `longest_streak` INTEGER NOT NULL, `hearts` INTEGER NOT NULL, `max_hearts` INTEGER NOT NULL, `last_heart_refill_at` INTEGER NOT NULL, `daily_xp_goal` INTEGER NOT NULL, `today_xp` INTEGER NOT NULL, `today_xp_date` TEXT, `weekly_league_xp` INTEGER NOT NULL, `league_name` TEXT NOT NULL, `league_week_id` TEXT, `updated_at` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '1006a6e24547c2377c68705c0f144d68')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `phrase_review`");
        db.execSQL("DROP TABLE IF EXISTS `favorite_phrase`");
        db.execSQL("DROP TABLE IF EXISTS `progress_stats`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsPhraseReview = new HashMap<String, TableInfo.Column>(9);
        _columnsPhraseReview.put("phrase_id", new TableInfo.Column("phrase_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhraseReview.put("due_date", new TableInfo.Column("due_date", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhraseReview.put("interval_days", new TableInfo.Column("interval_days", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhraseReview.put("ease_factor", new TableInfo.Column("ease_factor", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhraseReview.put("review_count", new TableInfo.Column("review_count", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhraseReview.put("correct_streak", new TableInfo.Column("correct_streak", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhraseReview.put("last_score", new TableInfo.Column("last_score", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhraseReview.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsPhraseReview.put("synced", new TableInfo.Column("synced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysPhraseReview = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesPhraseReview = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoPhraseReview = new TableInfo("phrase_review", _columnsPhraseReview, _foreignKeysPhraseReview, _indicesPhraseReview);
        final TableInfo _existingPhraseReview = TableInfo.read(db, "phrase_review");
        if (!_infoPhraseReview.equals(_existingPhraseReview)) {
          return new RoomOpenHelper.ValidationResult(false, "phrase_review(com.duddy.portugues.data.local.entity.PhraseReviewEntity).\n"
                  + " Expected:\n" + _infoPhraseReview + "\n"
                  + " Found:\n" + _existingPhraseReview);
        }
        final HashMap<String, TableInfo.Column> _columnsFavoritePhrase = new HashMap<String, TableInfo.Column>(3);
        _columnsFavoritePhrase.put("phrase_id", new TableInfo.Column("phrase_id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavoritePhrase.put("created_at", new TableInfo.Column("created_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFavoritePhrase.put("synced", new TableInfo.Column("synced", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFavoritePhrase = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesFavoritePhrase = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoFavoritePhrase = new TableInfo("favorite_phrase", _columnsFavoritePhrase, _foreignKeysFavoritePhrase, _indicesFavoritePhrase);
        final TableInfo _existingFavoritePhrase = TableInfo.read(db, "favorite_phrase");
        if (!_infoFavoritePhrase.equals(_existingFavoritePhrase)) {
          return new RoomOpenHelper.ValidationResult(false, "favorite_phrase(com.duddy.portugues.data.local.entity.FavoritePhraseEntity).\n"
                  + " Expected:\n" + _infoFavoritePhrase + "\n"
                  + " Found:\n" + _existingFavoritePhrase);
        }
        final HashMap<String, TableInfo.Column> _columnsProgressStats = new HashMap<String, TableInfo.Column>(20);
        _columnsProgressStats.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("lessons_started", new TableInfo.Column("lessons_started", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("practiced_phrases", new TableInfo.Column("practiced_phrases", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("sample_audio_plays", new TableInfo.Column("sample_audio_plays", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("speaking_attempts", new TableInfo.Column("speaking_attempts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("ai_coach_requests", new TableInfo.Column("ai_coach_requests", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("streak_days", new TableInfo.Column("streak_days", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("last_active_date", new TableInfo.Column("last_active_date", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("total_xp", new TableInfo.Column("total_xp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("longest_streak", new TableInfo.Column("longest_streak", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("hearts", new TableInfo.Column("hearts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("max_hearts", new TableInfo.Column("max_hearts", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("last_heart_refill_at", new TableInfo.Column("last_heart_refill_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("daily_xp_goal", new TableInfo.Column("daily_xp_goal", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("today_xp", new TableInfo.Column("today_xp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("today_xp_date", new TableInfo.Column("today_xp_date", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("weekly_league_xp", new TableInfo.Column("weekly_league_xp", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("league_name", new TableInfo.Column("league_name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("league_week_id", new TableInfo.Column("league_week_id", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsProgressStats.put("updated_at", new TableInfo.Column("updated_at", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysProgressStats = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesProgressStats = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoProgressStats = new TableInfo("progress_stats", _columnsProgressStats, _foreignKeysProgressStats, _indicesProgressStats);
        final TableInfo _existingProgressStats = TableInfo.read(db, "progress_stats");
        if (!_infoProgressStats.equals(_existingProgressStats)) {
          return new RoomOpenHelper.ValidationResult(false, "progress_stats(com.duddy.portugues.data.local.entity.ProgressStatsEntity).\n"
                  + " Expected:\n" + _infoProgressStats + "\n"
                  + " Found:\n" + _existingProgressStats);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "1006a6e24547c2377c68705c0f144d68", "3b94983baf8e696e797138afa870f8bc");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "phrase_review","favorite_phrase","progress_stats");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `phrase_review`");
      _db.execSQL("DELETE FROM `favorite_phrase`");
      _db.execSQL("DELETE FROM `progress_stats`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(PhraseReviewDao.class, PhraseReviewDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FavoritePhraseDao.class, FavoritePhraseDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(ProgressStatsDao.class, ProgressStatsDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public PhraseReviewDao reviewDao() {
    if (_phraseReviewDao != null) {
      return _phraseReviewDao;
    } else {
      synchronized(this) {
        if(_phraseReviewDao == null) {
          _phraseReviewDao = new PhraseReviewDao_Impl(this);
        }
        return _phraseReviewDao;
      }
    }
  }

  @Override
  public FavoritePhraseDao favoriteDao() {
    if (_favoritePhraseDao != null) {
      return _favoritePhraseDao;
    } else {
      synchronized(this) {
        if(_favoritePhraseDao == null) {
          _favoritePhraseDao = new FavoritePhraseDao_Impl(this);
        }
        return _favoritePhraseDao;
      }
    }
  }

  @Override
  public ProgressStatsDao progressDao() {
    if (_progressStatsDao != null) {
      return _progressStatsDao;
    } else {
      synchronized(this) {
        if(_progressStatsDao == null) {
          _progressStatsDao = new ProgressStatsDao_Impl(this);
        }
        return _progressStatsDao;
      }
    }
  }
}
