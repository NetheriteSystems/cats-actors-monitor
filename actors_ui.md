# cats-actors-monitor

## Context

`cats-actors` is a purely functional actor system built on cats-effect. Many projects that use it (like Macaws) create actor trees with zero observability — no way to see how many actors exist, what state they're in, or whether their mailboxes are backing up.

The `cats-actors` library already exposes rich introspection APIs (`allChildren`, `cell`, `mailbox`, `childrenRefs`) that most consumers don't use. This project is a standalone monitoring library that surfaces that data through an HTTP API and a browser-based dashboard. It works with **any** `cats-actors` system — no coupling to any specific application.

---

## Feasibility: Fully Possible

Every piece of data we need is already readable from the library without modifying `cats-actors`:

| Data Needed | Library API | Import |
|---|---|---|
| List all actors in the system | `system.allChildren: F[List[NoSendActorRef[F]]]` | `com.suprnation.typelevel.actors.syntax.*` |
| Actor name | `ref.path.name: String` | Built-in on `ActorRef` |
| Actor path | `ref.path: ActorPath` (full `/system/user/...`) | Built-in |
| Parent path | `ref.path.parent: ActorPath` | Built-in |
| Direct children count | `context.children.map(_.size)` or `cell.childrenRefs` | `ActorContext` / `Cell` |
| Mailbox message count | `ref.cellOp.flatMap(_.numberOfMessages)` | `Cell` / `Mailbox` |
| Actor idle | `ref.cellOp.flatMap(_.isIdle)` | `Cell` |
| Actor terminated | `ref.cellOp.flatMap(_.isTerminated)` | `Cell` |
| System uptime | `system.uptime: F[Long]` | `ActorSystem` |
| System name | `system.name: String` | `ActorSystem` |

### Key Import for Debug Syntax

```scala
import com.suprnation.typelevel.actors.syntax.*
import com.suprnation.typelevel.actors.syntax.given
```

This brings `allChildren`, `cellOp`, `cell`, and `allChildrenFromThisActor` extension methods into scope.

### Known Limitations

- `cellOp` returns `F[Option[ActorCell[...]]]` — returns `None` for remote actors or actors whose cell has been cleared. The collector must handle this gracefully.
- `allChildren` walks the full tree on every call. For a system with many actors this is O(n). Acceptable for monitoring polled every few seconds.
- The library does not expose mailbox **contents** — only `numberOfMessages`. The UI shows count, not the actual messages.
- No message processing latency or throughput stats. Only instantaneous mailbox depth and idle state.

---

## Architecture

### Project: `cats-actors-monitor`

A standalone Scala 3 library. Consumers add it as a dependency and pass their `ActorSystem[F]` to start monitoring.

```
cats-actors-monitor/
├── build.sbt
└── src/
    ├── main/scala/com/suprnation/catsactors/monitor/
    │   ├── MonitorConfig.scala
    │   ├── ActorSnapshot.scala
    │   ├── ActorTreeCollector.scala
    │   ├── MonitorRoutes.scala
    │   └── MonitorServer.scala
    └── main/resources/
        └── monitor/
            └── index.html
```

### Dependency Graph

```
cats-actors-monitor
       |
       +--->  cats-actors 2.2.0
       +--->  http4s-ember-server
       +--->  http4s-dsl
       +--->  http4s-circe
       +--->  circe-generic
```

No dependency on any specific application (no macaws, no petlove, etc.).

---

## Data Model

### `ActorSnapshot`

Represents a single actor's current state at query time.

```scala
package com.suprnation.catsactors.monitor

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto.*

case class ActorSnapshot(
  name: String,              // ref.path.name
  path: String,              // full path, e.g. "/system/user/my-router/chat-actor-1"
  parentPath: Option[String],// parent path (None for guardian/root)
  childCount: Int,           // direct children from childrenRefs
  mailboxSize: Int,          // numberOfMessages from Cell
  isIdle: Boolean,           // isIdle from Cell
  isTerminated: Boolean      // isTerminated from Cell
)

object ActorSnapshot {
  given Encoder[ActorSnapshot] = deriveEncoder
  given Decoder[ActorSnapshot] = deriveDecoder
}
```

### `ActorTreeSnapshot`

Full system snapshot returned by the API.

