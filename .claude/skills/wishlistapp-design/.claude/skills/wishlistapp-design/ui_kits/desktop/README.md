# WishlistApp — Desktop UI kit

Interactive recreation of the WishlistApp **Desktop** client (Compose for Desktop,
Material 3) in a desktop window frame.

`index.html` mounts `../material/material-app.jsx` (the shared Material 3 app) inside
a plain window chrome (title bar with traffic lights — the Compose for Desktop app has
no sidebar), using `../web/data.js`. The Desktop and Android
clients share one Compose codebase, so this is the *same* Material 3 surface as the
Android kit — just in a resizable window, where the adaptive grid shows more columns.
See `../material/README.md` for the shared internals.
