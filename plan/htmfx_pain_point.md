# htmfx Pain Points — cats-actors-monitor

A detailed list of problems encountered while integrating htmfx into cats-actors-monitor, with code examples from this repo. Each section includes the symptom, root cause, and suggested fix direction for the htmfx library.

---

## 1. `resolve()` Bakes Input Permanently

**Symptom:** `resolve(input)` returns `HtmFx[F, Unit]` that always renders the *same* input, ignoring future calls. A monitor dashboard that polls every 2 seconds needs fresh data on each request, but `resolve()` freezes the first snapshot.

**Root cause:** `resolve` creates a new `HtmFx` that captures the input in a closure:

```scala
// htmfx-core/.../HtmFx.scala:59-63
def resolve(input: Input)(using Applicative[F]): HtmFx[F, Unit] =
  new HtmFx(
    function = _ => this.function(input),  // <-- input captured once, reused forever
    config = this.config
  )
```

**This repo's workaround:** `MonitorRoutes.scala` calls `.function(snapshot)` directly on every request instead of using `resolve()`:

```scala
// MonitorRoutes.scala:46-51
case GET -> Root / path if path == summaryComponent.endpointPath =>
  for {
    snapshot <- ActorTreeCollector.collect(system)   // fresh data each request
    html     <- summaryComponent.function(snapshot)   // direct call, not resolve()
    resp     <- Ok(html.render).map(...)
  } yield resp
```

**Suggested fix:** Either make `resolve()` re-evaluate the input on each call (via `F[Input] => F[Tag]`), or provide an `evaluate(input)` method that returns `F[Tag]` directly without wrapping in a new `HtmFx`.

---

## 2. Tapir Server Endpoints Completely Unused

**Symptom:** htmfx provides `.get.serverEndpoint`, `.post.serverEndpoint`, `.delete.serverEndpoint` to wire components into Tapir. None are used. The project builds manual http4s routes instead.

**Root cause:** The Tapir endpoints expect input via query params (`jsonQuery[Input]("input")`) or body, but the monitor's input comes from a side effect (`ActorTreeCollector.collect(system)`), not from the HTTP request. There's no way to inject a `system` dependency into the Tapir endpoint wiring.

```scala
// htmfx-core/.../HtmFx.scala:71-76
object get {
  val serverEndpoint: ServerEndpoint[Any, F] =
    config.basePartialServerEndpoint.get
      .in(endpointPath)
      .in(jsonQuery[Input]("input"))           // <-- input must come from query param
      .serverLogicRecoverErrors { _ => function }  // <-- no room for side effects
}
```

**This repo's workaround:** Use only `.endpointPath` (for UUID generation) and `.function()` (raw render), bypassing Tapir entirely:

```scala
// MonitorRoutes.scala:16-17
private val summaryComponent = SummaryComponent.build
private val treeComponent    = TreeComponent.build

// Only used for path generation:
pageTag = DashboardPage.build(summaryComponent.endpointPath, treeComponent.endpointPath, ...)

// Manual http4s routes instead of Tapir:
case GET -> Root / path if path == summaryComponent.endpointPath =>
  for {
    snapshot <- ActorTreeCollector.collect(system)
    html     <- summaryComponent.function(snapshot)
    resp     <- Ok(html.render)...
  } yield resp
```

**Suggested fix:** Add a `serverEndpointWithEffect` variant that accepts an `Input => F[Tag]` effectful function, or provide a way to compose htmfx components with Tapir's `serverLogicSuccess` that can handle side effects.

---

## 3. `htmfx-core` Has `publish / skip := true`

**Symptom:** Any project depending on `htmfx2` or `htmfx4` fails to resolve `htmfx-core` transitively. Build errors:

```
sbt.librarymanagement.ResolveException: Error downloading
  com.netherite_systems:htmfx-core_3:0.1.0-SNAPSHOT
```

**Root cause:** In htmfx's `build.sbt`:

```scala
// htmfx/build.sbt:19-20
lazy val htmfxCore = (projectMatrix in file("htmfx-core"))
  .settings(
    name := "htmfx-core",
    publish / skip := true,   // <-- never published to any repo
```

Both `htmfx2` and `htmfx4` depend on `htmfxCore` (`.dependsOn(htmfxCore)`), so the transitive dependency is required but unavailable.

**This repo's workaround:** Manually flip to `false`, run `sbt htmfxCore3/publishLocal`, then revert. This must be repeated every time htmfx-core changes.

**Suggested fix:** Either publish htmfx-core normally, or bundle its classes into htmfx2/htmfx4 as a fat JAR, or use sbt's `exportJars` + `packageBin` to include core classes.

---

## 4. Three Separate Artifacts With Confusing Split

**Symptom:** Adding htmfx to a project requires knowing which artifacts to include and in what order. The split is confusing.

**Root cause:** htmfx is split into three modules:

| Module | Contains | Published? |
|--------|----------|-----------|
| `htmfx-core` | `HtmFx` class, Tapir endpoints, codecs, `Monoid[Tag]` | No (`publish / skip := true`) |
| `htmfx2` | HTMX 2.x Scalatags attributes (`hxGet`, `hxTrigger`, `hxSwap`, etc.) | Yes |
| `htmfx4` | HTMX 4.x Scalatags attributes (`hxQuery`, `hxAction`, etc.) | Yes |

**This repo's build.sbt** declares both `htmfx2` and `htmfx4`:

```scala
// build.sbt:34-35
"com.netherite_systems" %% "htmfx4" % "0.1.0-SNAPSHOT",
"com.netherite_systems" %% "htmfx2" % "0.1.0-SNAPSHOT",
```

