# Centralisation des Données

## Objectif
Centraliser toutes les sources de données (HealthTracker, Withings, Gadgetbridge, saisie manuelle de séances) dans MariaDB afin d'automatiser l'import, réduire la saisie manuelle et alimenter Grafana.

## Sources de Données
- **HealthTracker (app mobile)** : poids, tour de taille, masse graisseuse, notes.
- **Withings** : poids/masse grasse importés automatiquement (Excel `Withings_Weight_Data.xlsx`).
- **Gadgetbridge** : fréquences cardiaques et métriques issues des bracelets (SQLite `Gadgetbridge/Gadgetbridge.db`).
- **Saisie manuelle séances** : programme, durée, distance, calories (remplace l'Excel `Entrainement vélo elliptique3.xlsx`).

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
