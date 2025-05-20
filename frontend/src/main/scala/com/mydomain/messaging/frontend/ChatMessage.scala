package com.mydomain.messaging.frontend

import io.circe.generic.semiauto._
import io.circe.{Decoder, Encoder}

case class ChatMessage(sender: String, content: String)
object ChatMessage {
  implicit val encoder: Encoder[ChatMessage] = deriveEncoder[ChatMessage]
  implicit val decoder: Decoder[ChatMessage] = deriveDecoder[ChatMessage]
}