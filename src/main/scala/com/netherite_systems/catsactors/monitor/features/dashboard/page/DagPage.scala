package com.netherite_systems.catsactors.monitor.features.dashboard.page

import com.netherite_systems.htmfx4.HtmxAttributes.*
import scalatags.Text.all.*

object DagPage {

  def build(dagEndpointPath: String, pollingSeconds: Int): Tag =
    html(cls := "dark", lang := "en")(
      head(
        meta(charset := "utf-8"),
        meta(name    := "viewport", content := "width=device-width, initial-scale=1.0"),
        tag("title")("Topology DAG - cats-actors Monitor"),
        tag("link")(
          href := "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200",
          rel  := "stylesheet"
        ),
        link(href := "https://fonts.googleapis.com", rel := "preconnect"),
        link(href := "https://fonts.gstatic.com", rel    := "preconnect", attr("crossorigin") := ""),
        link(
          href := "https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap",
          rel  := "stylesheet"
        ),
        tag("link")(
          href := "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap",
          rel  := "stylesheet"
        ),
        script(src := "https://cdn.tailwindcss.com"),
        tag("script")(id := "tailwind-config")(raw(DashboardPage.tailwindConfig)),
        tag("script")(src := "https://cdn.jsdelivr.net/npm/htmx.org@4.0.0"),
        DashboardPage.baseStyleTag
      ),
      body(cls := "bg-background font-body-md text-on-surface antialiased")(
        DashboardPage.pageHeader(pollingSeconds, "hub"),
        DashboardPage.pageSidebar("dag"),
        mainContent(dagEndpointPath, pollingSeconds)
      )
    )

  private def mainContent(dagEndpointPath: String, pollingSeconds: Int): Tag =
    tag("main")(cls := "pl-64 relative pt-16 min-h-screen bg-background w-full px-gutter-lg pb-gutter-lg")(
      div(cls := "flex flex-col w-full")(
        div(cls := "flex items-center justify-between pb-space-sm")(
          div(cls := "flex items-center gap-space-sm")(
            span(cls := "material-symbols-outlined text-primary text-[18px]")("hub"),
            span(cls := "font-headline-sm text-headline-sm text-on-surface")("Topology DAG"),
            span(cls := "font-label-sm text-label-sm bg-surface-container text-secondary px-space-xs rounded")("LIVE")
          ),
          div(cls := "flex items-center gap-space-xs")(
            a(
              cls := "bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface px-space-sm py-space-xs rounded font-label-sm text-label-sm transition-colors flex items-center gap-space-xs",
              href := "/"
            )(
              span(cls := "material-symbols-outlined text-[14px]")("arrow_back"),
              "Back to Hierarchy"
            )
          )
        ),
        div(cls := "relative w-full")(
          div(
            id        := "dag-content",
            hxGet     := dagEndpointPath,
            hxTrigger := s"every ${pollingSeconds}s",
            hxSwap    := "innerHTML"
          )(
            DashboardPage.loadingPlaceholder("Loading DAG topology...")
          ),
          inspectorPanel
        )
      )
    )

  private def inspectorPanel: Tag =
    div(
      cls := "absolute right-space-md top-space-md bottom-space-md w-72 xl:w-80 bg-surface-container-high/95 backdrop-blur-md rounded-xl p-space-md shadow-2xl flex flex-col justify-between overflow-y-auto z-20",
      id := "dag-inspector"
    )(
      div(cls := "flex flex-col gap-space-md")(
        div(cls := "flex items-start justify-between pb-space-xs")(
          div(cls := "flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-primary font-mono tracking-wider uppercase")("ACTOR INSPECTION"),
            span(cls := "font-headline-sm text-headline-sm text-on-surface", id := "insp-title")("Select a Node"),
            span(cls := "font-code-sm text-code-sm text-on-surface-variant", id := "insp-path")("Click any actor node")
          ),
          span(
            cls := "font-label-sm text-label-sm bg-surface-container text-on-surface-variant px-space-xs py-0.5 rounded font-mono",
            id  := "insp-badge"
          )("IDLE")
        ),
        div(cls := "grid grid-cols-2 gap-space-xs")(
          div(cls := "bg-surface-container-low p-space-sm rounded-lg flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Mailbox Queue"),
            div(cls := "flex items-baseline gap-1 mt-0.5")(
              span(cls := "font-code-lg text-code-lg text-primary font-bold", id := "insp-mailbox")("0"),
              span(cls := "font-label-sm text-label-sm text-on-surface-variant")("msgs")
            )
          ),
          div(cls := "bg-surface-container-low p-space-sm rounded-lg flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Children"),
            div(cls := "flex items-baseline gap-1 mt-0.5")(
              span(cls := "font-code-lg text-code-lg text-on-surface font-bold", id := "insp-children")("0"),
              span(cls := "font-label-sm text-label-sm text-on-surface-variant")("actors")
            )
          )
        ),
        div(cls := "bg-surface-container-low p-space-sm rounded-lg flex flex-col gap-space-xs")(
          div(cls := "flex justify-between items-center")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Status"),
            span(cls := "font-code-sm text-code-sm text-on-surface font-semibold", id := "insp-status-detail")("IDLE")
          ),
          div(cls := "w-full bg-surface-container-lowest h-1.5 rounded-full overflow-hidden")(
            div(cls := "bg-secondary h-full w-0 transition-all duration-300", id := "insp-bar")
          )
        ),
        div(
          cls := "bg-surface-container-low p-space-sm rounded-lg flex flex-col gap-1 text-on-surface-variant font-label-sm text-label-sm"
        )(
          div(cls := "flex justify-between")(
            span("Path:"),
            span(cls := "text-on-surface font-code-sm text-code-sm truncate", id := "insp-full-path")("n/a")
          ),
          div(cls := "flex justify-between")(
            span("Status:"),
            span(cls := "text-secondary font-code-sm text-code-sm", id := "insp-status-text")("IDLE")
          ),
          div(cls := "flex justify-between")(
            span("Is Idle:"),
            span(cls := "text-on-surface font-code-sm text-code-sm", id := "insp-is-idle")("true")
          )
        )
      )
    )
}
