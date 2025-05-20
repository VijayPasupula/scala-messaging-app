package com.mydomain.messaging.backend

import io.circe.{Encoder, Decoder}
import io.circe.generic.semiauto._

case class ChatMessage(sender: String, content: String)
object ChatMessage {
  implicit val encoder: Encoder[ChatMessage] = deriveEncoder[ChatMessage]
  implicit val decoder: Decoder[ChatMessage] = deriveDecoder[ChatMessage]
}