# Billing and subscription readiness

MatchCats currently exposes a safe subscription foundation without charging users.

## Current API

- `GET /billing/plans` is public and returns the `FREE` and `PREMIUM` plan catalog.
- `GET /billing/me` requires an authenticated session and currently reports the active `FREE` plan.
- `POST /billing/checkout` is intentionally disabled and returns `409 PAYMENT_NOT_CONFIGURED` until a verified payment provider is configured.

The frontend labels Premium as coming soon. No card details are collected and no payment is attempted.

## Activation requirements

Before enabling a paid plan, the project owner must select a provider, define prices and limits, configure store agreements for Android, iOS and Windows, add tax and payout details, and implement signed webhook verification. The backend must grant entitlements only from verified provider events; a client request must never be enough to grant Premium access.

The free plan remains available if billing is unavailable. Existing account, cat, document and conversation data must remain usable during a provider outage.

