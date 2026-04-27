# Status Server

Spring-Boot-Service zur Verwaltung und Replikation von Statusmeldungen im TEVS-Kontext. Der Service speichert Statusdaten pro Node in PostgreSQL, repliziert Änderungen über RabbitMQ und verteilt Updates an Clients per WebSocket/STOMP.

## Funktionen

- REST-API zum Anlegen, Lesen und Löschen von Statusmeldungen
- PostgreSQL-Persistenz pro Server-Node
- Node-zu-Node-Replikation über RabbitMQ
- Live-Updates für Clients über WebSocket/STOMP
- Konfliktauflösung per `Last-Writer-Wins` auf Basis von `uhrzeit`

## Technologien

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring AMQP
- Spring WebSocket
- PostgreSQL
- RabbitMQ
- Maven
- Docker / Docker Compose

## Projektstruktur

```text
src/main/java/com/statusserver
├─ StatusServerApplication.java
├─ config
│  ├─ RabbitConfig.java
│  ├─ RabbitMessageConfig.java
│  └─ WebSocketConfig.java
└─ status
   ├─ api
   │  └─ StatusController.java
   ├─ application
   │  ├─ StatusService.java
   │  └─ dto
   │     └─ StatusDto.java
   ├─ domain
   │  └─ StatusMessage.java
   ├─ messaging
   │  ├─ StatusChannels.java
   │  ├─ StatusEvents.java
   │  └─ replication
   │     ├─ StatusReplicationListener.java
   │     ├─ StatusReplicationMessage.java
   │     └─ StatusReplicationPublisher.java
   └─ persistence
      └─ StatusMessageRepository.java
```

## Voraussetzungen

- Java 21 oder neuer
- Maven 3.9+ oder Nutzung des Maven Wrappers `mvnw`
- Optional: Docker Desktop für Containerbetrieb

## Lokaler Start

Zuerst die benötigten Infrastruktur-Dienste starten, zum Beispiel PostgreSQL und RabbitMQ. Die Default-Konfiguration in `src/main/resources/application.yaml` erwartet:

- PostgreSQL auf `localhost:5432`
- Datenbank `statusdb`
- Benutzer `statususer`
- Passwort `statuspass`
- RabbitMQ auf `localhost:5672`

Danach den Service starten:

```bash
./mvnw spring-boot:run
```

Unter Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Der Service läuft standardmäßig auf `http://localhost:8080`.

## Start mit Docker Compose

Das Repository enthält ein Compose-Setup mit:

- `postgres1` und `postgres2`
- einem RabbitMQ-Broker
- zwei `status-server`-Instanzen

Start:

```bash
docker compose up --build
```

Danach sind die Instanzen erreichbar unter:

- `http://localhost:8080`
- `http://localhost:8081`

RabbitMQ Management UI:

- `http://localhost:15672`
- Standard-Login: `guest` / `guest`

## Konfiguration

Wichtige Properties:

- `server.port`
- `app.node-id`
- `spring.datasource.url`
- `spring.datasource.username`
- `spring.datasource.password`
- `spring.rabbitmq.host`
- `spring.rabbitmq.port`
- `spring.rabbitmq.username`
- `spring.rabbitmq.password`

Im Compose-Setup wird `APP_NODE_ID` pro Instanz gesetzt, damit jede Node eine eigene Replikations-Queue erhält.

## REST-API

Base-Pfad: `/api/status`

### Alle Statusmeldungen lesen

```http
GET /api/status
```

### Status eines Benutzers lesen

```http
GET /api/status/{username}
```

### Status anlegen oder aktualisieren

```http
POST /api/status
Content-Type: application/json
```

Beispiel:

```json
{
  "username": "RECON-01",
  "statustext": "Am Weg zum Einsatz",
  "uhrzeit": "2026-03-03T13:30:00+01:00",
  "latitude": 48.215,
  "longitude": 16.385
}
```

### Status löschen

```http
DELETE /api/status/{username}
```

## WebSocket / STOMP

- Endpoint: `/ws`
- Application Prefix: `/app`
- Topic für Status-Updates: `/topic/status-feed`
- Topic für Lösch-Events: `/topic/status-events`

Lösch-Events werden aktuell als String mit Präfix `DELETED:` versendet.

## Replikationsverhalten

- Schreibvorgänge werden lokal gespeichert.
- Danach wird ein Replikations-Event an RabbitMQ veröffentlicht.
- Andere Nodes konsumieren dieses Event und übernehmen die Änderung lokal.
- Wenn bereits ein Eintrag existiert, gewinnt die Meldung mit der neueren `uhrzeit`.

## Tests

Testlauf:

```bash
./mvnw clean test
```

Unter Windows:

```powershell
.\mvnw.cmd clean test
```

Die Tests verwenden H2 im Speicher und deaktivieren RabbitMQ-Listener im Testkontext.

## Hinweise

- `spring.jpa.hibernate.ddl-auto=update` ist aktuell auf einfache Entwicklung ausgelegt.
- Das Projekt ist derzeit ein PoC ohne TLS, Authentifizierung oder ausgearbeitete Fehlerverträge.
