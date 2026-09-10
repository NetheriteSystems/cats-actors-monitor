package com.netherite_systems.catsactors.monitor.example

import cats.effect.{IO, IOApp, Resource}
import cats.syntax.functor.*
import cats.syntax.semigroupk.*
import cats.syntax.traverse.*
import scala.concurrent.duration.*
import com.comcast.ip4s.*
import com.suprnation.actor.{ActorSystem, ReplyingActor}
import com.suprnation.actor.Actor.Actor
import com.netherite_systems.catsactors.monitor.MonitorRoutes
import com.netherite_systems.catsactors.monitor.config.MonitorConfig
import org.http4s.*
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*

object ExampleApp extends IOApp.Simple {

  def slowBehavior(name: String)(delayMs: Long): Actor[IO, Any] =
    new ReplyingActor[IO, Any, Any] {
      override def receive: PartialFunction[Any, IO[Any]] = { case msg =>
        IO.println(s"[$name] processing $msg") >> IO.sleep(delayMs.millis) >> IO(msg)
      }
    }

  val fastBehavior: Actor[IO, Any] = new ReplyingActor[IO, Any, Any] {
    override def receive: PartialFunction[Any, IO[Any]] = { case msg =>
      IO(msg)
    }
  }

  override def run: IO[Unit] =
    app.use(_ => IO.unit)

  private def app: Resource[IO, Unit] =
    for {
      system  <- ActorSystem[IO]("example-system")
      router  <- Resource.eval(system.replyingActorOf(IO(fastBehavior), "router"))
      workers <- Resource.eval(
        (1 to 3).toList.traverse { i =>
          system.replyingActorOf(IO(slowBehavior(s"worker-$i")(3000)), s"worker-$i")
        }
      )
      sink    <- Resource.eval(system.replyingActorOf(IO(slowBehavior("sink")(5000)), "sink"))
      batches <- Resource.eval(
        (1 to 2).toList.traverse { i =>
          system.replyingActorOf(IO(slowBehavior(s"batch-$i")(2000)), s"batch-$i")
        }
      )

      actions = Map(
        "Ping router"   -> (() => (router ! "ping").map(_ => "sent")),
        "Ping worker-1" -> (() => (workers(0) ! "ping").map(_ => "sent")),
        "Ping worker-2" -> (() => (workers(1) ! "ping").map(_ => "sent")),
        "Ping worker-3" -> (() => (workers(2) ! "ping").map(_ => "sent")),
        "Ping sink"     -> (() => (sink ! "ping").map(_ => "sent")),
        "Ping batch-1"  -> (() => (batches(0) ! "ping").map(_ => "sent")),
        "Ping batch-2"  -> (() => (batches(1) ! "ping").map(_ => "sent")),
        "Burst worker-1" -> (() =>
          (1 to 50).toList.traverse(i => (workers(0) ! s"msg-$i").start).map(_ => "fired 50 messages")
        ),
        "Burst all workers" -> (() =>
          (1 to 50).toList.flatTraverse { i =>
            workers.traverse(w => (w ! s"msg-$i").start)
          }.map(_ => "fired 50 to each worker")
        )
      )

      monitorRoutes = MonitorRoutes.routes(system, MonitorConfig())
      controlRoutes = ControlRoutes.routes(actions)
      allRoutes     = monitorRoutes <+> controlRoutes

      _ <- EmberServerBuilder
        .default[IO]
        .withHost(host"0.0.0.0")
        .withPort(port"9090")
        .withHttpApp(allRoutes.orNotFound)
        .build
        .void

      _ <- Resource.eval(
        IO.println("Monitor running at http://localhost:9090") >>
          IO.println("Controls at http://localhost:9090/controls") >>
          IO.println("Press ENTER to stop") >>
          IO.readLine.handleError(_ => "").flatMap {
            case "" => IO.println("No TTY detected, running until SIGTERM...") >> IO.never
            case _  => IO.println("Shutting down...") >> system.terminate()
          }
      )
    } yield ()
}