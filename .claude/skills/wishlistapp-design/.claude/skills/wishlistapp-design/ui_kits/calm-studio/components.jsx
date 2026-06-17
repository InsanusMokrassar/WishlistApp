/* Calm Studio — shared UI: icons, shells, cards. Exposes components on window. */
const { useState, useRef, useEffect } = React;
const D = window.CS_DATA;

/* ---------- icons (Lucide-style, 2px stroke) ---------- */
const PATHS = {
  home: <path d="M3 10.5 12 4l9 6.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z" />,
  compass: <><circle cx="12" cy="12" r="9" /><path d="m15.5 8.5-2 5-5 2 2-5z" /></>,
  bookmark: <path d="M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z" />,
  settings: <><circle cx="12" cy="12" r="3" /><path d="M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 0 1-4 0v-.1A1.6 1.6 0 0 0 7 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H1a2 2 0 0 1 0-4h.1A1.6 1.6 0 0 0 2.6 7a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H7a1.6 1.6 0 0 0 1-1.5V1a2 2 0 0 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V7a1.6 1.6 0 0 0 1.5 1H23a2 2 0 0 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z" /></>,
  search: <><circle cx="11" cy="11" r="7" /><path d="m20 20-3-3" /></>,
  plus: <path d="M12 5v14M5 12h14" />,
  list: <rect x="4" y="4" width="16" height="16" rx="3" />,
  share: <><circle cx="18" cy="5" r="3" /><circle cx="6" cy="12" r="3" /><circle cx="18" cy="19" r="3" /><path d="m8.6 13.5 6.8 4M15.4 6.5l-6.8 4" /></>,
  back: <path d="M19 12H5M12 19l-7-7 7-7" />,
  edit: <path d="M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z" />,
  trash: <path d="M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6" />,
  ext: <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14 21 3" />,
  check: <path d="M20 6 9 17l-5-5" />,
  gift: <><rect x="3" y="8" width="18" height="4" rx="1" /><path d="M12 8v13M5 12v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8M12 8S10.5 3 8 3a2.5 2.5 0 0 0 0 5M12 8s1.5-5 4-5a2.5 2.5 0 0 1 0 5" /></>,
  user: <><circle cx="12" cy="8" r="4" /><path d="M4 21a8 8 0 0 1 16 0" /></>,
  lock: <><rect x="5" y="11" width="14" height="10" rx="2" /><path d="M8 11V7a4 4 0 0 1 8 0v4" /></>,
};
function Icon({ name }) {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
      {PATHS[name]}
    </svg>
  );
}

const PRI_COLOR = { High: "var(--pri-high)", Medium: "var(--pri-med)", Low: "var(--pri-low)", Custom: "var(--pri-high)" };
function PriorityPill({ p }) {
  return <span className="pill"><span className="dot" style={{ background: PRI_COLOR[p] }} />{p}</span>;
}

/* ---------- sidebar ---------- */
function Sidebar({ route, me, go, openLogin }) {
  const myLists = me ? D.wishlistsByOwner(me.id) : [];
  const reservedCount = me ? D.reservedByYou().length : 0;
  const Item = ({ icon, label, to, count }) => (
    <button className={"navitem" + (route.screen === to ? " on" : "")} onClick={() => go(to)}>
      <Icon name={icon} /> {label}
      {count > 0 && <span className="count">{count}</span>}
    </button>
  );
  return (
    <aside className="sidebar">
      <div className="logo"><span className="mk"><Icon name="gift" /></span> wishlist</div>
      <nav className="navsec">
        <Item icon="home" label="My Lists" to="home" />
        <Item icon="compass" label="Discover" to="discover" />
        <Item icon="bookmark" label="Reserved" to="reserved" count={reservedCount} />
        <Item icon="settings" label="Settings" to="settings" />
      </nav>
      {me && myLists.length > 0 && (
        <nav className="navsec">
          <div className="navlabel">Your lists</div>
          {myLists.map((w) => (
            <button key={w.id} className={"navitem" + (route.screen === "list" && route.id === w.id ? " on" : "")} onClick={() => go("list", { id: w.id })}>
              <span className="swatch" style={{ background: w.cover }} /> {w.title}
            </button>
          ))}
          <button className="navitem" onClick={() => go("home")}><Icon name="plus" /> New list</button>
        </nav>
      )}
      <div className="spacer" />
      {me ? (
        <div className="me" onClick={() => go("profile", { id: me.id })}>
          <span className="av" style={{ background: me.tint }} />
          <span className="nm">{me.username}<small>View profile</small></span>
        </div>
      ) : (
        <button className="btn primary block" onClick={openLogin}>Log in</button>
      )}
    </aside>
  );
}

/* ---------- top bar ---------- */
function TopBar({ me, onSearch, openLogin, logout, primary }) {
  const [q, setQ] = useState("");
  const ref = useRef(null);
  useEffect(() => {
    const h = (e) => { if ((e.metaKey || e.ctrlKey) && e.key === "k") { e.preventDefault(); ref.current && ref.current.focus(); } };
    window.addEventListener("keydown", h);
    return () => window.removeEventListener("keydown", h);
  }, []);
  return (
    <div className="topbar">
      <label className="search">
        <Icon name="search" />
        <input ref={ref} value={q} placeholder="Search people, lists, items…"
          onChange={(e) => { setQ(e.target.value); onSearch(e.target.value); }} />
        <span className="kbd">⌘K</span>
      </label>
      <div className="sp" />
      {primary}
      {me ? (
        <button className="btn ghost" onClick={logout}>Log out</button>
      ) : (
        <button className="btn" onClick={openLogin}>Log in</button>
      )}
    </div>
  );
}

/* ---------- item card / row ---------- */
function ItemCard({ it, onOpen, viewerCanReserve }) {
  const reserved = D.isReserved(it.id);
  return (
    <div className="card" onClick={onOpen}>
      <div className={"media " + it.tint}>
        {reserved
          ? <span className="reserved-flag">Reserved</span>
          : <span className="badge"><span className="dot" style={{ background: PRI_COLOR[it.priority] }} />{it.priority}</span>}
      </div>
      <div className="c">
        <h3>{it.title}</h3>
        {it.description && <p className="desc">{it.description}</p>}
        <div className="price">{D.priceText(it)}</div>
      </div>
    </div>
  );
}
function ItemRow({ it, onOpen }) {
  const reserved = D.isReserved(it.id);
  return (
    <div className="row" onClick={onOpen}>
      <span className={"thumb " + it.tint} />
      <div className="rmain">
        <div style={{ display: "flex", alignItems: "center", gap: 9 }}>
          <h3>{it.title}</h3>
          {reserved ? <span className="pill" style={{ background: "var(--ok-soft)", color: "var(--ok)" }}><span className="dot" style={{ background: "var(--ok)" }} />Reserved</span> : <PriorityPill p={it.priority} />}
        </div>
        {it.description && <p className="desc">{it.description}</p>}
      </div>
      <span className="rprice">{D.priceText(it)}</span>
    </div>
  );
}

Object.assign(window, { Icon, PriorityPill, Sidebar, TopBar, ItemCard, ItemRow, PRI_COLOR });
