/* WishlistApp web UI kit — interactive recreation of the Compose-HTML web client.
 * Composes the design-system primitives (window.WishlistApp_ef9ce8) over the fake
 * data in data.js. Single-file app with a small navigation stack.
 *
 * IMPORTANT: this file is also compiled into _ds_bundle.js, which loads BEFORE the
 * namespace and WL_DATA are populated. So everything is resolved at RENDER time
 * (inside component bodies / TITLES getters), never at module scope, and the mount
 * happens from index.html — not here. */
const { useState } = React;

const SORTS = ["Default", "Cost", "Priority", "Title"];
const PRIORITY_ORDER = { High: 3, Custom: 2.5, Medium: 2, Small: 1 };

function sortItems(items, mode) {
  const a = [...items];
  if (mode === "Cost") a.sort((x, y) => (x.price || 0) - (y.price || 0));
  else if (mode === "Title") a.sort((x, y) => x.title.localeCompare(y.title));
  else if (mode === "Priority")
    a.sort((x, y) => (PRIORITY_ORDER[y.priority] || 0) - (PRIORITY_ORDER[x.priority] || 0));
  return a;
}

function Container({ children }) {
  return <div className="container py-3">{children}</div>;
}

/* ------------------------------------------------------------- users list */
function UsersListScreen({ me, nav }) {
  const { Button, ListRow, Avatar } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  return (
    <Container>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <h1 className="h3 mb-0">Users</h1>
        {me && (
          <Button variant="outline-primary" onClick={() => nav.push("wishlists", { userId: me.id })}>
            My profile
          </Button>
        )}
      </div>
      <ul className="list-group">
        {D.users.map((u) => (
          <ListRow
            key={u.id}
            leading={<Avatar size={48} />}
            onSelect={() => nav.push("wishlists", { userId: u.id })}
          >
            <span>{u.username}</span>
            {u.you && <span className="text-muted small ms-2">(you)</span>}
            {u.admin && <span className="badge bg-secondary-subtle text-secondary-emphasis ms-2">admin</span>}
          </ListRow>
        ))}
      </ul>
    </Container>
  );
}

/* --------------------------------------------------------- user wishlists */
function UserWishlistsScreen({ me, params, nav }) {
  const { Button, ListRow } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const lists = D.wishlistsByOwner(params.userId);
  const isOwner = me && me.id === params.userId;
  return (
    <Container>
      <div className="d-flex justify-content-between align-items-center mb-3">
        <Button variant="outline-secondary" onClick={nav.pop}>Back</Button>
        <div className="d-flex gap-2">
          <Button variant="outline-secondary" onClick={() => nav.push("allItems", { userId: params.userId })}>
            All items
          </Button>
          {isOwner && <Button variant="success" onClick={() => alert("New wishlist (demo)")}>New Wishlist</Button>}
        </div>
      </div>
      {lists.length === 0 ? (
        <p className="text-muted">No wishlists yet</p>
      ) : (
        <ul className="list-group">
          {lists.map((w) => (
            <ListRow
              key={w.id}
              leading={<img src="../../assets/stacked-items.svg" alt="" width="48" height="48" className="rounded flex-shrink-0" />}
              onSelect={() => nav.push("wishlist", { wishlistId: w.id })}
            >
              <span>{w.title}</span>
            </ListRow>
          ))}
        </ul>
      )}
    </Container>
  );
}

/* ----------------------------------------------------------- item grid */
function ItemGrid({ items, nav, withSub }) {
  const { ItemCard } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  return (
    <div className="row row-cols-1 row-cols-sm-2 row-cols-md-3 g-3 mb-3">
      {items.map((it) => (
        <div className="col" key={it.id}>
          <ItemCard
            title={it.title}
            wishlistTitle={withSub ? it.wishlistTitle : undefined}
            description={it.description}
            priceText={D.priceText(it)}
            priority={it.priority}
            weight={it.weight}
            onSelect={() => nav.push("item", { itemId: it.id })}
          />
        </div>
      ))}
    </div>
  );
}

/* ------------------------------------------------------------ all items */
function AllItemsScreen({ params, nav }) {
  const { Button, Select } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const lists = D.wishlistsByOwner(params.userId);
  const items = lists.flatMap((w) => D.itemsByWishlist(w.id).map((i) => ({ ...i, wishlistTitle: w.title })));
  const [sort, setSort] = useState("Default");
  return (
    <Container>
      <div className="d-flex align-items-center mb-3 gap-2">
        <Button variant="outline-secondary" onClick={nav.pop}>Back</Button>
        <div className="flex-grow-1" />
      </div>
      <div className="d-flex gap-3 align-items-end mb-3" style={{ maxWidth: 220 }}>
        <Select label="Sort" value={sort} onChange={(e) => setSort(e.target.value)} options={SORTS} size="sm" />
      </div>
      <ItemGrid items={sortItems(items, sort)} nav={nav} withSub />
    </Container>
  );
}

