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
public final class MetricValueDao_Impl implements MetricValueDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MetricValue> __insertionAdapterOfMetricValue;

  private final EntityDeletionOrUpdateAdapter<MetricValue> __deletionAdapterOfMetricValue;

  private final EntityDeletionOrUpdateAdapter<MetricValue> __updateAdapterOfMetricValue;

  public MetricValueDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMetricValue = new EntityInsertionAdapter<MetricValue>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `metric_values` (`id`,`entryId`,`metricType`,`value`,`unit`,`notes`) VALUES (nullif(?, 0),?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MetricValue entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getEntryId());
        if (entity.getMetricType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getMetricType());
        }
        statement.bindDouble(4, entity.getValue());
        if (entity.getUnit() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getUnit());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getNotes());
        }
      }
    };
    this.__deletionAdapterOfMetricValue = new EntityDeletionOrUpdateAdapter<MetricValue>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `metric_values` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MetricValue entity) {
        statement.bindLong(1, entity.getId());
      }
    };
    this.__updateAdapterOfMetricValue = new EntityDeletionOrUpdateAdapter<MetricValue>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `metric_values` SET `id` = ?,`entryId` = ?,`metricType` = ?,`value` = ?,`unit` = ?,`notes` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MetricValue entity) {
        statement.bindLong(1, entity.getId());
        statement.bindLong(2, entity.getEntryId());
        if (entity.getMetricType() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getMetricType());
        }
        statement.bindDouble(4, entity.getValue());
        if (entity.getUnit() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getUnit());
        }
        if (entity.getNotes() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getNotes());
        }
        statement.bindLong(7, entity.getId());
      }
    };
  }

  @Override
  public Object insert(final MetricValue value, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMetricValue.insert(value);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final MetricValue value, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMetricValue.handle(value);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final MetricValue value, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMetricValue.handle(value);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MetricValue>> getValuesForEntry(final long entryId) {
    final String _sql = "SELECT * FROM metric_values WHERE entryId = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, entryId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"metric_values"}, new Callable<List<MetricValue>>() {
      @Override
      @NonNull
      public List<MetricValue> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEntryId = CursorUtil.getColumnIndexOrThrow(_cursor, "entryId");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metricType");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<MetricValue> _result = new ArrayList<MetricValue>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MetricValue _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpEntryId;
            _tmpEntryId = _cursor.getLong(_cursorIndexOfEntryId);
            final String _tmpMetricType;
            if (_cursor.isNull(_cursorIndexOfMetricType)) {
              _tmpMetricType = null;
            } else {
              _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            }
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new MetricValue(_tmpId,_tmpEntryId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpNotes);
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
  public Flow<List<MetricValue>> getValuesByType(final String metricType) {
    final String _sql = "SELECT * FROM metric_values WHERE metricType = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (metricType == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, metricType);
    }
    return CoroutinesRoom.createFlow(__db, false, new String[] {"metric_values"}, new Callable<List<MetricValue>>() {
      @Override
      @NonNull
      public List<MetricValue> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEntryId = CursorUtil.getColumnIndexOrThrow(_cursor, "entryId");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metricType");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final List<MetricValue> _result = new ArrayList<MetricValue>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MetricValue _item;
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpEntryId;
            _tmpEntryId = _cursor.getLong(_cursorIndexOfEntryId);
            final String _tmpMetricType;
            if (_cursor.isNull(_cursorIndexOfMetricType)) {
              _tmpMetricType = null;
            } else {
              _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            }
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _item = new MetricValue(_tmpId,_tmpEntryId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpNotes);
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
  public Object getValueById(final long id, final Continuation<? super MetricValue> $completion) {
    final String _sql = "SELECT * FROM metric_values WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindLong(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MetricValue>() {
      @Override
      @Nullable
      public MetricValue call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfEntryId = CursorUtil.getColumnIndexOrThrow(_cursor, "entryId");
          final int _cursorIndexOfMetricType = CursorUtil.getColumnIndexOrThrow(_cursor, "metricType");
          final int _cursorIndexOfValue = CursorUtil.getColumnIndexOrThrow(_cursor, "value");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfNotes = CursorUtil.getColumnIndexOrThrow(_cursor, "notes");
          final MetricValue _result;
          if (_cursor.moveToFirst()) {
            final long _tmpId;
            _tmpId = _cursor.getLong(_cursorIndexOfId);
            final long _tmpEntryId;
            _tmpEntryId = _cursor.getLong(_cursorIndexOfEntryId);
            final String _tmpMetricType;
            if (_cursor.isNull(_cursorIndexOfMetricType)) {
              _tmpMetricType = null;
            } else {
              _tmpMetricType = _cursor.getString(_cursorIndexOfMetricType);
            }
            final double _tmpValue;
            _tmpValue = _cursor.getDouble(_cursorIndexOfValue);
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpNotes;
            if (_cursor.isNull(_cursorIndexOfNotes)) {
              _tmpNotes = null;
            } else {
              _tmpNotes = _cursor.getString(_cursorIndexOfNotes);
            }
            _result = new MetricValue(_tmpId,_tmpEntryId,_tmpMetricType,_tmpValue,_tmpUnit,_tmpNotes);
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
