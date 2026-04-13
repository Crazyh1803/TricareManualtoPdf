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
import com.tricare.manuals.data.model.Highlight;
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
public final class HighlightDao_Impl implements HighlightDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Highlight> __insertionAdapterOfHighlight;

  private final SharedSQLiteStatement __preparedStmtOfDeleteHighlight;

  private final SharedSQLiteStatement __preparedStmtOfClearHighlightsForManual;

  public HighlightDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHighlight = new EntityInsertionAdapter<Highlight>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `highlights` (`id`,`manualCode`,`sectionFilename`,`startOffset`,`endOffset`,`selectedText`,`color`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Highlight entity) {
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
        statement.bindLong(4, entity.getStartOffset());
        statement.bindLong(5, entity.getEndOffset());
        if (entity.getSelectedText() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getSelectedText());
        }
        if (entity.getColor() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getColor());
        }
        statement.bindLong(8, entity.getCreatedAt());
      }
    };
    this.__preparedStmtOfDeleteHighlight = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM highlights WHERE id = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearHighlightsForManual = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM highlights WHERE manualCode = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertHighlight(final Highlight highlight,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHighlight.insert(highlight);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteHighlight(final long id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteHighlight.acquire();
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
          __preparedStmtOfDeleteHighlight.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearHighlightsForManual(final String manualCode,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearHighlightsForManual.acquire();
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
          __preparedStmtOfClearHighlightsForManual.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Highlight>> getHighlights(final String manualCode,
      final String sectionFilename) {
    final String _sql = "SELECT * FROM highlights WHERE manualCode = ? AND sectionFilename = ? ORDER BY startOffset ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 2);
    int _argIndex = 1;
    if (manualCode == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, manualCode);
    }
    _argIndex = 2;
    if (sectionFilename == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, sectionFilename);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"highlights"}, new Callable<List<Highlight>>() {
      @Override
      @NonNull
      public List<Highlight> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfManualCode = CursorUtil.getColumnIndexOrThrow(_cursor, "manualCode");
          final int _cursorIndexOfSectionFilename = CursorUtil.getColumnIndexOrThrow(_cursor, "sectionFilename");
          final int _cursorIndexOfStartOffset = CursorUtil.getColumnIndexOrThrow(_cursor, "startOffset");
          final int _cursorIndexOfEndOffset = CursorUtil.getColumnIndexOrThrow(_cursor, "endOffset");
          final int _cursorIndexOfSelectedText = CursorUtil.getColumnIndexOrThrow(_cursor, "selectedText");
          final int _cursorIndexOfColor = CursorUtil.getColumnIndexOrThrow(_cursor, "color");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final List<Highlight> _result = new ArrayList<Highlight>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Highlight _item;
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
            final int _tmpStartOffset;
            _tmpStartOffset = _cursor.getInt(_cursorIndexOfStartOffset);
            final int _tmpEndOffset;
            _tmpEndOffset = _cursor.getInt(_cursorIndexOfEndOffset);
            final String _tmpSelectedText;
            if (_cursor.isNull(_cursorIndexOfSelectedText)) {
              _tmpSelectedText = null;
            } else {
              _tmpSelectedText = _cursor.getString(_cursorIndexOfSelectedText);
            }
            final String _tmpColor;
            if (_cursor.isNull(_cursorIndexOfColor)) {
              _tmpColor = null;
            } else {
              _tmpColor = _cursor.getString(_cursorIndexOfColor);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            _item = new Highlight(_tmpId,_tmpManualCode,_tmpSectionFilename,_tmpStartOffset,_tmpEndOffset,_tmpSelectedText,_tmpColor,_tmpCreatedAt);
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
