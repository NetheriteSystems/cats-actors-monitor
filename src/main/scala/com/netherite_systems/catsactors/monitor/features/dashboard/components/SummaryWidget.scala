package com.netherite_systems.catsactors.monitor.features.dashboard.components

import cats.effect.IO
import com.netherite_systems.catsactors.monitor.shared.model.ActorTreeSnapshot
import com.netherite_systems.htmfx.*
import scalatags.Text.all.*

object SummaryWidget {

  def build: HtmFx[IO, ActorTreeSnapshot] =
    HtmFx
      .apply[IO, ActorTreeSnapshot] { snapshot =>
        IO.pure(render(snapshot))
      }
      .withPath("api/summary")

  def renderHeaderBadges(snapshot: ActorTreeSnapshot): Tag =
    div(style := "display:none")(
      div(id := "header-system-name-oob", attr("hx-swap-oob") := "true", cls := "font-code-sm text-code-sm text-primary")(
        snapshot.systemName
      ),
      div(id := "header-uptime-oob", attr("hx-swap-oob") := "true", cls := "font-code-sm text-code-sm text-on-surface")(
        formatUptime(snapshot.uptimeSeconds)
      ),
      div(id := "header-actors-oob", attr("hx-swap-oob") := "true", cls := "font-code-sm text-code-sm text-primary")(
        f"${snapshot.totalActors}%,d"
      ),
      div(id := "header-mailbox-oob", attr("hx-swap-oob") := "true", cls := "font-code-sm text-code-sm text-on-surface")(
        f"${snapshot.totalMailbox}%,d msgs"
      ),
      div(id := "sidebar-mailbox-bar-oob", attr("hx-swap-oob") := "true", style := s"width: ${mailboxPercent(snapshot)}%"),
      div(id := "sidebar-avg-mailbox-oob", attr("hx-swap-oob") := "true", cls := "font-label-sm text-label-sm text-secondary")(
        s"${snapshot.totalMailbox} msgs"
      )
    )

  private def render(snapshot: ActorTreeSnapshot): Tag =
    div(cls := "w-full bg-surface-container-lowest p-space-md rounded-xl shadow-md flex flex-col gap-space-md")(
      div(cls := "flex items-center gap-space-xs")(
        span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")("System"),
        span(cls := "font-code-sm text-code-sm text-primary")(snapshot.systemName)
      ),
      vitalsRow(snapshot),
      searchBar
    )

  private def vitalsRow(snapshot: ActorTreeSnapshot): Tag =
    div(cls := "grid grid-cols-1 sm:grid-cols-3 gap-space-sm w-full")(
      metricCard(
        label = "Active Actors",
        dotColor = "bg-secondary",
        value = f"${snapshot.totalActors}%,d",
        valueColor = "text-on-surface",
        sub = "LIVE",
        subColor = "text-secondary",
        barPercent = 100,
        barColor = "bg-secondary"
      ),
      metricCard(
        label = "Total Mailbox",
        labelRight = Some("q/depth"),
        value = f"${snapshot.totalMailbox}%,d",
        valueColor = "text-on-surface",
        sub = "queued",
        subColor = "text-on-surface-variant",
        barPercent = mailboxPercent(snapshot),
        barColor = "bg-primary"
      ),
      metricCard(
        label = "Cluster Uptime",
        icon = Some("timer"),
        value = formatUptimeCompact(snapshot.uptimeSeconds),
        valueColor = "text-on-surface",
        sub = "",
        subColor = "text-on-surface-variant",
        barPercent = 100,
        barColor = "bg-secondary"
      )
    )

