# Release readiness checklist

MatchCats is not ready for public store release until the following external items are supplied and tested.

| Area | Required before release | Current status |
| --- | --- | --- |
| Public API | HTTPS host, database, backups and monitoring | Not selected |
| Email | SMTP or transactional email provider for recovery and verification | Not configured |
| Store accounts | Apple Developer, Google Play Console and Microsoft Partner Center | Owner has not created accounts |
| Payments | Merchant account, provider webhooks and tax details if Premium is enabled | Checkout intentionally disabled |
| Mobile packages | Reviewed Capacitor/native wrapper, signing certificates and device builds | Planning only |
| Privacy | Public privacy policy, terms, support contact and data deletion process | Pages exist; public hosting still needed |
| Operations | Logs, alerting, backup restore drill and incident procedure | Development documentation only |

The application can be developed and tested locally without purchasing these services. Do not enable Premium checkout or publish packages until the relevant row has an owner, credentials stored outside Git, and a successful test in a non-production environment.
