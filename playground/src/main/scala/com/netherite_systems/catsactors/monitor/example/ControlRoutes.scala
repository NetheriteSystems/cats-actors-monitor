package com.netherite_systems.catsactors.monitor.example

import cats.effect.IO
import org.http4s.*
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.`Content-Type`

object ControlRoutes {

  def routes(actions: Map[String, () => IO[String]]): HttpRoutes[IO] = {
    val dsl = new Http4sDsl[IO] {}
    import dsl.*

    val controlsPage = ControlsPage.build(actions.keys.toList)

    HttpRoutes.of[IO] {
      case GET -> Root / "controls" =>
        Ok(controlsPage.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))

      case POST -> Root / "api" / "action" / name =>
        actions.get(name) match {
          case Some(action) =>
            action().attempt.flatMap {
              case Right(result) =>
                Ok(ControlsPage.renderResult(name, result).render)
                  .map(_.withContentType(`Content-Type`(MediaType.text.html)))
              case Left(err) =>
                Ok(ControlsPage.renderError(name, err.getMessage).render)
                  .map(_.withContentType(`Content-Type`(MediaType.text.html)))
            }
          case None => NotFound()
        }
    }
  }
}
