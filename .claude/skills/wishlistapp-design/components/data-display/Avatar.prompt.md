One-line: User avatar image that falls back to the brand gray-silhouette placeholder when no photo is set.

```jsx
<Avatar src={photoUrl} alt="Jane" size={48} />
<Avatar size={120} circle={false} />  {/* large profile preview */}
```

`circle` (default true) is the list-row / navbar shape; set `circle={false}` for the rounded-square large profile preview. The placeholder silhouette is embedded — no network request.
