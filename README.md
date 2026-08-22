# Multiplayer Trivia Game

A client–server multiplayer trivia game written in Java. A server hosts a quiz
session, multiple clients connect over sockets, and players answer the same
questions concurrently while the server tracks scores.

---

## Features

- Server that accepts and manages multiple simultaneous client connections
- Client application for joining a game and submitting answers
- Question bank loaded from the data layer
- Score tracking across players
- Custom exception handling for connection and game-state errors

---

## Tech stack

- **Java** — core language
- **Java Sockets** — client–server networking
- **Multithreading** — one thread per connected client
- **Object-oriented design** — separated model, data, client and server layers

---

## Project structure

```
├── client/       client application and user interaction
├── server/       server, connection handling and game loop
├── model/        game entities (players, questions, sessions)
├── data/         question bank and data access
└── exceptions/   custom exception types
```

---

## Running the game

**Prerequisites:** JDK 17 or newer.

```bash
git clone https://github.com/janaammrr/MultiplayerTriviaGame.git
cd MultiplayerTriviaGame
```

Compile the sources, then start the server first and connect one or more
clients:

```bash
# terminal 1 — start the server
java server.Server

# terminal 2, 3, ... — start a client per player
java client.Client
```

Class names may differ slightly depending on your package layout — check the
`server/` and `client/` folders for the entry point with a `main` method.

---

## What I built

- Socket-based communication between server and multiple clients
- Concurrent handling of player sessions using threads
- Game state and scoring logic
- Custom exceptions for network and game errors

---

## Author

**Jana Amr** — Software Engineer
[Portfolio](https://jana-portfolio-mauve.vercel.app) ·
[LinkedIn](https://www.linkedin.com/in/jana-amr-1380752a6/)
