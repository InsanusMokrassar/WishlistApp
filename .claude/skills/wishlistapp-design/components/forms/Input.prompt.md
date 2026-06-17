One-line: Labeled Bootstrap text input wrapped in the app's standard `mb-3` field block.

```jsx
<Input label="Title" id="wl-title" value={title}
  placeholder="Title" onChange={e => setTitle(e.target.value)} />
```

Renders label + `form-control` + optional `help` text. Pass `type="password"` for the login form. For dropdowns use `Select`.
