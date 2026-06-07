# Architektur

```mermaid
flowchart LR
    Client[REST Client]
    WsClient[WebSocket/STOMP Client]
    LB[NGINX<br/>http://localhost:8440]
    MQ[(RabbitMQ)]

    N1[status-server-1]
    N2[status-server-2]
    N3[status-server-3]

    DB1[(postgres1)]
    DB2[(postgres2)]
    DB3[(postgres3)]

    Client --> LB
    WsClient -->|ws://localhost:8440/ws| LB
    LB --> N1
    LB --> N2
    LB --> N3

    N1 --> DB1
    N2 --> DB2
    N3 --> DB3

    N1 <-->|Events| MQ
    N2 <-->|Events| MQ
    N3 <-->|Events| MQ

    N1 <-->|Snapshot Sync| N2
    N1 <-->|Snapshot Sync| N3
    N2 <-->|Snapshot Sync| N3
```

## Requestpfad

Clients senden REST-Requests an `http://localhost:8440`. WebSocket/STOMP-Clients verbinden sich mit `ws://localhost:8440/ws`. NGINX verteilt beide Verbindungsarten per `least_conn` auf eine erreichbare Statusserver-Node.

## Replikation

Eine Node speichert Schreibvorgänge lokal und veröffentlicht danach ein RabbitMQ-Event. Andere Nodes übernehmen fremde Events. Bei Konflikten gewinnt die neuere `uhrzeit`.

## WebSocket / STOMP

Statusänderungen werden zusätzlich an WebSocket-Clients gesendet:

- `/topic/status-feed` für Upserts
- `/topic/status-events` für Deletes

## Initialer Sync

Beim Start lädt eine Node per `/internal/status-sync/snapshot` den aktuellen Zustand von Peers. Währenddessen beantwortet sie öffentliche Status-Requests mit `503 Service Unavailable`.

## Ausfälle

- Node-Ausfall: NGINX routet auf verbleibende Nodes.
- Node-Neustart: Die Node synchronisiert sich per Snapshot.
- Gateway-Ausfall: Im Pflichtumfang nicht redundant.
