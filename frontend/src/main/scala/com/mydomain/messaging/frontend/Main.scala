package com.mydomain.messaging.frontend

import com.raquo.laminar.api.L._
import org.scalajs.dom
import io.circe.generic.auto._
import io.circe.syntax._
import io.circe.parser.decode
import com.mydomain.messaging.frontend.ChatMessage // <-- Import the correct definition

sealed trait Page
object Page {
  case object Registration extends Page
  case object UserList extends Page
  case class Chat(withUser: String) extends Page
}

// Data models (must match backend)
case class RegisterRequest(username: String, password: String)
case class RegisterResponse(success: Boolean, message: String)
case class AuthRequest(username: String, password: String)
case class AuthResponse(success: Boolean, message: String)
case class UserInfo(username: String, messageCount: Int)
case class UsersListResponse(users: List[UserInfo])

object Main {
  def main(args: Array[String]): Unit = {
    val currentPage = Var[Page](Page.Registration)
    val errorMsg = Var(Option.empty[String])

    // Used in registration/login
    val regUsername = Var("")
    val regPassword = Var("")

    // Used in chat
    val chatInput = Var("")
    val chatMessages = Var(Vector.empty[ChatMessage])
    var wsOpt: Option[org.scalajs.dom.WebSocket] = None

    // ========== Registration Page ==========
    def registrationPage: HtmlElement = {
      div(
        h2("Register or Login"),
        child <-- errorMsg.signal.map(_.map(msg => div(color.red, msg)).getOrElse(emptyNode)),
        form(
          onSubmit.preventDefault.mapTo(()) --> { _ =>
            val username = regUsername.now().trim
            val password = regPassword.now().trim
            if (username.isEmpty || password.isEmpty) {
              errorMsg.set(Some("Please enter both username and password"))
            } else {
              // Try registration first
              val regReq = RegisterRequest(username, password)
              val xhr = new dom.XMLHttpRequest()
              xhr.open("POST", "/api/register")
              xhr.setRequestHeader("Content-Type", "application/json")
              xhr.onload = { (_: dom.Event) =>
                decode[RegisterResponse](xhr.responseText) match {
                  case Right(resp) =>
                    if (resp.success) {
                      regPassword.set("")
                      errorMsg.set(None)
                      currentPage.set(Page.UserList)
                    } else if (resp.message.contains("already exists")) {
                      // Try login if already registered
                      val loginReq = AuthRequest(username, password)
                      val xhr2 = new dom.XMLHttpRequest()
                      xhr2.open("POST", "/api/login")
                      xhr2.setRequestHeader("Content-Type", "application/json")
                      xhr2.onload = { (_: dom.Event) =>
                        decode[AuthResponse](xhr2.responseText) match {
                          case Right(loginResp) =>
                            if (loginResp.success) {
                              regPassword.set("")
                              errorMsg.set(None)
                              currentPage.set(Page.UserList)
                            } else {
                              errorMsg.set(Some(loginResp.message))
                            }
                          case Left(err) =>
                            errorMsg.set(Some(s"Login error: ${err.getMessage}"))
                        }
                      }
                      xhr2.send(loginReq.asJson.noSpaces)
                    } else {
                      errorMsg.set(Some(resp.message))
                    }
                  case Left(err) =>
                    errorMsg.set(Some(s"Error: ${err.getMessage}"))
                }
              }
              xhr.send(regReq.asJson.noSpaces)
            }
          },
          div(
            label("Username: "),
            input(
              typ := "text",
              controlled(
                value <-- regUsername.signal,
                onInput.mapToValue --> regUsername
              )
            )
          ),
          div(
            label("Password: "),
            input(
              typ := "password",
              controlled(
                value <-- regPassword.signal,
                onInput.mapToValue --> regPassword
              )
            )
          ),
          button("Register / Login", typ := "submit")
        )
      )
    }

    // ========== User List Page ==========
    val usersList = Var(List.empty[UserInfo])
    val isLoadingUsers = Var(false)

    def fetchUsersList(): Unit = {
      isLoadingUsers.set(true)
      val xhr = new dom.XMLHttpRequest()
      xhr.open("GET", "/api/users")
      xhr.onload = { (_: dom.Event) =>
        isLoadingUsers.set(false)
        decode[UsersListResponse](xhr.responseText) match {
          case Right(resp) =>
            usersList.set(resp.users)
            errorMsg.set(None)
          case Left(err) =>
            errorMsg.set(Some(s"Failed to load user list: ${err.getMessage}"))
            if (xhr.status == 403 || xhr.status == 401) {
              currentPage.set(Page.Registration)
            }
        }
      }
      xhr.onerror = { (_: dom.Event) =>
        isLoadingUsers.set(false)
        errorMsg.set(Some("Failed to connect to server"))
      }
      xhr.send()
    }

    def userListPage: HtmlElement =
      div(
        h2("User List"),
        button(
          "Refresh",
          onClick --> { _ => fetchUsersList() }
        ),
        button(
          "Logout",
          onClick --> { _ =>
            // Clear cookie and reset state
            dom.document.cookie = "SESSION=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;"
            currentPage.set(Page.Registration)
            usersList.set(Nil)
          }
        ),
        child <-- errorMsg.signal.map(_.map(msg => div(color.red, msg)).getOrElse(emptyNode)),
        child <-- isLoadingUsers.signal.map {
          case true  => div("Loading users...")
          case false => emptyNode
        },
        ul(
          children <-- usersList.signal.map { users =>
            users.map { u =>
              li(
                span(b(u.username)),
                span(s" (${u.messageCount} messages) "),
                button(
                  "Chat",
                  onClick --> { _ =>
                    chatMessages.set(Vector.empty)
                    currentPage.set(Page.Chat(u.username))
                  }
                )
              )
            }
          }
        )
      )

    // ========== Chat Page ==========
    def openWebSocket(other: String): Unit = {
      // Close any old WS
      wsOpt.foreach(_.close())
      chatMessages.set(Vector.empty)
      val wsUrl =
        (if (dom.window.location.protocol == "https:") "wss://" else "ws://") +
          dom.window.location.host + s"/ws/private?with=$other"
      val ws = new dom.WebSocket(wsUrl)
      wsOpt = Some(ws)
      ws.onmessage = { (evt: dom.MessageEvent) =>
        decode[ChatMessage](evt.data.toString) match {
          case Right(msg) =>
            chatMessages.update(_ :+ msg)
          case Left(_) =>
        }
      }
      ws.onerror = { _ =>
        errorMsg.set(Some("WebSocket connection error."))
      }
      ws.onclose = { _ =>
        errorMsg.set(Some("WebSocket closed."))
      }
    }

    def chatPage(withUser: String): HtmlElement =
      div(
        h2(s"Chat with $withUser"),
        button("Back to users", onClick --> { _ =>
          wsOpt.foreach(_.close())
          wsOpt = None
          chatMessages.set(Vector.empty)
          fetchUsersList()
          currentPage.set(Page.UserList)
        }),
        child <-- errorMsg.signal.map(_.map(msg => div(color.red, msg)).getOrElse(emptyNode)),
        div(
          idAttr := "message-box",
          height := "200px",
          overflowY.auto,
          border := "1px solid #ccc",
          padding := "5px",
          marginBottom := "10px",
          children <-- chatMessages.signal.map(_.map { msg =>
            div(
              b(msg.sender + ": "),
              msg.content
            )
          })
        ),
        form(
          onSubmit.preventDefault.mapTo(()) --> { _ =>
            for {
              ws <- wsOpt
            } {
              val input = chatInput.now().trim
              if (input.nonEmpty) {
                // sender field will be filled by backend using session
                val msg = ChatMessage("", input)
                ws.send(msg.asJson.noSpaces)
                chatInput.set("")
              }
            }
          },
          input(
            typ := "text",
            placeholder := "Type a message...",
            width := "70%",
            controlled(
              value <-- chatInput.signal,
              onInput.mapToValue --> chatInput
            )
          ),
          button("Send", typ := "submit")
        )
      )

    // ========== Routing ==========
    val app: HtmlElement = div(
      child <-- currentPage.signal.map { // <-- changed from flatMap to map
        case Page.Registration => registrationPage
        case Page.UserList =>
          fetchUsersList()
          userListPage
        case Page.Chat(u) =>
          openWebSocket(u)
          chatPage(u)
      }
    )

    render(dom.document.getElementById("app"), app)
  }
}