  private def metricCard(
    label: String,
    dotColor: String = "",
    labelRight: Option[String] = None,
    icon: Option[String] = None,
    value: String,
    valueColor: String,
    sub: String,
    subColor: String,
    barPercent: Int,
    barColor: String
  ): Tag = {
    val labelTag: Tag = labelRight match {
      case Some(text)             => span(cls := "font-label-sm text-label-sm text-primary")(text)
      case None if icon.isDefined =>
        span(cls := "material-symbols-outlined text-primary text-[14px]")(icon.get)
      case None =>
        span(cls := s"w-1.5 h-1.5 rounded-full $dotColor")
    }
    div(cls := "bg-surface-container-low p-space-sm rounded-lg flex flex-col justify-between")(
      div(cls := "flex items-center justify-between")(
        span(cls := "font-label-sm text-label-sm text-on-surface-variant uppercase")(label),
        labelTag
      ),
      div(cls := "flex items-baseline gap-space-xs mt-space-xs")(
        span(cls := s"font-headline-sm text-headline-sm $valueColor font-semibold")(value),
        if sub.nonEmpty then span(cls := s"font-code-sm text-code-sm $subColor")(sub)
        else span()
      ),
      div(cls := "w-full bg-surface-container-highest h-0.5 rounded-full mt-space-xs overflow-hidden")(
        div(cls := s"$barColor h-full", style := s"width: $barPercent%")
      )
    )
  }

  private def searchBar: Tag =
    div(
      cls := "flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-space-sm bg-surface-container-low p-space-xs rounded-lg"
    )(
      div(cls := "flex-1 flex items-center gap-space-sm bg-surface-container-lowest px-space-md py-space-xs rounded")(
        span(cls := "material-symbols-outlined text-outline text-[16px]")("search"),
        input(
          cls := "bg-transparent border-none outline-none text-on-surface font-code-sm text-code-sm placeholder:text-outline w-full",
          id  := "actorSearchInput",
          attr("placeholder") := "Search by ActorPath (e.g. /user/*), status, or mailbox size...",
          attr("type")        := "text"
        ),
        span(cls := "font-label-sm text-label-sm text-outline bg-surface-container px-space-xs py-0.5 rounded")("ESC TO CLEAR")
      ),
      div(cls := "flex items-center gap-space-xs overflow-x-auto shrink-0 pb-1 lg:pb-0")(
        filterPill("ALL ACTORS", "all", isActive = true),
        filterPill("MAILBOX > 0", "warning", dotColor = Some("bg-primary")),
        filterPill("TERMINATED", "failed", dotColor = Some("bg-error")),
        filterPill("IDLE", "idle", dotColor = None),
        div(cls := "h-4 w-px bg-surface-variant mx-space-xs"),
        div(cls := "flex items-center gap-space-xs")(
          button(
            cls := "bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface p-space-xs rounded flex items-center",
            id            := "btnExpandAll",
            attr("title") := "Expand entire tree"
          )(
            span(cls := "material-symbols-outlined text-[16px]")("unfold_more")
          ),
          button(
            cls := "bg-surface-container hover:bg-surface-container-high text-on-surface-variant hover:text-on-surface p-space-xs rounded flex items-center",
            id            := "btnCollapseAll",
            attr("title") := "Collapse all levels"
          )(
            span(cls := "material-symbols-outlined text-[16px]")("unfold_less")
          )
        )
      )
    )

  private def filterPill(label: String, filter: String, isActive: Boolean = false, dotColor: Option[String] = None): Tag =
    button(
      cls := s"filter-pill ${
          if isActive then "bg-primary-container text-on-primary-container"
          else "bg-surface-container text-on-surface-variant hover:text-on-surface hover:bg-surface-container-high"
        } px-space-sm py-space-xs rounded font-label-sm text-label-sm flex items-center gap-space-xs transition-all",
      attr("data-filter") := filter
    )(
      dotColor.map(c => span(cls := s"w-1.5 h-1.5 rounded-full $c")).toSeq :+
        raw(label)
    )

  private def formatUptime(seconds: Long): String = {
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    val s = seconds  % 60
    if d > 0 then f"${d}d ${h}%02dh ${m}%02dm"
    else if h > 0 then f"${h}h ${m}%02dm ${s}%02ds"
    else f"${m}m ${s}%02ds"
  }

  private def formatUptimeCompact(seconds: Long): String = {
    val d = seconds / 86400
    val h = (seconds % 86400) / 3600
    val m = (seconds % 3600) / 60
    if d > 0 then f"${d}d ${h}%02dh"
    else if h > 0 then f"${h}h ${m}%02dm"
    else f"${m}%dm"
  }

  private def mailboxPercent(snapshot: ActorTreeSnapshot): Int =
    if snapshot.totalActors == 0 then 0
    else math.min(100, (snapshot.totalMailbox.toDouble / snapshot.totalActors * 100).toInt)
}
