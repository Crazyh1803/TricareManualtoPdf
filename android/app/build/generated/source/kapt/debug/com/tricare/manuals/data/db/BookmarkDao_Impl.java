package com.tricare.manuals.data.db;

import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.tricare.manuals.data.model.Bookmark;
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
public final class BookmarkDao_Impl implements BookmarkDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Bookmark> __insertionAdapterOfBookmark;

  private final SharedSQLiteStatement __preparedStmtOfDeleteBookmark;

  private final SharedSQLiteStatement __preparedStmtOfClearBookmarksForManual;

  public BookmarkDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfBookmark = new EntityInsertionAdapter<Bookmark>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `bookmarks` (`id`,`manualCode`,`sectionFilename`,`sectionTitle`,`scrollY`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Bookmark entity) {
        statement.bindLong(1, entity.getId());
        if (entity.getManualCode() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getManualCode());
        }
        if (entity.getSectionFilename() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getSectionFilename());
        }
        if (entity.getSectionTitle() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getSectionTitle());
        }
        statement.bindLong(5, entity.getScrollY());
        statement.bindLong(6, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDeleteBookmark = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bookmarks WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearBookmarksForManual = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM bookmarks WHERE manualCode = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertBookmark(final Bookmark bookmark,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfBookmark.insert(bookmark);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteBookmark(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteBookmark.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, id);
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
          __preparedStmtOfDeleteBookmark.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearBookmarksForManual(final String manualCode,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearBookmarksForManual.acquire();
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
          __preparedStmtOfClearBookmarksForManual.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Bookmark>> getBookmarks(final String manualCode) {
    final String _sql = "SELECT * FROM bookmarks WHERE manualCode = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (manualCode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, manualCode);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"bookmarks"}, new Callable<List<Bookmark>>() {
      @Override
      @NonNull
      public List<Bookmark> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfManualCode = CursorUtil.getColumnIndexOrThrow(_cursor, "manualCode");
          final int _cursorIndexOfSectionFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "sectionFilename");
          final int _cursorIndexOfSectionTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "sectionTitle");
          final int _cursorIndexOfScrollY = CursorUtil.getColumnIndexOrThrow(_cursor, "scrollY");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Bookmark> _result = new ArrayList<Bookmark>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Bookmark _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final String _tmpManualCode;
            if (_cursor.isNull(_cursorIndexOfManualCode)) {
              _tmpManualCode = null;
            } else {
              _tmpManualCode = _cursor.getString(_cursorIndexOfManualCode);
            }
            final String _tmpSectionFilename;
            if (_cursor.isNull(_cursorIndexOfSectionFilename)) {
              _tmpSectionFilename = null;
            } else {
              _tmpSectionFilename = _cursor.getString(_cursorIndexOfSectionFilename);
            }
            final String _tmpSectionTitle;
            if (_cursor.isNull(_cursorIndexOfSectionTitle)) {
              _tmpSectionTitle = null;
            } else {
              _tmpSectionTitle = _cursor.getString(_cursorIndexOfSectionTitle);
            }
            final int _tmpScrollY;
            _tmpScrollY = _cursor.getInt(_cursorIndexOfScrollY);
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Bookmark(_tmpId,_tmpManualCode,_tmpSectionFilename,_tmpSectionTitle,_tmpScrollY,_tmpCreatedAt);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
