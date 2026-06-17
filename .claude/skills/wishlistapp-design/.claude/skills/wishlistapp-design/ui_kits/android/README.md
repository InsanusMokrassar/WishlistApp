# WishlistApp — Android UI kit

Interactive recreation of the WishlistApp **Android** client (Jetpack Compose +
Material 3) in a phone device frame.

`index.html` mounts `../material/material-app.jsx` (the shared Material 3 app) inside
`../material/android-frame.jsx`, using `../web/data.js`. See `../material/README.md`
for the shared internals. Same flows as the web kit (users → wishlists → wishlist →
item → edit, login dialog), rendered in Material 3 instead of Bootstrap.
