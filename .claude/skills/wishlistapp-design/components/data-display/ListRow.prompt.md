One-line: One Bootstrap list-group row with optional leading (avatar) and trailing (actions) slots — the app's main list layout.

```jsx
<ul className="list-group">
  <ListRow
    leading={<Avatar src={url} size={48} />}
    onSelect={() => openUser(u.id)}
  >
    <span>{u.username}</span>
  </ListRow>
</ul>
```

Must live inside `<ul className="list-group">`. With no `leading`/`trailing`, the children fill the row directly. `onSelect` makes the primary area clickable (cursor pointer); `trailing` buttons sit outside the click target.
