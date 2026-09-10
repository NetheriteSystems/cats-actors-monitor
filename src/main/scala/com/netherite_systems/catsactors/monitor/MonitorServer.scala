package com.netherite_systems.catsactors.monitor

import cats.effect.{IO, Resource}
import cats.syntax.functor.*
import com.comcast.ip4s.*
import com.netherite_systems.catsactors.monitor.config.MonitorConfig
import com.suprnation.actor.ActorSystem
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*

object MonitorServer {

  def resource(
    system: ActorSystem[IO],
    config: MonitorConfig = MonitorConfig()
  ): Resource[IO, Unit] =
    for {
      server <- EmberServerBuilder
        .default[IO]
        .withHost(Host.fromString(config.host).get)
        .withPort(Port.fromInt(config.port).get)
        .withHttpApp(MonitorRoutes.routes(system, config).orNotFound)
        .build
        .void
    } yield server
}
