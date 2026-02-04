# Dashboards Grafana (versionnés)

Les dashboards Grafana sont stockés ici en JSON et chargés via
`grafana/provisioning/dashboards/dashboards.yaml`.

Workflow conseillé:
1. Créer/ajuster un dashboard dans l'UI Grafana.
2. Exporter le JSON et le placer dans `grafana/dashboards/`.
3. Redémarrer Grafana (ou utiliser l'API) pour recharger.
