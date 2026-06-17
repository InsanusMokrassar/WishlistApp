One-line: Bootstrap card for a single wishlist item — media, title, owning-wishlist subtitle, description, price footer, with the priority pill overlaid top-right.

```jsx
<ItemCard
  title="Mechanical keyboard"
  wishlistTitle="Birthday"
  description="Brown switches, TKL"
  priceText="≈ 120 USD"
  priority="High"
  imageUrl={photoUrl}
  onSelect={() => open(item.id)}
/>
```

With no `imageUrl` the gift-box placeholder fills the media area (contained, not cropped). `priceText` is pre-formatted by the caller (currency conversion happens upstream); omit it to drop the footer. The whole card is clickable via `onSelect`. Lay cards out in a Bootstrap grid: `row row-cols-1 row-cols-sm-2 row-cols-md-3 g-3`.
