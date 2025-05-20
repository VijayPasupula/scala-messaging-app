# Scala Messaging App

A simple messaging web application built with Scala 3, featuring a backend in HTTP4s + FS2 and a frontend using Scala.js and Laminar. The app supports registration, login, user listing, and real-time chat between users via WebSockets.

---

## Features

- **User Registration and Login** (with in-memory session management)
- **View Other Users** (with message counts for chats)
- **Private Messaging** (real-time chat via WebSockets)
- **Frontend in Scala.js** using Laminar for a reactive UI
- **Backend in HTTP4s** (Ember/Blaze) with FS2 for concurrency

---

## Project Structure

```
.
├── build.sbt
├── backend/
│   ├── Main.scala
│   ├── ChatStore.scala
│   ├── Routes.scala
│   ├── SessionStore.scala
│   ├── UserRegistry.scala
│   └── WebSocketPrivateChat.scala
├── frontend/
│   ├── Main.scala
│   └── ChatMessage.scala
└── frontend/src/main/resources/
    └── index.html
```

---

## Getting Started

### Prerequisites

- **JDK 17+**
- **SBT** (Scala Build Tool)

---

### Build & Run

1. **Clone the repository:**
    ```sh
    git clone <your-repo-url>
    cd <repo-name>
    ```

2. **Start the backend server (which also serves the frontend):**
    ```sh
    sbt run
    ```
    The backend will automatically build the Scala.js frontend as part of its run command.

3. **Open the app in your browser:**

    Visit: [http://localhost:8080](http://localhost:8080)

---

## Usage

1. **Register or Login:**  
   On first visit, enter a username and password to register. If the username exists, the app will try to log you in.

2. **User List:**  
   After login, you'll see a list of other users (excluding yourself) and the number of messages exchanged.

3. **Start a Chat:**  
   Click "Chat" next to a username to start a private chat. Messages are sent in real time using WebSockets.

4. **Logout:**  
   Click the "Logout" button to clear your session.

---

## Implementation Overview

### Backend

- **HTTP4s** serves API endpoints for registration, login, and listing users.
- **WebSocket** endpoint for real-time private chats.
- **SessionStore** and **UserRegistry** use in-memory `TrieMap`s for simple session and user management.
- **ChatStore** holds chat history per user pair.

### Frontend

- **Scala.js + Laminar** for reactive UI.
- **Circe** for JSON encoding/decoding.
- Handles registration, login, user list display, chat UI, and WebSocket connection.

---

## Configuration

- All dependencies are managed in `build.sbt`.
- Default HTTP server runs on **localhost:8080**.
- No persistent storage: all data (users, sessions, messages) are in-memory and reset on server restart.

---

## Main Dependencies

- **Backend:**  
  - org.http4s (Ember/Blaze)
  - cats-effect, fs2, circe, slf4j-simple

- **Frontend:**  
  - Laminar, Scala.js-dom, circe

(See `build.sbt` for versions)

---

## Sample Code Snippets

### Starting the Server

```scala
// backend/Main.scala
object Main extends IOApp {
  override def run(args: List[String]): IO[ExitCode] = {
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
```

### Sending and Receiving Messages

```scala
// frontend/Main.scala (inside openWebSocket)
val ws = new dom.WebSocket(wsUrl)
ws.onmessage = { (evt: dom.MessageEvent) =>
  decode[ChatMessage](evt.data.toString) match {
    case Right(msg) =>
      chatMessages.update(_ :+ msg)
    case Left(_) =>
  }
}
```

---

## Notes

- **Development Only:** This app has no persistent storage, no password hashing, and is not production-ready.
- **Sessions:** Managed via cookies, stored in-memory.
- **WebSocket:** Each chat between two users has a dedicated topic.

---

## License

MIT (or specify your license here)

---

## Credits

- [Laminar](https://github.com/raquo/Laminar)
- [HTTP4s](https://http4s.org/)
- [Scala.js](https://www.scala-js.org/)
- [Circe](https://circe.github.io/circe/)

---

## Questions?

Open an issue or discussion in this repository!
