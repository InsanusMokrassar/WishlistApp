One-line: The blue WishlistApp top navbar — breadcrumb brand title on the left, auth actions on the right.

```jsx
<NavBar
  title={["Jane's Wishlists", "Birthday"]}
  actions={<Button variant="outline-light" size="sm">Log out</Button>}
/>
```

`title` can be a single string or an array of breadcrumb segments (joined with " / "), mirroring how the app stacks screen titles. Put navbar buttons in `actions` and use `variant="outline-light"` so they read against the blue (`bg-primary`) bar.