/* -------------------------------------------------------- wishlist detail */
function WishlistDetailScreen({ me, params, nav }) {
  const { Button, Select, ListRow, PriorityBadge, Alert } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const w = D.wishlistById(params.wishlistId);
  const items = D.itemsByWishlist(w.id);
  const isOwner = me && me.id === w.ownerId;
  const [sort, setSort] = useState("Default");
  const [view, setView] = useState("Grid");
  const [copied, setCopied] = useState(false);
  const sorted = sortItems(items, sort);
  return (
    <Container>
      <div className="d-flex align-items-center mb-3 gap-2">
        <Button variant="outline-secondary" onClick={nav.pop}>Back</Button>
        <div className="flex-grow-1" />
        {!isOwner && (
          <Button variant="outline-success" disabled={copied} onClick={() => setCopied(true)}>
            Copy to my profile
          </Button>
        )}
        {isOwner && <Button variant="outline-primary" onClick={() => alert("Edit wishlist (demo)")}>Edit</Button>}
      </div>

      {copied && <Alert variant="success">Copy queued. It will appear in your profile shortly.</Alert>}

      <div className="d-flex gap-3 align-items-end mb-3 flex-wrap">
        <div style={{ width: 160 }}>
          <Select label="Sort" value={sort} onChange={(e) => setSort(e.target.value)} options={SORTS} size="sm" />
        </div>
        <div style={{ width: 140 }}>
          <Select label="View" value={view} onChange={(e) => setView(e.target.value)} options={["Grid", "List"]} size="sm" />
        </div>
      </div>

      {items.length === 0 ? (
        <p className="text-muted">No items yet</p>
      ) : view === "Grid" ? (
        <ItemGrid items={sorted} nav={nav} />
      ) : (
        <ul className="list-group mb-3">
          {sorted.map((it) => (
            <ListRow key={it.id} onSelect={() => nav.push("item", { itemId: it.id })}>
              <div className="flex-grow-1">
                <div className="d-flex justify-content-between align-items-center">
                  <div className="d-flex align-items-center gap-2">
                    <span>{it.title}</span>
                    <PriorityBadge priority={it.priority} weight={it.weight} />
                  </div>
                  {D.priceText(it) && <span className="text-muted small">{D.priceText(it)}</span>}
                </div>
                {it.description && <p className="mb-0 text-muted small mt-1">{it.description}</p>}
              </div>
            </ListRow>
          ))}
        </ul>
      )}

      {isOwner && <Button variant="success" onClick={() => nav.push("itemEdit", { wishlistId: w.id })}>Add Item</Button>}
    </Container>
  );
}

/* ------------------------------------------------------------ item detail */
function ItemDetailScreen({ me, params, nav }) {
  const { Button, PriorityBadge, ListRow } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const it = D.itemById(params.itemId);
  const w = D.wishlistById(it.wishlistId);
  const isOwner = me && me.id === w.ownerId;
  return (
    <Container>
      <div className="d-flex align-items-center mb-3 gap-2">
        <Button variant="outline-secondary" onClick={nav.pop}>Back</Button>
        <div className="flex-grow-1" />
        {!isOwner && <Button variant="outline-success" onClick={() => alert("Copy to my wishlist (demo)")}>Copy to my wishlist</Button>}
        {isOwner && <Button variant="outline-primary" onClick={() => nav.push("itemEdit", { itemId: it.id, wishlistId: w.id })}>Edit</Button>}
      </div>

      {it.description && (
        <div className="mb-3">
          <h6 className="text-muted">Description</h6>
          <p>{it.description}</p>
        </div>
      )}
      <div className="mb-3">
        <h6 className="text-muted">Approximate price</h6>
        {D.priceText(it) ? <p>{D.priceText(it)}</p> : <p className="text-muted">No price</p>}
      </div>
      <div className="mb-3">
        <h6 className="text-muted">Priority</h6>
        <p><PriorityBadge priority={it.priority} weight={it.weight} /></p>
      </div>
      <div className="mb-3">
        <h6 className="text-muted">Links</h6>
        {it.links.length === 0 ? (
          <p className="text-muted">No links</p>
        ) : (
          <ul className="list-group">
            {it.links.map((l, i) => (
              <ListRow key={i}><a href={l.url} target="_blank" rel="noreferrer">{l.title || l.url}</a></ListRow>
            ))}
          </ul>
        )}
      </div>
      <div className="mb-3">
        <h6 className="text-muted">Images</h6>
        <img src="../../assets/giftbox.svg" alt="Gift placeholder" className="rounded border" style={{ width: 160, height: 160, objectFit: "cover" }} />
      </div>
    </Container>
  );
}

