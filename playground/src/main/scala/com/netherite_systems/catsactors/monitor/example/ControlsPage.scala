package com.netherite_systems.catsactors.monitor.example

import com.netherite_systems.htmfx4.HtmxAttributes.*
import scalatags.Text.all.*

object ControlsPage {

  private val pageTitle = tag("title")

  def build(actions: List[String]): Tag =
    html(attr("data-theme") := "business")(
      head(
        meta(charset := "UTF-8"),
        meta(name    := "viewport", content := "width=device-width, initial-scale=1.0"),
        pageTitle("cats-actors Monitor — Controls"),
        link(href  := "https://cdn.jsdelivr.net/npm/daisyui@5", rel            := "stylesheet", attr("type") := "text/css"),
        link(href  := "https://cdn.jsdelivr.net/npm/daisyui@5/themes.css", rel := "stylesheet", attr("type") := "text/css"),
        script(src := "https://cdn.jsdelivr.net/npm/htmx.org@4.0.0"),
        script(src := "https://cdn.jsdelivr.net/npm/@tailwindcss/browser@4")
      ),
      body(cls := "min-h-screen bg-base-200 p-4")(
        div(cls := "max-w-3xl mx-auto space-y-6")(
          div(cls := "navbar bg-base-100 rounded-box shadow-md")(
            div(cls := "navbar-start")(
              a(href := "/", cls := "text-xl font-bold text-primary")("cats-actors Monitor"),
              span(cls := "ml-2 text-sm text-base-content/50")("/ controls")
            )
          ),
          div(cls := "card card-border bg-base-100 shadow-md")(
            div(cls := "card-body")(
              h2(cls := "card-title")("Actor Actions"),
              p(cls := "text-sm text-base-content/60 mb-4")("Send events to actors in the system"),
              div(cls := "flex flex-wrap gap-3", id := "action-buttons")(
                actions.map { name =>
                  button(
                    cls    := "btn btn-primary",
                    hxPost := s"/api/action/$name",
                    hxTarget := "#action-result",
                    hxSwap := "innerHTML"
                  )(name)
                }*
              ),
              div(id := "action-result", cls := "mt-4 min-h-[2rem]")
            )
          )
        )
      )
    )

  def renderResult(actionName: String, result: String): Tag =
    div(cls := "alert alert-success shadow-sm")(
      span(cls := "text-sm")(s"$actionName → $result")
    )

  def renderError(actionName: String, error: String): Tag =
    div(cls := "alert alert-error shadow-sm")(
      span(cls := "text-sm")(s"$actionName → $error")
    )
}
