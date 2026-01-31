# Operations

## ADB / SQLite - base locale (Android)
Commandes utiles pour diagnostiquer et manipuler la base locale sur le telephone.

### Extraction de la DB locale (avec WAL/SHM)
```bash
adb exec-out run-as com.healthtracker cat /data/data/com.healthtracker/databases/health_tracker.db > /tmp/health_tracker.db
adb exec-out run-as com.healthtracker cat /data/data/com.healthtracker/databases/health_tracker.db-wal > /tmp/health_tracker.db-wal
adb exec-out run-as com.healthtracker cat /data/data/com.healthtracker/databases/health_tracker.db-shm > /tmp/health_tracker.db-shm
```

### Inspection locale (sur la machine hote)
```bash
sqlite3 /tmp/health_tracker.db ".tables"
sqlite3 /tmp/health_tracker.db "PRAGMA table_info(users);"
sqlite3 /tmp/health_tracker.db "select id,name,isDefault from users;"
sqlite3 /tmp/health_tracker.db "select count(*) from workout_entries;"
sqlite3 /tmp/health_tracker.db "select count(*) from health_entries;"
```

### Ajout d'un user local (ex: Antoine/Vincent)
```bash
adb shell am force-stop com.healthtracker
sqlite3 /tmp/health_tracker.db "insert or ignore into users(id,name,isDefault) values (2,'Antoine',0);"
sqlite3 /tmp/health_tracker.db "insert or ignore into users(id,name,isDefault) values (3,'Vincent',0);"
sqlite3 /tmp/health_tracker.db "PRAGMA wal_checkpoint(FULL);"
```

### Reinjection de la DB modifiee
```bash
adb exec-out run-as com.healthtracker sh -c 'cat > /data/data/com.healthtracker/databases/health_tracker.db' < /tmp/health_tracker.db
adb shell "run-as com.healthtracker rm -f /data/data/com.healthtracker/databases/health_tracker.db-wal /data/data/com.healthtracker/databases/health_tracker.db-shm"
```

### Redemarrage app + resync
```bash
adb shell monkey -p com.healthtracker -c android.intent.category.LAUNCHER 1
```

### Alternative (copie via /data/local/tmp)
```bash
adb push /tmp/health_tracker.db /data/local/tmp/health_tracker.db
adb shell "run-as com.healthtracker cp /data/local/tmp/health_tracker.db /data/data/com.healthtracker/databases/health_tracker.db"
```
