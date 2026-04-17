package com.tricare.manuals.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.tricare.manuals.data.model.Section;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
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
public final class SectionDao_Impl implements SectionDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Section> __insertionAdapterOfSection;

  private final SharedSQLiteStatement __preparedStmtOfClearSections;

  private final SharedSQLiteStatement __preparedStmtOfClearAllSectionsForManual;

  public SectionDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfSection = new EntityInsertionAdapter<Section>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `sections` (`id`,`manualCode`,`change`,`filename`,`title`,`sortOrder`,`contentMd`) VALUES (nullif(?, 0),?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Section entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getManualCode() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getManualCode());
        }
        statement.bindLong(3, entity.getChange());
        if (entity.getFilename() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getFilename());
        }
        if (entity.getTitle() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getTitle());
        }
        statement.bindLong(6, entity.getSortOrder());
        if (entity.getContentMd() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getContentMd());
        }
      }
    };
    this.__preparedStmtOfClearSections = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sections WHERE manualCode = ? AND `change` = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllSectionsForManual = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM sections WHERE manualCode = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertSections(final List<Section> sections,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfSection.insert(sections);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object clearSections(final String manualCode, final int change,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearSections.acquire();
        int _argIndex = 1;
        if (manualCode == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, manualCode);
        }
        _argIndex = 2;
        _stmt.bindLong(_argIndex, change);
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
          __preparedStmtOfClearSections.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllSectionsForManual(final String manualCode,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllSectionsForManual.acquire();
        int _argIndex = 1;
        if (manualCode == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, manualCode);
        }
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
          __preparedStmtOfClearAllSectionsForManual.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Section>> getSections(final String manualCode, final int change) {
    final String _sql = "SELECT * FROM sections WHERE manualCode = ? AND `change` = ? ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (manualCode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, manualCode);
    }
    _argIndex = 2;
    _statement.bindLong(_argIndex, change);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"sections"}, new Callable<List<Section>>() {
      @Override
      @NonNull
      public List<Section> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfManualCode = CursorUtil.getColumnIndexOrThrow(_cursor, "manualCode");
          final int _cursorIndexOfChange = CursorUtil.getColumnIndexOrThrow(_cursor, "change");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfContentMd = CursorUtil.getColumnIndexOrThrow(_cursor, "contentMd");
          final List<Section> _result = new ArrayList<Section>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Section _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpManualCode;
            if (_cursor.isNull(_cursorIndexOfManualCode)) {
              _tmpManualCode = null;
            } else {
              _tmpManualCode = _cursor.getString(_cursorIndexOfManualCode);
            }
            final int _tmpChange;
            _tmpChange = _cursor.getInt(_cursorIndexOfChange);
            final String _tmpFilename;
            if (_cursor.isNull(_cursorIndexOfFilename)) {
              _tmpFilename = null;
            } else {
              _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpContentMd;
            if (_cursor.isNull(_cursorIndexOfContentMd)) {
              _tmpContentMd = null;
            } else {
              _tmpContentMd = _cursor.getString(_cursorIndexOfContentMd);
            }
            _item = new Section(_tmpId,_tmpManualCode,_tmpChange,_tmpFilename,_tmpTitle,_tmpSortOrder,_tmpContentMd);
            _result.add(_item);
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
  public Object getSectionsList(final String manualCode, final int change,
      final Continuation<? super List<Section>> $completion) {
    final String _sql = "SELECT * FROM sections WHERE manualCode = ? AND `change` = ? ORDER BY sortOrder ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (manualCode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, manualCode);
    }
    _argIndex = 2;
    _statement.bindLong(_argIndex, change);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<List<Section>>() {
      @Override
      @NonNull
      public List<Section> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfManualCode = CursorUtil.getColumnIndexOrThrow(_cursor, "manualCode");
          final int _cursorIndexOfChange = CursorUtil.getColumnIndexOrThrow(_cursor, "change");
          final int _cursorIndexOfFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "filename");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfSortOrder = CursorUtil.getColumnIndexOrThrow(_cursor, "sortOrder");
          final int _cursorIndexOfContentMd = CursorUtil.getColumnIndexOrThrow(_cursor, "contentMd");
          final List<Section> _result = new ArrayList<Section>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Section _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpManualCode;
            if (_cursor.isNull(_cursorIndexOfManualCode)) {
              _tmpManualCode = null;
            } else {
              _tmpManualCode = _cursor.getString(_cursorIndexOfManualCode);
            }
            final int _tmpChange;
            _tmpChange = _cursor.getInt(_cursorIndexOfChange);
            final String _tmpFilename;
            if (_cursor.isNull(_cursorIndexOfFilename)) {
              _tmpFilename = null;
            } else {
              _tmpFilename = _cursor.getString(_cursorIndexOfFilename);
            }
            final String _tmpTitle;
            if (_cursor.isNull(_cursorIndexOfTitle)) {
              _tmpTitle = null;
            } else {
              _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            }
            final int _tmpSortOrder;
            _tmpSortOrder = _cursor.getInt(_cursorIndexOfSortOrder);
            final String _tmpContentMd;
            if (_cursor.isNull(_cursorIndexOfContentMd)) {
              _tmpContentMd = null;
            } else {
              _tmpContentMd = _cursor.getString(_cursorIndexOfContentMd);
            }
            _item = new Section(_tmpId,_tmpManualCode,_tmpChange,_tmpFilename,_tmpTitle,_tmpSortOrder,_tmpContentMd);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
