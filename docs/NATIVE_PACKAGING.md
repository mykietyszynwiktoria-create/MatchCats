# Native store packaging roadmap

MatchCats currently ships a responsive web application with a manifest and service worker. It can be installed from a supported browser on Windows, Android and iOS. This installable web shell is useful for testing the product before store accounts and signing certificates are purchased.

## Recommended packaging path

Use Capacitor as a thin native wrapper around the built MatchCats web interface. Keep the Java/PostgreSQL server as the backend and configure the production API base URL for the mobile builds. Do not put database credentials or SMTP secrets in a mobile package.

The native projects still need to be generated and tested:

- Windows: choose a signed desktop package (for example MSIX) and test installation, updates and deep links.
- Android: generate the Android project, set the application id, create a release keystore, test on a physical phone and prepare the Play Console listing.
- iOS: generate the iOS project on macOS, configure the bundle id and signing team, test on a physical iPhone and prepare App Store Connect metadata.

## Release blockers

Native packaging is not complete until a production HTTPS API, privacy/support pages, store developer accounts, signing credentials and device tests exist. Store submissions are intentionally outside automated CI. The current CI validates the backend and browser interface only; it does not sign or publish native packages.

## Safe order

1. Finish browser acceptance testing with sample breeders.
2. Deploy a protected staging API and verify login, files and messages over HTTPS.
3. Generate the three native projects from the same frontend build.
4. Run device accessibility, network-loss, update and account-deletion tests.
5. Create signed release artifacts only after the staging tests pass.
