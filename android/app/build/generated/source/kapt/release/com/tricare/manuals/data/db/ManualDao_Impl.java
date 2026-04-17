package com.tricare.manuals.data.db;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.tricare.manuals.data.model.Manual;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class ManualDao_Impl implements ManualDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Manual> __insertionAdapterOfManual;

  private final EntityDeletionOrUpdateAdapter<Manual> __deletionAdapterOfManual;

  private final EntityDeletionOrUpdateAdapter<Manual> __updateAdapterOfManual;

  private final SharedSQLiteStatement __preparedStmtOfUpdateDownloadInfo;

  private final SharedSQLiteStatement __preparedStmtOfUpdateLatestChange;

  private final SharedSQLiteStatement __preparedStmtOfClearDownloadInfo;

  private final SharedSQLiteStatement __preparedStmtOfClearAllDownloadInfo;

  public ManualDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfManual = new EntityInsertionAdapter<Manual>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `manuals` (`code`,`name`,`latestChange`,`downloadedChange`,`downloadedFormat`,`filePath`,`downloadedAt`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Manual entity) {
        if (entity.getCode() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getCode());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        statement.bindLong(3, entity.getLatestChange());
        if (entity.getDownloadedChange() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getDownloadedChange());
        }
        if (entity.getDownloadedFormat() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDownloadedFormat());
        }
        if (entity.getFilePath() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getFilePath());
        }
        if (entity.getDownloadedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getDownloadedAt());
        }
      }
    };
    this.__deletionAdapterOfManual = new EntityDeletionOrUpdateAdapter<Manual>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `manuals` WHERE `code` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Manual entity) {
        if (entity.getCode() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getCode());
        }
      }
    };
    this.__updateAdapterOfManual = new EntityDeletionOrUpdateAdapter<Manual>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `manuals` SET `code` = ?,`name` = ?,`latestChange` = ?,`downloadedChange` = ?,`downloadedFormat` = ?,`filePath` = ?,`downloadedAt` = ? WHERE `code` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Manual entity) {
        if (entity.getCode() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getCode());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        statement.bindLong(3, entity.getLatestChange());
        if (entity.getDownloadedChange() == null) {
          statement.bindNull(4);
        } else {
          statement.bindLong(4, entity.getDownloadedChange());
        }
        if (entity.getDownloadedFormat() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getDownloadedFormat());
        }
        if (entity.getFilePath() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getFilePath());
        }
        if (entity.getDownloadedAt() == null) {
          statement.bindNull(7);
        } else {
          statement.bindLong(7, entity.getDownloadedAt());
        }
        if (entity.getCode() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getCode());
        }
      }
    };
    this.__preparedStmtOfUpdateDownloadInfo = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE manuals\n"
                + "        SET downloadedChange = ?,\n"
                + "            downloadedFormat = ?,\n"
                + "            filePath = ?,\n"
                + "            downloadedAt = ?\n"
                + "        WHERE code = ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfUpdateLatestChange = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE manuals SET latestChange = ? WHERE code = ?";
        return _query;
      }
    };
    this.__preparedStmtOfClearDownloadInfo = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "\n"
                + "        UPDATE manuals\n"
                + "        SET downloadedChange = NULL,\n"
                + "            downloadedFormat = NULL,\n"
                + "            filePath = NULL,\n"
                + "            downloadedAt = NULL\n"
                + "        WHERE code = ?\n"
                + "    ";
        return _query;
      }
    };
    this.__preparedStmtOfClearAllDownloadInfo = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "UPDATE manuals SET downloadedChange = NULL, downloadedFormat = NULL, filePath = NULL, downloadedAt = NULL";
        return _query;
      }
    };
  }

  @Override
  public Object upsertManual(final Manual manual, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfManual.insert(manual);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object upsertManuals(final List<Manual> manuals,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfManual.insert(manuals);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteManual(final Manual manual, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfManual.handle(manual);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateManual(final Manual manual, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfManual.handle(manual);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateDownloadInfo(final String code, final int change, final String format,
      final String path, final long timestamp, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateDownloadInfo.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, change);
        _argIndex = 2;
        if (format == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, format);
        }
        _argIndex = 3;
        if (path == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, path);
        }
        _argIndex = 4;
        _stmt.bindLong(_argIndex, timestamp);
        _argIndex = 5;
        if (code == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, code);
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
          __preparedStmtOfUpdateDownloadInfo.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object updateLatestChange(final String code, final int latestChange,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfUpdateLatestChange.acquire();
        int _argIndex = 1;
        _stmt.bindLong(_argIndex, latestChange);
        _argIndex = 2;
        if (code == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, code);
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
          __preparedStmtOfUpdateLatestChange.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearDownloadInfo(final String code, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearDownloadInfo.acquire();
        int _argIndex = 1;
        if (code == null) {
          _stmt.bindNull(_argIndex);
        } else {
          _stmt.bindString(_argIndex, code);
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
          __preparedStmtOfClearDownloadInfo.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Object clearAllDownloadInfo(final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfClearAllDownloadInfo.acquire();
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
          __preparedStmtOfClearAllDownloadInfo.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<Manual>> getAllManuals() {
    final String _sql = "SELECT * FROM manuals ORDER BY name ASC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"manuals"}, new Callable<List<Manual>>() {
      @Override
      @NonNull
      public List<Manual> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCode = CursorUtil.getColumnIndexOrThrow(_cursor, "code");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatestChange = CursorUtil.getColumnIndexOrThrow(_cursor, "latestChange");
          final int _cursorIndexOfDownloadedChange = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedChange");
          final int _cursorIndexOfDownloadedFormat = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedFormat");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfDownloadedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedAt");
          final List<Manual> _result = new ArrayList<Manual>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Manual _item;
            final String _tmpCode;
            if (_cursor.isNull(_cursorIndexOfCode)) {
              _tmpCode = null;
            } else {
              _tmpCode = _cursor.getString(_cursorIndexOfCode);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final int _tmpLatestChange;
            _tmpLatestChange = _cursor.getInt(_cursorIndexOfLatestChange);
            final Integer _tmpDownloadedChange;
            if (_cursor.isNull(_cursorIndexOfDownloadedChange)) {
              _tmpDownloadedChange = null;
            } else {
              _tmpDownloadedChange = _cursor.getInt(_cursorIndexOfDownloadedChange);
            }
            final String _tmpDownloadedFormat;
            if (_cursor.isNull(_cursorIndexOfDownloadedFormat)) {
              _tmpDownloadedFormat = null;
            } else {
              _tmpDownloadedFormat = _cursor.getString(_cursorIndexOfDownloadedFormat);
            }
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final Long _tmpDownloadedAt;
            if (_cursor.isNull(_cursorIndexOfDownloadedAt)) {
              _tmpDownloadedAt = null;
            } else {
              _tmpDownloadedAt = _cursor.getLong(_cursorIndexOfDownloadedAt);
            }
            _item = new Manual(_tmpCode,_tmpName,_tmpLatestChange,_tmpDownloadedChange,_tmpDownloadedFormat,_tmpFilePath,_tmpDownloadedAt);
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
  public Object getManual(final String code, final Continuation<? super Manual> $completion) {
    final String _sql = "SELECT * FROM manuals WHERE code = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (code == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, code);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Manual>() {
      @Override
      @Nullable
      public Manual call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfCode = CursorUtil.getColumnIndexOrThrow(_cursor, "code");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfLatestChange = CursorUtil.getColumnIndexOrThrow(_cursor, "latestChange");
          final int _cursorIndexOfDownloadedChange = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedChange");
          final int _cursorIndexOfDownloadedFormat = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedFormat");
          final int _cursorIndexOfFilePath = CursorUtil.getColumnIndexOrThrow(_cursor, "filePath");
          final int _cursorIndexOfDownloadedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "downloadedAt");
          final Manual _result;
          if (_cursor.moveToFirst()) {
            final String _tmpCode;
            if (_cursor.isNull(_cursorIndexOfCode)) {
              _tmpCode = null;
            } else {
              _tmpCode = _cursor.getString(_cursorIndexOfCode);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final int _tmpLatestChange;
            _tmpLatestChange = _cursor.getInt(_cursorIndexOfLatestChange);
            final Integer _tmpDownloadedChange;
            if (_cursor.isNull(_cursorIndexOfDownloadedChange)) {
              _tmpDownloadedChange = null;
            } else {
              _tmpDownloadedChange = _cursor.getInt(_cursorIndexOfDownloadedChange);
            }
            final String _tmpDownloadedFormat;
            if (_cursor.isNull(_cursorIndexOfDownloadedFormat)) {
              _tmpDownloadedFormat = null;
            } else {
              _tmpDownloadedFormat = _cursor.getString(_cursorIndexOfDownloadedFormat);
            }
            final String _tmpFilePath;
            if (_cursor.isNull(_cursorIndexOfFilePath)) {
              _tmpFilePath = null;
            } else {
              _tmpFilePath = _cursor.getString(_cursorIndexOfFilePath);
            }
            final Long _tmpDownloadedAt;
            if (_cursor.isNull(_cursorIndexOfDownloadedAt)) {
              _tmpDownloadedAt = null;
            } else {
              _tmpDownloadedAt = _cursor.getLong(_cursorIndexOfDownloadedAt);
            }
            _result = new Manual(_tmpCode,_tmpName,_tmpLatestChange,_tmpDownloadedChange,_tmpDownloadedFormat,_tmpFilePath,_tmpDownloadedAt);
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
