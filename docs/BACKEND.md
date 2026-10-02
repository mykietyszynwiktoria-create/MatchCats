# Backend implementation progress

## Accounts and configuration

The original source compiled, but its only context test failed because CatRepository opened a hard-coded database connection during construction. The new backend uses Spring-managed repositories and environment-based database configuration. Legacy implementation packages are excluded from application scanning while their replacements are introduced and tested.

The backend now targets Java 21, Spring Boot 3.5.16 and Gradle 8.14.3. Database changes are versioned with Flyway. A fresh database is required; do not run these migrations over an existing legacy database without an explicit data migration plan. Legacy records have not been copied or deleted.

Account APIs use validated registration data, BCrypt password hashing, session authentication, CSRF protection and controlled JSON responses. Password hashes are excluded from response objects and logging. Authentication does not use a password stored in browser localStorage.

Clients first call GET /auth/csrf and send its token in the returned header for state-changing requests, including registration and login. Fetch a fresh CSRF token after login/logout. Keep the session cookie; send credentials with browser requests. Use HTTPS and COOKIE_SECURE=true when deploying.

This file is extended after each checked backend module. The prototype frontend is still separate until API integration is implemented.
