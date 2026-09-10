package com.netherite_systems.catsactors.monitor.features.dashboard

import cats.effect.IO
import com.netherite_systems.catsactors.monitor.config.MonitorConfig
import com.netherite_systems.catsactors.monitor.features.dashboard.components.{ActorTreeWidget, SummaryWidget}
import com.netherite_systems.catsactors.monitor.features.dashboard.page.DashboardPage
import com.netherite_systems.catsactors.monitor.shared.services.ActorTreeCollector
import com.suprnation.actor.ActorSystem
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`

object DashboardRoutes {

  private val summaryComponent = SummaryWidget.build.withPath("api/summary")
  private val treeComponent    = ActorTreeWidget.build.withPath("api/tree")

  def routes(
    system: ActorSystem[IO],
    config: MonitorConfig = MonitorConfig()
  ): HttpRoutes[IO] = {
    val pageTag = DashboardPage.build(
      summaryComponent.endpointPath,
      treeComponent.endpointPath,
      "api/status",
      config.pollingIntervalSeconds
    )
    buildRoutes(system, pageTag)
  }

  private def buildRoutes(
    system: ActorSystem[IO],
    pageTag: scalatags.Text.all.Tag
  ): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    HttpRoutes.of[IO] {
      case GET -> Root =>
        Ok(pageTag.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))

      case GET -> Root / "api" / "summary" =>
        for {
          snapshot <- ActorTreeCollector.collect(system)
          html     <- summaryComponent.evaluate(snapshot)
          resp     <- Ok(html.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
        } yield resp

      case GET -> Root / "api" / "tree" =>
        for {
          snapshot <- ActorTreeCollector.collect(system)
          html     <- treeComponent.evaluate(snapshot)
          resp     <- Ok(html.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
        } yield resp

      case GET -> Root / "api" / "status" =>
        for {
          snapshot <- ActorTreeCollector.collect(system)
          html = ActorTreeWidget.renderStatusBadges(snapshot)
          resp <- Ok(html.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
        } yield resp
    }
  }
}
