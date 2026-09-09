package com.netherite_systems.catsactors.monitor

case class MonitorConfig(
  host: String = "0.0.0.0",
  port: Int = 8080,
  pollingIntervalSeconds: Int = 2
)