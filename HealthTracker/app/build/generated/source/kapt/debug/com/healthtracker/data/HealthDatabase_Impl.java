package com.healthtracker.data;

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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class HealthDatabase_Impl extends HealthDatabase {
  private volatile HealthEntryDao _healthEntryDao;

  private volatile MetricValueDao _metricValueDao;

  private volatile MetricTypeDao _metricTypeDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(1) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `health_entries` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` TEXT NOT NULL, `weight` REAL, `waistMeasurement` REAL, `notes` TEXT)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `metric_values` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `entryId` INTEGER NOT NULL, `metricType` TEXT NOT NULL, `value` REAL NOT NULL, `unit` TEXT, `notes` TEXT, FOREIGN KEY(`entryId`) REFERENCES `health_entries`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE TABLE IF NOT EXISTS `metric_types` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `unit` TEXT, `description` TEXT, `minValue` REAL, `maxValue` REAL, `stepSize` REAL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '9b62c27913cb6c2f0081f2419de4512b')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `health_entries`");
        db.execSQL("DROP TABLE IF EXISTS `metric_values`");
        db.execSQL("DROP TABLE IF EXISTS `metric_types`");
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
        db.execSQL("PRAGMA foreign_keys = ON");
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
        final HashMap<String, TableInfo.Column> _columnsHealthEntries = new HashMap<String, TableInfo.Column>(5);
        _columnsHealthEntries.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthEntries.put("timestamp", new TableInfo.Column("timestamp", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthEntries.put("weight", new TableInfo.Column("weight", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthEntries.put("waistMeasurement", new TableInfo.Column("waistMeasurement", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsHealthEntries.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysHealthEntries = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesHealthEntries = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoHealthEntries = new TableInfo("health_entries", _columnsHealthEntries, _foreignKeysHealthEntries, _indicesHealthEntries);
        final TableInfo _existingHealthEntries = TableInfo.read(db, "health_entries");
        if (!_infoHealthEntries.equals(_existingHealthEntries)) {
          return new RoomOpenHelper.ValidationResult(false, "health_entries(com.healthtracker.data.HealthEntry).\n"
                  + " Expected:\n" + _infoHealthEntries + "\n"
                  + " Found:\n" + _existingHealthEntries);
        }
        final HashMap<String, TableInfo.Column> _columnsMetricValues = new HashMap<String, TableInfo.Column>(6);
        _columnsMetricValues.put("id", new TableInfo.Column("id", "INTEGER", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricValues.put("entryId", new TableInfo.Column("entryId", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricValues.put("metricType", new TableInfo.Column("metricType", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricValues.put("value", new TableInfo.Column("value", "REAL", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricValues.put("unit", new TableInfo.Column("unit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricValues.put("notes", new TableInfo.Column("notes", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMetricValues = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysMetricValues.add(new TableInfo.ForeignKey("health_entries", "CASCADE", "NO ACTION", Arrays.asList("entryId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesMetricValues = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMetricValues = new TableInfo("metric_values", _columnsMetricValues, _foreignKeysMetricValues, _indicesMetricValues);
        final TableInfo _existingMetricValues = TableInfo.read(db, "metric_values");
        if (!_infoMetricValues.equals(_existingMetricValues)) {
          return new RoomOpenHelper.ValidationResult(false, "metric_values(com.healthtracker.data.MetricValue).\n"
                  + " Expected:\n" + _infoMetricValues + "\n"
                  + " Found:\n" + _existingMetricValues);
        }
        final HashMap<String, TableInfo.Column> _columnsMetricTypes = new HashMap<String, TableInfo.Column>(7);
        _columnsMetricTypes.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricTypes.put("name", new TableInfo.Column("name", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricTypes.put("unit", new TableInfo.Column("unit", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricTypes.put("description", new TableInfo.Column("description", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricTypes.put("minValue", new TableInfo.Column("minValue", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricTypes.put("maxValue", new TableInfo.Column("maxValue", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsMetricTypes.put("stepSize", new TableInfo.Column("stepSize", "REAL", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysMetricTypes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesMetricTypes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoMetricTypes = new TableInfo("metric_types", _columnsMetricTypes, _foreignKeysMetricTypes, _indicesMetricTypes);
        final TableInfo _existingMetricTypes = TableInfo.read(db, "metric_types");
        if (!_infoMetricTypes.equals(_existingMetricTypes)) {
          return new RoomOpenHelper.ValidationResult(false, "metric_types(com.healthtracker.data.MetricType).\n"
                  + " Expected:\n" + _infoMetricTypes + "\n"
                  + " Found:\n" + _existingMetricTypes);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "9b62c27913cb6c2f0081f2419de4512b", "57a2a8cf9e8d27e546447f62109d43c4");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "health_entries","metric_values","metric_types");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `health_entries`");
      _db.execSQL("DELETE FROM `metric_values`");
      _db.execSQL("DELETE FROM `metric_types`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
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
    _typeConvertersMap.put(HealthEntryDao.class, HealthEntryDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MetricValueDao.class, MetricValueDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(MetricTypeDao.class, MetricTypeDao_Impl.getRequiredConverters());
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
  public HealthEntryDao healthEntryDao() {
    if (_healthEntryDao != null) {
      return _healthEntryDao;
    } else {
      synchronized(this) {
        if(_healthEntryDao == null) {
          _healthEntryDao = new HealthEntryDao_Impl(this);
        }
        return _healthEntryDao;
      }
    }
  }

  @Override
  public MetricValueDao metricValueDao() {
    if (_metricValueDao != null) {
      return _metricValueDao;
    } else {
      synchronized(this) {
        if(_metricValueDao == null) {
          _metricValueDao = new MetricValueDao_Impl(this);
        }
        return _metricValueDao;
      }
    }
  }

  @Override
  public MetricTypeDao metricTypeDao() {
    if (_metricTypeDao != null) {
      return _metricTypeDao;
    } else {
      synchronized(this) {
        if(_metricTypeDao == null) {
          _metricTypeDao = new MetricTypeDao_Impl(this);
        }
        return _metricTypeDao;
      }
    }
  }
}
