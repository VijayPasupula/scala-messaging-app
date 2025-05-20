package com.mydomain.messaging.backend

import cats.effect._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.http4s.implicits._
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.StaticFile
import fs2.io.file.{Path, Files}
import org.http4s.headers.`Content-Type`
import org.http4s.MediaType
import java.nio.file.Paths

object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
    implicit val files: Files[IO] = Files.forIO

    val staticResources = HttpRoutes.of[IO] {
      case req @ GET -> Root / path if path.nonEmpty =>
        StaticFile.fromPath(Path("frontend/src/main/resources/" + path), Some(req))
          .map(_.putHeaders(`Content-Type`(MediaType.text.html)))
          .getOrElseF(NotFound())
      case GET -> Root =>
        val filePath = Path(Paths.get("frontend", "src", "main", "resources", "index.html").toString)
        StaticFile.fromPath(filePath, None)
          .map(_.putHeaders(`Content-Type`(MediaType.text.html)))
          .getOrElseF(NotFound())
    }

    val staticJS = HttpRoutes.of[IO] {
      case GET -> Root / "messaging-frontend-fastopt.js" =>
        val filePath = Path(Paths.get("frontend", "target", "scala-3.4.3", "messaging-frontend-fastopt.js").toString)
        StaticFile.fromPath(filePath, None)
          .map(_.putHeaders(`Content-Type`(MediaType.application.javascript)))
          .getOrElseF(NotFound())
    }

    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withHttpWebSocketApp { wsBuilder =>
        Router(
          "/"   -> staticResources,
          "/js" -> staticJS,
          "/api" -> Routes.routes,
          "/ws" -> WebSocketPrivateChat.routes(wsBuilder)
        ).orNotFound
      }
      .serve
      .compile
      .drain
      .as(ExitCode.Success)
  }
}
