# Installable app foundation

MatchCats now includes a small web app manifest and a service worker for the frontend shell.

## What this provides

- Modern browsers can offer an install action on Windows, Android and compatible desktop environments.
- The service worker caches the static interface shell after the first successful load.
- API requests are not cached by the service worker, so private account data still comes from the server.
- The manifest uses the blue MatchCats theme and a standalone display mode.

## Store packaging

This is a preparation step, not a store submission. Windows, Google Play and Apple App Store packages still need a native wrapper (for example, a reviewed Capacitor setup), signing certificates, store developer accounts, privacy details and device testing. Those steps are intentionally not performed automatically because they require the owner's accounts and paid store decisions.
