package com.netherite_systems.catsactors.monitor

import cats.effect.IO
import com.netherite_systems.catsactors.monitor.config.MonitorConfig
import com.netherite_systems.catsactors.monitor.features.dashboard.DashboardRoutes
import com.suprnation.actor.ActorSystem
import org.http4s.*

object MonitorRoutes {

  def routes(
    system: ActorSystem[IO],
    config: MonitorConfig = MonitorConfig()
  ): HttpRoutes[IO] =
    DashboardRoutes.routes(system, config)
}