```scala
case class ActorTreeSnapshot(
  systemName: String,         // ActorSystem.name
  uptimeSeconds: Long,        // system.uptime
  totalActors: Int,           // actors.length
  idleCount: Int,             // actors.count(_.isIdle)
  busyCount: Int,             // actors.count(a => !a.isIdle && !a.isTerminated)
  terminatedCount: Int,       // actors.count(_.isTerminated)
  actors: List[ActorSnapshot] // all actors in tree order (depth-first)
)

object ActorTreeSnapshot {
  given Encoder[ActorTreeSnapshot] = deriveEncoder
  given Decoder[ActorTreeSnapshot] = deriveDecoder
}
```

### `MonitorConfig`

```scala
case class MonitorConfig(
  host: String = "0.0.0.0",
  port: Int = 8080,
  pollingIntervalSeconds: Int = 2  // used by frontend, not backend
)
```

---

## Components

### 1. `ActorTreeCollector`

**Responsibility:** Walk the actor system tree and produce an `ActorTreeSnapshot`.

```scala
package com.suprnation.catsactors.monitor

import cats.effect.Async
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import cats.syntax.traverse.*
import com.suprnation.actor.ActorSystem
import com.suprnation.actor.NoSendActorRef
import com.suprnation.typelevel.actors.syntax.*

object ActorTreeCollector {

  def collect[F[_]: Async](system: ActorSystem[F]): F[ActorTreeSnapshot] =
    for {
      allRefs   <- system.allChildren
      snapshots <- allRefs.traverse(ref => snapshotOne[F](ref))
      uptime    <- system.uptime
    } yield ActorTreeSnapshot(
      systemName      = system.name,
      uptimeSeconds   = uptime,
      totalActors     = snapshots.length,
      idleCount       = snapshots.count(_.isIdle),
      busyCount       = snapshots.count(a => !a.isIdle && !a.isTerminated),
      terminatedCount = snapshots.count(_.isTerminated),
      actors          = snapshots
    )

  private def snapshotOne[F[_]: Async](ref: NoSendActorRef[F]): F[ActorSnapshot] =
    for {
      cellOpt  <- ref.cellOp
      children <- cellOpt.traverse(_.childrenRefs.get).map(_.map(_.children.size).getOrElse(0))
      msgs     <- cellOpt.traverse(_.numberOfMessages).map(_.getOrElse(0))
      idle     <- cellOpt.traverse(_.isIdle).map(_.getOrElse(false))
      term     <- cellOpt.traverse(_.isTerminated).map(_.getOrElse(true))
    } yield ActorSnapshot(
      name         = ref.path.name,
      path         = ref.path.toString,
      parentPath   = if (ref.path == ref.path.root) None else Some(ref.path.parent.toString),
      childCount   = children,
      mailboxSize  = msgs,
      isIdle       = idle,
      isTerminated = term
    )
}
```

**Notes:**
- `cellOp` returns `None` for actors whose cell is unavailable. We treat those as terminated with 0 children.
- `allChildren` is depth-first from the guardian. The list order naturally reflects the tree structure.
- Each `cellOp` + `numberOfMessages` + `isIdle` + `isTerminated` is a pure read from `Ref` — no blocking, no side effects.

### 2. `MonitorRoutes`

**Responsibility:** HTTP endpoints.

```scala
package com.suprnation.catsactors.monitor

import cats.effect.Async
import com.suprnation.actor.ActorSystem
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.circe.CirceEntityCodec.*
import org.http4s.StaticFile
import java.io.File

object MonitorRoutes {

  def routes[F[_]: Async](system: ActorSystem[F]): HttpRoutes[F] = {
    val dsl = new Http4sDsl[F] {}
    import dsl.*

    HttpRoutes.of[F] {
      case GET -> Root / "api" / "actors" =>
        for {
          snapshot <- ActorTreeCollector.collect(system)
          resp     <- Ok(snapshot)
        } yield resp

      case GET -> Root / "api" / "health" =>
        for {
          terminated <- system.isTerminated
          uptime     <- system.uptime
          resp       <- Ok(HealthResponse(!terminated, uptime))
        } yield resp

      case req @ GET -> Root =>
        StaticFile.fromResource("/monitor/index.html", Some(req)).getOrElseF(NotFound())

      case req @ GET -> "static" /: path =>
        StaticFile.fromResource(s"/monitor${path.toString}", Some(req)).getOrElseF(NotFound())
    }
  }
}

case class HealthResponse(healthy: Boolean, uptimeSeconds: Long)
object HealthResponse {
  given io.circe.Encoder[HealthResponse] = io.circe.generic.semiauto.deriveEncoder
}
```

