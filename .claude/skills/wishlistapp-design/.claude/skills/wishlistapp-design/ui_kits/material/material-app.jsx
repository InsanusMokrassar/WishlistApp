/* WishlistApp Material 3 app — shared by the Android and Desktop UI kits.
 * Recreates the Compose Material 3 client screens. Exposes window.MaterialApp.
 * Depends on window.WL_DATA (data.js). */
(function () {
  const { useState } = React;
  const D = window.WL_DATA;
  const SORTS = ["Default", "Cost", "Priority", "Title"];
  const PRI = { High: 3, Custom: 2.5, Medium: 2, Small: 1 };
  const PLABEL = { Small: "Low", Medium: "Medium", High: "High", Custom: "Custom" };

  function sortItems(items, mode) {
    const a = [...items];
    if (mode === "Cost") a.sort((x, y) => (x.price || 0) - (y.price || 0));
    else if (mode === "Title") a.sort((x, y) => x.title.localeCompare(y.title));
    else if (mode === "Priority") a.sort((x, y) => (PRI[y.priority] || 0) - (PRI[x.priority] || 0));
    return a;
  }
  const pLabel = (it) => PLABEL[it.priority] + (it.priority === "Custom" && it.weight != null ? ` (${it.weight})` : "");

  function Btn({ kind = "filled", className = "", ...p }) {
    const c = ["m3-btn"];
    if (kind === "outlined") c.push("m3-btn--outlined");
    if (kind === "text") c.push("m3-btn--text");
    if (className) c.push(className);
    return <button className={c.join(" ")} {...p} />;
  }
  const Badge = ({ it }) => <span className="m3-badge">{pLabel(it)}</span>;

  /* ---- screens ---- */
  function Users({ me, nav }) {
    return (
      <div className="m3-list">
        {D.users.map((u) => (
          <div className="m3-row" key={u.id} onClick={() => nav.push("wishlists", { userId: u.id })}>
            <img className="m3-row__thumb m3-row__thumb--circle" src="../../assets/user-silhouette.svg" alt="" />
            <div className="m3-row__main">
              <div className="m3-row__title">{u.username}{u.you && <span className="m3-muted"> (you)</span>}</div>
              {u.admin && <div className="m3-row__sub">admin</div>}
            </div>
          </div>
        ))}
      </div>
    );
  }

  function UserWishlists({ me, params, nav }) {
    const lists = D.wishlistsByOwner(params.userId);
    const isOwner = me && me.id === params.userId;
    return (
      <>
        <div className="m3-toolbar">
          <Btn kind="outlined" className="m3-btn--sm" onClick={nav.pop}>Back</Btn>
          <span className="m3-spacer" />
          <Btn kind="outlined" className="m3-btn--sm" onClick={() => nav.push("allItems", { userId: params.userId })}>All items</Btn>
          {isOwner && <Btn className="m3-btn--sm" onClick={() => {}}>New Wishlist</Btn>}
        </div>
        {lists.length === 0 ? <p className="m3-muted">No wishlists yet</p> : (
          <div className="m3-list">
            {lists.map((w) => (
              <div className="m3-row" key={w.id} onClick={() => nav.push("wishlist", { wishlistId: w.id })}>
                <img className="m3-row__thumb" src="../../assets/stacked-items.svg" alt="" />
                <div className="m3-row__main"><div className="m3-row__title">{w.title}</div></div>
              </div>
            ))}
          </div>
        )}
      </>
    );
  }

  function AllItems({ params, nav }) {
    const lists = D.wishlistsByOwner(params.userId);
    const items = lists.flatMap((w) => D.itemsByWishlist(w.id).map((i) => ({ ...i, wishlistTitle: w.title })));
    const [sort, setSort] = useState("Default");
    return (
      <>
        <div className="m3-toolbar"><Btn kind="outlined" className="m3-btn--sm" onClick={nav.pop}>Back</Btn></div>
        <div className="m3-selectors">
          <div className="m3-textfield" style={{ marginBottom: 0, minWidth: 140 }}>
            <label>Sort</label>
            <select value={sort} onChange={(e) => setSort(e.target.value)}>{SORTS.map((s) => <option key={s}>{s}</option>)}</select>
          </div>
        </div>
        <Grid items={sortItems(items, sort)} nav={nav} withSub />
      </>
    );
  }

  const Grid = ({ items, nav, withSub }) => (
    <div className="m3-grid">
      {items.map((it) => (
        <div className="m3-card" key={it.id} onClick={() => nav.push("item", { itemId: it.id })}>
          <span className="m3-card__badge"><Badge it={it} /></span>
          <img className="m3-card__media m3-card__media--placeholder" src="../../assets/giftbox.svg" alt="" />
          <div className="m3-card__body">
            <p className="m3-card__title">{it.title}</p>
            {withSub && it.wishlistTitle && <p className="m3-card__subtitle">{it.wishlistTitle}</p>}
            {it.description && <p className="m3-card__desc">{it.description}</p>}
            {D.priceText(it) && <p className="m3-card__price">{D.priceText(it)}</p>}
          </div>
        </div>
      ))}
    </div>
  );

  function Wishlist({ me, params, nav }) {
    const w = D.wishlistById(params.wishlistId);
    const items = D.itemsByWishlist(w.id);
    const isOwner = me && me.id === w.ownerId;
    const [sort, setSort] = useState("Default");
    const [view, setView] = useState("Grid");
    const [copied, setCopied] = useState(false);
    const sorted = sortItems(items, sort);
    return (
      <>
        <div className="m3-toolbar">
          <Btn kind="outlined" className="m3-btn--sm" onClick={nav.pop}>Back</Btn>
          <span className="m3-spacer" />
          {!isOwner && <Btn kind="outlined" className="m3-btn--sm" disabled={copied} onClick={() => setCopied(true)}>Copy to my profile</Btn>}
          {isOwner && <Btn className="m3-btn--sm" onClick={() => {}}>Edit</Btn>}
        </div>
        {copied && <p className="m3-status">Copy queued. It will appear in your profile shortly.</p>}
        <div className="m3-selectors">
          <div className="m3-textfield" style={{ marginBottom: 0, minWidth: 140 }}>
            <label>Sort</label>
            <select value={sort} onChange={(e) => setSort(e.target.value)}>{SORTS.map((s) => <option key={s}>{s}</option>)}</select>
          </div>
          <div className="m3-textfield" style={{ marginBottom: 0, minWidth: 120 }}>
            <label>View</label>
            <select value={view} onChange={(e) => setView(e.target.value)}>{["Grid", "List"].map((s) => <option key={s}>{s}</option>)}</select>
          </div>
        </div>
        {items.length === 0 ? <p className="m3-muted">No items yet</p> : view === "Grid" ? (
          <Grid items={sorted} nav={nav} />
        ) : (
          <div className="m3-list">
            {sorted.map((it) => (
              <div className="m3-row" key={it.id} onClick={() => nav.push("item", { itemId: it.id })}>
                <div className="m3-row__main">
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 8 }}>
                    <div style={{ display: "flex", alignItems: "center", gap: 8 }}>
                      <span className="m3-row__title">{it.title}</span><Badge it={it} />
                    </div>
                    {D.priceText(it) && <span className="m3-row__sub">{D.priceText(it)}</span>}
                  </div>
                  {it.description && <div className="m3-row__sub">{it.description}</div>}
                </div>
              </div>
            ))}
          </div>
        )}
        {isOwner && <div style={{ marginTop: 12 }}><Btn className="m3-btn--full" onClick={() => nav.push("itemEdit", { wishlistId: w.id })}>Add Item</Btn></div>}
      </>
    );
  }

  function Item({ me, params, nav }) {
    const it = D.itemById(params.itemId);
    const w = D.wishlistById(it.wishlistId);
    const isOwner = me && me.id === w.ownerId;
    return (
      <>
        <div className="m3-toolbar">
          <Btn kind="outlined" className="m3-btn--sm" onClick={nav.pop}>Back</Btn>
          <span className="m3-spacer" />
          {!isOwner && <Btn kind="outlined" className="m3-btn--sm" onClick={() => {}}>Copy to my wishlist</Btn>}
          {isOwner && <Btn className="m3-btn--sm" onClick={() => nav.push("itemEdit", { itemId: it.id, wishlistId: w.id })}>Edit</Btn>}
        </div>
        {it.description && <div className="m3-field"><div className="m3-field__label">Description</div><div className="m3-field__value">{it.description}</div></div>}
        <div className="m3-field"><div className="m3-field__label">Approximate price</div><div className="m3-field__value">{D.priceText(it) || <span className="m3-muted">No price</span>}</div></div>
        <div className="m3-field"><div className="m3-field__label">Priority</div><div><Badge it={it} /></div></div>
        <div className="m3-field"><div className="m3-field__label">Links</div>
          {it.links.length === 0 ? <div className="m3-muted">No links</div> : it.links.map((l, i) => <div key={i}><a href={l.url} target="_blank" rel="noreferrer">{l.title || l.url}</a></div>)}
        </div>
        <div className="m3-field"><div className="m3-field__label">Images</div>
          <img src="../../assets/giftbox.svg" alt="" style={{ width: 160, height: 160, borderRadius: "var(--m3-corner-md)", objectFit: "cover" }} />
        </div>
      </>
    );
  }

  function ItemEdit({ params, nav }) {
    const editing = params.itemId ? D.itemById(params.itemId) : null;
    const [title, setTitle] = useState(editing ? editing.title : "");
    const [desc, setDesc] = useState(editing ? editing.description : "");
    const [price, setPrice] = useState(editing ? editing.price : "");
    const [priority, setPriority] = useState(editing ? editing.priority : "Medium");
    const [confirm, setConfirm] = useState(false);
    return (
      <>
        <div className="m3-toolbar"><Btn kind="outlined" className="m3-btn--sm" onClick={nav.pop}>Back</Btn></div>
        <div style={{ maxWidth: 460 }}>
          <div className="m3-textfield"><label>Title</label><input value={title} onChange={(e) => setTitle(e.target.value)} /></div>
          <div className="m3-textfield"><label>Description</label><input value={desc} onChange={(e) => setDesc(e.target.value)} /></div>
          <div className="m3-textfield"><label>Approximate price</label><input type="number" value={price} onChange={(e) => setPrice(e.target.value)} /></div>
          <div className="m3-textfield"><label>Priority</label>
            <select value={priority} onChange={(e) => setPriority(e.target.value)}>
              <option value="Small">Low</option><option value="Medium">Medium</option><option value="High">High</option><option value="Custom">Custom</option>
            </select>
          </div>
          <div style={{ display: "flex", gap: 8 }}>
            <Btn disabled={!title.trim()} onClick={nav.pop}>Save</Btn>
            {editing && <Btn kind="outlined" onClick={() => setConfirm(true)} style={{ color: "var(--m3-error)", borderColor: "var(--m3-error)" }}>Delete</Btn>}
          </div>
        </div>
        {confirm && (
          <div className="m3-scrim" onClick={() => setConfirm(false)}>
            <div className="m3-dialog" onClick={(e) => e.stopPropagation()}>
              <h3 className="m3-dialog__title">Delete item?</h3>
              <div>This item will be permanently removed. Continue?</div>
              <div className="m3-dialog__actions">
                <Btn kind="text" onClick={() => setConfirm(false)}>Cancel</Btn>
                <Btn kind="text" onClick={() => { setConfirm(false); nav.pop(); }} style={{ color: "var(--m3-error)" }}>Delete</Btn>
              </div>
            </div>
          </div>
        )}
      </>
    );
  }

  function LoginDialog({ onClose, onLogin, register }) {
    const [u, setU] = useState("you");
    const [p, setP] = useState("");
    return (
      <div className="m3-scrim" onClick={onClose}>
        <div className="m3-dialog" onClick={(e) => e.stopPropagation()}>
          <h3 className="m3-dialog__title">{register ? "Register" : "Log in"}</h3>
          <div className="m3-textfield"><label>Username</label><input value={u} onChange={(e) => setU(e.target.value)} /></div>
          <div className="m3-textfield"><label>Password</label><input type="password" value={p} onChange={(e) => setP(e.target.value)} /></div>
          <div className="m3-dialog__actions">
            <Btn kind="text" onClick={onClose}>Cancel</Btn>
            <Btn onClick={() => onLogin(u)}>{register ? "Create account" : "Log in"}</Btn>
          </div>
        </div>
      </div>
    );
  }

  const TITLES = {
    users: () => "Users",
    wishlists: (p) => { const u = D.usersById(p.userId); return u ? `${u.username}'s Wishlists` : "Wishlists"; },
    allItems: (p) => { const u = D.usersById(p.userId); return u ? `${u.username}'s wishes` : "All items"; },
    wishlist: (p) => { const w = D.wishlistById(p.wishlistId); return w ? w.title : "Wishlist"; },
    item: (p) => { const i = D.itemById(p.itemId); return i ? i.title : "Item"; },
    itemEdit: (p) => (p.itemId ? "Edit Item" : "New Item"),
  };
  const SCREENS = { users: Users, wishlists: UserWishlists, allItems: AllItems, wishlist: Wishlist, item: Item, itemEdit: ItemEdit };

  function MaterialApp() {
    const [stack, setStack] = useState([{ screen: "users", params: {} }]);
    const [me, setMe] = useState(null);
    const [login, setLogin] = useState(null);
    const nav = {
      push: (screen, params = {}) => setStack((s) => [...s, { screen, params }]),
      pop: () => setStack((s) => (s.length > 1 ? s.slice(0, -1) : s)),
    };
    const top = stack[stack.length - 1];
    const crumbs = stack.map((f) => TITLES[f.screen](f.params)).join(" / ");
    const Screen = SCREENS[top.screen];
    return (
      <div className="m3-root">
        <div className="m3-screen">
          <div className="m3-topbar">
            <div className="m3-topbar__title">{crumbs}</div>
            <div className="m3-topbar__actions">
              {me ? (
                <Btn className="m3-btn--sm" onClick={() => { setMe(null); setStack([{ screen: "users", params: {} }]); }}>Log out</Btn>
              ) : (
                <>
                  <Btn className="m3-btn--sm" onClick={() => setLogin({ register: false })}>Log in</Btn>
                  <Btn className="m3-btn--sm" onClick={() => setLogin({ register: true })}>Register</Btn>
                </>
              )}
            </div>
          </div>
          <div className="m3-scroll">
            <Screen me={me} params={top.params} nav={nav} />
          </div>
          {login && <LoginDialog register={login.register} onClose={() => setLogin(null)} onLogin={() => { setMe(D.usersById("u_you")); setLogin(null); }} />}
        </div>
      </div>
    );
  }

  window.MaterialApp = MaterialApp;
})();
