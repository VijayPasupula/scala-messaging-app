package com.mydomain.messaging.backend

import cats.effect.IO
import org.http4s.server.websocket.WebSocketBuilder2
import org.http4s.HttpRoutes
import org.http4s.dsl.io._
import org.http4s.websocket.WebSocketFrame
import org.http4s.{Request, Response, Status}
import io.circe.parser.decode
import io.circe.syntax._
import fs2.concurrent.Topic
import fs2.Pipe
import fs2.Stream
import cats.effect.unsafe.implicits.global

object WebSocketPrivateChat {

  import org.http4s.dsl.io.QueryParamDecoderMatcher

  object WithParam extends QueryParamDecoderMatcher[String]("with")

  // Each private chat between 2 users has its own Topic
  private val topics = scala.collection.concurrent.TrieMap.empty[(String, String), Topic[IO, ChatMessage]]

  private def getOrCreateTopic(userA: String, userB: String): IO[Topic[IO, ChatMessage]] =
    IO {
      val key = ChatStore.pairKey(userA, userB)
      topics.getOrElseUpdate(key, Topic[IO, ChatMessage].unsafeRunSync())
    }

  // Helper: Extract session from cookie
  private def getSessionUser(req: Request[IO]): Option[String] =
    req.cookies.find(_.name == "SESSION").flatMap(c => SessionStore.getUsername(c.content))

  def routes(wsb: WebSocketBuilder2[IO]): HttpRoutes[IO] = HttpRoutes.of[IO] {
    case req @ GET -> Root / "private" :? WithParam(withUser) =>
      getSessionUser(req) match {
        case Some(me) =>
          for {
            topic <- getOrCreateTopic(me, withUser)
            resp <- {
              // 1. Stream previous chat history
              val history = Stream.emits(ChatStore.getMessages(me, withUser))
                .map(m => WebSocketFrame.Text(m.asJson.noSpaces))
              // 2. Stream new messages as they arrive
              val live = topic.subscribe(1000)
                .filter(m => m.sender == me || m.sender == withUser)
                .map(m => WebSocketFrame.Text(m.asJson.noSpaces))
              val send = history ++ live
              // 3. Pipe to receive and broadcast messages
              val receive: Pipe[IO, WebSocketFrame, Unit] = _.collect {
                case WebSocketFrame.Text(text, _) => text
              }.evalMap { text =>
                decode[ChatMessage](text) match {
                  case Right(msg) =>
                    val realMsg = msg.copy(sender = me)
                    IO(ChatStore.addMessage(me, withUser, realMsg.content)) *>
                      topic.publish1(realMsg).void
                  case Left(_) => IO.unit
                }
              }
              wsb.build(send, receive)
            }
          } yield resp
        case None =>
          IO.pure(Response[IO](status = Status.Forbidden))
      }
}
}