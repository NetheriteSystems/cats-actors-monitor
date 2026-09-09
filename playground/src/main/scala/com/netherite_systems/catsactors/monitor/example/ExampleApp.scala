package com.netherite_systems.catsactors.monitor.example

import cats.effect.{IO, IOApp, Resource}
import cats.syntax.traverse.*
import com.suprnation.actor.{ActorSystem, ReplyingActor}
import com.suprnation.actor.Actor.Actor
import com.netherite_systems.catsactors.monitor.{MonitorConfig, MonitorRoutes}

object ExampleApp extends IOApp.Simple {

  val routerBehavior: Actor[IO, Any] = new ReplyingActor[IO, Any, Any] {
    override def receive: PartialFunction[Any, IO[Any]] = { case msg =>
      IO(msg)
    }
  }

  val workerBehavior: Actor[IO, Any] = new ReplyingActor[IO, Any, Any] {
    override def receive: PartialFunction[Any, IO[Any]] = { case msg =>
      IO(msg)
    }
  }

  val sinkBehavior: Actor[IO, Any] = new ReplyingActor[IO, Any, Any] {
    override def receive: PartialFunction[Any, IO[Any]] = { case msg =>
      IO(msg)
    }
  }

  override def run: IO[Unit] =
    app.use(_ => IO.unit)

  private def app: Resource[IO, Unit] =
    for {
      system <- ActorSystem[IO]("example-system")
      _      <- Resource.eval(system.replyingActorOf(IO(routerBehavior), "router"))
      _      <- Resource.eval(
        (1 to 3).toList.traverse { i =>
          system.replyingActorOf(IO(workerBehavior), s"worker-$i")
        }
      )
      _ <- Resource.eval(system.replyingActorOf(IO(sinkBehavior), "sink"))
      _ <- Resource.eval(
        (1 to 2).toList.traverse { i =>
          system.replyingActorOf(IO(workerBehavior), s"batch-$i")
        }
      )
      _ <- MonitorRoutes.resource(system, MonitorConfig(port = 9090))
      _ <- Resource.eval(
        IO.println("Monitor running at http://localhost:9090") >>
          IO.println("Press ENTER to stop") >>
          IO.readLine >>
          IO.println("Shutting down...") >>
          system.terminate()
      )
    } yield ()
}
