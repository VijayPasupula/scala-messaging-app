package com.mydomain.messaging.backend

import java.util.UUID
import scala.collection.concurrent.TrieMap

object SessionStore {
  private val sessions = TrieMap.empty[String, String] // sessionToken -> username

  def createSession(username: String): String = {
    val token = UUID.randomUUID().toString
    sessions.put(token, username)
    token
  }

  def getUsername(token: String): Option[String] = sessions.get(token)

  def invalidate(token: String): Unit = sessions.remove(token)
}