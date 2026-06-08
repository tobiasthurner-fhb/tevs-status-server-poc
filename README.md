# Status Server

Drei gleichwertige Statusserver-Nodes hinter einem zentralen NGINX-Loadbalancer. REST- und WebSocket/STOMP-Clients verwenden nur:

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
- Jede Node hat eine eigene Queue anhand ihrer `APP_NODE_ID`.
- Eigene Echo-Events werden ignoriert.
- Fremde Events werden lokal übernommen.
- Bei konkurrierenden Updates gewinnt die neuere `uhrzeit`.
- Deletes werden als Delete-Events repliziert und als Tombstones mit Löschzeitpunkt gespeichert.
- Tombstones verhindern, dass veraltete Statusmeldungen nach Node-Ausfällen oder späterem Sync wieder auftauchen.

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

Beim Start fragt eine Node Snapshots ihrer Peers über `/internal/status-sync/snapshot` ab. Während dieser Grace Period beantwortet sie öffentliche `/api/status`-Requests mit `503 Service Unavailable`.

## Tests

```powershell
.\mvnw.cmd clean test
```

Die Tests decken unter anderem ab:

- Delete-Replikation inklusive Tombstones
- Snapshot-Recovery nach verpassten Delete-Events
- parallele Verarbeitung von mindestens 10 simultanen Clients
