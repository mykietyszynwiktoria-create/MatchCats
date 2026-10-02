# MatchCats breeder panels

A bilingual, responsive interface prototype next to the existing Java backend. It is not connected to the server or PostgreSQL yet.

## Open the preview

Open index.html in a modern browser. Application assets are local files and do not require downloading libraries to run the preview. You can also serve this directory with a local HTTP server.

## Main tasks

1. **Home:** choose Find a partner or Add your cat; use the shortcuts to My cats, search and conversations.
2. **My cats:** add a sample profile, open its details and change contact availability.
3. **Find a cat:** filter by name/cattery, breed, sex, city and availability. Clear filters to restart your search.
4. **Cat profile:** review the owner, description and sample document entries, then open a demo conversation.
5. **Conversations:** add sample messages. No message is sent to another person.
6. **Documents and settings:** review the planned document organisation, edit local cattery details or reset the demo. On phones, open settings with the profile avatar at the top.

The PL / EN control switches the interface between Polish and English and remembers the choice. Names, breeder input and conversation messages remain in the author's language.

## Sky Garden interface

The selected design uses a light blue background, horizontal desktop navigation, generated photographic sample cards and clear blue action buttons. Home search transfers breed, sex and city to the full search screen. Shortcuts lead to actual screens. Phone navigation keeps five main destinations visible; the profile avatar opens settings. The mobile hero places the photograph beneath its controls, and conversations use a vertically stacked list and message pane.

`sky-garden.css` loads after the existing `styles.css` and overrides the presentation while preserving the existing demo workflows. No external image requests or UI libraries are required. Asset provenance and generation prompts are documented in `assets/README.md`.

## Run the interface check

Requirements: Node.js and a Playwright browser. From this directory, run npm install, then npx playwright install chromium, then npm test.

The check starts its own temporary local HTTP server and closes it when finished. Set CHROME_PATH to use an installed Chrome executable instead of Playwright's downloaded Chromium. It uses an isolated browser profile and does not change the user's demo data.

Checks cover home shortcuts, filters and empty results, cat details, conversations, safe text rendering, persistence after reload, adding a cat, availability, settings, demo reset and language switching. Layout checks cover seven screens in Polish and English at widths of 320, 390, 768 and 1440 pixels. These are browser viewport checks, not tests on physical Android or iOS devices.

## Prototype boundaries

All other breeders, conversations, document entries and show achievements are fictional samples. The sample photographs are AI-generated and do not depict verified breeder profiles. Newly added cats have an empty photo placeholder. Data is stored in this browser's localStorage, not in PostgreSQL. If local storage is unavailable, the interface displays a warning. Do not enter confidential information.

There is no authentication, document/photo upload, breeder verification, full profile editing, push notification service, installer or store release. Listed documents have no attached files. Displaying a document does not confirm health, ancestry, breed purity or breeding suitability.

## Windows, Android and iOS

The product owner wants installed applications distributed through the appropriate stores. This browser prototype establishes the interface and interaction flow; it is not a native app or an installable PWA. Choosing a shared client technology, signing packages, preparing store accounts and testing on real devices remain separate steps.

## Next work

The backend now includes accounts, cat profiles, conversations, matching and document storage; see the root backend documentation for its validation and boundaries. Next, implement bilingual registration and login screens connected to the session API, including validation, generic invalid-credential errors, network failures and expired sessions. Then replace the remaining demo workflows with server-backed profiles, photos, messages and documents. Password recovery and store packaging remain separate work.

Commit messages and repository documentation are written in English; changes are saved in small, focused commits.
