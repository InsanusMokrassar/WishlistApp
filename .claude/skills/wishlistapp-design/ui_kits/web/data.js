/* Fake data for the WishlistApp web UI kit. Mirrors the server's domain model:
 * users own wishlists, wishlists hold items (title, description, price, priority,
 * images). Exposed on window for the Babel-transpiled screens. */
window.WL_DATA = (function () {
  // Priority: "Small" (shown as Low), "Medium", "High", or "Custom" (+weight).
  const users = [
    { id: "u_you", username: "you", you: true },
    { id: "u_alice", username: "alice" },
    { id: "u_bob", username: "bob" },
    { id: "u_mira", username: "mira" },
    { id: "u_root", username: "root", admin: true },
  ];

  const wishlists = [
    { id: "w_bday", ownerId: "u_you", title: "Birthday", defaultUnits: "USD" },
    { id: "w_home", ownerId: "u_you", title: "New apartment", defaultUnits: "USD" },
    { id: "w_alice_wed", ownerId: "u_alice", title: "Wedding registry", defaultUnits: "EUR" },
    { id: "w_alice_books", ownerId: "u_alice", title: "Books to read", defaultUnits: "EUR" },
    { id: "w_bob_bike", ownerId: "u_bob", title: "Bike build", defaultUnits: "USD" },
    { id: "w_mira_art", ownerId: "u_mira", title: "Art supplies", defaultUnits: "USD" },
  ];

  const items = [
    { id: "i1", wishlistId: "w_bday", title: "Mechanical keyboard", description: "Brown switches, TKL layout, wireless.", price: 120, units: "USD", amount: 1, priority: "High", links: [{ url: "https://example.com/kbd", title: "Product page" }], images: [] },
    { id: "i2", wishlistId: "w_bday", title: "Linen apron", description: "Natural color, with front pocket.", price: 35, units: "USD", amount: 1, priority: "Medium", links: [], images: [] },
    { id: "i3", wishlistId: "w_bday", title: "Espresso cups", description: "Set of 4, matte ceramic.", price: 28, units: "USD", amount: 2, priority: "Small", links: [], images: [] },
    { id: "i4", wishlistId: "w_bday", title: "Noise-cancelling headphones", description: "Over-ear, long battery life.", price: 240, units: "USD", amount: 1, priority: "Custom", weight: 90, links: [{ url: "https://example.com/hp", title: "Review" }], images: [] },
    { id: "i5", wishlistId: "w_home", title: "Floor lamp", description: "Warm dimmable LED, oak base.", price: 85, units: "USD", amount: 1, priority: "Medium", links: [], images: [] },
    { id: "i6", wishlistId: "w_home", title: "Cast-iron skillet", description: "12-inch, pre-seasoned.", price: 45, units: "USD", amount: 1, priority: "High", links: [], images: [] },
    { id: "i7", wishlistId: "w_alice_wed", title: "Dinner plate set", description: "Stoneware, service for 8.", price: 160, units: "EUR", amount: 1, priority: "High", links: [], images: [] },
    { id: "i8", wishlistId: "w_alice_wed", title: "Wool throw blanket", description: "", price: 70, units: "EUR", amount: 1, priority: "Medium", links: [], images: [] },
    { id: "i9", wishlistId: "w_alice_books", title: "The Overstory", description: "Richard Powers, hardcover.", price: 18, units: "EUR", amount: 1, priority: "Small", links: [], images: [] },
    { id: "i10", wishlistId: "w_bob_bike", title: "Carbon handlebars", description: "31.8mm clamp, 420mm.", price: 130, units: "USD", amount: 1, priority: "High", links: [], images: [] },
    { id: "i11", wishlistId: "w_mira_art", title: "Gouache set", description: "24 tubes, artist grade.", price: 52, units: "USD", amount: 1, priority: "Medium", links: [], images: [] },
  ];

  function priceText(it) {
    if (it.price == null) return "";
    const base = `≈ ${it.price} ${it.units}`;
    if (it.amount > 1) return `${base} ×${it.amount}`;
    return base;
  }

  return {
    users,
    wishlists,
    items,
    priceText,
    usersById: (id) => users.find((u) => u.id === id),
    wishlistsByOwner: (id) => wishlists.filter((w) => w.ownerId === id),
    wishlistById: (id) => wishlists.find((w) => w.id === id),
    itemsByWishlist: (id) => items.filter((i) => i.wishlistId === id),
    itemById: (id) => items.find((i) => i.id === id),
  };
})();
