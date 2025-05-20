package com.mydomain.messaging.backend

import scala.collection.concurrent.TrieMap

object UserRegistry {
  private val users = TrieMap.empty[String, String] // username -> password

  def register(username: String, password: String): Either[String, Unit] =
    if (username.isEmpty || password.isEmpty) Left("Username and password required")
    else if (users.contains(username)) Left("Username already exists")
    else {
      users.put(username, password)
      Right(())
    }

  def userExists(username: String): Boolean = users.contains(username)

  def authenticate(username: String, password: String): Boolean =
    users.get(username).contains(password)

  def allUsers: List[String] = users.keys.toList.sorted
}