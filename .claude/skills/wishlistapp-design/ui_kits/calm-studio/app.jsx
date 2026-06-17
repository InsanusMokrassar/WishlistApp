/* Calm Studio — screens + app shell + routing. Mounted from index.html. */
const { useState: useS } = React;
const DT = window.CS_DATA;

function priceBig(it) { return it.price == null ? "No price" : `≈ ${it.price} ${it.units}`; }

/* ---------------- My Lists (home) ---------------- */
function HomeScreen({ me, nav, toast }) {
  if (!me) return <SignedOut nav={nav} />;
  const lists = DT.wishlistsByOwner(me.id);
  return (
    <div className="content-inner">
      <div className="pagehead">
        <div><h1>My Lists</h1><p className="subline">{lists.length} lists · your wishlists live here</p></div>
        <div className="acts"><button className="btn primary" onClick={() => toast("New list created (demo)")}><window.Icon name="plus" /> New list</button></div>
      </div>
      <div className="toolbar"><div /><div /></div>
      <div className="listgrid">
        {lists.map((w) => {
          const items = DT.itemsByWishlist(w.id);
          const reserved = items.filter((i) => DT.isReserved(i.id)).length;
          return (
            <div key={w.id} className="listcard" onClick={() => nav.go("list", { id: w.id })}>
              <div className="cover" style={{ background: w.cover }}>
                <span className="vis">{w.visibility === "Private" ? "Private" : "Public"}</span>
              </div>
              <div className="c">
                <h3>{w.title}</h3>
                <div className="meta">{items.length} items{reserved ? ` · ${reserved} reserved` : ""}</div>
              </div>
            </div>
          );
        })}
        <div className="listcard new" onClick={() => toast("New list created (demo)")}>
          <window.Icon name="plus" /> New list
        </div>
      </div>
    </div>
  );
}

