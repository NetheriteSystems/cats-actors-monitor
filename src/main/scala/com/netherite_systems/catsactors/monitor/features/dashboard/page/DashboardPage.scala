package com.netherite_systems.catsactors.monitor.features.dashboard.page

import com.netherite_systems.htmfx4.HtmxAttributes.*
import scalatags.Text.all.*

object DashboardPage {

  private val pageTitle = tag("title")

  def build(summaryEndpointPath: String, treeEndpointPath: String, statusEndpointPath: String, pollingSeconds: Int): Tag =
    html(attr("data-theme") := "business")(
      head(
        meta(charset := "UTF-8"),
        meta(name    := "viewport", content := "width=device-width, initial-scale=1.0"),
        pageTitle("cats-actors Monitor"),
        link(href  := "https://cdn.jsdelivr.net/npm/daisyui@5", rel            := "stylesheet", attr("type") := "text/css"),
        link(href  := "https://cdn.jsdelivr.net/npm/daisyui@5/themes.css", rel := "stylesheet", attr("type") := "text/css"),
        script(src := "https://cdn.jsdelivr.net/npm/htmx.org@4.0.0"),
        script(src := "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4")
      ),
      body(cls := "min-h-screen bg-base-200 p-4")(
        div(cls := "max-w-5xl mx-auto space-y-6")(
          div(cls := "navbar bg-base-100 rounded-box shadow-md")(
            div(cls := "navbar-start")(
              span(cls := "text-xl font-bold text-primary")("cats-actors Monitor")
            ),
            div(cls := "navbar-end")(
              div(
                id        := "uptime-badge",
                hxGet     := summaryEndpointPath,
                hxTrigger := s"every ${pollingSeconds}s",
                hxSwap    := "innerHTML",
                cls       := "badge badge-outline badge-lg"
              )("loading...")
            )
          ),
          div(
            id        := "summary",
            hxGet     := summaryEndpointPath,
            hxTrigger := s"every ${pollingSeconds}s",
            hxSwap    := "innerHTML",
            cls       := "card card-border bg-base-100 shadow-md p-4"
          )(
            div(cls := "flex items-center gap-2")(
              span(cls := "loading loading-spinner loading-sm text-primary"),
              span("Connecting to actor system...")
            )
          ),
          div(cls := "card card-border bg-base-100 shadow-md")(
            div(cls := "card-body")(
              h2(cls := "card-title")("Actor Tree"),
              div(
                id        := "actor-tree",
                hxGet     := treeEndpointPath,
                hxTrigger := "load",
                hxSwap    := "innerHTML"
              )(
                div(cls := "flex items-center gap-2")(
                  span(cls := "loading loading-spinner loading-sm text-primary"),
                  span("Loading actors...")
                )
              ),
              div(
                id        := "status-poller",
                hxGet     := statusEndpointPath,
                hxTrigger := s"every ${pollingSeconds}s",
                hxSwap    := "none"
              )
            )
          )
        )
      )
    )
}
