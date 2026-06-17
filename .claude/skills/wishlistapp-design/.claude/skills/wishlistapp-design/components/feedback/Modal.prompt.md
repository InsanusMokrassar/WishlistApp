One-line: Centered Bootstrap modal with backdrop — the app's login/register form and destructive-confirmation dialogs.

```jsx
<Modal open={confirming} title="Delete item?"
  onCancel={cancel} onConfirm={remove}
  confirmLabel="Delete" confirmVariant="danger">
  This item will be permanently removed. Continue?
</Modal>
```

Renders nothing when `open` is false. The default footer is a Cancel/Confirm pair built from the `*Label` / `on*` props; pass `footer` to supply custom actions (e.g. the login form's submit). Use `confirmVariant="danger"` for deletes.
