# htmfx Pain Points — cats-actors-monitor

> **Updated after htmfx commit `570901a` (feat: side effects and custom paths)**

A detailed list of problems encountered while integrating htmfx into cats-actors-monitor. Each section includes symptom, root cause, code examples, and current status.

---

## FIXED in `570901a`

### 1. ~~`htmfx-core` Has `publish / skip := true`~~ — FIXED

`publish / skip` removed from `htmfx/build.sbt:20`. `htmfx-core` now publishes normally alongside `htmfx2`/`htmfx4`.

---

### 2. ~~No `serverEndpointEffect` for Side Effects~~ — FIXED

Added `serverEndpointEffect(effect: F[Input])` on all three HTTP methods (get/post/delete). This allows injecting side effects (like `ActorTreeCollector.collect(system)`) into Tapir endpoints.

**New API in `HtmFx.scala:82-88`:**
```scala
object get {
  def serverEndpointEffect(effect: F[Input])(using Monad[F]): ServerEndpoint[Any, F] =
    config.basePartialServerEndpoint.get
      .in(endpointPath)
      .serverLogicRecoverErrors { _ => _ =>
        Monad[F].flatMap(effect)(input => function(input))
      }
}
```

**How cats-actors-monitor can now use it:**
```scala
val summaryEndpoint = summaryComponent.get.serverEndpointEffect(
  ActorTreeCollector.collect(system)
)
```

---

### 3. ~~No Path Control~~ — FIXED

Added `withPath(path: String)` method on `HtmFx` and `HtmFxConfig`.

**New API in `HtmFx.scala:32-33`:**
```scala
def withPath(path: String): HtmFx[F, Input] =
  new HtmFx(function = function, config = config.withPath(path))
```

**Usage:**
```scala
val summary = SummaryComponent.build.withPath("api/summary")
// summary.endpointPath == "api/summary"
```

---

### 4. ~~`evaluate()` Missing~~ — FIXED

Added `evaluate(input: Input): F[Tag]` that returns the rendered tag directly without wrapping in a new `HtmFx`.

**New API in `HtmFx.scala:68`:**
```scala
def evaluate(input: Input): F[Tag] = function(input)
```

**Usage (replaces manual `.function()` call):**
```scala
// Before (workaround):
html <- summaryComponent.function(snapshot)

// After (proper API):
html <- summaryComponent.evaluate(snapshot)
```

---

## STILL OPEN

### 5. `resolve()` Still Bakes Input Permanently

`resolve(input)` remains unchanged — it captures the input in a closure and reuses it forever. `evaluate` is the correct alternative for dynamic data, but `resolve` is still exposed and could mislead users.

```scala
// HtmFx.scala:62-66 — unchanged
def resolve(input: Input)(using Applicative[F]): HtmFx[F, Unit] =
  new HtmFx(
    function = _ => this.function(input),  // <-- input captured once
    config = this.config
  )
```

**Impact:** Low — `evaluate` is now the clear alternative. Consider deprecating `resolve` or adding a docstring warning.

---

### 6. Three Separate Artifacts With Confusing Split

Still split into `htmfx-core`, `htmfx2`, `htmfx4`. Consumers need to know which to depend on.

| Module | Contains | Used by this repo? |
|--------|----------|-------------------|
| `htmfx-core` | `HtmFx` class, Tapir endpoints, codecs | Yes (transitive) |
| `htmfx2` | HTMX 2.x attrs (`hxGet`, `hxTrigger`, `hxSwap`) | Yes |
| `htmfx4` | HTMX 4.x attrs (`hxQuery`, `hxAction`) | **No — unused** |

**Impact:** `htmfx4` dependency in `build.sbt:34` is dead weight.

---

### 7. Hardcoded `IO` Breaks Generic `F[_]` Contexts

`SummaryComponent.build` and `TreeComponent.build` return `HtmFx[IO, ...]` (hardcoded). Can't be used in methods parameterized over `F[_]`.

```scala
// SummaryComponent.scala:10-13 — hardcoded IO
def build: HtmFx[IO, ActorTreeSnapshot] =
  HtmFx.apply[IO, ActorTreeSnapshot] { snapshot =>
    IO.pure(render(snapshot))
  }
```

**Workaround:** `MonitorRoutes` drops generic `F` and hardcodes `IO`:
```scala
// MonitorRoutes.scala:19-22
def resource(
  system: ActorSystem[IO],    // <-- not generic F
  config: MonitorConfig = MonitorConfig()
): Resource[IO, Unit] =
```

**Fix direction:** Make component builders accept `F[_]` with `Applicative[F]` constraint.

---

### 8. No http4s Route Helper

htmfx only exposes Tapir `ServerEndpoint`s. No `toRoutes` or `toHttpApp` method for http4s users who don't want to manually bridge.

**Current workaround:** Manual http4s routes in `MonitorRoutes.scala:42-59`.

**Fix direction:** Add `toRoutes(using Http4sDsl[F]): HttpRoutes[F]` or `toHttpApp` that does the Tapir-to-http4s conversion internally.

---

### 9. HTMX CDN Version Mismatch

Dashboard loads HTMX 2.x from CDN but depends on both `htmfx2` and `htmfx4`.

```scala
// DashboardPage.scala:18 — HTMX 2.x
script(src := "https://unpkg.com/htmx.org@2.0.4")

// build.sbt:34 — htmfx4 (HTMX 4.x attrs) unused
"com.netherite_systems" %% "htmfx4" % "0.1.0-SNAPSHOT",
```

**Impact:** `htmfx4` attributes won't work with HTMX 2.x. Dependency is unused.

---

## Summary

| # | Pain Point | Status | Action |
|---|-----------|--------|--------|
| 1 | `htmfx-core` unpublished | **FIXED** | — |
| 2 | No `serverEndpointEffect` | **FIXED** | — |
| 3 | No path control | **FIXED** | — |
| 4 | No `evaluate()` | **FIXED** | — |
| 5 | `resolve()` bakes input | Open | Deprecate or document |
| 6 | Three confusing artifacts | Open | Remove unused `htmfx4` dep |
| 7 | Hardcoded `IO` | Open | Parameterize over `F[_]` |
| 8 | No http4s helper | Open | Add `toRoutes` method |
| 9 | HTMX version mismatch | Open | Remove `htmfx4` dep |
