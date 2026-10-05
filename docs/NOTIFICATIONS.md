# Notifications

MatchCats now shows private in-app notifications for account events.

## Current behavior

- A new chat message creates a notification for the other breeder.
- The authenticated user can load up to 100 notifications with `GET /notifications?limit=50`.
- Unread totals are available with `GET /notifications/unread-count`.
- One action can mark all current-account notifications read with `POST /notifications/read-all`.
- A notification can be marked read with `POST /notifications/{id}/read`.
- The frontend exposes a bilingual Notifications screen and keeps unread cards visually highlighted.
- Notifications are scoped to the current account; one breeder cannot read another breeder's notifications.

## Privacy and future work

Notifications currently contain a generic message notice. A later iteration can add links to the related conversation, notification preferences, and retention rules after the product requirements are agreed.

The unread-count query is supported by the `V19__notification_unread_index.sql` database index. Flyway applies it during the normal application startup migration.
