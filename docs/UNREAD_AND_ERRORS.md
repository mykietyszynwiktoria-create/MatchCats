# Unread messages and recoverable interface errors

The integrated interface shows a private unread-message badge next to Conversations and unread counts for conversations on the current list page. Counts are stored/derived on the server and survive reloads, logout and device changes. They exclude messages authored by the requester.

## Read state

`V12__message_read_receipts.sql` adds a receipt per message and reader, with foreign-key cascades and a unique composite primary key. Earlier received messages have no receipts and initially count as unread. This is a UI read-state record, not proof that somebody physically read every line.

- `GET /chats/unread` returns the authenticated account's `unreadMessages` and `unreadConversations`.
- Conversation responses include `unreadMessages` for the requester.
- `POST /chats/{id}/read` accepts `{ "messageIds": [123, 124] }` and returns 204. Authentication and CSRF are required.

The server checks conversation membership and requires all submitted IDs to be received messages from that conversation. A malformed/mixed list is rejected before insertion; duplicate IDs and repeat requests do not duplicate receipts. Lists accept 1–100 positive non-null IDs. A GET of message history does not modify read state. Only explicitly rendered received-message IDs on the opened history page are acknowledged; later arrivals and unopened pages remain unread. The sender is not given the other person's detailed read receipts. Account/conversation/message deletion cascades remove associated read state.

The frontend refreshes counts after navigation/read acknowledgement and every 30 seconds while signed in and visible. It also refreshes on visibility/connection recovery. Requests do not overlap; a queued refresh reconciles changes while an earlier summary is in flight. Responses are checked against the current account before updating badges. Counts may stay stale during a network failure. The conversation's content still requires manual refresh, preserving text being entered. This is not an email or OS/mobile push service.

## Error screens and cat artwork

`frontend/error-pages.js` renders bilingual interface states for missing pages/resources (404), forbidden views (403), server failures (5xx), and connection failures. Missing/forbidden views feature a sleeping cat; connection/server failures feature a playful cat with a ball. Return-home and retry controls provide recovery. Form-validation and credential errors remain inline so users retain input. Raw server exception details are not rendered.

The `/#404` route previews the missing-page screen without authentication. Unknown authenticated hash routes and failed resource loads use the corresponding screen. These are client application views; they do not change all API responses or reverse-proxy/public-hosting error pages into branded HTML. API status codes/JSON remain intact.

The header's previous cat outline is replaced with a friendly blue cat-face SVG. Both the logo and error artwork are native vector shapes with no external image requests. Error illustration labels use the selected PL/EN language; the redundant logo illustration is hidden from assistive technology. Layout stacks on narrow screens. Artwork is static and introduces no animation requirement.

## Validation

The backend suite passes 40 tests, including unread privacy, explicit page/read state, idempotence, later arrivals, membership/CSRF/input validation and deletion cascades. The existing live browser test now checks an unread badge across reload and clears it after opening received messages. It also checks unknown routes, real missing profiles, simulated 503/network failures and successful retry. The 404 view is tested in PL/EN at 320, 390, 768 and 1440 pixels; existing authenticated/auth-form layout and demo regressions remain. Browser checks do not establish physical native-device compatibility or production provider delivery.
