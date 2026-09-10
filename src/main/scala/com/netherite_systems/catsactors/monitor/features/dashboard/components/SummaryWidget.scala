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

  private def render(snapshot: ActorTreeSnapshot): Tag =
    div(cls := "stats stats-horizontal shadow-lg w-full")(
      div(cls := "stat")(
        div(cls := "stat-title")("Actors"),
        div(cls := "stat-value text-primary")(snapshot.totalActors.toString),
        div(cls := "stat-desc")(s"in ${snapshot.systemName}")
      ),
      div(cls := "stat")(
        div(cls := "stat-title")("Idle"),
        div(cls := "stat-value text-success")(snapshot.idleCount.toString),
        div(cls := "stat-desc")("waiting for messages")
      ),
      div(cls := "stat")(
        div(cls := "stat-title")("Busy"),
        div(cls := "stat-value text-warning")(snapshot.busyCount.toString),
        div(cls := "stat-desc")("processing")
      ),
      div(cls := "stat")(
        div(cls := "stat-title")("Terminated"),
        div(cls := "stat-value text-error")(snapshot.terminatedCount.toString),
        div(cls := "stat-desc")("stopped")
      ),
      div(cls := "stat")(
        div(cls := "stat-title")("Uptime"),
        div(cls := "stat-value text-info")(formatUptime(snapshot.uptimeSeconds)),
        div(cls := "stat-desc")("polling every 2s")
      )
    )

  private def formatUptime(seconds: Long): String = {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds  % 60
    f"$h%d:$m%02d:$s%02d"
  }
}
