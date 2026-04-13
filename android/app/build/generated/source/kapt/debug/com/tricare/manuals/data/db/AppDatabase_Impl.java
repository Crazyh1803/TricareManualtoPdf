package com.tricare.manuals.data.db;

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
public final class AppDatabase_Impl extends AppDatabase {
  private volatile ManualDao _manualDao;

  private volatile SectionDao _sectionDao;

  private volatile BookmarkDao _bookmarkDao;

  private volatile HighlightDao _highlightDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `manuals` (`code` TEXT NOT NULL, `name` TEXT NOT NULL, `latestChange` INTEGER NOT NULL, `downloadedChange` INTEGER, `downloadedFormat` TEXT, `filePath` TEXT, `downloadedAt` INTEGER, PRIMARY KEY(`code`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `sections` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manualCode` TEXT NOT NULL, `change` INTEGER NOT NULL, `filename` TEXT NOT NULL, `title` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `contentMd` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `bookmarks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manualCode` TEXT NOT NULL, `sectionFilename` TEXT NOT NULL, `sectionTitle` TEXT NOT NULL, `scrollY` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `highlights` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `manualCode` TEXT NOT NULL, `sectionFilename` TEXT NOT NULL, `startOffset` INTEGER NOT NULL, `endOffset` INTEGER NOT NULL, `selectedText` TEXT NOT NULL, `color` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'ded2ac5531dddcd10c9c62f39e903c1a')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `manuals`");
        db.execSQL("DROP TABLE IF EXISTS `sections`");
        db.execSQL("DROP TABLE IF EXISTS `bookmarks`");
        db.execSQL("DROP TABLE IF EXISTS `highlights`");
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
        final HashMap<String, TableInfo.Column> _columnsManuals = new HashMap<String, TableInfo.Column>(7);
        _columnsManuals.put("code", new TableInfo.Column("code", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsManuals.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsManuals.put("latestChange", new TableInfo.Column("latestChange", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsManuals.put("downloadedChange", new TableInfo.Column("downloadedChange", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsManuals.put("downloadedFormat", new TableInfo.Column("downloadedFormat", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsManuals.put("filePath", new TableInfo.Column("filePath", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsManuals.put("downloadedAt", new TableInfo.Column("downloadedAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysManuals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesManuals = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoManuals = new TableInfo("manuals", _columnsManuals, _foreignKeysManuals, _indicesManuals);
        final TableInfo _existingManuals = TableInfo.read(db, "manuals");
        if (!_infoManuals.equals(_existingManuals)) {
          return new RoomOpenHelper.ValidationResult(false, "manuals(com.tricare.manuals.data.model.Manual).\n"
                  + " Expected:\n" + _infoManuals + "\n"
                  + " Found:\n" + _existingManuals);
        }
        final HashMap<String, TableInfo.Column> _columnsSections = new HashMap<String, TableInfo.Column>(7);
        _columnsSections.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSections.put("manualCode", new TableInfo.Column("manualCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSections.put("change", new TableInfo.Column("change", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSections.put("filename", new TableInfo.Column("filename", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSections.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSections.put("sortOrder", new TableInfo.Column("sortOrder", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsSections.put("contentMd", new TableInfo.Column("contentMd", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysSections = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesSections = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoSections = new TableInfo("sections", _columnsSections, _foreignKeysSections, _indicesSections);
        final TableInfo _existingSections = TableInfo.read(db, "sections");
        if (!_infoSections.equals(_existingSections)) {
          return new RoomOpenHelper.ValidationResult(false, "sections(com.tricare.manuals.data.model.Section).\n"
                  + " Expected:\n" + _infoSections + "\n"
                  + " Found:\n" + _existingSections);
        }
        final HashMap<String, TableInfo.Column> _columnsBookmarks = new HashMap<String, TableInfo.Column>(6);
        _columnsBookmarks.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("manualCode", new TableInfo.Column("manualCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("sectionFilename", new TableInfo.Column("sectionFilename", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("sectionTitle", new TableInfo.Column("sectionTitle", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("scrollY", new TableInfo.Column("scrollY", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsBookmarks.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysBookmarks = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesBookmarks = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoBookmarks = new TableInfo("bookmarks", _columnsBookmarks, _foreignKeysBookmarks, _indicesBookmarks);
        final TableInfo _existingBookmarks = TableInfo.read(db, "bookmarks");
        if (!_infoBookmarks.equals(_existingBookmarks)) {
          return new RoomOpenHelper.ValidationResult(false, "bookmarks(com.tricare.manuals.data.model.Bookmark).\n"
                  + " Expected:\n" + _infoBookmarks + "\n"
                  + " Found:\n" + _existingBookmarks);
        }
        final HashMap<String, TableInfo.Column> _columnsHighlights = new HashMap<String, TableInfo.Column>(8);
        _columnsHighlights.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHighlights.put("manualCode", new TableInfo.Column("manualCode", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHighlights.put("sectionFilename", new TableInfo.Column("sectionFilename", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHighlights.put("startOffset", new TableInfo.Column("startOffset", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHighlights.put("endOffset", new TableInfo.Column("endOffset", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHighlights.put("selectedText", new TableInfo.Column("selectedText", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHighlights.put("color", new TableInfo.Column("color", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHighlights.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHighlights = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHighlights = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHighlights = new TableInfo("highlights", _columnsHighlights, _foreignKeysHighlights, _indicesHighlights);
        final TableInfo _existingHighlights = TableInfo.read(db, "highlights");
        if (!_infoHighlights.equals(_existingHighlights)) {
          return new RoomOpenHelper.ValidationResult(false, "highlights(com.tricare.manuals.data.model.Highlight).\n"
                  + " Expected:\n" + _infoHighlights + "\n"
                  + " Found:\n" + _existingHighlights);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "ded2ac5531dddcd10c9c62f39e903c1a", "2229ec335bb17c248bed5c42842b0940");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "manuals","sections","bookmarks","highlights");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    try {
      super.beginTransaction();
      _db.execSQL("DELETE FROM `manuals`");
      _db.execSQL("DELETE FROM `sections`");
      _db.execSQL("DELETE FROM `bookmarks`");
      _db.execSQL("DELETE FROM `highlights`");
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
    _typeConvertersMap.put(ManualDao.class, ManualDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(SectionDao.class, SectionDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(BookmarkDao.class, BookmarkDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(HighlightDao.class, HighlightDao_Impl.getRequiredConverters());
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
  public ManualDao manualDao() {
    if (_manualDao != null) {
      return _manualDao;
    } else {
      synchronized(this) {
        if(_manualDao == null) {
          _manualDao = new ManualDao_Impl(this);
        }
        return _manualDao;
      }
    }
  }

  @Override
  public SectionDao sectionDao() {
    if (_sectionDao != null) {
      return _sectionDao;
    } else {
      synchronized(this) {
        if(_sectionDao == null) {
          _sectionDao = new SectionDao_Impl(this);
        }
        return _sectionDao;
      }
    }
  }

  @Override
  public BookmarkDao bookmarkDao() {
    if (_bookmarkDao != null) {
      return _bookmarkDao;
    } else {
      synchronized(this) {
        if(_bookmarkDao == null) {
          _bookmarkDao = new BookmarkDao_Impl(this);
        }
        return _bookmarkDao;
      }
    }
  }

  @Override
  public HighlightDao highlightDao() {
    if (_highlightDao != null) {
      return _highlightDao;
    } else {
      synchronized(this) {
        if(_highlightDao == null) {
          _highlightDao = new HighlightDao_Impl(this);
        }
        return _highlightDao;
      }
    }
  }
}
