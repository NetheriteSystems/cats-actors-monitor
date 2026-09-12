package com.netherite_systems.catsactors.monitor.features.dashboard.page

import com.netherite_systems.htmfx4.HtmxAttributes.*
import scalatags.Text.all.*

object DagPage {

  private val pageTitle    = tag("title")
  private val materialLink = tag("link")
  private val asideTag     = tag("aside")
  private val navTag       = tag("nav")
  private val mainTag      = tag("main")
  private val headerTag    = tag("header")

  def build(dagEndpointPath: String, pollingSeconds: Int): Tag =
    html(cls := "dark", lang := "en")(
      head(
        meta(charset := "utf-8"),
        meta(name    := "viewport", content := "width=device-width, initial-scale=1.0"),
        pageTitle("Topology DAG - cats-actors Monitor"),
        materialLink(
          href := "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:opsz,wght,FILL,GRAD@20..48,100..700,0..1,-50..200",
          rel  := "stylesheet"
        ),
        link(href := "https://fonts.googleapis.com", rel := "preconnect"),
        link(href := "https://fonts.gstatic.com", rel    := "preconnect", attr("crossorigin") := ""),
        link(
          href := "https://fonts.googleapis.com/css2?family=Geist:wght@400;500;600;700&family=JetBrains+Mono:wght@400;500;600&display=swap",
          rel  := "stylesheet"
        ),
        materialLink(
          href := "https://fonts.googleapis.com/css2?family=Material+Symbols+Outlined:wght,FILL@100..700,0..1&display=swap",
          rel  := "stylesheet"
        ),
        script(src := "https://cdn.tailwindcss.com"),
        tag("script")(id := "tailwind-config")(raw(DashboardPage.tailwindConfig)),
        tag("script")(src := "https://cdn.jsdelivr.net/npm/htmx.org@4.0.0"),
        DashboardPage.baseStyleTag
      ),
      body(cls := "bg-background font-body-md text-on-surface antialiased")(
        header(pollingSeconds),
        sidebar,
        mainContent(dagEndpointPath, pollingSeconds)
      )
    )

  private def header(pollingSeconds: Int): Tag =
    headerTag(
      cls := "fixed top-0 left-0 right-0 z-50 h-16 bg-surface-container-lowest/90 backdrop-blur-md shadow-[0_1px_8px_rgba(0,0,0,0.4)]"
    )(
      div(cls := "w-full h-16 px-gutter-lg flex items-center justify-between gap-space-lg")(
        div(cls := "flex items-center gap-space-md shrink-0")(
          div(cls := "flex flex-col")(
            span(cls := "font-headline-sm text-headline-sm tracking-tight text-on-surface uppercase")("cats-actors Monitor"),
            div(cls := "flex items-center gap-space-sm")(
              span(cls := "font-code-sm text-code-sm text-primary", id := "header-system-name")("loading..."),
              span(cls := "font-label-sm text-label-sm px-space-xs bg-surface-container text-on-surface-variant rounded")("cats-actors")
            )
          )
        ),
        div(cls := "hidden lg:flex items-center gap-space-md bg-surface-container-low px-space-md py-space-xs rounded-lg")(
          div(cls := "flex items-center gap-space-xs", id := "header-health")(
            span(cls := "w-2 h-2 rounded-full bg-secondary animate-pulse"),
            span(cls := "font-label-sm text-label-sm text-secondary uppercase")("HEALTHY")
          ),
          div(cls := "h-4 w-px bg-surface-variant"),
          div(cls := "flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Uptime"),
            span(cls := "font-code-sm text-code-sm text-on-surface", id := "header-uptime")("loading...")
          ),
          div(cls := "h-4 w-px bg-surface-variant"),
          div(cls := "flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Active Actors"),
            span(cls := "font-code-sm text-code-sm text-on-surface", id := "header-actors")("loading...")
          ),
          div(cls := "h-4 w-px bg-surface-variant"),
          div(cls := "flex flex-col")(
            span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Mailbox"),
            span(cls := "font-code-sm text-code-sm text-on-surface", id := "header-mailbox")("loading...")
          )
        ),
        div(cls := "flex items-center gap-space-md shrink-0")(
          div(cls := "flex flex-col items-end")(
            span(cls := "font-code-sm text-code-sm text-on-surface")("cats-actors-monitor"),
            span(cls := "font-label-sm text-label-sm text-primary-container uppercase")(
              s"${pollingSeconds}s interval"
            )
          ),
          div(cls := "w-8 h-8 rounded-full bg-primary flex items-center justify-center")(
            span(cls := "material-symbols-outlined text-on-primary text-[18px]")("hub")
          )
        )
      )
    )

  private def sidebar: Tag =
    asideTag(
      cls := "fixed left-0 top-16 bottom-0 w-64 bg-surface-container-lowest z-40 flex flex-col p-space-md"
    )(
      div(cls := "flex items-center justify-between px-space-sm mb-space-md")(
        span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Runtime Operations"),
        span(cls := "font-label-sm text-label-sm text-secondary bg-surface-container px-space-xs rounded")("cats-actors")
      ),
      navTag(cls := "flex flex-col gap-space-xs flex-1")(
        DashboardPage.sidebarLinkComponent("Actor Hierarchy", "/user", isActive = false, linkHref = "/"),
        DashboardPage.sidebarLinkComponent("Topology DAG", "LIVE", isActive = true, linkHref = "/dag"),
        DashboardPage.sidebarLinkComponent("Dead Letter Stream", "0", isActive = false, disabled = true),
        DashboardPage.sidebarLinkComponent("Cluster Nodes", "N/A", isActive = false, disabled = true)
      ),
      div(cls := "bg-surface-container-low p-space-sm rounded-lg flex flex-col gap-space-xs mt-auto", id := "sidebar-mailbox")(
        span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("Mailbox Saturation"),
        div(cls := "w-full bg-surface-container-highest h-1 rounded-full overflow-hidden")(
          div(cls := "bg-primary h-full", id := "sidebar-mailbox-bar", style := "width: 0%")
        ),
        div(cls := "flex justify-between font-label-sm text-label-sm text-on-surface-variant")(
          span("Avg mailbox"),
          span(cls := "text-secondary", id := "sidebar-avg-mailbox")("0 msgs")
        )
      )
    )

  private def mainContent(dagEndpointPath: String, pollingSeconds: Int): Tag =
    div(cls := "pl-64")(
      mainTag(cls := "relative pt-16 min-h-screen bg-background w-full px-gutter-lg pb-gutter-lg")(
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
            loadingPlaceholder
          )
        )
      )
    )

  private def loadingPlaceholder: Tag =
    div(cls := "flex items-center justify-center gap-2 p-8 bg-surface-container-low rounded-xl")(
      span(cls := "material-symbols-outlined text-primary animate-spin text-[24px]")("sync"),
      span(cls := "font-code-sm text-code-sm text-on-surface-variant")("Loading DAG topology...")
    )
}