**Endpoints:**

| Method | Path | Response | Description |
|---|---|---|---|
| `GET` | `/` | `text/html` | Dashboard SPA |
| `GET` | `/api/actors` | `application/json` → `ActorTreeSnapshot` | Full actor tree snapshot |
| `GET` | `/api/health` | `application/json` → `HealthResponse` | System health + uptime |

### 3. `MonitorServer`

**Responsibility:** Lifecycle-managed http4s Ember server as a `Resource[F, Unit]`.

```scala
package com.suprnation.catsactors.monitor

import cats.effect.{Async, Resource}
import com.comcast.ip4s.*
import com.suprnation.actor.ActorSystem
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*

object MonitorServer {

  def resource[F[_]: Async](
    system: ActorSystem[F],
    config: MonitorConfig = MonitorConfig()
  ): Resource[F, Unit] =
    EmberServerBuilder.default[F]
      .withHost(Host.fromString(config.host).get)
      .withPort(Port.fromInt(config.port).get)
      .withHttpApp(MonitorRoutes.routes[F](system).orNotFound)
      .build
      .void
}
```

### 4. `index.html` (Dashboard)

**Responsibility:** Single-page browser UI. Vanilla JS, no build step. Polled updates.

**Location:** `src/main/resources/monitor/index.html`

**Layout:**

```
┌──────────────────────────────────────────────────────┐
│  cats-actors Monitor                                 │
│  System: my-actor-system  |  Uptime: 1h 23m 45s      │
├──────────────────────────────────────────────────────┤
│                                                      │
│  Summary: 12 actors | 10 idle | 1 busy | 1 terminated│
│                                                      │
│  ─────────────────────────────────────────────────── │
│                                                      │
│  ▼ my-router                   [idle]   mailbox: 0   │
│    ▼ worker-1                  [busy]   mailbox: 3   │
│    ▼ worker-2                  [idle]   mailbox: 0   │
│    ▼ worker-3                  [idle]   mailbox: 0   │
│  ▼ post-processor              [idle]   mailbox: 0   │
│  ▼ scheduler                   [idle]   mailbox: 0   │
│                                                      │
└──────────────────────────────────────────────────────┘
```

**Behavior:**
- Polls `GET /api/actors` every 2 seconds (configurable via `MonitorConfig.pollingIntervalSeconds`)
- Renders actors as a tree (parent-child derived from `parentPath` matching `path`)
- Each row shows: expand/collapse toggle, name, state badge (idle=green, busy=yellow, terminated=red), mailbox count
- Summary bar at top with aggregate counts
- Auto-reconnects if the server is unreachable
- No external dependencies (no React, no npm)

**Tree Construction:**

The API returns a flat list. The frontend builds the tree client-side:

```javascript
function buildTree(actors) {
  const byPath = Object.fromEntries(actors.map(a => [a.path, {...a, children: []}]));
  const roots = [];
  for (const actor of actors) {
    if (actor.parentPath && byPath[actor.parentPath]) {
      byPath[actor.parentPath].children.push(byPath[actor.path]);
    } else {
      roots.push(byPath[actor.path]);
    }
  }
  return roots;
}
```

---

## Usage

### With any cats-actors project

```scala
import com.suprnation.catsactors.monitor.{MonitorServer, MonitorConfig}

val myApp: Resource[IO, Unit] =
  for {
    system <- ActorSystem[IO]("my-system")
    _      <- MonitorServer.resource(system, MonitorConfig(port = 9090))
    // ... spawn actors, run your app ...
  } yield ()
```

The monitor server runs as long as the `Resource` is alive. When the actor system shuts down, the monitor shuts down with it.

### Custom actor labels (extending the monitor)

Applications can implement `MonitorableActor` to provide semantic labels for their actors. The collector checks if any actor implements this trait and merges labels into the snapshot.

