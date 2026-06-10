# Status Server

Drei gleichwertige Statusserver-Nodes hinter einem zentralen NGINX-Loadbalancer. REST- und WebSocket/STOMP-Clients verwenden nur den Gateway:

```text
http://localhost:8440
```

## Start

```bash
docker compose up --build
```

Testaufruf:

```powershell
curl.exe http://localhost:8440/api/status
```

Im Compose-Setup ist nur der Gateway lokal veröffentlicht. PostgreSQL, RabbitMQ und die Statusserver-Nodes sind nur intern im Docker-Netz erreichbar.
Der Gateway lauscht lokal per HTTP auf Port `8440` und leitet intern per HTTPS an die Statusserver-Nodes auf Port `8443` weiter. RabbitMQ wird intern per TLS auf Port `5671` genutzt.

## CORS

REST-API und WebSocket-Endpunkt akzeptieren CORS nur von konfigurierten Angular-Frontend-Origins. Standardmäßig sind die lokalen Angular-Dev-Server erlaubt:

```text
http://localhost:4200
http://127.0.0.1:4200
```

Für andere Frontend-URLs wird die kommagetrennte Umgebungsvariable `APP_ALLOWED_ORIGINS` gesetzt:

```powershell
$env:APP_ALLOWED_ORIGINS="https://frontend.example.com,https://admin.example.com"
```

Wildcard-Origins wie `*` werden nicht verwendet.

## REST-API

Base-URL:

```text
http://localhost:8440/api/status
```

```http
GET /api/status
GET /api/status/{username}
POST /api/status
DELETE /api/status/{username}
```

`GET /api/status/{username}` liefert `404 Not Found`, wenn für den Benutzer kein aktueller Status existiert. `POST /api/status` validiert Pflichtfelder, Zeitstempel und Koordinaten und liefert den lokal gültigen Zustand nach Konfliktauflösung zurück. `DELETE /api/status/{username}` erzeugt einen Tombstone mit dem aktuellen Löschzeitpunkt und liefert `204 No Content`.

Beispiel für `POST /api/status`:

```json
{
  "username": "RECON-01",
  "statustext": "Am Weg zum Einsatz",
  "uhrzeit": "2026-03-03T13:30:00+01:00",
  "latitude": 48.215,
  "longitude": 16.385
}
```

## Loadbalancer

NGINX verteilt Requests per `least_conn` auf `status-server-1`, `status-server-2` und `status-server-3`.

Node-Ausfälle werden durch diese NGINX-Einstellungen abgefangen:

- `max_fails=1`
- `fail_timeout=5s`
- `proxy_next_upstream`
- `proxy_next_upstream_tries=3`

Wenn eine Node ausfällt, versucht NGINX automatisch eine andere Node. Die Client-URL bleibt gleich.

NGINX leitet auch WebSocket-Verbindungen weiter, indem die `Upgrade`- und `Connection`-Header an die Statusserver-Nodes weitergegeben werden.

## Replikation

- Schreibvorgänge werden lokal gespeichert und als RabbitMQ-Event veröffentlicht.
- Jede Node hat eine eigene transiente, auto-delete Queue anhand ihrer `APP_NODE_ID`.
- Eigene Echo-Events werden ignoriert.
- Fremde Events werden lokal übernommen.
- RabbitMQ dient nur als Transportmedium. Offline verpasste Events werden nicht gepuffert, sondern beim Neustart per Snapshot-Sync nachgeladen.
- Bei konkurrierenden Updates gewinnt die neuere `uhrzeit`.
- Deletes werden als Delete-Events repliziert und als Tombstones mit Löschzeitpunkt gespeichert.
- Gleich alte oder ältere Updates und Deletes werden ignoriert.
- Tombstones verhindern, dass veraltete Statusmeldungen nach Node-Ausfällen oder späterem Sync wieder auftauchen. Ein bewusst neuerer Status desselben Benutzers kann einen Tombstone wieder ersetzen.

## WebSocket / STOMP

STOMP/WebSocket wird für Live-Updates an Clients verwendet.

```text
Endpoint: /ws
Application Prefix: /app
Topic Prefix: /topic
Status-Updates: /topic/status-feed
Delete-Events: /topic/status-events
```

Über den Gateway ist der Endpoint erreichbar unter:

```text
ws://localhost:8440/ws
```

## Initialer Sync

Beim Start fragt eine Node Snapshots ihrer Peers über `/internal/status-sync/snapshot` ab. Der Snapshot enthält aktuelle Statusmeldungen und Tombstones. Fehlende Statusmeldungen in einem Peer-Snapshot löschen keine lokalen Statusmeldungen; nur explizite Tombstones lösen Löschungen aus.

Während des Bootstraps beantwortet die Node öffentliche `/api/status/**`-Requests mit `503 Service Unavailable`. Interne Sync-Endpunkte bleiben erreichbar. Mit `APP_BOOTSTRAP_REQUIRE_PEER=true` wartet die Node bis mindestens ein Peer erreichbar war. Mit `false` wird sie nach `app.bootstrap-timeout` auch ohne erreichbaren Peer bereit.

## Tests

```powershell
.\mvnw.cmd clean test
```

Die Tests decken unter anderem ab:

- Delete-Replikation inklusive Tombstones
- Snapshot-Recovery nach verpassten Delete-Events
- parallele Verarbeitung von mindestens 10 simultanen Clients