/* -------------------------------------------------------------- item edit */
function ItemEditScreen({ params, nav }) {
  const { Button, Input, Select, Modal } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const editing = params.itemId ? D.itemById(params.itemId) : null;
  const [title, setTitle] = useState(editing ? editing.title : "");
  const [desc, setDesc] = useState(editing ? editing.description : "");
  const [price, setPrice] = useState(editing ? editing.price : "");
  const [priority, setPriority] = useState(editing ? editing.priority : "Medium");
  const [confirm, setConfirm] = useState(false);
  return (
    <Container>
      <div className="d-flex align-items-center mb-3 gap-2">
        <Button variant="outline-secondary" onClick={nav.pop}>Back</Button>
      </div>
      <div style={{ maxWidth: 520 }}>
        <Input label="Title" id="it-title" value={title} placeholder="Title" onChange={(e) => setTitle(e.target.value)} />
        <Input label="Description" id="it-desc" value={desc} placeholder="Description" onChange={(e) => setDesc(e.target.value)} />
        <Input label="Approximate price" id="it-price" type="number" value={price} placeholder="0" onChange={(e) => setPrice(e.target.value)} />
        <Select label="Priority" value={priority} onChange={(e) => setPriority(e.target.value)}
          options={[{ value: "Small", label: "Low" }, { value: "Medium", label: "Medium" }, { value: "High", label: "High" }, { value: "Custom", label: "Custom" }]} />
        <div className="d-flex gap-2">
          <Button variant="primary" disabled={!title.trim()} onClick={nav.pop}>Save</Button>
          {editing && <Button variant="danger" onClick={() => setConfirm(true)}>Delete</Button>}
        </div>
      </div>
      <Modal open={confirm} title="Delete item?" onCancel={() => setConfirm(false)} onConfirm={() => { setConfirm(false); nav.pop(); }}
        confirmLabel="Delete" confirmVariant="danger">
        This item will be permanently removed. Continue?
      </Modal>
    </Container>
  );
}

/* -------------------------------------------------------------- login modal */
function LoginModal({ onClose, onLogin, registerMode }) {
  const { Button, Modal } = window.WishlistApp_ef9ce8;
  const [username, setUsername] = useState("you");
  const [password, setPassword] = useState("");
  return (
    <Modal
      open
      title={registerMode ? "Register" : "Log in"}
      onCancel={onClose}
      footer={
        <>
          <Button variant="outline-secondary" onClick={onClose}>Cancel</Button>
          <Button variant="primary" onClick={() => onLogin(username)}>{registerMode ? "Create account" : "Log in"}</Button>
        </>
      }
    >
      <div className="d-flex flex-column gap-2">
        <input className="form-control" value={username} placeholder="Username" onChange={(e) => setUsername(e.target.value)} />
        <input className="form-control" type="password" value={password} placeholder="Password" onChange={(e) => setPassword(e.target.value)} />
      </div>
    </Modal>
  );
}

/* --------------------------------------------------------------------- App */
const TITLES = {
  users: () => "Users",
  wishlists: (p) => { const u = window.WL_DATA.usersById(p.userId); return u ? `${u.username}'s Wishlists` : "Wishlists"; },
  allItems: (p) => { const u = window.WL_DATA.usersById(p.userId); return u ? `${u.username}'s wishes` : "All items"; },
  wishlist: (p) => { const w = window.WL_DATA.wishlistById(p.wishlistId); return w ? w.title : "Wishlist"; },
  item: (p) => { const i = window.WL_DATA.itemById(p.itemId); return i ? i.title : "Item"; },
  itemEdit: (p) => (p.itemId ? "Edit Item" : "New Item"),
};

const SCREENS = {
  users: UsersListScreen,
  wishlists: UserWishlistsScreen,
  allItems: AllItemsScreen,
  wishlist: WishlistDetailScreen,
  item: ItemDetailScreen,
  itemEdit: ItemEditScreen,
};

function App() {
  const { NavBar, Button } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const [stack, setStack] = useState([{ screen: "users", params: {} }]);
  const [me, setMe] = useState(null);
  const [login, setLogin] = useState(null); // null | {register}

  const nav = {
    push: (screen, params = {}) => setStack((s) => [...s, { screen, params }]),
    pop: () => setStack((s) => (s.length > 1 ? s.slice(0, -1) : s)),
  };

  const top = stack[stack.length - 1];
  const crumbs = stack.map((f) => TITLES[f.screen](f.params));
  const Screen = SCREENS[top.screen];

  return (
    <div style={{ minHeight: "100vh", background: "var(--wl-surface-page)" }}>
      <NavBar
        title={crumbs}
        actions={
          me ? (
            <Button variant="outline-light" size="sm" onClick={() => { setMe(null); setStack([{ screen: "users", params: {} }]); }}>
              Log out
            </Button>
          ) : (
            <>
              <Button variant="outline-light" size="sm" onClick={() => setLogin({ register: false })}>Log in</Button>
              <Button variant="outline-light" size="sm" onClick={() => setLogin({ register: true })}>Register</Button>
            </>
          )
        }
      />
      <Screen me={me} params={top.params} nav={nav} />
      {login && (
        <LoginModal
          registerMode={login.register}
          onClose={() => setLogin(null)}
          onLogin={() => { setMe(D.usersById("u_you")); setLogin(null); }}
        />
      )}
    </div>
  );
}

// Exposed for ui_kits/web/index.html to mount. No top-level createRoot here:
// this file is also compiled into _ds_bundle.js, and a top-level mount would run
// at bundle-load (before the components exist) and double-mount.
window.WishlistWebApp = App;
