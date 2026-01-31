# TODO - Data Centralization

## 0. Prerequis
- [x] Valider la base cible (MariaDB `healthtracker`) et les acces.
  - Host: `192.168.0.13:3306`
  - User: `healthuser`
  - DB: `healthtracker`
  - Tables existantes: `users`, `health_entries`
- [x] Lister les sources actives et leurs fichiers (Withings, Gadgetbridge, saisies).
  - Withings: `samples/Withings_Weight_Data.xlsx`
  - Gadgetbridge: `Gadgetbridge/Gadgetbridge.db`
  - Saisie seances (historique): `samples/Entrainement velo elliptique3.xlsx` (a remplacer)

## 1. Schema
- [x] Ecrire le DDL MariaDB (script `docker-sqlite/schema-mariadb.sql`).
- [x] Creer `sources` (id, name, created_at).
- [x] Creer `users` (id, name, alias, created_at).
- [x] Creer `user_source_map` (source_id, source_user_id, device_id, user_id).
- [x] Creer `workouts` (start_time, duration, distance, calories, fc_avg/min/max, notes, source_uid).
- [x] Creer `weight_measurements` (weight, fat_mass, fat_pct, waist, notes, source_uid).
- [x] Optionnel : `gadgetbridge_samples` (timestamp, heart_rate, steps, device_id).

## 2. Mapping utilisateurs
- [x] Renseigner les profils (nom, alias).
- [x] Associer chaque bracelet/app a un `user_id`.
  - MB5-3 (Vincent) -> Vincent
  - Note: "ZPERDU" signifie bracelet perdu, les donnees doivent rester associees a l'utilisateur d'origine.
  - ZHS MB1 -> Christophe (bracelet HS)
  - MB3-1 -> Christophe (probable)
  - ZPERDU Antoine - MB3-2 -> Antoine
  - ZPERDU Antoine - MB5-1 -> Antoine
  - ZPERDU Antoine - MB5-4 -> Antoine
  - ZPERDU Vincent - MB5-2 -> Vincent
  - Christophe - MB4-1 -> Christophe
  - ZHS Antoine - MB4-2 -> Antoine
  - Antoine - MB4-3 -> Antoine
  - Device 11 (alias vide) -> Antoine (donnees faibles, exclure des stats si steps=0)

## 3. Import Withings
- [x] Parser `Withings_Weight_Data.xlsx` (script `docker-sqlite/import_withings_mariadb.py`).
- [x] Inserer dans `weight_measurements` (profil Christophe, 4272 lignes).
- [x] Generer `source_uid` unique (format `device_id:timestamp`).

## 4. Import Gadgetbridge
- [x] Lister les `DEVICE_ID` et alias.
- [x] Extraire les sessions (BASE_ACTIVITY_SUMMARY).
- [x] Calculer FC moyenne/min/max via samples.
- [x] Inserer dans `workouts` avec `source_uid` (305 lignes).
- [x] Optionnel : inserer les samples dans `gadgetbridge_samples` (5,078,999 lignes).

## 5. Saisie manuelle
- [x] Definir un format de saisie minimal (programme, duree, distance, calories).
- [x] Implementer une petite UI mobile (HealthTracker: WorkoutEntry + AddWorkoutActivity).
- [x] Inserer dans `workouts` avec `source=manual` (1140 lignes).

## 6. Fusion et deduplication
- Regles de priorite (Withings > HealthTracker pour poids).
- Fusion par fenetre temporelle et device.
- Dedup via `source_uid` unique.

## 7. Automatisation
- Planifier les imports (cron, systemd, ou job Docker).
- Logger les imports (nb lignes, erreurs).

## 8. Grafana
- Datasources MariaDB.
- Dashboards poids/masse grasse/tour de taille.
- Dashboards seances (duree, distance, calories, FC).

## 9. Migration prod (2026-01-31)
- [x] Migration user_profiles -> users + FK user_id sur prod (192.168.0.103).
- [x] Ajout colonnes workouts manquantes: calories_per_km, sleep_heart_rate_avg, vo2_max, soundtrack, client_id, last_modified.
- [x] Withings import (profil Christophe) avec INSERT IGNORE pour doublons (source_uid).
- [x] Manual import: filtrage des lignes futures (ghost rows).
  - Script mis a jour: docker-sqlite/import_manual_workouts_mariadb.py
  - Par defaut, ignore dates > aujourd'hui.
  - Flags: --max-date YYYY-MM-DD, --allow-future
  - Purge ghost rows: DELETE FROM workouts WHERE source_id=4 AND start_time > '2026-01-31 23:59:59';
- [x] Gadgetbridge import (workouts) avec INSERT IGNORE pour doublons (source_uid).
- [x] Suppression de la table legacy workout_entries (server) apres migration.
- [x] Fix API sync (GET): ajout de users dans la reponse (sync.php).
  - Necessaire pour creer les users locaux (ex: Vincent) quand seules des seances existent.
- [x] Fix API sync (GET): ajout/backup last_modified + client_id sur workouts pour download complet.

## 10. ADB / SQLite - base locale (Android)
- Voir `docs/operations.md`.
