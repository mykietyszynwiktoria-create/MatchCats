# Device testing checklist

This checklist is for the installable web shell before native store packaging.

## Windows

1. Serve the 'frontend' directory over HTTPS or use the local development server.
2. Open the page in a Chromium based browser.
3. Use the browser's install action and confirm that MatchCats opens in a standalone window.
4. Sign in and verify that account data still comes from the API.

## Android

1. Open the HTTPS address in Chrome.
2. Use 'Add to home screen' or the install prompt.
3. Test rotation, offline opening of the shell, sign in, navigation and file upload.
4. Confirm that a lost connection does not display stale private data.

## iOS

1. Open the HTTPS address in Safari.
2. Use Share > Add to Home Screen.
3. Test returning from background, keyboard behavior, sign in and upload controls.

The service worker only caches static interface files. It deliberately does not cache account, message, document or notification API responses. Native store packages still require a reviewed wrapper, signing and store accounts.
