One-line: Neutral pill badge showing a wishlist item's priority (Low / Medium / High / Custom).

```jsx
<PriorityBadge priority="High" />
<PriorityBadge priority="Custom" weight={42} />
```

`priority="Small"` renders the label "Low". A `Custom` priority appends `weight` in parentheses. Always the subtle-gray pill (`bg-secondary-subtle text-secondary-emphasis`) — priority is metadata, not a status alert. Used overlaid top-right on item cards, inline on list rows, and on the item detail screen.
