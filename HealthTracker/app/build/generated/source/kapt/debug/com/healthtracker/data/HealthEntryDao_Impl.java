package com.healthtracker.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import com.healthtracker.data.converters.DateTimeConverters;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Float;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.time.LocalDateTime;
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
public final class HealthEntryDao_Impl implements HealthEntryDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<HealthEntry> __insertionAdapterOfHealthEntry;

  private final DateTimeConverters __dateTimeConverters = new DateTimeConverters();

  private final EntityDeletionOrUpdateAdapter<HealthEntry> __deletionAdapterOfHealthEntry;

  private final EntityDeletionOrUpdateAdapter<HealthEntry> __updateAdapterOfHealthEntry;

  public HealthEntryDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfHealthEntry = new EntityInsertionAdapter<HealthEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `health_entries` (`id`,`timestamp`,`weight`,`waistMeasurement`,`notes`) VALUES (nullif(?, 0),?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthEntry entity) {
        statement.bindLong(1, entity.getId());
        final String _tmp = __dateTimeConverters.fromLocalDateTime(entity.getTimestamp());
        if (_tmp == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, _tmp);
        }
        if (entity.getWeight() == null) {
          statement.bindNull(3);
        } else {
          statement.bindDouble(3, entity.getWeight());
        }
        if (entity.getWaistMeasurement() == null) {
          statement.bindNull(4);
        } else {
          statement.bindDouble(4, entity.getWaistMeasurement());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getNotes());
        }
      }
    };
    this.__deletionAdapterOfHealthEntry = new EntityDeletionOrUpdateAdapter<HealthEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `health_entries` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthEntry entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfHealthEntry = new EntityDeletionOrUpdateAdapter<HealthEntry>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `health_entries` SET `id` = ?,`timestamp` = ?,`weight` = ?,`waistMeasurement` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final HealthEntry entity) {
        statement.bindLong(1, entity.getId());
        final String _tmp = __dateTimeConverters.fromLocalDateTime(entity.getTimestamp());
        if (_tmp == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, _tmp);
        }
        if (entity.getWeight() == null) {
          statement.bindNull(3);
        } else {
          statement.bindDouble(3, entity.getWeight());
        }
        if (entity.getWaistMeasurement() == null) {
          statement.bindNull(4);
        } else {
          statement.bindDouble(4, entity.getWaistMeasurement());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getNotes());
        }
        statement.bindLong(6, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final HealthEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfHealthEntry.insert(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final HealthEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfHealthEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final HealthEntry entry, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfHealthEntry.handle(entry);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<HealthEntry>> getAllEntries() {
    final String _sql = "SELECT * FROM health_entries ORDER BY timestamp DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"health_entries"}, new Callable<List<HealthEntry>>() {
      @Override
      @NonNull
      public List<HealthEntry> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfWaistMeasurement = CursorUtil.getColumnIndexOrThrow(_cursor, "waistMeasurement");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<HealthEntry> _result = new ArrayList<HealthEntry>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final HealthEntry _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final LocalDateTime _tmpTimestamp;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfTimestamp);
            }
            _tmpTimestamp = __dateTimeConverters.toLocalDateTime(_tmp);
            final Float _tmpWeight;
            if (_cursor.isNull(_cursorIndexOfWeight)) {
              _tmpWeight = null;
            } else {
              _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            }
            final Float _tmpWaistMeasurement;
            if (_cursor.isNull(_cursorIndexOfWaistMeasurement)) {
              _tmpWaistMeasurement = null;
            } else {
              _tmpWaistMeasurement = _cursor.getFloat(_cursorIndexOfWaistMeasurement);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new HealthEntry(_tmpId,_tmpTimestamp,_tmpWeight,_tmpWaistMeasurement,_tmpNotes);
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
  public Object getEntryById(final long id, final Continuation<? super HealthEntry> $completion) {
    final String _sql = "SELECT * FROM health_entries WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<HealthEntry>() {
      @Override
      @Nullable
      public HealthEntry call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTimestamp = CursorUtil.getColumnIndexOrThrow(_cursor, "timestamp");
          final int _cursorIndexOfWeight = CursorUtil.getColumnIndexOrThrow(_cursor, "weight");
          final int _cursorIndexOfWaistMeasurement = CursorUtil.getColumnIndexOrThrow(_cursor, "waistMeasurement");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final HealthEntry _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final LocalDateTime _tmpTimestamp;
            final String _tmp;
            if (_cursor.isNull(_cursorIndexOfTimestamp)) {
              _tmp = null;
            } else {
              _tmp = _cursor.getString(_cursorIndexOfTimestamp);
            }
            _tmpTimestamp = __dateTimeConverters.toLocalDateTime(_tmp);
            final Float _tmpWeight;
            if (_cursor.isNull(_cursorIndexOfWeight)) {
              _tmpWeight = null;
            } else {
              _tmpWeight = _cursor.getFloat(_cursorIndexOfWeight);
            }
            final Float _tmpWaistMeasurement;
            if (_cursor.isNull(_cursorIndexOfWaistMeasurement)) {
              _tmpWaistMeasurement = null;
            } else {
              _tmpWaistMeasurement = _cursor.getFloat(_cursorIndexOfWaistMeasurement);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _result = new HealthEntry(_tmpId,_tmpTimestamp,_tmpWeight,_tmpWaistMeasurement,_tmpNotes);
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