But the dashboard only uses `htmfx2` attributes. The `htmfx4` dependency is unused.

**Suggested fix:** Merge into a single `htmfx` artifact, or at minimum publish `htmfx-core` so consumers don't need manual `publishLocal` steps.

---

## 5. Hardcoded `IO` Breaks Generic `F[_]` Contexts

**Symptom:** Building an `HtmFx[IO, ActorTreeSnapshot]` in a method parameterized over `F[_]` causes type mismatch errors:

```
Found:    (summary : com.netherite_systems.htmfx.HtmFx[cats.effect.IO, Unit])
Required: com.netherite_systems.htmfx.HtmFx[F, Unit]
```

**Root cause:** `SummaryComponent.build` and `TreeComponent.build` return `HtmFx[IO, ...]` (hardcoded to `IO`), but `MonitorRoutes.resource` was originally parameterized as `def resource[F[_]](...)`. The types don't unify.

**This repo's workaround:** Drop the generic `F` from `MonitorRoutes` and hardcode `IO`:

```scala
// MonitorRoutes.scala:19-22
def resource(
  system: ActorSystem[IO],    // <-- hardcoded IO, not generic F
  config: MonitorConfig = MonitorConfig()
): Resource[IO, Unit] =
```

**Suggested fix:** Make `HtmFx.apply` and component builders accept a type parameter `F[_]` and use `Applicative[F]` constraint instead of hardcoding `IO`.

---

## 6. `endpointPath` Generates UUID Paths — No Control

**Symptom:** Each `HtmFx` component gets a random UUID-based path like `htmfx-a3f2b1c4-...`. No way to set a meaningful path like `/api/summary`.

**Root cause:**

```scala
// HtmFxConfig.scala:34
private def defaultEndpointPathGenerator: String = "htmfx-" + UUID.randomUUID()
```

**This repo's impact:** The dashboard HTML embeds these UUID paths:

```scala
// DashboardPage.scala:10
def build(summaryEndpointPath: String, treeEndpointPath: String, ...): Tag =
  // summaryEndpointPath = "htmfx-a3f2b1c4-..."
  // treeEndpointPath    = "htmfx-d5e6f7a8-..."
```

And routes match them:

```scala
// MonitorRoutes.scala:46
case GET -> Root / path if path == summaryComponent.endpointPath =>
```

This works but produces opaque URLs with no semantic meaning.

**Suggested fix:** Allow `HtmFx.apply` to accept an optional `path: String` parameter, or provide `withPath("/api/summary")` builder method.

---

## 7. No Integration With http4s Route Composition

**Symptom:** htmfx components can't be composed into an existing http4s `HttpRoutes` without manual wiring. There's no `toRoutes` or `toHttpApp` method.

**Root cause:** htmfx only exposes Tapir `ServerEndpoint`s, not http4s `HttpRoutes`. Bridging requires:

1. Importing `tapir-http4s-server`
2. Converting each endpoint to a route manually
3. Composing with existing routes

**This repo's workaround:** All route logic is manual:

```scala
// MonitorRoutes.scala:42-59
HttpRoutes.of[IO] {
  case GET -> Root =>
    Ok(pageTag.render)...

  case GET -> Root / path if path == summaryComponent.endpointPath =>
    for {
      snapshot <- ActorTreeCollector.collect(system)
      html     <- summaryComponent.function(snapshot)
      resp     <- Ok(html.render)...
    } yield resp

  case GET -> Root / path if path == treeComponent.endpointPath =>
    for {
      snapshot <- ActorTreeCollector.collect(system)
      html     <- treeComponent.function(snapshot)
      resp     <- Ok(html.render)...
    } yield resp
}
```

**Suggested fix:** Provide a `toRoutes(using Http4sDsl[F]): HttpRoutes[F]` method on `HtmFx` that handles the Tapir-to-http4s conversion internally.

---

## 8. HTMX CDN Version Mismatch With Library Modules

**Symptom:** The dashboard loads HTMX 2.x from CDN (`htmx.org@2.0.4`) but depends on both `htmfx2` (HTMX 2.x attrs) and `htmfx4` (HTMX 4.x attrs). The `htmfx4` attributes won't work with HTMX 2.x.

**This repo's code:**

```scala
// DashboardPage.scala:18 — loads HTMX 2.x
script(src := "https://unpkg.com/htmx.org@2.0.4")

// build.sbt:34 — depends on htmfx4 (HTMX 4.x attrs)
"com.netherite_systems" %% "htmfx4" % "0.1.0-SNAPSHOT",
```

**Impact:** The `htmfx4` dependency is unused dead weight. If someone tried to use `htmfx4` attributes (like `hxQuery`, `hxAction`), they wouldn't work with the HTMX 2.x CDN script.

**Suggested fix:** Either merge `htmfx2` and `htmfx4` into one module that detects the HTMX version, or document clearly which module to use with which HTMX version. Remove unused dependencies from the example.

---

## Summary Table

| # | Pain Point | Severity | Current Workaround |
|---|-----------|----------|-------------------|
| 1 | `resolve()` bakes input | High | Call `.function()` directly |
| 2 | Tapir endpoints unused | High | Manual http4s routes |
| 3 | `htmfx-core` unpublished | High | Manual `publishLocal` |
| 4 | Three confusing artifacts | Medium | Depend on both `htmfx2` + `htmfx4` |
| 5 | Hardcoded `IO` | Medium | Drop generic `F[_]` from consumer |
| 6 | No path control | Low | Accept UUID paths |
| 7 | No http4s integration | Medium | Manual route wiring |
| 8 | HTMX version mismatch | Low | Only use `htmfx2` attrs |
