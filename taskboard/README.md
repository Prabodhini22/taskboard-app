# Taskboard — Backend (Spring Boot)

**Live API:** 
https://taskboard-app-ay48.onrender.com 
**Frontend:** 
https://taskboard-frontend-iota.vercel.app 
Note: this free-tier instance spins down after inactivity — the first request after idle time may take 30-50s to respond.


A mini-Trello style task board backend: boards → lists → cards, with JWT auth
and real-time updates over WebSocket (STOMP).

## Stack
- Java 17, Spring Boot 3.3
- Spring Security + JWT (jjwt)
- Spring Data JPA (H2 for local dev, Postgres for prod via `prod` profile)
- Spring WebSocket (STOMP over SockJS) for live board updates

## Run locally
```bash
./mvnw spring-boot:run
```
Runs on `http://localhost:8080` with an in-memory H2 database (profile `dev`,
active by default). H2 console at `/h2-console` if enabled.

## Auth
- `POST /api/auth/signup` — { name, email, password } → returns JWT
- `POST /api/auth/login` — { email, password } → returns JWT

Send the JWT as `Authorization: Bearer <token>` on all other requests.

## Boards / Lists / Cards
- `POST /api/boards` — create a board
- `GET /api/boards` — list your boards
- `GET /api/boards/{boardId}` — board detail (lists + cards)
- `POST /api/boards/{boardId}/lists` — create a list
- `POST /api/boards/{boardId}/lists/{listId}/cards` — create a card
- `PATCH /api/boards/{boardId}/cards/{cardId}` — update/move a card
- `DELETE /api/boards/{boardId}/cards/{cardId}` — delete a card

## Live updates
Connect to `ws://localhost:8080/ws` (SockJS) and subscribe to
`/topic/board/{boardId}`. Every card create/update/move/delete on that board
broadcasts a `BoardEvent { type, payload }` to all subscribers — this is what
gives other users' browsers the "live" update without polling.

Event types: `LIST_CREATED`, `CARD_CREATED`, `CARD_UPDATED`, `CARD_MOVED`, `CARD_DELETED`.

## Deploying (prod profile)
Set `SPRING_PROFILES_ACTIVE=prod` and provide:
- `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD` — your managed Postgres
- `JWT_SECRET` — a long random string
- `PORT` — usually set automatically by Render/Railway

## What's still needed (frontend + polish)
- [ ] React frontend (auth pages, board view, WebSocket subscription)
- [ ] Deploy backend (Render/Railway) + frontend (Vercel)
- [ ] Tighten CORS / WebSocket allowed origins to the real frontend URL
- [ ] Optional: drag-and-drop reordering (@dnd-kit) that also emits WebSocket events
- [ ] Tests for auth + card CRUD (mirror what you did on the wallet-transfer project)
