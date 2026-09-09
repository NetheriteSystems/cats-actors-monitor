package com.netherite_systems.catsactors.monitor

import cats.effect.{IO, Resource}
import cats.syntax.functor.*
import com.comcast.ip4s.*
import com.suprnation.actor.ActorSystem
import com.netherite_systems.catsactors.monitor.dashboard.{DashboardPage, SummaryComponent, TreeComponent}
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.headers.`Content-Type`
import org.http4s.implicits.*

object MonitorRoutes {

  private val summaryComponent = SummaryComponent.build
  private val treeComponent    = TreeComponent.build

  def resource(
    system: ActorSystem[IO],
    config: MonitorConfig = MonitorConfig()
  ): Resource[IO, Unit] =
    for {
      pageTag = DashboardPage.build(summaryComponent.endpointPath, treeComponent.endpointPath, config.pollingIntervalSeconds)
      routes  = buildRoutes(system, pageTag)
      server <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(config.host).get)
        .withPort(Port.fromInt(config.port).get)
        .withHttpApp(routes.orNotFound)
        .build
        .void
    } yield server

  private def buildRoutes(
    system: ActorSystem[IO],
    pageTag: scalatags.Text.all.Tag
  ): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    HttpRoutes.of[IO] {
      case GET -> Root =>
        Ok(pageTag.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))

      case GET -> Root / path if path == summaryComponent.endpointPath =>
        for {
          snapshot <- ActorTreeCollector.collect(system)
          html     <- summaryComponent.function(snapshot)
          resp     <- Ok(html.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
        } yield resp

      case GET -> Root / path if path == treeComponent.endpointPath =>
        for {
          snapshot <- ActorTreeCollector.collect(system)
          html     <- treeComponent.function(snapshot)
          resp     <- Ok(html.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
        } yield resp
    }
  }
}