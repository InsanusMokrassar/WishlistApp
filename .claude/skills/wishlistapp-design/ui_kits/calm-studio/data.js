/* Data for the Calm Studio prototype. Richer than the kit data: adds list
 * visibility, cover tints, and a reservation model (the booking feature the
 * redesign surfaces). window.CS_DATA. */
window.CS_DATA = (function () {
  const users = [
    { id: "you", username: "you", name: "You", tint: "linear-gradient(135deg,#5B5BD6,#8e8ee9)", you: true },
    { id: "alice", username: "alice", name: "Alice Werner", tint: "linear-gradient(135deg,#d98bb0,#e9b3cd)" },
    { id: "bob", username: "bob", name: "Bob Tran", tint: "linear-gradient(135deg,#6fae8f,#a7d4bf)" },
    { id: "mira", username: "mira", name: "Mira Solé", tint: "linear-gradient(135deg,#e0a36f,#f1c79e)" },
    { id: "root", username: "root", name: "Root", tint: "linear-gradient(135deg,#8a8a99,#bcbcc9)", admin: true },
  ];

  const wishlists = [
    { id: "w_bday", ownerId: "you", title: "Birthday", visibility: "Public", units: "USD", cover: "linear-gradient(135deg,#c9c9f0,#a9a9e6)" },
    { id: "w_home", ownerId: "you", title: "New apartment", visibility: "Public", units: "USD", cover: "linear-gradient(135deg,#cfe0d6,#aeccbb)" },
    { id: "w_priv", ownerId: "you", title: "Someday / maybe", visibility: "Private", units: "USD", cover: "linear-gradient(135deg,#e2dccf,#cdc3ad)" },
    { id: "w_alice_wed", ownerId: "alice", title: "Wedding registry", visibility: "Public", units: "EUR", cover: "linear-gradient(135deg,#f0d3e0,#e6b3cd)" },
    { id: "w_alice_books", ownerId: "alice", title: "Books to read", visibility: "Public", units: "EUR", cover: "linear-gradient(135deg,#dfe0ee,#c6c8e2)" },
    { id: "w_bob_bike", ownerId: "bob", title: "Bike build", visibility: "Public", units: "USD", cover: "linear-gradient(135deg,#d0e6da,#aed4bf)" },
    { id: "w_mira_art", ownerId: "mira", title: "Art supplies", visibility: "Public", units: "USD", cover: "linear-gradient(135deg,#f3e2cf,#e9c79e)" },
  ];

  let items = [
    { id: "i1", wishlistId: "w_bday", title: "Mechanical keyboard", description: "Brown switches, TKL layout, wireless.", price: 120, units: "USD", amount: 1, priority: "High", links: [{ url: "https://example.com/kbd", title: "Product page" }], tint: "t0" },
    { id: "i2", wishlistId: "w_bday", title: "Noise-cancelling headphones", description: "Over-ear, long battery life.", price: 240, units: "USD", amount: 1, priority: "High", links: [{ url: "https://example.com/hp", title: "Review" }], tint: "t1" },
    { id: "i3", wishlistId: "w_bday", title: "Floor lamp", description: "Warm dimmable LED, oak base.", price: 85, units: "USD", amount: 1, priority: "Medium", links: [], tint: "t2" },
    { id: "i4", wishlistId: "w_bday", title: "Linen apron", description: "Natural color, with front pocket.", price: 35, units: "USD", amount: 1, priority: "Medium", links: [], tint: "t3" },
    { id: "i5", wishlistId: "w_bday", title: "Espresso cups", description: "Set of 4, matte ceramic.", price: 28, units: "USD", amount: 2, priority: "Low", links: [], tint: "t4" },
    { id: "i6", wishlistId: "w_bday", title: "Cast-iron skillet", description: "12-inch, pre-seasoned.", price: 45, units: "USD", amount: 1, priority: "Medium", links: [], tint: "t5" },
    { id: "i7", wishlistId: "w_home", title: "Wool throw blanket", description: "Oatmeal, queen size.", price: 70, units: "USD", amount: 1, priority: "Medium", links: [], tint: "t6" },
    { id: "i8", wishlistId: "w_home", title: "Dinner plate set", description: "Stoneware, service for 8.", price: 160, units: "USD", amount: 1, priority: "High", links: [], tint: "t7" },
    { id: "i9", wishlistId: "w_alice_wed", title: "KitchenAid mixer", description: "Stand mixer, 5qt.", price: 380, units: "EUR", amount: 1, priority: "High", links: [{ url: "https://example.com/mixer", title: "Product page" }], tint: "t1" },
    { id: "i10", wishlistId: "w_alice_wed", title: "Wool throw blanket", description: "", price: 70, units: "EUR", amount: 1, priority: "Medium", links: [], tint: "t6" },
    { id: "i11", wishlistId: "w_alice_books", title: "The Overstory", description: "Richard Powers, hardcover.", price: 18, units: "EUR", amount: 1, priority: "Low", links: [], tint: "t3" },
    { id: "i12", wishlistId: "w_bob_bike", title: "Carbon handlebars", description: "31.8mm clamp, 420mm.", price: 130, units: "USD", amount: 1, priority: "High", links: [], tint: "t0" },
    { id: "i13", wishlistId: "w_mira_art", title: "Gouache set", description: "24 tubes, artist grade.", price: 52, units: "USD", amount: 1, priority: "Medium", links: [], tint: "t7" },
  ];

  // Reservations: itemId -> reserverId. Seeded so "you" have reserved some gifts
  // and some of your own items are reserved by others (shown as a count, never who).
  let reservations = { i9: "you", i11: "you", i3: "bob", i5: "mira" };

  function priceText(it) {
    if (it.price == null) return "";
    const base = `≈ ${it.price} ${it.units}`;
    return it.amount > 1 ? `${base} · ×${it.amount}` : base;
  }
  const PRI = { High: 3, Medium: 2, Low: 1, Custom: 2.5 };

  return {
    users, wishlists,
    get items() { return items; },
    get reservations() { return reservations; },
    priceText, PRI,
    usersById: (id) => users.find((u) => u.id === id),
    wishlistsByOwner: (id) => wishlists.filter((w) => w.ownerId === id),
    publicWishlistsByOwner: (id) => wishlists.filter((w) => w.ownerId === id && w.visibility === "Public"),
    wishlistById: (id) => wishlists.find((w) => w.id === id),
    itemsByWishlist: (id) => items.filter((i) => i.wishlistId === id),
    itemById: (id) => items.find((i) => i.id === id),
    isReserved: (id) => id in reservations,
    reservedByYou: () => items.filter((i) => reservations[i.id] === "you"),
    reserve: (id, by = "you") => { reservations = { ...reservations, [id]: by }; },
    unreserve: (id) => { const r = { ...reservations }; delete r[id]; reservations = r; },
    addItem: (it) => { items = [{ ...it, id: "i" + (items.length + 100) }, ...items]; },
    deleteItem: (id) => { items = items.filter((i) => i.id !== id); },
  };
})();