/* ---------------- Discover (people) ---------------- */
function DiscoverScreen({ me, nav }) {
  const people = DT.users.filter((u) => !me || u.id !== me.id);
  return (
    <div className="content-inner">
      <div className="pagehead"><div><h1>Discover</h1><p className="subline">Browse people and their public wishlists</p></div></div>
      <div className="toolbar"><div /><div /></div>
      <div className="people">
        {people.map((u) => {
          const lists = DT.publicWishlistsByOwner(u.id);
          const items = lists.reduce((n, w) => n + DT.itemsByWishlist(w.id).length, 0);
          return (
            <div key={u.id} className="person" onClick={() => nav.go("profile", { id: u.id })}>
              <span className="av" style={{ background: u.tint }} />
              <h3>{u.name}</h3>
              <div className="meta">{lists.length} public lists · {items} items</div>
              {u.admin && <div className="adm">admin</div>}
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ---------------- Profile (a user's lists) ---------------- */
function ProfileScreen({ me, route, nav }) {
  const user = DT.usersById(route.id);
  const isMe = me && me.id === user.id;
  const lists = isMe ? DT.wishlistsByOwner(user.id) : DT.publicWishlistsByOwner(user.id);
  return (
    <div className="content-inner">
      <div className="crumb"><a onClick={() => nav.go("discover")}>Discover</a><span className="sep">/</span><b>{user.name}</b></div>
      <div className="pagehead">
        <div style={{ display: "flex", gap: 16, alignItems: "center" }}>
          <span className="av" style={{ width: 60, height: 60, borderRadius: 999, background: user.tint, display: "block" }} />
          <div><h1>{user.name}</h1><p className="subline">@{user.username} · {lists.length} {isMe ? "lists" : "public lists"}</p></div>
        </div>
      </div>
      <div className="toolbar"><div /><div /></div>
      <div className="listgrid">
        {lists.map((w) => {
          const items = DT.itemsByWishlist(w.id);
          return (
            <div key={w.id} className="listcard" onClick={() => nav.go("list", { id: w.id })}>
              <div className="cover" style={{ background: w.cover }}><span className="vis">{w.visibility}</span></div>
              <div className="c"><h3>{w.title}</h3><div className="meta">{items.length} items</div></div>
            </div>
          );
        })}
      </div>
    </div>
  );
}

/* ---------------- List detail ---------------- */
function ListScreen({ me, route, nav, toast }) {
  const w = DT.wishlistById(route.id);
  const owner = DT.usersById(w.ownerId);
  const isOwner = me && me.id === w.ownerId;
  const [view, setView] = useS("Grid");
  const [filter, setFilter] = useS("All");
  const [sort, setSort] = useS("Priority");
  const [, force] = useS(0);

  let items = DT.itemsByWishlist(w.id);
  if (filter === "Available") items = items.filter((i) => !DT.isReserved(i.id));
  if (filter === "Reserved") items = items.filter((i) => DT.isReserved(i.id));
  items = [...items].sort((a, b) =>
    sort === "Priority" ? DT.PRI[b.priority] - DT.PRI[a.priority]
      : sort === "Cost" ? (a.price || 0) - (b.price || 0)
      : sort === "Title" ? a.title.localeCompare(b.title) : 0);
  const reservedCount = DT.itemsByWishlist(w.id).filter((i) => DT.isReserved(i.id)).length;

  return (
    <div className="content-inner">
      <div className="crumb">
        {isOwner ? <a onClick={() => nav.go("home")}>My Lists</a> : <><a onClick={() => nav.go("discover")}>Discover</a><span className="sep">/</span><a onClick={() => nav.go("profile", { id: owner.id })}>{owner.name}</a></>}
        <span className="sep">/</span><b>{w.title}</b>
      </div>
      <div className="pagehead">
        <div>
          <h1>{w.title}</h1>
          <p className="subline">{DT.itemsByWishlist(w.id).length} items · {w.visibility.toLowerCase()}{reservedCount ? ` · ${reservedCount} reserved` : ""}{!isOwner ? ` · by ${owner.name}` : ""}</p>
        </div>
        <div className="acts">
          <button className="btn" onClick={() => toast("Link copied to clipboard")}><window.Icon name="share" /> Share</button>
          {isOwner
            ? <button className="btn primary" onClick={() => nav.go("itemEdit", { listId: w.id })}><window.Icon name="plus" /> Add item</button>
            : <button className="btn primary" onClick={() => toast(`“${w.title}” copied to your profile`)}>Copy to my profile</button>}
        </div>
      </div>

      <div className="toolbar">
        <div className="seg">
          {["All", "Available", "Reserved"].map((f) => <button key={f} className={filter === f ? "on" : ""} onClick={() => setFilter(f)}>{f}</button>)}
        </div>
        <div className="right">
          <select className="select" value={sort} onChange={(e) => setSort(e.target.value)}>
            {["Priority", "Cost", "Title"].map((s) => <option key={s}>{s}</option>)}
          </select>
          <div className="seg"><button className={view === "Grid" ? "on" : ""} onClick={() => setView("Grid")}>Grid</button><button className={view === "List" ? "on" : ""} onClick={() => setView("List")}>List</button></div>
        </div>
      </div>

      {items.length === 0 ? (
        <div className="empty">
          <div className="ic"><window.Icon name="gift" /></div>
          <h3>{filter === "Reserved" ? "Nothing reserved yet" : filter === "Available" ? "Everything's reserved" : "No items yet"}</h3>
          <p>{isOwner ? "Add the first thing you'd love to receive." : "Check back soon — this list is still being filled."}</p>
          {isOwner && filter === "All" && <button className="btn primary" onClick={() => nav.go("itemEdit", { listId: w.id })}><window.Icon name="plus" /> Add item</button>}
        </div>
      ) : view === "Grid" ? (
        <div className="grid">{items.map((it) => <window.ItemCard key={it.id} it={it} onOpen={() => nav.go("item", { id: it.id })} />)}</div>
      ) : (
        <div className="rows">{items.map((it) => <window.ItemRow key={it.id} it={it} onOpen={() => nav.go("item", { id: it.id })} />)}</div>
      )}
    </div>
  );
}

/* ---------------- Item detail ---------------- */
function ItemScreen({ me, route, nav, toast }) {
  const it = DT.itemById(route.id);
  const w = DT.wishlistById(it.wishlistId);
  const owner = DT.usersById(w.ownerId);
  const isOwner = me && me.id === w.ownerId;
  const [, force] = useS(0);
  const reserved = DT.isReserved(it.id);
  const reservedByYou = DT.reservations[it.id] === "you";

  const doReserve = () => {
    if (!me) return nav.openLogin();
    if (reservedByYou) { DT.unreserve(it.id); toast("Reservation cancelled"); }
    else { DT.reserve(it.id); toast("Reserved — only you can see this"); }
    force((n) => n + 1);
  };

  return (
    <div className="content-inner">
      <div className="crumb">
        {isOwner ? <a onClick={() => nav.go("home")}>My Lists</a> : <a onClick={() => nav.go("profile", { id: owner.id })}>{owner.name}</a>}
        <span className="sep">/</span><a onClick={() => nav.go("list", { id: w.id })}>{w.title}</a><span className="sep">/</span><b>{it.title}</b>
      </div>
      <div className="detail">
        <div className="gallery"><div className={"main-img " + it.tint} /></div>
        <div>
          <h1>{it.title}</h1>
          <div style={{ display: "flex", alignItems: "center", gap: 10, marginBottom: 18 }}>
            <window.PriorityPill p={it.priority} />
            {reserved && <span className="pill" style={{ background: "var(--ok-soft)", color: "var(--ok)" }}><span className="dot" style={{ background: "var(--ok)" }} />Reserved{reservedByYou ? " by you" : ""}</span>}
          </div>

          {!isOwner && (
            <div className="actbar">
              <button className={"btn " + (reservedByYou ? "" : "primary")} onClick={doReserve}>
                {reservedByYou ? "Cancel reservation" : reserved ? "Reserved by someone" : "Reserve this gift"}
              </button>
              <button className="btn" onClick={() => toast("Copied to your wishlist")}>Copy to my wishlist</button>
            </div>
          )}
          {isOwner && (
            <div className="actbar">
              <button className="btn" onClick={() => nav.go("itemEdit", { id: it.id, listId: w.id })}><window.Icon name="edit" /> Edit</button>
              {reserved && <span className="hint" style={{ alignSelf: "center" }}>Someone has reserved this — you won't see who.</span>}
            </div>
          )}

          {it.description && <div className="field"><div className="lbl">Description</div><div className="val">{it.description}</div></div>}
          <div className="field"><div className="lbl">Approximate price</div><div className="pricetag">{priceBig(it)}{it.amount > 1 ? <span style={{ fontSize: 15, color: "var(--muted)", fontWeight: 600 }}> · ×{it.amount}</span> : null}</div></div>
          <div className="field">
            <div className="lbl">Links</div>
            {it.links.length === 0 ? <div className="val" style={{ color: "var(--muted)" }}>No links</div>
              : it.links.map((l, i) => <a key={i} className="linkrow" href={l.url} target="_blank" rel="noreferrer">{l.title || l.url}<window.Icon name="ext" /></a>)}
          </div>
        </div>
      </div>
    </div>
  );
}

/* ---------------- Item edit ---------------- */
function ItemEditScreen({ route, nav, toast }) {
  const editing = route.id ? DT.itemById(route.id) : null;
  const w = DT.wishlistById(route.listId);
  const [title, setTitle] = useS(editing ? editing.title : "");
  const [desc, setDesc] = useS(editing ? editing.description : "");
  const [price, setPrice] = useS(editing ? editing.price : "");
  const [amount, setAmount] = useS(editing ? editing.amount : 1);
  const [priority, setPriority] = useS(editing ? editing.priority : "Medium");
  const [confirm, setConfirm] = useS(false);

  const save = () => {
    if (!editing) DT.addItem({ wishlistId: w.id, title, description: desc, price: Number(price) || null, units: w.units, amount: Number(amount) || 1, priority, links: [], tint: "t" + Math.floor(Math.random() * 8) });
    toast(editing ? "Changes saved" : "Item added");
    nav.go("list", { id: w.id });
  };

  return (
    <div className="content-inner">
      <div className="crumb"><a onClick={() => nav.go("list", { id: w.id })}>{w.title}</a><span className="sep">/</span><b>{editing ? "Edit item" : "New item"}</b></div>
      <div className="pagehead"><div><h1>{editing ? "Edit item" : "Add an item"}</h1><p className="subline">to “{w.title}”</p></div></div>
      <div className="form" style={{ marginTop: 22 }}>
        <div className="fieldset"><label>Title</label><input className="input" value={title} placeholder="What do you want?" onChange={(e) => setTitle(e.target.value)} /></div>
        <div className="fieldset"><label>Description</label><textarea className="textarea" value={desc} placeholder="Color, size, any details a gift-giver should know…" onChange={(e) => setDesc(e.target.value)} /></div>
        <div className="form-row">
          <div className="fieldset"><label>Approximate price ({w.units})</label><input className="input" type="number" value={price} placeholder="0" onChange={(e) => setPrice(e.target.value)} /></div>
          <div className="fieldset"><label>Amount</label><input className="input" type="number" min="1" value={amount} onChange={(e) => setAmount(e.target.value)} /></div>
        </div>
        <div className="fieldset">
          <label>Priority</label>
          <div className="priopts">{["Low", "Medium", "High"].map((p) => <div key={p} className={"priopt" + (priority === p ? " on" : "")} onClick={() => setPriority(p)}>{p}</div>)}</div>
          <div className="hint">High-priority items are shown first to gift-givers.</div>
        </div>
        <div style={{ display: "flex", gap: 9, marginTop: 24 }}>
          <button className="btn primary" disabled={!title.trim()} onClick={save}>{editing ? "Save changes" : "Add item"}</button>
          <button className="btn ghost" onClick={() => nav.go("list", { id: w.id })}>Cancel</button>
          <div style={{ flex: 1 }} />
          {editing && <button className="btn danger" onClick={() => setConfirm(true)}><window.Icon name="trash" /> Delete</button>}
        </div>
      </div>
      {confirm && (
        <ConfirmModal title="Delete item?" body="This item will be permanently removed. Continue?" confirmLabel="Delete" danger
          onCancel={() => setConfirm(false)} onConfirm={() => { DT.deleteItem(editing.id); toast("Item deleted"); nav.go("list", { id: w.id }); }} />
      )}
    </div>
  );
}

/* ---------------- Reserved ---------------- */
function ReservedScreen({ me, nav }) {
  if (!me) return <SignedOut nav={nav} />;
  const items = DT.reservedByYou();
  return (
    <div className="content-inner">
      <div className="pagehead"><div><h1>Reserved</h1><p className="subline">Gifts you've committed to give. Only you can see these.</p></div></div>
      <div className="toolbar"><div /><div /></div>
      {items.length === 0 ? (
        <div className="empty"><div className="ic"><window.Icon name="bookmark" /></div><h3>No reservations yet</h3><p>When you reserve a gift on someone's list, it shows up here.</p>
          <button className="btn primary" onClick={() => nav.go("discover")}><window.Icon name="compass" /> Browse people</button></div>
      ) : (
        <div className="grid">
          {items.map((it) => {
            const w = DT.wishlistById(it.wishlistId); const owner = DT.usersById(w.ownerId);
            return (
              <div key={it.id} className="card" onClick={() => nav.go("item", { id: it.id })}>
                <div className={"media " + it.tint}><span className="reserved-flag">For {owner.name.split(" ")[0]}</span></div>
                <div className="c"><h3>{it.title}</h3><p className="desc">{w.title} · {owner.name}</p><div className="price">{DT.priceText(it)}</div></div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

/* ---------------- Settings (light) ---------------- */
function SettingsScreen({ me, nav }) {
  if (!me) return <SignedOut nav={nav} />;
  return (
    <div className="content-inner">
      <div className="pagehead"><div><h1>Settings</h1><p className="subline">Your account and server</p></div></div>
      <div className="form" style={{ marginTop: 22 }}>
        <div className="fieldset"><label>Display name</label><input className="input" defaultValue={me.name} /></div>
        <div className="fieldset"><label>Username</label><input className="input" defaultValue={me.username} /></div>
        <div className="fieldset"><label>Default currency</label><select className="input"><option>USD</option><option>EUR</option><option>GBP</option></select></div>
        <div className="fieldset"><label>Server</label><input className="input" defaultValue="https://wish.myserver.home" /><div className="hint">Self-hosted — your data stays on your server.</div></div>
        <button className="btn primary" style={{ marginTop: 8 }}>Save changes</button>
      </div>
    </div>
  );
}

/* ---------------- shared bits ---------------- */
function SignedOut({ nav }) {
  return (
    <div className="content-inner"><div className="empty" style={{ paddingTop: 90 }}>
      <div className="ic"><window.Icon name="lock" /></div>
      <h3>Log in to see this</h3>
      <p>Your lists, reservations and settings are private to your account.</p>
      <button className="btn primary" onClick={() => nav.openLogin()}>Log in</button>
    </div></div>
  );
}
function ConfirmModal({ title, body, confirmLabel, danger, onCancel, onConfirm }) {
  return (
    <div className="scrim" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="mhead"><h2>{title}</h2><p>{body}</p></div>
        <div className="mfoot">
          <button className="btn ghost" onClick={onCancel}>Cancel</button>
          <button className={"btn " + (danger ? "danger" : "primary")} onClick={onConfirm}>{confirmLabel}</button>
        </div>
      </div>
    </div>
  );
}
function LoginModal({ onClose, onLogin }) {
  const [mode, setMode] = useS("login");
  const [u, setU] = useS("you");
  return (
    <div className="scrim" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <div className="mhead"><h2>{mode === "login" ? "Welcome back" : "Create account"}</h2><p>Self-hosted wishlists, just for your people.</p></div>
        <div className="mbody">
          <div className="tabs"><button className={mode === "login" ? "on" : ""} onClick={() => setMode("login")}>Log in</button><button className={mode === "register" ? "on" : ""} onClick={() => setMode("register")}>Register</button></div>
          <div className="fieldset"><label>Username</label><input className="input" value={u} onChange={(e) => setU(e.target.value)} /></div>
          <div className="fieldset"><label>Password</label><input className="input" type="password" defaultValue="" placeholder="••••••••" /></div>
        </div>
        <div className="mfoot"><button className="btn ghost" onClick={onClose}>Cancel</button><button className="btn primary" onClick={onLogin}>{mode === "login" ? "Log in" : "Create account"}</button></div>
      </div>
    </div>
  );
}

/* ---------------- search overlay ---------------- */
function SearchResults({ q, nav }) {
  const ql = q.toLowerCase();
  const people = DT.users.filter((u) => u.name.toLowerCase().includes(ql) || u.username.includes(ql));
  const lists = DT.wishlists.filter((w) => w.title.toLowerCase().includes(ql));
  const items = DT.items.filter((i) => i.title.toLowerCase().includes(ql));
  const Section = ({ label, children }) => <><div className="navlabel" style={{ paddingLeft: 0 }}>{label}</div>{children}</>;
  return (
    <div className="content-inner">
      <div className="pagehead"><div><h1>Results for “{q}”</h1><p className="subline">{people.length + lists.length + items.length} matches</p></div></div>
      <div style={{ marginTop: 18 }}>
        {people.length > 0 && <Section label="People"><div className="people" style={{ marginBottom: 22 }}>{people.map((u) => <div key={u.id} className="person" onClick={() => nav.go("profile", { id: u.id })}><span className="av" style={{ background: u.tint }} /><h3>{u.name}</h3><div className="meta">@{u.username}</div></div>)}</div></Section>}
        {lists.length > 0 && <Section label="Lists"><div className="listgrid" style={{ marginBottom: 22 }}>{lists.map((w) => { const o = DT.usersById(w.ownerId); return <div key={w.id} className="listcard" onClick={() => nav.go("list", { id: w.id })}><div className="cover" style={{ background: w.cover }}><span className="vis">{w.visibility}</span></div><div className="c"><h3>{w.title}</h3><div className="meta">{o.name}</div></div></div>; })}</div></Section>}
        {items.length > 0 && <Section label="Items"><div className="grid">{items.map((it) => <window.ItemCard key={it.id} it={it} onOpen={() => nav.go("item", { id: it.id })} />)}</div></Section>}
        {people.length + lists.length + items.length === 0 && <div className="empty"><div className="ic"><window.Icon name="search" /></div><h3>No matches</h3><p>Try a different name, list, or item.</p></div>}
      </div>
    </div>
  );
}

/* ---------------- App ---------------- */
const SCREENS = { home: HomeScreen, discover: DiscoverScreen, profile: ProfileScreen, list: ListScreen, item: ItemScreen, itemEdit: ItemEditScreen, reserved: ReservedScreen, settings: SettingsScreen };

const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "accent": "#5B5BD6",
  "density": "regular"
}/*EDITMODE-END*/;

const DENSITY = { compact: { gap: "10px", pad: "12px" }, regular: { gap: "14px", pad: "14px" }, comfy: { gap: "20px", pad: "18px" } };

function App() {
  const [tw, setTweak] = window.useTweaks(TWEAK_DEFAULTS);
  const [route, setRoute] = useS({ screen: "home" });
  const [me, setMe] = useS(DT.usersById("you"));
  const [login, setLogin] = useS(false);
  const [q, setQ] = useS("");
  const [toastMsg, setToastMsg] = useS(null);

  const toast = (m) => { setToastMsg(m); clearTimeout(window.__t); window.__t = setTimeout(() => setToastMsg(null), 2600); };
  const nav = {
    go: (screen, extra = {}) => { setQ(""); setRoute({ screen, ...extra }); document.querySelector(".content") && (document.querySelector(".content").scrollTop = 0); },
    openLogin: () => setLogin(true),
  };

  const Screen = SCREENS[route.screen] || HomeScreen;
  const dens = DENSITY[tw.density] || DENSITY.regular;
  const shellStyle = { "--accent": tw.accent, "--gap": dens.gap, "--card-pad": dens.pad };
  const primaryAction = route.screen === "home" && me
    ? <button className="btn primary" onClick={() => toast("New list created (demo)")}><window.Icon name="plus" /> New list</button>
    : null;

  return (
    <div className="app" style={shellStyle}>
      <window.Sidebar route={route} me={me} go={nav.go} openLogin={() => setLogin(true)} />
      <div className="main">
        <window.TopBar me={me} onSearch={setQ} openLogin={() => setLogin(true)} logout={() => { setMe(null); nav.go("discover"); }} primary={primaryAction} />
        <div className="content">
          {q.trim() ? <SearchResults q={q} nav={nav} /> : <Screen me={me} route={route} nav={nav} toast={toast} />}
        </div>
      </div>
      {login && <LoginModal onClose={() => setLogin(false)} onLogin={() => { setMe(DT.usersById("you")); setLogin(false); nav.go("home"); toast("Welcome back"); }} />}
      <div className={"toast" + (toastMsg ? " show" : "")}><span className="ok"><window.Icon name="check" /></span>{toastMsg}</div>
      <window.TweaksPanel>
        <window.TweakSection label="Theme" />
        <window.TweakColor label="Accent" value={tw.accent}
          options={["#5B5BD6", "#2A6FDB", "#1F8A5B", "#C75B39", "#7A5AE0", "#18181B"]}
          onChange={(v) => setTweak("accent", v)} />
        <window.TweakSection label="Layout" />
        <window.TweakRadio label="Density" value={tw.density}
          options={["compact", "regular", "comfy"]}
          onChange={(v) => setTweak("density", v)} />
      </window.TweaksPanel>
    </div>
  );
}

ReactDOM.createRoot(document.getElementById("root")).render(<App />);
