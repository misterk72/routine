# Grafana (docker-compose)

Ce dossier contient le `docker-compose.yml` versionné pour Grafana/Prometheus/SNMP,
ainsi que le provisioning des dashboards et des datasources MariaDB.

## Démarrage

```bash
docker compose up -d
```

## Provisioning

- Datasources : `grafana/provisioning/datasources/`
- Dashboards : `grafana/dashboards/`
- Provider : `grafana/provisioning/dashboards/dashboards.yaml`

## Workflows

### Via l'UI Grafana
1. Créer/éditer un dashboard dans l'UI.
2. Exporter le JSON.
3. Placer le JSON dans `grafana/dashboards/`.
4. Redémarrer Grafana.

### Via l'API (sans redémarrage)
Utiliser `grafana/scripts/export_dashboard.sh` et
`grafana/scripts/import_dashboard.sh` avec un token API.
