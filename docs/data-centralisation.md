# Centralisation des Données

## Objectif
Centraliser toutes les sources de données (HealthTracker, Withings, Gadgetbridge, saisie manuelle de séances) dans MariaDB afin d'automatiser l'import, réduire la saisie manuelle et alimenter Grafana.

Voir aussi: `docs/db-import-and-schema.md` pour la structure MariaDB et les imports.

## Sources de Données
- **HealthTracker (app mobile)** : poids, tour de taille, masse graisseuse, notes.
- **Withings** : poids/masse grasse importés automatiquement (Excel `Withings_Weight_Data.xlsx`).
- **Gadgetbridge** : fréquences cardiaques et métriques issues des bracelets (SQLite `Gadgetbridge/Gadgetbridge.db`).
- **Saisie manuelle séances** : programme, durée, distance, calories (remplace l'Excel `Entrainement vélo elliptique3.xlsx`).
- **Localisations (app mobile)** : lieux enregistrés pour le suivi santé.

## Principes de Centralisation
- **Unifier sans mélanger** : chaque source conserve un identifiant d'origine (`source`, `source_uid`).
- **Mapper les personnes** : les bracelets et les apps sont rattachés à un `user_profile`.
- **Calculer les agrégats** : ex. FC moyenne/max/min par séance.
- **Relier les localisations** : `health_entries.location_id` pointe vers `locations`.

## Schéma Cible (proposition)
- `sources` : registre des sources (`healthtracker`, `withings`, `gadgetbridge`, `manual`).
- `user_profiles` : personnes réelles (nom, alias).
- `user_source_map` : mapping `source_user_id` / `device_id` vers `user_profile_id`.
- `workouts` : séances (date, durée, distance, calories, fc_avg/fc_min/fc_max, fond sonore, observations).
- `weight_measurements` : poids, masse grasse, tour de taille, notes.
- `gadgetbridge_samples` (optionnel) : échantillons détaillés FC/steps.
- `locations` : lieux (nom, latitude, longitude, rayon, défaut).

## Règles d'Association (fusion)
- **Par fenêtre temporelle** : associer les sources dans un intervalle configurable (ex. +/- 6h).
- **Par device** : limiter aux données d'un bracelet si `device_id` est connu.
- **Par priorité** :
  - FC : Gadgetbridge
  - Poids/masse grasse : Withings (prioritaire) > HealthTracker
  - Tour de taille : saisie manuelle

## Automatisation
- **Imports planifiés** :
  - Withings (nocturne) -> `weight_measurements`
  - Gadgetbridge (quotidien) -> `workouts` + agrégats FC
- **Saisie mobile** :
  - Fin de séance : programme, durée, distance, calories
  - Après pesée : tour de taille + notes

## Qualité des Données
- Déduplication via `source_uid` unique.
- Normalisation des unités (kg, bpm, km).
- Journal d'import (statut, erreurs, lignes rejetées).

## Grafana
- Dashboards principaux : poids, masse grasse, tour de taille, FC.
- Dashboards séances : durée, distance, calories, FC moyenne/max/min.
  

## Workflow Cible
1. **Fin de séance** : saisie minimale (programme, heure de départ, durée, distance, calories).
2. **Après la pesée (smartphone)** : saisie mobile rapide (tour de taille + notes si besoin).
3. **Nuit** : import automatique Withings (poids, masse grasse).
4. **Import Gadgetbridge** : FC moyenne/max/min + échantillons de la séance.
5. **Fusion** : associer séance + FC + poids + saisie manuelle via fenêtre temporelle.
6. **Grafana** : dashboards sur séances, poids, FC, tour de taille.

## TODO
- Définir le schéma cible dans MariaDB pour `workouts`, `weight_measurements`, `sources`, `user_profiles`, `user_source_map`.
- Créer le mapping bracelet/personne pour éviter les mélanges (device_id → profil).
- Écrire les scripts d'import :
  - Withings Excel → `weight_measurements`
  - Gadgetbridge DB → `workouts` + stats FC
  - Saisie manuelle séances → `workouts`
- Implémenter la fusion automatique (par fenêtre temporelle + device).
- Ajouter un mini formulaire mobile pour saisies post‑séance/pesée.
- Mettre en place les dashboards Grafana (séances, poids, FC, tour de taille).
