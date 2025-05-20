package com.mydomain.messaging.backend

import cats.effect._
import org.http4s._
import org.http4s.dsl.io._
import org.http4s.circe._
import io.circe.generic.auto._
import io.circe.syntax._

object Routes {

  case class RegisterRequest(username: String, password: String)
  case class RegisterResponse(success: Boolean, message: String)
  case class UsersListResponse(users: List[UserInfo])
  case class UserInfo(username: String, messageCount: Int)
  case class AuthRequest(username: String, password: String)
  case class AuthResponse(success: Boolean, message: String)

  import org.http4s.circe.CirceEntityCodec._
  import org.http4s.dsl.io.QueryParamDecoderMatcher

  implicit val registerRequestDecoder: EntityDecoder[IO, RegisterRequest] = jsonOf[IO, RegisterRequest]
  implicit val authRequestDecoder: EntityDecoder[IO, AuthRequest] = jsonOf[IO, AuthRequest]

  object MeParam extends QueryParamDecoderMatcher[String]("me")

  // Helper: Extract session from cookie
  private def getSessionUser(req: Request[IO]): Option[String] =
    req.cookies.find(_.name == "SESSION").flatMap(c => SessionStore.getUsername(c.content))

  def routes: HttpRoutes[IO] = HttpRoutes.of[IO] {
    // Register user
    case req @ POST -> Root / "register" =>
      for {
        registerReq <- req.as[RegisterRequest]
        result = UserRegistry.register(registerReq.username, registerReq.password)
        resp <- result match {
          case Right(_) =>
            val sessionToken = SessionStore.createSession(registerReq.username)
            Ok(RegisterResponse(true, "Registration successful").asJson)
              .map(_.addCookie(ResponseCookie("SESSION", sessionToken, path = Some("/"))))
          case Left(msg) =>
            Ok(RegisterResponse(false, msg).asJson)
        }
      } yield resp

    // Authenticate user (login)
    case req @ POST -> Root / "login" =>
      for {
        loginReq <- req.as[AuthRequest]
        valid = UserRegistry.authenticate(loginReq.username, loginReq.password)
        resp <- if (valid) {
          val sessionToken = SessionStore.createSession(loginReq.username)
          Ok(AuthResponse(true, "Login successful").asJson)
            .map(_.addCookie(ResponseCookie("SESSION", sessionToken, path = Some("/"))))
        } else {
          Ok(AuthResponse(false, "Invalid credentials").asJson)
        }
      } yield resp

    // List users and message counts with requester (session-based)
    case req @ GET -> Root / "users" =>
      getSessionUser(req) match {
        case Some(me) =>
          val allUsers = UserRegistry.allUsers.filterNot(_ == me)
          val userInfos = allUsers.map { u =>
            UserInfo(u, ChatStore.getMessageCount(me, u))
          }
          Ok(UsersListResponse(userInfos).asJson)
        case None =>
          Forbidden("No valid session")
      }
  }
}