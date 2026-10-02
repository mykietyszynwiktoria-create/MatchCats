# MatchCats breeder interface prototype

Open frontend/index.html in a modern browser. The interface supports Polish and English via PL / EN. All profiles and messages are demonstration data stored locally in this browser. Messages are not sent to real breeders. There is no server connection, authentication, file upload or installable application yet.

Panels: home, own cats, cat search, details, conversations, documents and settings. The owner wants installed Windows, Android and iOS applications; this interface is a preliminary prototype.

For interface checks: install Node.js, run npm install in frontend, then npx playwright install chromium and npm test. Set CHROME_PATH to use an installed Chrome. The interaction check uses an isolated browser profile.
