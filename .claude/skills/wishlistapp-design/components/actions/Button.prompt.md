One-line: The Bootstrap button used for every action in WishlistApp — solid for primary flows, outline for toolbar actions.

```jsx
<Button variant="primary">Save</Button>
<Button variant="success">Add Item</Button>
<Button variant="outline-secondary">Back</Button>
<Button variant="danger" disabled>Delete</Button>
```

Variants: `primary` (Save), `success` (Add Item / Copy to my wishlist), `danger` (Delete), `outline-secondary` (Back), `outline-primary` (Edit), `outline-success` (Copy), `outline-light` (navbar Log in / Log out). Sizes: `sm` (navbar auth buttons), `lg`. Pass `type="submit"` inside forms. Any native button attribute (onClick, aria-*) passes through.
