package com.netherite_systems.catsactors.monitor.shared.model

import io.circe.generic.semiauto.*
import io.circe.{Decoder, Encoder}
import sttp.tapir.Schema

case class ActorSnapshot(
  name: String,
  path: String,
  parentPath: Option[String],
  childCount: Int,
  mailboxSize: Int,
  isIdle: Boolean,
  isTerminated: Boolean
)

object ActorSnapshot {
  given Encoder[ActorSnapshot] = deriveEncoder
  given Decoder[ActorSnapshot] = deriveDecoder
  given Schema[ActorSnapshot]  = Schema.derived
}

case class ActorTreeSnapshot(
  systemName: String,
  uptimeSeconds: Long,
  totalActors: Int,
  idleCount: Int,
  busyCount: Int,
  terminatedCount: Int,
  actors: List[ActorSnapshot]
)

object ActorTreeSnapshot {
  given Encoder[ActorTreeSnapshot] = deriveEncoder
  given Decoder[ActorTreeSnapshot] = deriveDecoder
  given Schema[ActorTreeSnapshot]  = Schema.derived
}

case class HealthResponse(healthy: Boolean, uptimeSeconds: Long)

object HealthResponse {
  given Encoder[HealthResponse] = deriveEncoder
  given Decoder[HealthResponse] = deriveDecoder
  given Schema[HealthResponse]  = Schema.derived
}
