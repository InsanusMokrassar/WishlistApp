One-line: Labeled Bootstrap select that drives the sort, view-mode, and currency pickers.

```jsx
<Select label="Sort" value={sort} onChange={e => setSort(e.target.value)}
  options={["Default", "Cost", "Priority", "Title"]} />
```

`options` accepts bare strings or `{value, label}` objects. `size="sm"` for compact toolbar selectors.
