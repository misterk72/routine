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
import java.lang.Double;
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
public final class MetricTypeDao_Impl implements MetricTypeDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<MetricType> __insertionAdapterOfMetricType;

  private final EntityDeletionOrUpdateAdapter<MetricType> __deletionAdapterOfMetricType;

  private final EntityDeletionOrUpdateAdapter<MetricType> __updateAdapterOfMetricType;

  public MetricTypeDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfMetricType = new EntityInsertionAdapter<MetricType>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR ABORT INTO `metric_types` (`id`,`name`,`unit`,`description`,`minValue`,`maxValue`,`stepSize`) VALUES (?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MetricType entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getUnit() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getUnit());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        if (entity.getMinValue() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getMinValue());
        }
        if (entity.getMaxValue() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getMaxValue());
        }
        if (entity.getStepSize() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getStepSize());
        }
      }
    };
    this.__deletionAdapterOfMetricType = new EntityDeletionOrUpdateAdapter<MetricType>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "DELETE FROM `metric_types` WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MetricType entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
      }
    };
    this.__updateAdapterOfMetricType = new EntityDeletionOrUpdateAdapter<MetricType>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `metric_types` SET `id` = ?,`name` = ?,`unit` = ?,`description` = ?,`minValue` = ?,`maxValue` = ?,`stepSize` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final MetricType entity) {
        if (entity.getId() == null) {
          statement.bindNull(1);
        } else {
          statement.bindString(1, entity.getId());
        }
        if (entity.getName() == null) {
          statement.bindNull(2);
        } else {
          statement.bindString(2, entity.getName());
        }
        if (entity.getUnit() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getUnit());
        }
        if (entity.getDescription() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getDescription());
        }
        if (entity.getMinValue() == null) {
          statement.bindNull(5);
        } else {
          statement.bindDouble(5, entity.getMinValue());
        }
        if (entity.getMaxValue() == null) {
          statement.bindNull(6);
        } else {
          statement.bindDouble(6, entity.getMaxValue());
        }
        if (entity.getStepSize() == null) {
          statement.bindNull(7);
        } else {
          statement.bindDouble(7, entity.getStepSize());
        }
        if (entity.getId() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getId());
        }
      }
    };
  }

  @Override
  public Object insert(final MetricType type, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfMetricType.insert(type);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object delete(final MetricType type, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __deletionAdapterOfMetricType.handle(type);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object update(final MetricType type, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfMetricType.handle(type);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<MetricType>> getAllTypes() {
    final String _sql = "SELECT * FROM metric_types";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"metric_types"}, new Callable<List<MetricType>>() {
      @Override
      @NonNull
      public List<MetricType> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfMinValue = CursorUtil.getColumnIndexOrThrow(_cursor, "minValue");
          final int _cursorIndexOfMaxValue = CursorUtil.getColumnIndexOrThrow(_cursor, "maxValue");
          final int _cursorIndexOfStepSize = CursorUtil.getColumnIndexOrThrow(_cursor, "stepSize");
          final List<MetricType> _result = new ArrayList<MetricType>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final MetricType _item;
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final Double _tmpMinValue;
            if (_cursor.isNull(_cursorIndexOfMinValue)) {
              _tmpMinValue = null;
            } else {
              _tmpMinValue = _cursor.getDouble(_cursorIndexOfMinValue);
            }
            final Double _tmpMaxValue;
            if (_cursor.isNull(_cursorIndexOfMaxValue)) {
              _tmpMaxValue = null;
            } else {
              _tmpMaxValue = _cursor.getDouble(_cursorIndexOfMaxValue);
            }
            final Double _tmpStepSize;
            if (_cursor.isNull(_cursorIndexOfStepSize)) {
              _tmpStepSize = null;
            } else {
              _tmpStepSize = _cursor.getDouble(_cursorIndexOfStepSize);
            }
            _item = new MetricType(_tmpId,_tmpName,_tmpUnit,_tmpDescription,_tmpMinValue,_tmpMaxValue,_tmpStepSize);
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
  public Object getTypeById(final String id, final Continuation<? super MetricType> $completion) {
    final String _sql = "SELECT * FROM metric_types WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (id == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, id);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MetricType>() {
      @Override
      @Nullable
      public MetricType call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfMinValue = CursorUtil.getColumnIndexOrThrow(_cursor, "minValue");
          final int _cursorIndexOfMaxValue = CursorUtil.getColumnIndexOrThrow(_cursor, "maxValue");
          final int _cursorIndexOfStepSize = CursorUtil.getColumnIndexOrThrow(_cursor, "stepSize");
          final MetricType _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final Double _tmpMinValue;
            if (_cursor.isNull(_cursorIndexOfMinValue)) {
              _tmpMinValue = null;
            } else {
              _tmpMinValue = _cursor.getDouble(_cursorIndexOfMinValue);
            }
            final Double _tmpMaxValue;
            if (_cursor.isNull(_cursorIndexOfMaxValue)) {
              _tmpMaxValue = null;
            } else {
              _tmpMaxValue = _cursor.getDouble(_cursorIndexOfMaxValue);
            }
            final Double _tmpStepSize;
            if (_cursor.isNull(_cursorIndexOfStepSize)) {
              _tmpStepSize = null;
            } else {
              _tmpStepSize = _cursor.getDouble(_cursorIndexOfStepSize);
            }
            _result = new MetricType(_tmpId,_tmpName,_tmpUnit,_tmpDescription,_tmpMinValue,_tmpMaxValue,_tmpStepSize);
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
  public Object getTypeByName(final String name,
      final Continuation<? super MetricType> $completion) {
    final String _sql = "SELECT * FROM metric_types WHERE name = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    if (name == null) {
      _statement.bindNull(_argIndex);
    } else {
      _statement.bindString(_argIndex, name);
    }
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<MetricType>() {
      @Override
      @Nullable
      public MetricType call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfName = CursorUtil.getColumnIndexOrThrow(_cursor, "name");
          final int _cursorIndexOfUnit = CursorUtil.getColumnIndexOrThrow(_cursor, "unit");
          final int _cursorIndexOfDescription = CursorUtil.getColumnIndexOrThrow(_cursor, "description");
          final int _cursorIndexOfMinValue = CursorUtil.getColumnIndexOrThrow(_cursor, "minValue");
          final int _cursorIndexOfMaxValue = CursorUtil.getColumnIndexOrThrow(_cursor, "maxValue");
          final int _cursorIndexOfStepSize = CursorUtil.getColumnIndexOrThrow(_cursor, "stepSize");
          final MetricType _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            if (_cursor.isNull(_cursorIndexOfId)) {
              _tmpId = null;
            } else {
              _tmpId = _cursor.getString(_cursorIndexOfId);
            }
            final String _tmpName;
            if (_cursor.isNull(_cursorIndexOfName)) {
              _tmpName = null;
            } else {
              _tmpName = _cursor.getString(_cursorIndexOfName);
            }
            final String _tmpUnit;
            if (_cursor.isNull(_cursorIndexOfUnit)) {
              _tmpUnit = null;
            } else {
              _tmpUnit = _cursor.getString(_cursorIndexOfUnit);
            }
            final String _tmpDescription;
            if (_cursor.isNull(_cursorIndexOfDescription)) {
              _tmpDescription = null;
            } else {
              _tmpDescription = _cursor.getString(_cursorIndexOfDescription);
            }
            final Double _tmpMinValue;
            if (_cursor.isNull(_cursorIndexOfMinValue)) {
              _tmpMinValue = null;
            } else {
              _tmpMinValue = _cursor.getDouble(_cursorIndexOfMinValue);
            }
            final Double _tmpMaxValue;
            if (_cursor.isNull(_cursorIndexOfMaxValue)) {
              _tmpMaxValue = null;
            } else {
              _tmpMaxValue = _cursor.getDouble(_cursorIndexOfMaxValue);
            }
            final Double _tmpStepSize;
            if (_cursor.isNull(_cursorIndexOfStepSize)) {
              _tmpStepSize = null;
            } else {
              _tmpStepSize = _cursor.getDouble(_cursorIndexOfStepSize);
            }
            _result = new MetricType(_tmpId,_tmpName,_tmpUnit,_tmpDescription,_tmpMinValue,_tmpMaxValue,_tmpStepSize);
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