```scala
trait MonitorableActor[F[_]] {
  def actorLabels: F[Map[String, String]]  // actorPath -> semanticLabel
}
```

This is optional — raw actor names are already informative.

---

## Implementation Steps

### Phase 1: Core Monitoring

| Step | Description | Files |
|---|---|---|
| 1 | Create `cats-actors-monitor` project structure | `build.sbt`, directory tree |
| 2 | Add cats-actors + http4s + circe dependencies | `build.sbt` |
| 3 | Implement `ActorSnapshot` + `ActorTreeSnapshot` data models | `ActorSnapshot.scala` |
| 4 | Implement `ActorTreeCollector.collect` | `ActorTreeCollector.scala` |
| 5 | Implement `MonitorRoutes` (3 endpoints) | `MonitorRoutes.scala` |
| 6 | Implement `MonitorServer` (Ember resource) | `MonitorServer.scala` |
| 7 | Implement `MonitorConfig` | `MonitorConfig.scala` |
| 8 | Build `index.html` dashboard | `monitor/index.html` |
| 9 | Verify with a manual test: create system, spawn actors, open browser | — |

### Phase 2: Enrichment (Optional)

| Step | Description | Files |
|---|---|---|
| 10 | Add `MonitorableActor` trait | `MonitorableActor.scala` |
| 11 | Update `ActorTreeCollector` to merge labels from `MonitorableActor` | `ActorTreeCollector.scala` |
| 12 | Update dashboard to show custom labels | `index.html` |

### Phase 3: Event Stream Monitoring (Optional)

| Step | Description | Files |
|---|---|---|
| 13 | Subscribe to `system.eventStream` for dead letters | `EventStreamMonitor.scala` |
| 14 | Add `/api/events` SSE endpoint for real-time events | `MonitorRoutes.scala` |
| 15 | Add event log panel to dashboard | `index.html` |

---

## `build.sbt`

```scala
ThisBuild / scalaVersion := "3.8.4"

val http4sVersion = "0.23.30"
val circeVersion  = "0.14.10"

lazy val root = project
  .in(file("."))
  .settings(
    name := "cats-actors-monitor",
    resolvers += "jitpack" at "https://jitpack.io",
    libraryDependencies ++= Seq(
      "com.github.cloudmark.cats-actors" %% "cats-actors"          % "2.2.0",
      "org.http4s"                       %% "http4s-ember-server"   % http4sVersion,
      "org.http4s"                       %% "http4s-dsl"            % http4sVersion,
      "org.http4s"                       %% "http4s-circe"          % http4sVersion,
      "io.circe"                         %% "circe-generic"         % circeVersion,

      /* Testing */
      "org.typelevel"                    %% "weaver-cats"           % "0.13.0" % Test,
      "com.github.cloudmark.cats-actors" %% "cats-actors-testkit"   % "2.2.0"  % Test,
    )
  )
```

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| `cellOp` returns `None` for some refs | Incomplete data | Treat as terminated with 0 children; log a warning |
| `allChildren` is O(n) per call | Slow with 1000+ actors | Cache snapshot with `Ref` + TTL (e.g., 1 second); serve cached on rapid requests |
| http4s version conflicts with consumer's deps | Build failure | Use http4s 0.23.x which aligns with cats-effect 3.x (standard in the ecosystem) |
| `ActorCell.childrenRefs` is internal API | May break on cats-actors upgrade | Abstract behind `ActorTreeCollector`; pin cats-actors version |
| Port conflict on `MonitorConfig.port` | Server startup failure | Return `Resource` error with clear message; let caller choose another port |
| No CORS headers on API | Dashboard served from different origin won't work | Dashboard is served from the same server (same origin) — no CORS needed |

---

## Open Questions

1. **Should we use Server-Sent Events (SSE) or WebSocket for live updates?**
   - Recommendation: SSE for Phase 3 events. Polling is sufficient for the tree snapshot (Phase 1).

2. **Should the dashboard support filtering/sorting actors?**
   - Not in Phase 1. The tree view with collapsible nodes is sufficient for typical actor counts.

3. **Should the monitor expose an API to query individual actors by path?**
   - Useful for large systems. Add `GET /api/actors/:path` in Phase 2 if needed.
