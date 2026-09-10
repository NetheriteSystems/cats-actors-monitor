package com.netherite_systems.catsactors.monitor

import scala.concurrent.duration.*

import cats.effect.IO
import com.suprnation.actor.Actor.Actor
import com.suprnation.actor.ReplyingActor

object ActorFixture {

  def fastBehavior: Actor[IO, Any] = new ReplyingActor[IO, Any, Any] {
    override def receive: PartialFunction[Any, IO[Any]] = { case msg =>
      IO(msg)
    }
  }

  def slowBehavior(ms: Long): Actor[IO, Any] = new ReplyingActor[IO, Any, Any] {
    override def receive: PartialFunction[Any, IO[Any]] = { case msg =>
      IO.sleep(ms.millis) >> IO(msg)
    }
  }
}
