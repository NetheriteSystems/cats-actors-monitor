package com.netherite_systems.catsactors.monitor

import cats.effect.IO
import cats.syntax.traverse.*
import com.suprnation.actor.ActorRef.NoSendActorRef
import com.suprnation.actor.ActorSystem
import com.suprnation.typelevel.actors.syntax.{ActorRefSyntaxOps, ActorSystemDebugOps}

object ActorTreeCollector {

  def collect(system: ActorSystem[IO]): IO[ActorTreeSnapshot] =
    for {
      allRefs   <- system.allChildren
      snapshots <- allRefs.traverse(ref => snapshotOne(ref))
      uptime    <- system.uptime
    } yield ActorTreeSnapshot(
      systemName = system.name,
      uptimeSeconds = uptime,
      totalActors = snapshots.length,
      idleCount = snapshots.count(_.isIdle),
      busyCount = snapshots.count(a => !a.isIdle && !a.isTerminated),
      terminatedCount = snapshots.count(_.isTerminated),
      actors = snapshots
    )

  private def snapshotOne(ref: NoSendActorRef[IO]): IO[ActorSnapshot] =
    for {
      cellOpt  <- ref.cellOp
      children <- cellOpt.traverse(_.childrenRefs.get).map(_.map(_.children.size).getOrElse(0))
      msgs     <- cellOpt.traverse(_.numberOfMessages).map(_.getOrElse(0))
      idle     <- cellOpt.traverse(_.isIdle).map(_.getOrElse(false))
      term     <- cellOpt.traverse(_.isTerminated).map(_.getOrElse(true))
    } yield ActorSnapshot(
      name = ref.path.name,
      path = ref.path.toString,
      parentPath = if (ref.path == ref.path.root) None else Some(ref.path.parent.toString),
      childCount = children,
      mailboxSize = msgs,
      isIdle = idle,
      isTerminated = term
    )
}