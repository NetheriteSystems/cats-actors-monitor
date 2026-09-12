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
        div(
          id        := "dag-content",
          hxGet     := dagEndpointPath,
          hxTrigger := s"every ${pollingSeconds}s",
          hxSwap    := "innerHTML"
        )(
          DashboardPage.loadingPlaceholder("Loading DAG topology...")
        )
      )
    )
}
