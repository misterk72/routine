# Routine - Health Data Stack

## Overview
Ce depot rassemble l'app HealthTracker, les scripts d'import (Gadgetbridge, Withings, Excel) et le backend MariaDB/PHP pour centraliser les donnees et alimenter Grafana.

## Documentation
- `HealthTracker/README.md` : documentation complete de l'app Android.
- `docs/data-centralisation.md` : workflow cible, sources et TODO.

## Architecture Backend (MariaDB + API PHP)
- Base de donnees MariaDB pour la synchronisation.
- API PHP dans `docker-sqlite/api` pour l'app mobile.
- Visualisation via Grafana.

## Demarrage Rapide (Backend)
1. Construire et demarrer les conteneurs Docker :
   ```bash
   cd docker-sqlite
   docker-compose build
   docker-compose up -d
   ```
2. Verifier l'API :
   ```bash
   curl http://localhost:5001/sync.php
   ```
