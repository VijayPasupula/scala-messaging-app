package com.mydomain.messaging.backend

import scala.collection.concurrent.TrieMap

object ChatStore {
  // Key is (sorted user pair), value is Vector[ChatMessage]
  private val store = TrieMap.empty[(String, String), Vector[ChatMessage]]

  // Always store with sorted key so (alice, bob) == (bob, alice)
  def pairKey(user1: String, user2: String): (String, String) =
    if (user1 <= user2) (user1, user2) else (user2, user1)

  def addMessage(sender: String, receiver: String, content: String): Unit = {
    val msg = ChatMessage(sender, content)
    val key = pairKey(sender, receiver)
    store.updateWith(key) {
      case Some(msgs) => Some(msgs :+ msg)
      case None       => Some(Vector(msg))
    }
  }

  def getMessages(user1: String, user2: String): Vector[ChatMessage] = {
    store.getOrElse(pairKey(user1, user2), Vector.empty)
  }

  def getMessageCount(user1: String, user2: String): Int =
    getMessages(user1, user2).size
}