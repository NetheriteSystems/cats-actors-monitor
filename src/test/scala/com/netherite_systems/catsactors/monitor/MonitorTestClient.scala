package com.netherite_systems.catsactors.monitor

import cats.effect.IO
import com.comcast.ip4s.*
import com.netherite_systems.catsactors.monitor.config.MonitorConfig
import com.suprnation.actor.ActorSystem
import org.http4s.*
import org.http4s.client.Client
import org.http4s.ember.client.EmberClientBuilder
import org.http4s.ember.server.EmberServerBuilder
import org.http4s.implicits.*
import weaver.Expectations

final class MonitorTestClient(client: Client[IO], baseUri: Uri) {

  def getDashboard(): IO[String] =
    client.expect[String](Request[IO](Method.GET, baseUri))

  def getSummary(): IO[String] =
    client.expect[String](Request[IO](Method.GET, baseUri / "api" / "summary"))

  def getTree(): IO[String] =
    client.expect[String](Request[IO](Method.GET, baseUri / "api" / "tree"))

  def getStatus(): IO[String] =
    client.expect[String](Request[IO](Method.GET, baseUri / "api" / "status"))
}

object MonitorTestClient {

  def apply(
    system: ActorSystem[IO],
    config: MonitorConfig = MonitorConfig()
  )(run: MonitorTestClient => IO[Expectations]): IO[Expectations] =
    (for {
      server <- EmberServerBuilder
        .default[IO]
        .withPort(Port.fromInt(0).get)
        .withHttpApp(MonitorRoutes.routes(system, config).orNotFound)
        .build
      client <- EmberClientBuilder.default[IO].build
    } yield (server, client)).use { case (server, httpClient) =>
      val testClient = new MonitorTestClient(
        client = httpClient,
        baseUri = Uri.unsafeFromString(s"http://localhost:${server.address.getPort}")
      )
      run(testClient)
    }
}
