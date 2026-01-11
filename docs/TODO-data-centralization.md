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
- [x] Creer `user_profiles` (id, name, alias, created_at).
- [x] Creer `user_source_map` (source_id, source_user_id, device_id, user_profile_id).
- [x] Creer `workouts` (start_time, duration, distance, calories, fc_avg/min/max, notes, source_uid).
- [x] Creer `weight_measurements` (weight, fat_mass, fat_pct, waist, notes, source_uid).
- [x] Optionnel : `gadgetbridge_samples` (timestamp, heart_rate, steps, device_id).

## 2. Mapping utilisateurs
- Renseigner les profils (nom, alias).
- Associer chaque bracelet/app a un `user_profile_id`.

## 3. Import Withings
- Parser `Withings_Weight_Data.xlsx`.
- Inserer dans `weight_measurements`.
- Generer `source_uid` unique (ex. `withings:<measure_id>`).

## 4. Import Gadgetbridge
- Lister les `DEVICE_ID` et alias.
- Extraire les sessions (BASE_ACTIVITY_SUMMARY).
- Calculer FC moyenne/min/max via samples.
- Inserer dans `workouts` avec `source_uid`.
- Optionnel : inserer les samples dans `gadgetbridge_samples`.

## 5. Saisie manuelle
- Definir un format de saisie minimal (programme, duree, distance, calories).
- Implementer une petite UI mobile (ou reutiliser HealthTracker).
- Inserer dans `workouts` avec `source=manual`.

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
