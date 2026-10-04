# Native wrapper plan

MatchCats currently serves one responsive frontend and API. The next packaging stage can reuse the frontend inside a reviewed Capacitor wrapper after Node.js is available.

## Planned sequence

1. Install a supported Node.js LTS version and run the existing frontend tests.
2. Add Capacitor only after the browser build and API base URL are verified.
3. Create separate development configurations for Windows, Android and iOS.
4. Keep API calls on HTTPS and keep session cookies and CSRF behavior unchanged.
5. Test sign in, recovery, file upload, document review, conversations and notifications on real devices.
6. Add platform icons, splash assets, privacy declarations and accessibility checks.
7. Configure signing certificates and store metadata only when the owner has the required accounts.

## Current blockers

- This environment has no Node.js/npm, so a wrapper cannot be installed or tested yet.
- Apple, Google Play and Microsoft developer accounts are not available.
- A public HTTPS API host and email provider still need to be selected before store release.

No store package or paid service is created by this document.
