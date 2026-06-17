/* @ds-bundle: {"format":3,"namespace":"WishlistApp_ef9ce8","components":[{"name":"Button","sourcePath":"components/actions/Button.jsx"},{"name":"Avatar","sourcePath":"components/data-display/Avatar.jsx"},{"name":"Badge","sourcePath":"components/data-display/Badge.jsx"},{"name":"ItemCard","sourcePath":"components/data-display/ItemCard.jsx"},{"name":"ListRow","sourcePath":"components/data-display/ListRow.jsx"},{"name":"PriorityBadge","sourcePath":"components/data-display/PriorityBadge.jsx"},{"name":"Alert","sourcePath":"components/feedback/Alert.jsx"},{"name":"Modal","sourcePath":"components/feedback/Modal.jsx"},{"name":"Input","sourcePath":"components/forms/Input.jsx"},{"name":"Select","sourcePath":"components/forms/Select.jsx"},{"name":"NavBar","sourcePath":"components/navigation/NavBar.jsx"}],"sourceHashes":{"components/actions/Button.jsx":"68d836ff1eee","components/data-display/Avatar.jsx":"114e9e5c875e","components/data-display/Badge.jsx":"c4caa5d8fb9d","components/data-display/ItemCard.jsx":"b51696b3607b","components/data-display/ListRow.jsx":"91d1dde8a389","components/data-display/PriorityBadge.jsx":"e3e90300c6a0","components/feedback/Alert.jsx":"633cc391d4df","components/feedback/Modal.jsx":"701052c4609d","components/forms/Input.jsx":"65d36c8aff6a","components/forms/Select.jsx":"e036763df9a9","components/navigation/NavBar.jsx":"32e28a5c4968","ui_kits/calm-studio/app.jsx":"b62c0932befc","ui_kits/calm-studio/components.jsx":"5dc21547654d","ui_kits/calm-studio/data.js":"b5c74e5bf006","ui_kits/calm-studio/tweaks-panel.jsx":"6591467622ed","ui_kits/material/android-frame.jsx":"70c8c3059eeb","ui_kits/material/material-app.jsx":"1cb45ac33aaa","ui_kits/web/app.jsx":"b10631511884","ui_kits/web/data.js":"dfd9a74f2183"},"inlinedExternals":[],"unexposedExports":[]} */

(() => {

const __ds_ns = (window.WishlistApp_ef9ce8 = window.WishlistApp_ef9ce8 || {});

const __ds_scope = {};

(__ds_ns.__errors = __ds_ns.__errors || []);

// components/actions/Button.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Bootstrap button used across every WishlistApp screen. Solid variants drive
 * primary flows (Save = primary, Add Item / Copy = success, Delete = danger);
 * outline variants are the lighter-weight toolbar actions (Back, Edit). The
 * `outline-light` variant is reserved for the blue navbar (Log in / Log out).
 */
function Button({
  variant = "primary",
  size,
  type = "button",
  disabled = false,
  className = "",
  children,
  ...rest
}) {
  const classes = ["btn", `btn-${variant}`];
  if (size) classes.push(`btn-${size}`);
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("button", _extends({
    type: type,
    className: classes.join(" "),
    disabled: disabled
  }, rest), children);
}
Object.assign(__ds_scope, { Button });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/actions/Button.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Avatar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const SILHOUETTE = "data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Crect width='100' height='100' fill='%23d0d3d8'/%3E%3Ccircle cx='50' cy='38' r='18' fill='%23f1f3f5'/%3E%3Cpath d='M20 86 a30 30 0 0 1 60 0 Z' fill='%23f1f3f5'/%3E%3C/svg%3E";

/**
 * User avatar. Shows the uploaded photo when `src` is given, otherwise the
 * default gray silhouette placeholder. Circle shape for list rows and the
 * navbar; rounded-square for large profile previews. Sized by `size` (px).
 */
function Avatar({
  src,
  alt = "",
  size = 48,
  circle = true,
  className = "",
  ...rest
}) {
  const classes = [circle ? "rounded-circle" : "rounded", "flex-shrink-0"];
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("img", _extends({
    src: src || SILHOUETTE,
    alt: alt,
    width: size,
    height: size,
    className: classes.join(" "),
    style: {
      width: size,
      height: size,
      objectFit: "cover"
    }
  }, rest));
}
Object.assign(__ds_scope, { Avatar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Avatar.jsx", error: String((e && e.message) || e) }); }

// components/data-display/Badge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Generic Bootstrap badge. WishlistApp uses badges sparingly for small counts
 * and labels. For item priority, prefer the dedicated PriorityBadge. Set `pill`
 * for the rounded-pill shape; `bg` accepts a Bootstrap color or a *-subtle name.
 */
function Badge({
  bg = "secondary",
  pill = false,
  className = "",
  children,
  ...rest
}) {
  const subtle = bg.endsWith("-subtle");
  const classes = ["badge", `bg-${bg}`];
  if (subtle) classes.push(`text-${bg.replace("-subtle", "")}-emphasis`);
  if (pill) classes.push("rounded-pill");
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("span", _extends({
    className: classes.join(" ")
  }, rest), children);
}
Object.assign(__ds_scope, { Badge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/Badge.jsx", error: String((e && e.message) || e) }); }

// components/data-display/ListRow.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * A single Bootstrap list-group row — the workhorse layout for the users list,
 * wishlists list, and link lists. Optional `leading` (avatar/thumbnail) and
 * `trailing` (action buttons) slots flank the primary content. Must be rendered
 * inside a `<ul className="list-group">`. The row is clickable when `onSelect`
 * is set.
 */
function ListRow({
  onSelect,
  leading,
  trailing,
  children,
  className = "",
  ...rest
}) {
  const classes = ["list-group-item", "list-group-item-action", "d-flex", "justify-content-between", "align-items-center"];
  if (className) classes.push(className);
  const bare = !leading && !trailing;
  return /*#__PURE__*/React.createElement("li", _extends({
    className: classes.join(" "),
    style: bare && onSelect ? {
      cursor: "pointer"
    } : undefined,
    onClick: bare ? onSelect : undefined
  }, rest), bare ? children : /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    className: "d-flex align-items-center gap-3 flex-grow-1",
    style: onSelect ? {
      cursor: "pointer"
    } : undefined,
    onClick: onSelect
  }, leading, /*#__PURE__*/React.createElement("div", {
    className: "flex-grow-1"
  }, children)), trailing));
}
Object.assign(__ds_scope, { ListRow });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/ListRow.jsx", error: String((e && e.message) || e) }); }

// components/data-display/PriorityBadge.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const LABELS = {
  Small: "Low",
  Medium: "Medium",
  High: "High",
  Custom: "Custom"
};

/**
 * Pill badge showing a wishlist item's priority. Renders the localized preset
 * label; for a Custom priority it appends the weight in parentheses, e.g.
 * "Custom (42)". Used on item cards (overlaid top-right), list rows, and the
 * item detail screen. Visual is intentionally neutral (subtle gray pill) so it
 * reads as metadata, not a status alarm.
 */
function PriorityBadge({
  priority = "Medium",
  weight,
  className = "",
  ...rest
}) {
  const label = LABELS[priority] || priority;
  const suffix = priority === "Custom" && weight != null ? ` (${weight})` : "";
  const classes = ["badge", "rounded-pill", "bg-secondary-subtle", "text-secondary-emphasis"];
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("span", _extends({
    className: classes.join(" ")
  }, rest), label, suffix);
}
Object.assign(__ds_scope, { PriorityBadge });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/PriorityBadge.jsx", error: String((e && e.message) || e) }); }

// components/data-display/ItemCard.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
const GIFTBOX = "data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Crect width='100' height='100' fill='%23eef1f4'/%3E%3Crect x='24' y='44' width='52' height='38' rx='3' fill='%23c0c6cf'/%3E%3Crect x='20' y='36' width='60' height='14' rx='3' fill='%23a8b0bb'/%3E%3Crect x='46' y='36' width='8' height='46' fill='%238b94a1'/%3E%3Cpath d='M50 36 C40 24 26 30 50 36 C74 30 60 24 50 36 Z' fill='%238b94a1'/%3E%3C/svg%3E";

/**
 * Bootstrap card presenting a single wishlist item — the building block of the
 * grid view. Media = first image (or the gift-box placeholder), title = item
 * title, subtitle = owning wishlist, body = description, footer = price text.
 * The priority pill is overlaid in the top-right corner. The whole card is
 * clickable.
 */
function ItemCard({
  title,
  wishlistTitle,
  description,
  priceText,
  priority = "Medium",
  weight,
  imageUrl,
  onSelect,
  className = "",
  ...rest
}) {
  const classes = ["card", "h-100", "position-relative"];
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("div", _extends({
    className: classes.join(" "),
    style: {
      cursor: onSelect ? "pointer" : undefined
    },
    onClick: onSelect
  }, rest), /*#__PURE__*/React.createElement("div", {
    className: "position-absolute top-0 end-0 m-2"
  }, /*#__PURE__*/React.createElement(__ds_scope.PriorityBadge, {
    priority: priority,
    weight: weight
  })), /*#__PURE__*/React.createElement("img", {
    src: imageUrl || GIFTBOX,
    alt: "",
    className: "card-img-top",
    style: {
      width: "100%",
      height: "var(--wl-card-media-h, 180px)",
      objectFit: imageUrl ? "cover" : "contain",
      backgroundColor: "var(--wl-surface-placeholder, #eef1f4)"
    }
  }), /*#__PURE__*/React.createElement("div", {
    className: "card-body"
  }, /*#__PURE__*/React.createElement("h5", {
    className: "card-title"
  }, title), wishlistTitle && /*#__PURE__*/React.createElement("h6", {
    className: "card-subtitle mb-2 text-muted"
  }, wishlistTitle), description && /*#__PURE__*/React.createElement("p", {
    className: "card-text"
  }, description)), priceText && /*#__PURE__*/React.createElement("div", {
    className: "card-footer text-muted small"
  }, priceText));
}
Object.assign(__ds_scope, { ItemCard });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/data-display/ItemCard.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Alert.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Bootstrap contextual alert. WishlistApp uses success alerts to confirm queued
 * copies and danger alerts for failed actions. Compact (`py-2`) by default to
 * match the app's inline banners.
 */
function Alert({
  variant = "success",
  className = "",
  children,
  ...rest
}) {
  const classes = ["alert", `alert-${variant}`, "py-2"];
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("div", _extends({
    className: classes.join(" "),
    role: "alert"
  }, rest), children);
}
Object.assign(__ds_scope, { Alert });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Alert.jsx", error: String((e && e.message) || e) }); }

// components/feedback/Modal.jsx
try { (() => {
/**
 * Bootstrap modal dialog (centered, with backdrop) — the app's pattern for the
 * login/register form and for destructive confirmations (Discard changes?,
 * Delete item?). Render conditionally when `open` is true. `footer` overrides
 * the default Cancel/Confirm pair built from the `*Label` / `on*` props.
 */
function Modal({
  open = true,
  title,
  children,
  onCancel,
  onConfirm,
  cancelLabel = "Cancel",
  confirmLabel = "OK",
  confirmVariant = "primary",
  footer,
  centered = true
}) {
  if (!open) return null;
  const dialogClasses = ["modal-dialog"];
  if (centered) dialogClasses.push("modal-dialog-centered");
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    className: "modal-backdrop fade show"
  }), /*#__PURE__*/React.createElement("div", {
    className: "modal fade show d-block",
    tabIndex: -1
  }, /*#__PURE__*/React.createElement("div", {
    className: dialogClasses.join(" ")
  }, /*#__PURE__*/React.createElement("div", {
    className: "modal-content"
  }, /*#__PURE__*/React.createElement("div", {
    className: "modal-header"
  }, /*#__PURE__*/React.createElement("span", {
    className: "modal-title h5"
  }, title), onCancel && /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: "btn-close",
    "aria-label": "Close",
    onClick: onCancel
  })), /*#__PURE__*/React.createElement("div", {
    className: "modal-body"
  }, children), /*#__PURE__*/React.createElement("div", {
    className: "modal-footer"
  }, footer || /*#__PURE__*/React.createElement(React.Fragment, null, onCancel && /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: "secondary",
    onClick: onCancel
  }, cancelLabel), onConfirm && /*#__PURE__*/React.createElement(__ds_scope.Button, {
    variant: confirmVariant,
    onClick: onConfirm
  }, confirmLabel)))))));
}
Object.assign(__ds_scope, { Modal });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/feedback/Modal.jsx", error: String((e && e.message) || e) }); }

// components/forms/Input.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Labeled Bootstrap text input (`form-control`). Wraps the input in the app's
 * standard `mb-3` field block with an optional label and helper text. Used on
 * the wishlist/item edit screens and the login form.
 */
function Input({
  label,
  id,
  type = "text",
  value,
  placeholder,
  onChange,
  disabled = false,
  help,
  className = "",
  ...rest
}) {
  const classes = ["form-control"];
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("div", {
    className: "mb-3"
  }, label && /*#__PURE__*/React.createElement("label", {
    htmlFor: id,
    className: "form-label"
  }, label), /*#__PURE__*/React.createElement("input", _extends({
    id: id,
    type: type,
    className: classes.join(" "),
    value: value,
    placeholder: placeholder,
    onChange: onChange,
    disabled: disabled
  }, rest)), help && /*#__PURE__*/React.createElement("div", {
    className: "form-text"
  }, help));
}
Object.assign(__ds_scope, { Input });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Input.jsx", error: String((e && e.message) || e) }); }

// components/forms/Select.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * Labeled Bootstrap select (`form-select`) in the standard `mb-3` field block.
 * Drives the sort, view-mode, and currency selectors. `options` is a list of
 * `{ value, label }`; a bare string is used as both.
 */
function Select({
  label,
  id,
  value,
  options = [],
  onChange,
  disabled = false,
  size,
  className = "",
  ...rest
}) {
  const classes = ["form-select"];
  if (size) classes.push(`form-select-${size}`);
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("div", {
    className: "mb-3"
  }, label && /*#__PURE__*/React.createElement("label", {
    htmlFor: id,
    className: "form-label"
  }, label), /*#__PURE__*/React.createElement("select", _extends({
    id: id,
    className: classes.join(" "),
    value: value,
    onChange: onChange,
    disabled: disabled
  }, rest), options.map(o => {
    const opt = typeof o === "string" ? {
      value: o,
      label: o
    } : o;
    return /*#__PURE__*/React.createElement("option", {
      key: opt.value,
      value: opt.value
    }, opt.label);
  })));
}
Object.assign(__ds_scope, { Select });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/forms/Select.jsx", error: String((e && e.message) || e) }); }

// components/navigation/NavBar.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/**
 * The WishlistApp top navigation bar: a blue (`bg-primary`) dark navbar whose
 * brand shows the current screen's breadcrumb title (segments joined with " / ")
 * and whose right side holds auth actions. `title` accepts a string or an array
 * of breadcrumb segments. `actions` is the right-aligned slot (Log in / Log out).
 */
function NavBar({
  title = "Wishlists",
  actions,
  className = "",
  ...rest
}) {
  const text = Array.isArray(title) ? title.filter(Boolean).join(" / ") : title;
  const classes = ["navbar", "navbar-expand", "navbar-dark", "bg-primary"];
  if (className) classes.push(className);
  return /*#__PURE__*/React.createElement("nav", _extends({
    className: classes.join(" ")
  }, rest), /*#__PURE__*/React.createElement("div", {
    className: "container-fluid"
  }, /*#__PURE__*/React.createElement("a", {
    href: "#",
    className: "navbar-brand"
  }, text), /*#__PURE__*/React.createElement("div", {
    className: "d-flex gap-2"
  }, actions)));
}
Object.assign(__ds_scope, { NavBar });
})(); } catch (e) { __ds_ns.__errors.push({ path: "components/navigation/NavBar.jsx", error: String((e && e.message) || e) }); }

// ui_kits/calm-studio/app.jsx
try { (() => {
/* Calm Studio — screens + app shell + routing. Mounted from index.html. */
const {
  useState: useS
} = React;
const DT = window.CS_DATA;
function priceBig(it) {
  return it.price == null ? "No price" : `≈ ${it.price} ${it.units}`;
}

/* ---------------- My Lists (home) ---------------- */
function HomeScreen({
  me,
  nav,
  toast
}) {
  if (!me) return /*#__PURE__*/React.createElement(SignedOut, {
    nav: nav
  });
  const lists = DT.wishlistsByOwner(me.id);
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "pagehead"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, "My Lists"), /*#__PURE__*/React.createElement("p", {
    className: "subline"
  }, lists.length, " lists \xB7 your wishlists live here")), /*#__PURE__*/React.createElement("div", {
    className: "acts"
  }, /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    onClick: () => toast("New list created (demo)")
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "plus"
  }), " New list"))), /*#__PURE__*/React.createElement("div", {
    className: "toolbar"
  }, /*#__PURE__*/React.createElement("div", null), /*#__PURE__*/React.createElement("div", null)), /*#__PURE__*/React.createElement("div", {
    className: "listgrid"
  }, lists.map(w => {
    const items = DT.itemsByWishlist(w.id);
    const reserved = items.filter(i => DT.isReserved(i.id)).length;
    return /*#__PURE__*/React.createElement("div", {
      key: w.id,
      className: "listcard",
      onClick: () => nav.go("list", {
        id: w.id
      })
    }, /*#__PURE__*/React.createElement("div", {
      className: "cover",
      style: {
        background: w.cover
      }
    }, /*#__PURE__*/React.createElement("span", {
      className: "vis"
    }, w.visibility === "Private" ? "Private" : "Public")), /*#__PURE__*/React.createElement("div", {
      className: "c"
    }, /*#__PURE__*/React.createElement("h3", null, w.title), /*#__PURE__*/React.createElement("div", {
      className: "meta"
    }, items.length, " items", reserved ? ` · ${reserved} reserved` : "")));
  }), /*#__PURE__*/React.createElement("div", {
    className: "listcard new",
    onClick: () => toast("New list created (demo)")
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "plus"
  }), " New list")));
}

/* ---------------- Discover (people) ---------------- */
function DiscoverScreen({
  me,
  nav
}) {
  const people = DT.users.filter(u => !me || u.id !== me.id);
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "pagehead"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, "Discover"), /*#__PURE__*/React.createElement("p", {
    className: "subline"
  }, "Browse people and their public wishlists"))), /*#__PURE__*/React.createElement("div", {
    className: "toolbar"
  }, /*#__PURE__*/React.createElement("div", null), /*#__PURE__*/React.createElement("div", null)), /*#__PURE__*/React.createElement("div", {
    className: "people"
  }, people.map(u => {
    const lists = DT.publicWishlistsByOwner(u.id);
    const items = lists.reduce((n, w) => n + DT.itemsByWishlist(w.id).length, 0);
    return /*#__PURE__*/React.createElement("div", {
      key: u.id,
      className: "person",
      onClick: () => nav.go("profile", {
        id: u.id
      })
    }, /*#__PURE__*/React.createElement("span", {
      className: "av",
      style: {
        background: u.tint
      }
    }), /*#__PURE__*/React.createElement("h3", null, u.name), /*#__PURE__*/React.createElement("div", {
      className: "meta"
    }, lists.length, " public lists \xB7 ", items, " items"), u.admin && /*#__PURE__*/React.createElement("div", {
      className: "adm"
    }, "admin"));
  })));
}

/* ---------------- Profile (a user's lists) ---------------- */
function ProfileScreen({
  me,
  route,
  nav
}) {
  const user = DT.usersById(route.id);
  const isMe = me && me.id === user.id;
  const lists = isMe ? DT.wishlistsByOwner(user.id) : DT.publicWishlistsByOwner(user.id);
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "crumb"
  }, /*#__PURE__*/React.createElement("a", {
    onClick: () => nav.go("discover")
  }, "Discover"), /*#__PURE__*/React.createElement("span", {
    className: "sep"
  }, "/"), /*#__PURE__*/React.createElement("b", null, user.name)), /*#__PURE__*/React.createElement("div", {
    className: "pagehead"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 16,
      alignItems: "center"
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "av",
    style: {
      width: 60,
      height: 60,
      borderRadius: 999,
      background: user.tint,
      display: "block"
    }
  }), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, user.name), /*#__PURE__*/React.createElement("p", {
    className: "subline"
  }, "@", user.username, " \xB7 ", lists.length, " ", isMe ? "lists" : "public lists")))), /*#__PURE__*/React.createElement("div", {
    className: "toolbar"
  }, /*#__PURE__*/React.createElement("div", null), /*#__PURE__*/React.createElement("div", null)), /*#__PURE__*/React.createElement("div", {
    className: "listgrid"
  }, lists.map(w => {
    const items = DT.itemsByWishlist(w.id);
    return /*#__PURE__*/React.createElement("div", {
      key: w.id,
      className: "listcard",
      onClick: () => nav.go("list", {
        id: w.id
      })
    }, /*#__PURE__*/React.createElement("div", {
      className: "cover",
      style: {
        background: w.cover
      }
    }, /*#__PURE__*/React.createElement("span", {
      className: "vis"
    }, w.visibility)), /*#__PURE__*/React.createElement("div", {
      className: "c"
    }, /*#__PURE__*/React.createElement("h3", null, w.title), /*#__PURE__*/React.createElement("div", {
      className: "meta"
    }, items.length, " items")));
  })));
}

/* ---------------- List detail ---------------- */
function ListScreen({
  me,
  route,
  nav,
  toast
}) {
  const w = DT.wishlistById(route.id);
  const owner = DT.usersById(w.ownerId);
  const isOwner = me && me.id === w.ownerId;
  const [view, setView] = useS("Grid");
  const [filter, setFilter] = useS("All");
  const [sort, setSort] = useS("Priority");
  const [, force] = useS(0);
  let items = DT.itemsByWishlist(w.id);
  if (filter === "Available") items = items.filter(i => !DT.isReserved(i.id));
  if (filter === "Reserved") items = items.filter(i => DT.isReserved(i.id));
  items = [...items].sort((a, b) => sort === "Priority" ? DT.PRI[b.priority] - DT.PRI[a.priority] : sort === "Cost" ? (a.price || 0) - (b.price || 0) : sort === "Title" ? a.title.localeCompare(b.title) : 0);
  const reservedCount = DT.itemsByWishlist(w.id).filter(i => DT.isReserved(i.id)).length;
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "crumb"
  }, isOwner ? /*#__PURE__*/React.createElement("a", {
    onClick: () => nav.go("home")
  }, "My Lists") : /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("a", {
    onClick: () => nav.go("discover")
  }, "Discover"), /*#__PURE__*/React.createElement("span", {
    className: "sep"
  }, "/"), /*#__PURE__*/React.createElement("a", {
    onClick: () => nav.go("profile", {
      id: owner.id
    })
  }, owner.name)), /*#__PURE__*/React.createElement("span", {
    className: "sep"
  }, "/"), /*#__PURE__*/React.createElement("b", null, w.title)), /*#__PURE__*/React.createElement("div", {
    className: "pagehead"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, w.title), /*#__PURE__*/React.createElement("p", {
    className: "subline"
  }, DT.itemsByWishlist(w.id).length, " items \xB7 ", w.visibility.toLowerCase(), reservedCount ? ` · ${reservedCount} reserved` : "", !isOwner ? ` · by ${owner.name}` : "")), /*#__PURE__*/React.createElement("div", {
    className: "acts"
  }, /*#__PURE__*/React.createElement("button", {
    className: "btn",
    onClick: () => toast("Link copied to clipboard")
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "share"
  }), " Share"), isOwner ? /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    onClick: () => nav.go("itemEdit", {
      listId: w.id
    })
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "plus"
  }), " Add item") : /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    onClick: () => toast(`“${w.title}” copied to your profile`)
  }, "Copy to my profile"))), /*#__PURE__*/React.createElement("div", {
    className: "toolbar"
  }, /*#__PURE__*/React.createElement("div", {
    className: "seg"
  }, ["All", "Available", "Reserved"].map(f => /*#__PURE__*/React.createElement("button", {
    key: f,
    className: filter === f ? "on" : "",
    onClick: () => setFilter(f)
  }, f))), /*#__PURE__*/React.createElement("div", {
    className: "right"
  }, /*#__PURE__*/React.createElement("select", {
    className: "select",
    value: sort,
    onChange: e => setSort(e.target.value)
  }, ["Priority", "Cost", "Title"].map(s => /*#__PURE__*/React.createElement("option", {
    key: s
  }, s))), /*#__PURE__*/React.createElement("div", {
    className: "seg"
  }, /*#__PURE__*/React.createElement("button", {
    className: view === "Grid" ? "on" : "",
    onClick: () => setView("Grid")
  }, "Grid"), /*#__PURE__*/React.createElement("button", {
    className: view === "List" ? "on" : "",
    onClick: () => setView("List")
  }, "List")))), items.length === 0 ? /*#__PURE__*/React.createElement("div", {
    className: "empty"
  }, /*#__PURE__*/React.createElement("div", {
    className: "ic"
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "gift"
  })), /*#__PURE__*/React.createElement("h3", null, filter === "Reserved" ? "Nothing reserved yet" : filter === "Available" ? "Everything's reserved" : "No items yet"), /*#__PURE__*/React.createElement("p", null, isOwner ? "Add the first thing you'd love to receive." : "Check back soon — this list is still being filled."), isOwner && filter === "All" && /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    onClick: () => nav.go("itemEdit", {
      listId: w.id
    })
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "plus"
  }), " Add item")) : view === "Grid" ? /*#__PURE__*/React.createElement("div", {
    className: "grid"
  }, items.map(it => /*#__PURE__*/React.createElement(window.ItemCard, {
    key: it.id,
    it: it,
    onOpen: () => nav.go("item", {
      id: it.id
    })
  }))) : /*#__PURE__*/React.createElement("div", {
    className: "rows"
  }, items.map(it => /*#__PURE__*/React.createElement(window.ItemRow, {
    key: it.id,
    it: it,
    onOpen: () => nav.go("item", {
      id: it.id
    })
  }))));
}

/* ---------------- Item detail ---------------- */
function ItemScreen({
  me,
  route,
  nav,
  toast
}) {
  const it = DT.itemById(route.id);
  const w = DT.wishlistById(it.wishlistId);
  const owner = DT.usersById(w.ownerId);
  const isOwner = me && me.id === w.ownerId;
  const [, force] = useS(0);
  const reserved = DT.isReserved(it.id);
  const reservedByYou = DT.reservations[it.id] === "you";
  const doReserve = () => {
    if (!me) return nav.openLogin();
    if (reservedByYou) {
      DT.unreserve(it.id);
      toast("Reservation cancelled");
    } else {
      DT.reserve(it.id);
      toast("Reserved — only you can see this");
    }
    force(n => n + 1);
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "crumb"
  }, isOwner ? /*#__PURE__*/React.createElement("a", {
    onClick: () => nav.go("home")
  }, "My Lists") : /*#__PURE__*/React.createElement("a", {
    onClick: () => nav.go("profile", {
      id: owner.id
    })
  }, owner.name), /*#__PURE__*/React.createElement("span", {
    className: "sep"
  }, "/"), /*#__PURE__*/React.createElement("a", {
    onClick: () => nav.go("list", {
      id: w.id
    })
  }, w.title), /*#__PURE__*/React.createElement("span", {
    className: "sep"
  }, "/"), /*#__PURE__*/React.createElement("b", null, it.title)), /*#__PURE__*/React.createElement("div", {
    className: "detail"
  }, /*#__PURE__*/React.createElement("div", {
    className: "gallery"
  }, /*#__PURE__*/React.createElement("div", {
    className: "main-img " + it.tint
  })), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, it.title), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 10,
      marginBottom: 18
    }
  }, /*#__PURE__*/React.createElement(window.PriorityPill, {
    p: it.priority
  }), reserved && /*#__PURE__*/React.createElement("span", {
    className: "pill",
    style: {
      background: "var(--ok-soft)",
      color: "var(--ok)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "dot",
    style: {
      background: "var(--ok)"
    }
  }), "Reserved", reservedByYou ? " by you" : "")), !isOwner && /*#__PURE__*/React.createElement("div", {
    className: "actbar"
  }, /*#__PURE__*/React.createElement("button", {
    className: "btn " + (reservedByYou ? "" : "primary"),
    onClick: doReserve
  }, reservedByYou ? "Cancel reservation" : reserved ? "Reserved by someone" : "Reserve this gift"), /*#__PURE__*/React.createElement("button", {
    className: "btn",
    onClick: () => toast("Copied to your wishlist")
  }, "Copy to my wishlist")), isOwner && /*#__PURE__*/React.createElement("div", {
    className: "actbar"
  }, /*#__PURE__*/React.createElement("button", {
    className: "btn",
    onClick: () => nav.go("itemEdit", {
      id: it.id,
      listId: w.id
    })
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "edit"
  }), " Edit"), reserved && /*#__PURE__*/React.createElement("span", {
    className: "hint",
    style: {
      alignSelf: "center"
    }
  }, "Someone has reserved this \u2014 you won't see who.")), it.description && /*#__PURE__*/React.createElement("div", {
    className: "field"
  }, /*#__PURE__*/React.createElement("div", {
    className: "lbl"
  }, "Description"), /*#__PURE__*/React.createElement("div", {
    className: "val"
  }, it.description)), /*#__PURE__*/React.createElement("div", {
    className: "field"
  }, /*#__PURE__*/React.createElement("div", {
    className: "lbl"
  }, "Approximate price"), /*#__PURE__*/React.createElement("div", {
    className: "pricetag"
  }, priceBig(it), it.amount > 1 ? /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 15,
      color: "var(--muted)",
      fontWeight: 600
    }
  }, " \xB7 \xD7", it.amount) : null)), /*#__PURE__*/React.createElement("div", {
    className: "field"
  }, /*#__PURE__*/React.createElement("div", {
    className: "lbl"
  }, "Links"), it.links.length === 0 ? /*#__PURE__*/React.createElement("div", {
    className: "val",
    style: {
      color: "var(--muted)"
    }
  }, "No links") : it.links.map((l, i) => /*#__PURE__*/React.createElement("a", {
    key: i,
    className: "linkrow",
    href: l.url,
    target: "_blank",
    rel: "noreferrer"
  }, l.title || l.url, /*#__PURE__*/React.createElement(window.Icon, {
    name: "ext"
  })))))));
}

/* ---------------- Item edit ---------------- */
function ItemEditScreen({
  route,
  nav,
  toast
}) {
  const editing = route.id ? DT.itemById(route.id) : null;
  const w = DT.wishlistById(route.listId);
  const [title, setTitle] = useS(editing ? editing.title : "");
  const [desc, setDesc] = useS(editing ? editing.description : "");
  const [price, setPrice] = useS(editing ? editing.price : "");
  const [amount, setAmount] = useS(editing ? editing.amount : 1);
  const [priority, setPriority] = useS(editing ? editing.priority : "Medium");
  const [confirm, setConfirm] = useS(false);
  const save = () => {
    if (!editing) DT.addItem({
      wishlistId: w.id,
      title,
      description: desc,
      price: Number(price) || null,
      units: w.units,
      amount: Number(amount) || 1,
      priority,
      links: [],
      tint: "t" + Math.floor(Math.random() * 8)
    });
    toast(editing ? "Changes saved" : "Item added");
    nav.go("list", {
      id: w.id
    });
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "crumb"
  }, /*#__PURE__*/React.createElement("a", {
    onClick: () => nav.go("list", {
      id: w.id
    })
  }, w.title), /*#__PURE__*/React.createElement("span", {
    className: "sep"
  }, "/"), /*#__PURE__*/React.createElement("b", null, editing ? "Edit item" : "New item")), /*#__PURE__*/React.createElement("div", {
    className: "pagehead"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, editing ? "Edit item" : "Add an item"), /*#__PURE__*/React.createElement("p", {
    className: "subline"
  }, "to \u201C", w.title, "\u201D"))), /*#__PURE__*/React.createElement("div", {
    className: "form",
    style: {
      marginTop: 22
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Title"), /*#__PURE__*/React.createElement("input", {
    className: "input",
    value: title,
    placeholder: "What do you want?",
    onChange: e => setTitle(e.target.value)
  })), /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Description"), /*#__PURE__*/React.createElement("textarea", {
    className: "textarea",
    value: desc,
    placeholder: "Color, size, any details a gift-giver should know\u2026",
    onChange: e => setDesc(e.target.value)
  })), /*#__PURE__*/React.createElement("div", {
    className: "form-row"
  }, /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Approximate price (", w.units, ")"), /*#__PURE__*/React.createElement("input", {
    className: "input",
    type: "number",
    value: price,
    placeholder: "0",
    onChange: e => setPrice(e.target.value)
  })), /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Amount"), /*#__PURE__*/React.createElement("input", {
    className: "input",
    type: "number",
    min: "1",
    value: amount,
    onChange: e => setAmount(e.target.value)
  }))), /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Priority"), /*#__PURE__*/React.createElement("div", {
    className: "priopts"
  }, ["Low", "Medium", "High"].map(p => /*#__PURE__*/React.createElement("div", {
    key: p,
    className: "priopt" + (priority === p ? " on" : ""),
    onClick: () => setPriority(p)
  }, p))), /*#__PURE__*/React.createElement("div", {
    className: "hint"
  }, "High-priority items are shown first to gift-givers.")), /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      gap: 9,
      marginTop: 24
    }
  }, /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    disabled: !title.trim(),
    onClick: save
  }, editing ? "Save changes" : "Add item"), /*#__PURE__*/React.createElement("button", {
    className: "btn ghost",
    onClick: () => nav.go("list", {
      id: w.id
    })
  }, "Cancel"), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), editing && /*#__PURE__*/React.createElement("button", {
    className: "btn danger",
    onClick: () => setConfirm(true)
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "trash"
  }), " Delete"))), confirm && /*#__PURE__*/React.createElement(ConfirmModal, {
    title: "Delete item?",
    body: "This item will be permanently removed. Continue?",
    confirmLabel: "Delete",
    danger: true,
    onCancel: () => setConfirm(false),
    onConfirm: () => {
      DT.deleteItem(editing.id);
      toast("Item deleted");
      nav.go("list", {
        id: w.id
      });
    }
  }));
}

/* ---------------- Reserved ---------------- */
function ReservedScreen({
  me,
  nav
}) {
  if (!me) return /*#__PURE__*/React.createElement(SignedOut, {
    nav: nav
  });
  const items = DT.reservedByYou();
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "pagehead"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, "Reserved"), /*#__PURE__*/React.createElement("p", {
    className: "subline"
  }, "Gifts you've committed to give. Only you can see these."))), /*#__PURE__*/React.createElement("div", {
    className: "toolbar"
  }, /*#__PURE__*/React.createElement("div", null), /*#__PURE__*/React.createElement("div", null)), items.length === 0 ? /*#__PURE__*/React.createElement("div", {
    className: "empty"
  }, /*#__PURE__*/React.createElement("div", {
    className: "ic"
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "bookmark"
  })), /*#__PURE__*/React.createElement("h3", null, "No reservations yet"), /*#__PURE__*/React.createElement("p", null, "When you reserve a gift on someone's list, it shows up here."), /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    onClick: () => nav.go("discover")
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "compass"
  }), " Browse people")) : /*#__PURE__*/React.createElement("div", {
    className: "grid"
  }, items.map(it => {
    const w = DT.wishlistById(it.wishlistId);
    const owner = DT.usersById(w.ownerId);
    return /*#__PURE__*/React.createElement("div", {
      key: it.id,
      className: "card",
      onClick: () => nav.go("item", {
        id: it.id
      })
    }, /*#__PURE__*/React.createElement("div", {
      className: "media " + it.tint
    }, /*#__PURE__*/React.createElement("span", {
      className: "reserved-flag"
    }, "For ", owner.name.split(" ")[0])), /*#__PURE__*/React.createElement("div", {
      className: "c"
    }, /*#__PURE__*/React.createElement("h3", null, it.title), /*#__PURE__*/React.createElement("p", {
      className: "desc"
    }, w.title, " \xB7 ", owner.name), /*#__PURE__*/React.createElement("div", {
      className: "price"
    }, DT.priceText(it))));
  })));
}

/* ---------------- Settings (light) ---------------- */
function SettingsScreen({
  me,
  nav
}) {
  if (!me) return /*#__PURE__*/React.createElement(SignedOut, {
    nav: nav
  });
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "pagehead"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, "Settings"), /*#__PURE__*/React.createElement("p", {
    className: "subline"
  }, "Your account and server"))), /*#__PURE__*/React.createElement("div", {
    className: "form",
    style: {
      marginTop: 22
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Display name"), /*#__PURE__*/React.createElement("input", {
    className: "input",
    defaultValue: me.name
  })), /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Username"), /*#__PURE__*/React.createElement("input", {
    className: "input",
    defaultValue: me.username
  })), /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Default currency"), /*#__PURE__*/React.createElement("select", {
    className: "input"
  }, /*#__PURE__*/React.createElement("option", null, "USD"), /*#__PURE__*/React.createElement("option", null, "EUR"), /*#__PURE__*/React.createElement("option", null, "GBP"))), /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Server"), /*#__PURE__*/React.createElement("input", {
    className: "input",
    defaultValue: "https://wish.myserver.home"
  }), /*#__PURE__*/React.createElement("div", {
    className: "hint"
  }, "Self-hosted \u2014 your data stays on your server.")), /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    style: {
      marginTop: 8
    }
  }, "Save changes")));
}

/* ---------------- shared bits ---------------- */
function SignedOut({
  nav
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "empty",
    style: {
      paddingTop: 90
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "ic"
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "lock"
  })), /*#__PURE__*/React.createElement("h3", null, "Log in to see this"), /*#__PURE__*/React.createElement("p", null, "Your lists, reservations and settings are private to your account."), /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    onClick: () => nav.openLogin()
  }, "Log in")));
}
function ConfirmModal({
  title,
  body,
  confirmLabel,
  danger,
  onCancel,
  onConfirm
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "scrim",
    onClick: onCancel
  }, /*#__PURE__*/React.createElement("div", {
    className: "modal",
    onClick: e => e.stopPropagation()
  }, /*#__PURE__*/React.createElement("div", {
    className: "mhead"
  }, /*#__PURE__*/React.createElement("h2", null, title), /*#__PURE__*/React.createElement("p", null, body)), /*#__PURE__*/React.createElement("div", {
    className: "mfoot"
  }, /*#__PURE__*/React.createElement("button", {
    className: "btn ghost",
    onClick: onCancel
  }, "Cancel"), /*#__PURE__*/React.createElement("button", {
    className: "btn " + (danger ? "danger" : "primary"),
    onClick: onConfirm
  }, confirmLabel))));
}
function LoginModal({
  onClose,
  onLogin
}) {
  const [mode, setMode] = useS("login");
  const [u, setU] = useS("you");
  return /*#__PURE__*/React.createElement("div", {
    className: "scrim",
    onClick: onClose
  }, /*#__PURE__*/React.createElement("div", {
    className: "modal",
    onClick: e => e.stopPropagation()
  }, /*#__PURE__*/React.createElement("div", {
    className: "mhead"
  }, /*#__PURE__*/React.createElement("h2", null, mode === "login" ? "Welcome back" : "Create account"), /*#__PURE__*/React.createElement("p", null, "Self-hosted wishlists, just for your people.")), /*#__PURE__*/React.createElement("div", {
    className: "mbody"
  }, /*#__PURE__*/React.createElement("div", {
    className: "tabs"
  }, /*#__PURE__*/React.createElement("button", {
    className: mode === "login" ? "on" : "",
    onClick: () => setMode("login")
  }, "Log in"), /*#__PURE__*/React.createElement("button", {
    className: mode === "register" ? "on" : "",
    onClick: () => setMode("register")
  }, "Register")), /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Username"), /*#__PURE__*/React.createElement("input", {
    className: "input",
    value: u,
    onChange: e => setU(e.target.value)
  })), /*#__PURE__*/React.createElement("div", {
    className: "fieldset"
  }, /*#__PURE__*/React.createElement("label", null, "Password"), /*#__PURE__*/React.createElement("input", {
    className: "input",
    type: "password",
    defaultValue: "",
    placeholder: "\u2022\u2022\u2022\u2022\u2022\u2022\u2022\u2022"
  }))), /*#__PURE__*/React.createElement("div", {
    className: "mfoot"
  }, /*#__PURE__*/React.createElement("button", {
    className: "btn ghost",
    onClick: onClose
  }, "Cancel"), /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    onClick: onLogin
  }, mode === "login" ? "Log in" : "Create account"))));
}

/* ---------------- search overlay ---------------- */
function SearchResults({
  q,
  nav
}) {
  const ql = q.toLowerCase();
  const people = DT.users.filter(u => u.name.toLowerCase().includes(ql) || u.username.includes(ql));
  const lists = DT.wishlists.filter(w => w.title.toLowerCase().includes(ql));
  const items = DT.items.filter(i => i.title.toLowerCase().includes(ql));
  const Section = ({
    label,
    children
  }) => /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    className: "navlabel",
    style: {
      paddingLeft: 0
    }
  }, label), children);
  return /*#__PURE__*/React.createElement("div", {
    className: "content-inner"
  }, /*#__PURE__*/React.createElement("div", {
    className: "pagehead"
  }, /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement("h1", null, "Results for \u201C", q, "\u201D"), /*#__PURE__*/React.createElement("p", {
    className: "subline"
  }, people.length + lists.length + items.length, " matches"))), /*#__PURE__*/React.createElement("div", {
    style: {
      marginTop: 18
    }
  }, people.length > 0 && /*#__PURE__*/React.createElement(Section, {
    label: "People"
  }, /*#__PURE__*/React.createElement("div", {
    className: "people",
    style: {
      marginBottom: 22
    }
  }, people.map(u => /*#__PURE__*/React.createElement("div", {
    key: u.id,
    className: "person",
    onClick: () => nav.go("profile", {
      id: u.id
    })
  }, /*#__PURE__*/React.createElement("span", {
    className: "av",
    style: {
      background: u.tint
    }
  }), /*#__PURE__*/React.createElement("h3", null, u.name), /*#__PURE__*/React.createElement("div", {
    className: "meta"
  }, "@", u.username))))), lists.length > 0 && /*#__PURE__*/React.createElement(Section, {
    label: "Lists"
  }, /*#__PURE__*/React.createElement("div", {
    className: "listgrid",
    style: {
      marginBottom: 22
    }
  }, lists.map(w => {
    const o = DT.usersById(w.ownerId);
    return /*#__PURE__*/React.createElement("div", {
      key: w.id,
      className: "listcard",
      onClick: () => nav.go("list", {
        id: w.id
      })
    }, /*#__PURE__*/React.createElement("div", {
      className: "cover",
      style: {
        background: w.cover
      }
    }, /*#__PURE__*/React.createElement("span", {
      className: "vis"
    }, w.visibility)), /*#__PURE__*/React.createElement("div", {
      className: "c"
    }, /*#__PURE__*/React.createElement("h3", null, w.title), /*#__PURE__*/React.createElement("div", {
      className: "meta"
    }, o.name)));
  }))), items.length > 0 && /*#__PURE__*/React.createElement(Section, {
    label: "Items"
  }, /*#__PURE__*/React.createElement("div", {
    className: "grid"
  }, items.map(it => /*#__PURE__*/React.createElement(window.ItemCard, {
    key: it.id,
    it: it,
    onOpen: () => nav.go("item", {
      id: it.id
    })
  })))), people.length + lists.length + items.length === 0 && /*#__PURE__*/React.createElement("div", {
    className: "empty"
  }, /*#__PURE__*/React.createElement("div", {
    className: "ic"
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "search"
  })), /*#__PURE__*/React.createElement("h3", null, "No matches"), /*#__PURE__*/React.createElement("p", null, "Try a different name, list, or item."))));
}

/* ---------------- App ---------------- */
const SCREENS = {
  home: HomeScreen,
  discover: DiscoverScreen,
  profile: ProfileScreen,
  list: ListScreen,
  item: ItemScreen,
  itemEdit: ItemEditScreen,
  reserved: ReservedScreen,
  settings: SettingsScreen
};
const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
  "accent": "#5B5BD6",
  "density": "regular"
} /*EDITMODE-END*/;
const DENSITY = {
  compact: {
    gap: "10px",
    pad: "12px"
  },
  regular: {
    gap: "14px",
    pad: "14px"
  },
  comfy: {
    gap: "20px",
    pad: "18px"
  }
};
function App() {
  const [tw, setTweak] = window.useTweaks(TWEAK_DEFAULTS);
  const [route, setRoute] = useS({
    screen: "home"
  });
  const [me, setMe] = useS(DT.usersById("you"));
  const [login, setLogin] = useS(false);
  const [q, setQ] = useS("");
  const [toastMsg, setToastMsg] = useS(null);
  const toast = m => {
    setToastMsg(m);
    clearTimeout(window.__t);
    window.__t = setTimeout(() => setToastMsg(null), 2600);
  };
  const nav = {
    go: (screen, extra = {}) => {
      setQ("");
      setRoute({
        screen,
        ...extra
      });
      document.querySelector(".content") && (document.querySelector(".content").scrollTop = 0);
    },
    openLogin: () => setLogin(true)
  };
  const Screen = SCREENS[route.screen] || HomeScreen;
  const dens = DENSITY[tw.density] || DENSITY.regular;
  const shellStyle = {
    "--accent": tw.accent,
    "--gap": dens.gap,
    "--card-pad": dens.pad
  };
  const primaryAction = route.screen === "home" && me ? /*#__PURE__*/React.createElement("button", {
    className: "btn primary",
    onClick: () => toast("New list created (demo)")
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "plus"
  }), " New list") : null;
  return /*#__PURE__*/React.createElement("div", {
    className: "app",
    style: shellStyle
  }, /*#__PURE__*/React.createElement(window.Sidebar, {
    route: route,
    me: me,
    go: nav.go,
    openLogin: () => setLogin(true)
  }), /*#__PURE__*/React.createElement("div", {
    className: "main"
  }, /*#__PURE__*/React.createElement(window.TopBar, {
    me: me,
    onSearch: setQ,
    openLogin: () => setLogin(true),
    logout: () => {
      setMe(null);
      nav.go("discover");
    },
    primary: primaryAction
  }), /*#__PURE__*/React.createElement("div", {
    className: "content"
  }, q.trim() ? /*#__PURE__*/React.createElement(SearchResults, {
    q: q,
    nav: nav
  }) : /*#__PURE__*/React.createElement(Screen, {
    me: me,
    route: route,
    nav: nav,
    toast: toast
  }))), login && /*#__PURE__*/React.createElement(LoginModal, {
    onClose: () => setLogin(false),
    onLogin: () => {
      setMe(DT.usersById("you"));
      setLogin(false);
      nav.go("home");
      toast("Welcome back");
    }
  }), /*#__PURE__*/React.createElement("div", {
    className: "toast" + (toastMsg ? " show" : "")
  }, /*#__PURE__*/React.createElement("span", {
    className: "ok"
  }, /*#__PURE__*/React.createElement(window.Icon, {
    name: "check"
  })), toastMsg), /*#__PURE__*/React.createElement(window.TweaksPanel, null, /*#__PURE__*/React.createElement(window.TweakSection, {
    label: "Theme"
  }), /*#__PURE__*/React.createElement(window.TweakColor, {
    label: "Accent",
    value: tw.accent,
    options: ["#5B5BD6", "#2A6FDB", "#1F8A5B", "#C75B39", "#7A5AE0", "#18181B"],
    onChange: v => setTweak("accent", v)
  }), /*#__PURE__*/React.createElement(window.TweakSection, {
    label: "Layout"
  }), /*#__PURE__*/React.createElement(window.TweakRadio, {
    label: "Density",
    value: tw.density,
    options: ["compact", "regular", "comfy"],
    onChange: v => setTweak("density", v)
  })));
}
ReactDOM.createRoot(document.getElementById("root")).render(/*#__PURE__*/React.createElement(App, null));
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/calm-studio/app.jsx", error: String((e && e.message) || e) }); }

// ui_kits/calm-studio/components.jsx
try { (() => {
/* Calm Studio — shared UI: icons, shells, cards. Exposes components on window. */
const {
  useState,
  useRef,
  useEffect
} = React;
const D = window.CS_DATA;

/* ---------- icons (Lucide-style, 2px stroke) ---------- */
const PATHS = {
  home: /*#__PURE__*/React.createElement("path", {
    d: "M3 10.5 12 4l9 6.5V20a1 1 0 0 1-1 1h-5v-6H9v6H4a1 1 0 0 1-1-1z"
  }),
  compass: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "12",
    r: "9"
  }), /*#__PURE__*/React.createElement("path", {
    d: "m15.5 8.5-2 5-5 2 2-5z"
  })),
  bookmark: /*#__PURE__*/React.createElement("path", {
    d: "M19 21l-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2z"
  }),
  settings: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "12",
    r: "3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M19.4 15a1.6 1.6 0 0 0 .3 1.8l.1.1a2 2 0 1 1-2.8 2.8l-.1-.1a1.6 1.6 0 0 0-2.7 1.1V21a2 2 0 0 1-4 0v-.1A1.6 1.6 0 0 0 7 19.4a1.6 1.6 0 0 0-1.8.3l-.1.1a2 2 0 1 1-2.8-2.8l.1-.1a1.6 1.6 0 0 0-1.1-2.7H1a2 2 0 0 1 0-4h.1A1.6 1.6 0 0 0 2.6 7a1.6 1.6 0 0 0-.3-1.8l-.1-.1a2 2 0 1 1 2.8-2.8l.1.1a1.6 1.6 0 0 0 1.8.3H7a1.6 1.6 0 0 0 1-1.5V1a2 2 0 0 1 4 0v.1a1.6 1.6 0 0 0 1 1.5 1.6 1.6 0 0 0 1.8-.3l.1-.1a2 2 0 1 1 2.8 2.8l-.1.1a1.6 1.6 0 0 0-.3 1.8V7a1.6 1.6 0 0 0 1.5 1H23a2 2 0 0 1 0 4h-.1a1.6 1.6 0 0 0-1.5 1z"
  })),
  search: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("circle", {
    cx: "11",
    cy: "11",
    r: "7"
  }), /*#__PURE__*/React.createElement("path", {
    d: "m20 20-3-3"
  })),
  plus: /*#__PURE__*/React.createElement("path", {
    d: "M12 5v14M5 12h14"
  }),
  list: /*#__PURE__*/React.createElement("rect", {
    x: "4",
    y: "4",
    width: "16",
    height: "16",
    rx: "3"
  }),
  share: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("circle", {
    cx: "18",
    cy: "5",
    r: "3"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "6",
    cy: "12",
    r: "3"
  }), /*#__PURE__*/React.createElement("circle", {
    cx: "18",
    cy: "19",
    r: "3"
  }), /*#__PURE__*/React.createElement("path", {
    d: "m8.6 13.5 6.8 4M15.4 6.5l-6.8 4"
  })),
  back: /*#__PURE__*/React.createElement("path", {
    d: "M19 12H5M12 19l-7-7 7-7"
  }),
  edit: /*#__PURE__*/React.createElement("path", {
    d: "M12 20h9M16.5 3.5a2.1 2.1 0 0 1 3 3L7 19l-4 1 1-4z"
  }),
  trash: /*#__PURE__*/React.createElement("path", {
    d: "M3 6h18M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2m2 0v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"
  }),
  ext: /*#__PURE__*/React.createElement("path", {
    d: "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6M15 3h6v6M10 14 21 3"
  }),
  check: /*#__PURE__*/React.createElement("path", {
    d: "M20 6 9 17l-5-5"
  }),
  gift: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("rect", {
    x: "3",
    y: "8",
    width: "18",
    height: "4",
    rx: "1"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M12 8v13M5 12v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8M12 8S10.5 3 8 3a2.5 2.5 0 0 0 0 5M12 8s1.5-5 4-5a2.5 2.5 0 0 1 0 5"
  })),
  user: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("circle", {
    cx: "12",
    cy: "8",
    r: "4"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M4 21a8 8 0 0 1 16 0"
  })),
  lock: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("rect", {
    x: "5",
    y: "11",
    width: "14",
    height: "10",
    rx: "2"
  }), /*#__PURE__*/React.createElement("path", {
    d: "M8 11V7a4 4 0 0 1 8 0v4"
  }))
};
function Icon({
  name
}) {
  return /*#__PURE__*/React.createElement("svg", {
    viewBox: "0 0 24 24",
    fill: "none",
    stroke: "currentColor",
    strokeWidth: "2",
    strokeLinecap: "round",
    strokeLinejoin: "round"
  }, PATHS[name]);
}
const PRI_COLOR = {
  High: "var(--pri-high)",
  Medium: "var(--pri-med)",
  Low: "var(--pri-low)",
  Custom: "var(--pri-high)"
};
function PriorityPill({
  p
}) {
  return /*#__PURE__*/React.createElement("span", {
    className: "pill"
  }, /*#__PURE__*/React.createElement("span", {
    className: "dot",
    style: {
      background: PRI_COLOR[p]
    }
  }), p);
}

/* ---------- sidebar ---------- */
function Sidebar({
  route,
  me,
  go,
  openLogin
}) {
  const myLists = me ? D.wishlistsByOwner(me.id) : [];
  const reservedCount = me ? D.reservedByYou().length : 0;
  const Item = ({
    icon,
    label,
    to,
    count
  }) => /*#__PURE__*/React.createElement("button", {
    className: "navitem" + (route.screen === to ? " on" : ""),
    onClick: () => go(to)
  }, /*#__PURE__*/React.createElement(Icon, {
    name: icon
  }), " ", label, count > 0 && /*#__PURE__*/React.createElement("span", {
    className: "count"
  }, count));
  return /*#__PURE__*/React.createElement("aside", {
    className: "sidebar"
  }, /*#__PURE__*/React.createElement("div", {
    className: "logo"
  }, /*#__PURE__*/React.createElement("span", {
    className: "mk"
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "gift"
  })), " wishlist"), /*#__PURE__*/React.createElement("nav", {
    className: "navsec"
  }, /*#__PURE__*/React.createElement(Item, {
    icon: "home",
    label: "My Lists",
    to: "home"
  }), /*#__PURE__*/React.createElement(Item, {
    icon: "compass",
    label: "Discover",
    to: "discover"
  }), /*#__PURE__*/React.createElement(Item, {
    icon: "bookmark",
    label: "Reserved",
    to: "reserved",
    count: reservedCount
  }), /*#__PURE__*/React.createElement(Item, {
    icon: "settings",
    label: "Settings",
    to: "settings"
  })), me && myLists.length > 0 && /*#__PURE__*/React.createElement("nav", {
    className: "navsec"
  }, /*#__PURE__*/React.createElement("div", {
    className: "navlabel"
  }, "Your lists"), myLists.map(w => /*#__PURE__*/React.createElement("button", {
    key: w.id,
    className: "navitem" + (route.screen === "list" && route.id === w.id ? " on" : ""),
    onClick: () => go("list", {
      id: w.id
    })
  }, /*#__PURE__*/React.createElement("span", {
    className: "swatch",
    style: {
      background: w.cover
    }
  }), " ", w.title)), /*#__PURE__*/React.createElement("button", {
    className: "navitem",
    onClick: () => go("home")
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "plus"
  }), " New list")), /*#__PURE__*/React.createElement("div", {
    className: "spacer"
  }), me ? /*#__PURE__*/React.createElement("div", {
    className: "me",
    onClick: () => go("profile", {
      id: me.id
    })
  }, /*#__PURE__*/React.createElement("span", {
    className: "av",
    style: {
      background: me.tint
    }
  }), /*#__PURE__*/React.createElement("span", {
    className: "nm"
  }, me.username, /*#__PURE__*/React.createElement("small", null, "View profile"))) : /*#__PURE__*/React.createElement("button", {
    className: "btn primary block",
    onClick: openLogin
  }, "Log in"));
}

/* ---------- top bar ---------- */
function TopBar({
  me,
  onSearch,
  openLogin,
  logout,
  primary
}) {
  const [q, setQ] = useState("");
  const ref = useRef(null);
  useEffect(() => {
    const h = e => {
      if ((e.metaKey || e.ctrlKey) && e.key === "k") {
        e.preventDefault();
        ref.current && ref.current.focus();
      }
    };
    window.addEventListener("keydown", h);
    return () => window.removeEventListener("keydown", h);
  }, []);
  return /*#__PURE__*/React.createElement("div", {
    className: "topbar"
  }, /*#__PURE__*/React.createElement("label", {
    className: "search"
  }, /*#__PURE__*/React.createElement(Icon, {
    name: "search"
  }), /*#__PURE__*/React.createElement("input", {
    ref: ref,
    value: q,
    placeholder: "Search people, lists, items\u2026",
    onChange: e => {
      setQ(e.target.value);
      onSearch(e.target.value);
    }
  }), /*#__PURE__*/React.createElement("span", {
    className: "kbd"
  }, "\u2318K")), /*#__PURE__*/React.createElement("div", {
    className: "sp"
  }), primary, me ? /*#__PURE__*/React.createElement("button", {
    className: "btn ghost",
    onClick: logout
  }, "Log out") : /*#__PURE__*/React.createElement("button", {
    className: "btn",
    onClick: openLogin
  }, "Log in"));
}

/* ---------- item card / row ---------- */
function ItemCard({
  it,
  onOpen,
  viewerCanReserve
}) {
  const reserved = D.isReserved(it.id);
  return /*#__PURE__*/React.createElement("div", {
    className: "card",
    onClick: onOpen
  }, /*#__PURE__*/React.createElement("div", {
    className: "media " + it.tint
  }, reserved ? /*#__PURE__*/React.createElement("span", {
    className: "reserved-flag"
  }, "Reserved") : /*#__PURE__*/React.createElement("span", {
    className: "badge"
  }, /*#__PURE__*/React.createElement("span", {
    className: "dot",
    style: {
      background: PRI_COLOR[it.priority]
    }
  }), it.priority)), /*#__PURE__*/React.createElement("div", {
    className: "c"
  }, /*#__PURE__*/React.createElement("h3", null, it.title), it.description && /*#__PURE__*/React.createElement("p", {
    className: "desc"
  }, it.description), /*#__PURE__*/React.createElement("div", {
    className: "price"
  }, D.priceText(it))));
}
function ItemRow({
  it,
  onOpen
}) {
  const reserved = D.isReserved(it.id);
  return /*#__PURE__*/React.createElement("div", {
    className: "row",
    onClick: onOpen
  }, /*#__PURE__*/React.createElement("span", {
    className: "thumb " + it.tint
  }), /*#__PURE__*/React.createElement("div", {
    className: "rmain"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: "flex",
      alignItems: "center",
      gap: 9
    }
  }, /*#__PURE__*/React.createElement("h3", null, it.title), reserved ? /*#__PURE__*/React.createElement("span", {
    className: "pill",
    style: {
      background: "var(--ok-soft)",
      color: "var(--ok)"
    }
  }, /*#__PURE__*/React.createElement("span", {
    className: "dot",
    style: {
      background: "var(--ok)"
    }
  }), "Reserved") : /*#__PURE__*/React.createElement(PriorityPill, {
    p: it.priority
  })), it.description && /*#__PURE__*/React.createElement("p", {
    className: "desc"
  }, it.description)), /*#__PURE__*/React.createElement("span", {
    className: "rprice"
  }, D.priceText(it)));
}
Object.assign(window, {
  Icon,
  PriorityPill,
  Sidebar,
  TopBar,
  ItemCard,
  ItemRow,
  PRI_COLOR
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/calm-studio/components.jsx", error: String((e && e.message) || e) }); }

// ui_kits/calm-studio/data.js
try { (() => {
/* Data for the Calm Studio prototype. Richer than the kit data: adds list
 * visibility, cover tints, and a reservation model (the booking feature the
 * redesign surfaces). window.CS_DATA. */
window.CS_DATA = function () {
  const users = [{
    id: "you",
    username: "you",
    name: "You",
    tint: "linear-gradient(135deg,#5B5BD6,#8e8ee9)",
    you: true
  }, {
    id: "alice",
    username: "alice",
    name: "Alice Werner",
    tint: "linear-gradient(135deg,#d98bb0,#e9b3cd)"
  }, {
    id: "bob",
    username: "bob",
    name: "Bob Tran",
    tint: "linear-gradient(135deg,#6fae8f,#a7d4bf)"
  }, {
    id: "mira",
    username: "mira",
    name: "Mira Solé",
    tint: "linear-gradient(135deg,#e0a36f,#f1c79e)"
  }, {
    id: "root",
    username: "root",
    name: "Root",
    tint: "linear-gradient(135deg,#8a8a99,#bcbcc9)",
    admin: true
  }];
  const wishlists = [{
    id: "w_bday",
    ownerId: "you",
    title: "Birthday",
    visibility: "Public",
    units: "USD",
    cover: "linear-gradient(135deg,#c9c9f0,#a9a9e6)"
  }, {
    id: "w_home",
    ownerId: "you",
    title: "New apartment",
    visibility: "Public",
    units: "USD",
    cover: "linear-gradient(135deg,#cfe0d6,#aeccbb)"
  }, {
    id: "w_priv",
    ownerId: "you",
    title: "Someday / maybe",
    visibility: "Private",
    units: "USD",
    cover: "linear-gradient(135deg,#e2dccf,#cdc3ad)"
  }, {
    id: "w_alice_wed",
    ownerId: "alice",
    title: "Wedding registry",
    visibility: "Public",
    units: "EUR",
    cover: "linear-gradient(135deg,#f0d3e0,#e6b3cd)"
  }, {
    id: "w_alice_books",
    ownerId: "alice",
    title: "Books to read",
    visibility: "Public",
    units: "EUR",
    cover: "linear-gradient(135deg,#dfe0ee,#c6c8e2)"
  }, {
    id: "w_bob_bike",
    ownerId: "bob",
    title: "Bike build",
    visibility: "Public",
    units: "USD",
    cover: "linear-gradient(135deg,#d0e6da,#aed4bf)"
  }, {
    id: "w_mira_art",
    ownerId: "mira",
    title: "Art supplies",
    visibility: "Public",
    units: "USD",
    cover: "linear-gradient(135deg,#f3e2cf,#e9c79e)"
  }];
  let items = [{
    id: "i1",
    wishlistId: "w_bday",
    title: "Mechanical keyboard",
    description: "Brown switches, TKL layout, wireless.",
    price: 120,
    units: "USD",
    amount: 1,
    priority: "High",
    links: [{
      url: "https://example.com/kbd",
      title: "Product page"
    }],
    tint: "t0"
  }, {
    id: "i2",
    wishlistId: "w_bday",
    title: "Noise-cancelling headphones",
    description: "Over-ear, long battery life.",
    price: 240,
    units: "USD",
    amount: 1,
    priority: "High",
    links: [{
      url: "https://example.com/hp",
      title: "Review"
    }],
    tint: "t1"
  }, {
    id: "i3",
    wishlistId: "w_bday",
    title: "Floor lamp",
    description: "Warm dimmable LED, oak base.",
    price: 85,
    units: "USD",
    amount: 1,
    priority: "Medium",
    links: [],
    tint: "t2"
  }, {
    id: "i4",
    wishlistId: "w_bday",
    title: "Linen apron",
    description: "Natural color, with front pocket.",
    price: 35,
    units: "USD",
    amount: 1,
    priority: "Medium",
    links: [],
    tint: "t3"
  }, {
    id: "i5",
    wishlistId: "w_bday",
    title: "Espresso cups",
    description: "Set of 4, matte ceramic.",
    price: 28,
    units: "USD",
    amount: 2,
    priority: "Low",
    links: [],
    tint: "t4"
  }, {
    id: "i6",
    wishlistId: "w_bday",
    title: "Cast-iron skillet",
    description: "12-inch, pre-seasoned.",
    price: 45,
    units: "USD",
    amount: 1,
    priority: "Medium",
    links: [],
    tint: "t5"
  }, {
    id: "i7",
    wishlistId: "w_home",
    title: "Wool throw blanket",
    description: "Oatmeal, queen size.",
    price: 70,
    units: "USD",
    amount: 1,
    priority: "Medium",
    links: [],
    tint: "t6"
  }, {
    id: "i8",
    wishlistId: "w_home",
    title: "Dinner plate set",
    description: "Stoneware, service for 8.",
    price: 160,
    units: "USD",
    amount: 1,
    priority: "High",
    links: [],
    tint: "t7"
  }, {
    id: "i9",
    wishlistId: "w_alice_wed",
    title: "KitchenAid mixer",
    description: "Stand mixer, 5qt.",
    price: 380,
    units: "EUR",
    amount: 1,
    priority: "High",
    links: [{
      url: "https://example.com/mixer",
      title: "Product page"
    }],
    tint: "t1"
  }, {
    id: "i10",
    wishlistId: "w_alice_wed",
    title: "Wool throw blanket",
    description: "",
    price: 70,
    units: "EUR",
    amount: 1,
    priority: "Medium",
    links: [],
    tint: "t6"
  }, {
    id: "i11",
    wishlistId: "w_alice_books",
    title: "The Overstory",
    description: "Richard Powers, hardcover.",
    price: 18,
    units: "EUR",
    amount: 1,
    priority: "Low",
    links: [],
    tint: "t3"
  }, {
    id: "i12",
    wishlistId: "w_bob_bike",
    title: "Carbon handlebars",
    description: "31.8mm clamp, 420mm.",
    price: 130,
    units: "USD",
    amount: 1,
    priority: "High",
    links: [],
    tint: "t0"
  }, {
    id: "i13",
    wishlistId: "w_mira_art",
    title: "Gouache set",
    description: "24 tubes, artist grade.",
    price: 52,
    units: "USD",
    amount: 1,
    priority: "Medium",
    links: [],
    tint: "t7"
  }];

  // Reservations: itemId -> reserverId. Seeded so "you" have reserved some gifts
  // and some of your own items are reserved by others (shown as a count, never who).
  let reservations = {
    i9: "you",
    i11: "you",
    i3: "bob",
    i5: "mira"
  };
  function priceText(it) {
    if (it.price == null) return "";
    const base = `≈ ${it.price} ${it.units}`;
    return it.amount > 1 ? `${base} · ×${it.amount}` : base;
  }
  const PRI = {
    High: 3,
    Medium: 2,
    Low: 1,
    Custom: 2.5
  };
  return {
    users,
    wishlists,
    get items() {
      return items;
    },
    get reservations() {
      return reservations;
    },
    priceText,
    PRI,
    usersById: id => users.find(u => u.id === id),
    wishlistsByOwner: id => wishlists.filter(w => w.ownerId === id),
    publicWishlistsByOwner: id => wishlists.filter(w => w.ownerId === id && w.visibility === "Public"),
    wishlistById: id => wishlists.find(w => w.id === id),
    itemsByWishlist: id => items.filter(i => i.wishlistId === id),
    itemById: id => items.find(i => i.id === id),
    isReserved: id => id in reservations,
    reservedByYou: () => items.filter(i => reservations[i.id] === "you"),
    reserve: (id, by = "you") => {
      reservations = {
        ...reservations,
        [id]: by
      };
    },
    unreserve: id => {
      const r = {
        ...reservations
      };
      delete r[id];
      reservations = r;
    },
    addItem: it => {
      items = [{
        ...it,
        id: "i" + (items.length + 100)
      }, ...items];
    },
    deleteItem: id => {
      items = items.filter(i => i.id !== id);
    }
  };
}();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/calm-studio/data.js", error: String((e && e.message) || e) }); }

// ui_kits/calm-studio/tweaks-panel.jsx
try { (() => {
// @ds-adherence-ignore -- omelette starter scaffold (raw elements/hex/px by design)

/* BEGIN USAGE */
// tweaks-panel.jsx
// Reusable Tweaks shell + form-control helpers.
// Exports (to window): useTweaks, TweaksPanel, TweakSection, TweakRow, TweakSlider,
//   TweakToggle, TweakRadio, TweakSelect, TweakText, TweakNumber, TweakColor, TweakButton.
//
// Owns the host protocol (listens for __activate_edit_mode / __deactivate_edit_mode,
// posts __edit_mode_available / __edit_mode_set_keys / __edit_mode_dismissed) so
// individual prototypes don't re-roll it. Ships a consistent set of controls so you
// don't hand-draw <input type="range">, segmented radios, steppers, etc.
//
// Usage (in an HTML file that loads React + Babel):
//
//   const TWEAK_DEFAULTS = /*EDITMODE-BEGIN*/{
//     "primaryColor": "#D97757",
//     "palette": ["#D97757", "#29261b", "#f6f4ef"],
//     "fontSize": 16,
//     "density": "regular",
//     "dark": false
//   }/*EDITMODE-END*/;
//
//   function App() {
//     const [t, setTweak] = useTweaks(TWEAK_DEFAULTS);
//     return (
//       <div style={{ fontSize: t.fontSize, color: t.primaryColor }}>
//         Hello
//         <TweaksPanel>
//           <TweakSection label="Typography" />
//           <TweakSlider label="Font size" value={t.fontSize} min={10} max={32} unit="px"
//                        onChange={(v) => setTweak('fontSize', v)} />
//           <TweakRadio  label="Density" value={t.density}
//                        options={['compact', 'regular', 'comfy']}
//                        onChange={(v) => setTweak('density', v)} />
//           <TweakSection label="Theme" />
//           <TweakColor  label="Primary" value={t.primaryColor}
//                        options={['#D97757', '#2A6FDB', '#1F8A5B', '#7A5AE0']}
//                        onChange={(v) => setTweak('primaryColor', v)} />
//           <TweakColor  label="Palette" value={t.palette}
//                        options={[['#D97757', '#29261b', '#f6f4ef'],
//                                  ['#475569', '#0f172a', '#f1f5f9']]}
//                        onChange={(v) => setTweak('palette', v)} />
//           <TweakToggle label="Dark mode" value={t.dark}
//                        onChange={(v) => setTweak('dark', v)} />
//         </TweaksPanel>
//       </div>
//     );
//   }
//
// TweakRadio is the segmented control for 2–3 short options (auto-falls-back to
// TweakSelect past ~16/~10 chars per label); reach for TweakSelect directly when
// options are many or long. For color tweaks always curate 3-4 options rather than
// a free picker; an option can also be a whole 2–5 color palette (the stored value
// is the array). The Tweak* controls are a floor, not a ceiling — build custom
// controls inside the panel if a tweak calls for UI they don't cover.
/* END USAGE */
// ─────────────────────────────────────────────────────────────────────────────

const __TWEAKS_STYLE = `
  .twk-panel{position:fixed;right:16px;bottom:16px;z-index:2147483646;width:280px;
    max-height:calc(100vh - 32px);display:flex;flex-direction:column;
    transform:scale(var(--dc-inv-zoom,1));transform-origin:bottom right;
    background:rgba(250,249,247,.78);color:#29261b;
    -webkit-backdrop-filter:blur(24px) saturate(160%);backdrop-filter:blur(24px) saturate(160%);
    border:.5px solid rgba(255,255,255,.6);border-radius:14px;
    box-shadow:0 1px 0 rgba(255,255,255,.5) inset,0 12px 40px rgba(0,0,0,.18);
    font:11.5px/1.4 ui-sans-serif,system-ui,-apple-system,sans-serif;overflow:hidden}
  .twk-hd{display:flex;align-items:center;justify-content:space-between;
    padding:10px 8px 10px 14px;cursor:move;user-select:none}
  .twk-hd b{font-size:12px;font-weight:600;letter-spacing:.01em}
  .twk-x{appearance:none;border:0;background:transparent;color:rgba(41,38,27,.55);
    width:22px;height:22px;border-radius:6px;cursor:default;font-size:13px;line-height:1}
  .twk-x:hover{background:rgba(0,0,0,.06);color:#29261b}
  .twk-body{padding:2px 14px 14px;display:flex;flex-direction:column;gap:10px;
    overflow-y:auto;overflow-x:hidden;min-height:0;
    scrollbar-width:thin;scrollbar-color:rgba(0,0,0,.15) transparent}
  .twk-body::-webkit-scrollbar{width:8px}
  .twk-body::-webkit-scrollbar-track{background:transparent;margin:2px}
  .twk-body::-webkit-scrollbar-thumb{background:rgba(0,0,0,.15);border-radius:4px;
    border:2px solid transparent;background-clip:content-box}
  .twk-body::-webkit-scrollbar-thumb:hover{background:rgba(0,0,0,.25);
    border:2px solid transparent;background-clip:content-box}
  .twk-row{display:flex;flex-direction:column;gap:5px}
  .twk-row-h{flex-direction:row;align-items:center;justify-content:space-between;gap:10px}
  .twk-lbl{display:flex;justify-content:space-between;align-items:baseline;
    color:rgba(41,38,27,.72)}
  .twk-lbl>span:first-child{font-weight:500}
  .twk-val{color:rgba(41,38,27,.5);font-variant-numeric:tabular-nums}

  .twk-sect{font-size:10px;font-weight:600;letter-spacing:.06em;text-transform:uppercase;
    color:rgba(41,38,27,.45);padding:10px 0 0}
  .twk-sect:first-child{padding-top:0}

  .twk-field{appearance:none;box-sizing:border-box;width:100%;min-width:0;height:26px;padding:0 8px;
    border:.5px solid rgba(0,0,0,.1);border-radius:7px;
    background:rgba(255,255,255,.6);color:inherit;font:inherit;outline:none}
  .twk-field:focus{border-color:rgba(0,0,0,.25);background:rgba(255,255,255,.85)}
  select.twk-field{padding-right:22px;
    background-image:url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='10' height='6' viewBox='0 0 10 6'><path fill='rgba(0,0,0,.5)' d='M0 0h10L5 6z'/></svg>");
    background-repeat:no-repeat;background-position:right 8px center}

  .twk-slider{appearance:none;-webkit-appearance:none;width:100%;height:4px;margin:6px 0;
    border-radius:999px;background:rgba(0,0,0,.12);outline:none}
  .twk-slider::-webkit-slider-thumb{-webkit-appearance:none;appearance:none;
    width:14px;height:14px;border-radius:50%;background:#fff;
    border:.5px solid rgba(0,0,0,.12);box-shadow:0 1px 3px rgba(0,0,0,.2);cursor:default}
  .twk-slider::-moz-range-thumb{width:14px;height:14px;border-radius:50%;
    background:#fff;border:.5px solid rgba(0,0,0,.12);box-shadow:0 1px 3px rgba(0,0,0,.2);cursor:default}

  .twk-seg{position:relative;display:flex;padding:2px;border-radius:8px;
    background:rgba(0,0,0,.06);user-select:none}
  .twk-seg-thumb{position:absolute;top:2px;bottom:2px;border-radius:6px;
    background:rgba(255,255,255,.9);box-shadow:0 1px 2px rgba(0,0,0,.12);
    transition:left .15s cubic-bezier(.3,.7,.4,1),width .15s}
  .twk-seg.dragging .twk-seg-thumb{transition:none}
  .twk-seg button{appearance:none;position:relative;z-index:1;flex:1;border:0;
    background:transparent;color:inherit;font:inherit;font-weight:500;min-height:22px;
    border-radius:6px;cursor:default;padding:4px 6px;line-height:1.2;
    overflow-wrap:anywhere}

  .twk-toggle{position:relative;width:32px;height:18px;border:0;border-radius:999px;
    background:rgba(0,0,0,.15);transition:background .15s;cursor:default;padding:0}
  .twk-toggle[data-on="1"]{background:#34c759}
  .twk-toggle i{position:absolute;top:2px;left:2px;width:14px;height:14px;border-radius:50%;
    background:#fff;box-shadow:0 1px 2px rgba(0,0,0,.25);transition:transform .15s}
  .twk-toggle[data-on="1"] i{transform:translateX(14px)}

  .twk-num{display:flex;align-items:center;box-sizing:border-box;min-width:0;height:26px;padding:0 0 0 8px;
    border:.5px solid rgba(0,0,0,.1);border-radius:7px;background:rgba(255,255,255,.6)}
  .twk-num-lbl{font-weight:500;color:rgba(41,38,27,.6);cursor:ew-resize;
    user-select:none;padding-right:8px}
  .twk-num input{flex:1;min-width:0;height:100%;border:0;background:transparent;
    font:inherit;font-variant-numeric:tabular-nums;text-align:right;padding:0 8px 0 0;
    outline:none;color:inherit;-moz-appearance:textfield}
  .twk-num input::-webkit-inner-spin-button,.twk-num input::-webkit-outer-spin-button{
    -webkit-appearance:none;margin:0}
  .twk-num-unit{padding-right:8px;color:rgba(41,38,27,.45)}

  .twk-btn{appearance:none;height:26px;padding:0 12px;border:0;border-radius:7px;
    background:rgba(0,0,0,.78);color:#fff;font:inherit;font-weight:500;cursor:default}
  .twk-btn:hover{background:rgba(0,0,0,.88)}
  .twk-btn.secondary{background:rgba(0,0,0,.06);color:inherit}
  .twk-btn.secondary:hover{background:rgba(0,0,0,.1)}

  .twk-swatch{appearance:none;-webkit-appearance:none;width:56px;height:22px;
    border:.5px solid rgba(0,0,0,.1);border-radius:6px;padding:0;cursor:default;
    background:transparent;flex-shrink:0}
  .twk-swatch::-webkit-color-swatch-wrapper{padding:0}
  .twk-swatch::-webkit-color-swatch{border:0;border-radius:5.5px}
  .twk-swatch::-moz-color-swatch{border:0;border-radius:5.5px}

  .twk-chips{display:flex;gap:6px}
  .twk-chip{position:relative;appearance:none;flex:1;min-width:0;height:46px;
    padding:0;border:0;border-radius:6px;overflow:hidden;cursor:default;
    box-shadow:0 0 0 .5px rgba(0,0,0,.12),0 1px 2px rgba(0,0,0,.06);
    transition:transform .12s cubic-bezier(.3,.7,.4,1),box-shadow .12s}
  .twk-chip:hover{transform:translateY(-1px);
    box-shadow:0 0 0 .5px rgba(0,0,0,.18),0 4px 10px rgba(0,0,0,.12)}
  .twk-chip[data-on="1"]{box-shadow:0 0 0 1.5px rgba(0,0,0,.85),
    0 2px 6px rgba(0,0,0,.15)}
  .twk-chip>span{position:absolute;top:0;bottom:0;right:0;width:34%;
    display:flex;flex-direction:column;box-shadow:-1px 0 0 rgba(0,0,0,.1)}
  .twk-chip>span>i{flex:1;box-shadow:0 -1px 0 rgba(0,0,0,.1)}
  .twk-chip>span>i:first-child{box-shadow:none}
  .twk-chip svg{position:absolute;top:6px;left:6px;width:13px;height:13px;
    filter:drop-shadow(0 1px 1px rgba(0,0,0,.3))}
`;

// ── useTweaks ───────────────────────────────────────────────────────────────
// Single source of truth for tweak values. setTweak persists via the host
// (__edit_mode_set_keys → host rewrites the EDITMODE block on disk).
function useTweaks(defaults) {
  const [values, setValues] = React.useState(defaults);
  // Accepts either setTweak('key', value) or setTweak({ key: value, ... }) so a
  // useState-style call doesn't write a "[object Object]" key into the persisted
  // JSON block.
  const setTweak = React.useCallback((keyOrEdits, val) => {
    const edits = typeof keyOrEdits === 'object' && keyOrEdits !== null ? keyOrEdits : {
      [keyOrEdits]: val
    };
    setValues(prev => ({
      ...prev,
      ...edits
    }));
    window.parent.postMessage({
      type: '__edit_mode_set_keys',
      edits
    }, '*');
    // Same-window signal so in-page listeners (deck-stage rail thumbnails)
    // can react — the parent message only reaches the host, not peers.
    window.dispatchEvent(new CustomEvent('tweakchange', {
      detail: edits
    }));
  }, []);
  return [values, setTweak];
}

// ── TweaksPanel ─────────────────────────────────────────────────────────────
// Floating shell. Registers the protocol listener BEFORE announcing
// availability — if the announce ran first, the host's activate could land
// before our handler exists and the toolbar toggle would silently no-op.
// The close button posts __edit_mode_dismissed so the host's toolbar toggle
// flips off in lockstep; the host echoes __deactivate_edit_mode back which
// is what actually hides the panel.
function TweaksPanel({
  title = 'Tweaks',
  children
}) {
  const [open, setOpen] = React.useState(false);
  const dragRef = React.useRef(null);
  const offsetRef = React.useRef({
    x: 16,
    y: 16
  });
  const PAD = 16;
  const clampToViewport = React.useCallback(() => {
    const panel = dragRef.current;
    if (!panel) return;
    const w = panel.offsetWidth,
      h = panel.offsetHeight;
    const maxRight = Math.max(PAD, window.innerWidth - w - PAD);
    const maxBottom = Math.max(PAD, window.innerHeight - h - PAD);
    offsetRef.current = {
      x: Math.min(maxRight, Math.max(PAD, offsetRef.current.x)),
      y: Math.min(maxBottom, Math.max(PAD, offsetRef.current.y))
    };
    panel.style.right = offsetRef.current.x + 'px';
    panel.style.bottom = offsetRef.current.y + 'px';
  }, []);
  React.useEffect(() => {
    if (!open) return;
    clampToViewport();
    if (typeof ResizeObserver === 'undefined') {
      window.addEventListener('resize', clampToViewport);
      return () => window.removeEventListener('resize', clampToViewport);
    }
    const ro = new ResizeObserver(clampToViewport);
    ro.observe(document.documentElement);
    return () => ro.disconnect();
  }, [open, clampToViewport]);
  React.useEffect(() => {
    const onMsg = e => {
      const t = e?.data?.type;
      if (t === '__activate_edit_mode') setOpen(true);else if (t === '__deactivate_edit_mode') setOpen(false);
    };
    window.addEventListener('message', onMsg);
    window.parent.postMessage({
      type: '__edit_mode_available'
    }, '*');
    return () => window.removeEventListener('message', onMsg);
  }, []);
  const dismiss = () => {
    setOpen(false);
    window.parent.postMessage({
      type: '__edit_mode_dismissed'
    }, '*');
  };
  const onDragStart = e => {
    const panel = dragRef.current;
    if (!panel) return;
    const r = panel.getBoundingClientRect();
    const sx = e.clientX,
      sy = e.clientY;
    const startRight = window.innerWidth - r.right;
    const startBottom = window.innerHeight - r.bottom;
    const move = ev => {
      offsetRef.current = {
        x: startRight - (ev.clientX - sx),
        y: startBottom - (ev.clientY - sy)
      };
      clampToViewport();
    };
    const up = () => {
      window.removeEventListener('mousemove', move);
      window.removeEventListener('mouseup', up);
    };
    window.addEventListener('mousemove', move);
    window.addEventListener('mouseup', up);
  };
  if (!open) return null;
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("style", null, __TWEAKS_STYLE), /*#__PURE__*/React.createElement("div", {
    ref: dragRef,
    className: "twk-panel",
    "data-omelette-chrome": "",
    style: {
      right: offsetRef.current.x,
      bottom: offsetRef.current.y
    }
  }, /*#__PURE__*/React.createElement("div", {
    className: "twk-hd",
    onMouseDown: onDragStart
  }, /*#__PURE__*/React.createElement("b", null, title), /*#__PURE__*/React.createElement("button", {
    className: "twk-x",
    "aria-label": "Close tweaks",
    onMouseDown: e => e.stopPropagation(),
    onClick: dismiss
  }, "\u2715")), /*#__PURE__*/React.createElement("div", {
    className: "twk-body"
  }, children)));
}

// ── Layout helpers ──────────────────────────────────────────────────────────

function TweakSection({
  label,
  children
}) {
  return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
    className: "twk-sect"
  }, label), children);
}
function TweakRow({
  label,
  value,
  children,
  inline = false
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: inline ? 'twk-row twk-row-h' : 'twk-row'
  }, /*#__PURE__*/React.createElement("div", {
    className: "twk-lbl"
  }, /*#__PURE__*/React.createElement("span", null, label), value != null && /*#__PURE__*/React.createElement("span", {
    className: "twk-val"
  }, value)), children);
}

// ── Controls ────────────────────────────────────────────────────────────────

function TweakSlider({
  label,
  value,
  min = 0,
  max = 100,
  step = 1,
  unit = '',
  onChange
}) {
  return /*#__PURE__*/React.createElement(TweakRow, {
    label: label,
    value: `${value}${unit}`
  }, /*#__PURE__*/React.createElement("input", {
    type: "range",
    className: "twk-slider",
    min: min,
    max: max,
    step: step,
    value: value,
    onChange: e => onChange(Number(e.target.value))
  }));
}
function TweakToggle({
  label,
  value,
  onChange
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "twk-row twk-row-h"
  }, /*#__PURE__*/React.createElement("div", {
    className: "twk-lbl"
  }, /*#__PURE__*/React.createElement("span", null, label)), /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: "twk-toggle",
    "data-on": value ? '1' : '0',
    role: "switch",
    "aria-checked": !!value,
    onClick: () => onChange(!value)
  }, /*#__PURE__*/React.createElement("i", null)));
}
function TweakRadio({
  label,
  value,
  options,
  onChange
}) {
  const trackRef = React.useRef(null);
  const [dragging, setDragging] = React.useState(false);
  // The active value is read by pointer-move handlers attached for the lifetime
  // of a drag — ref it so a stale closure doesn't fire onChange for every move.
  const valueRef = React.useRef(value);
  valueRef.current = value;

  // Segments wrap mid-word once per-segment width runs out. The track is
  // ~248px (280 panel − 28 body pad − 4 seg pad), each button loses 12px
  // to its own padding, and 11.5px system-ui averages ~6.3px/char — so 2
  // options fit ~16 chars each, 3 fit ~10. Past that (or >3 options), fall
  // back to a dropdown rather than wrap.
  const labelLen = o => String(typeof o === 'object' ? o.label : o).length;
  const maxLen = options.reduce((m, o) => Math.max(m, labelLen(o)), 0);
  const fitsAsSegments = maxLen <= ({
    2: 16,
    3: 10
  }[options.length] ?? 0);
  if (!fitsAsSegments) {
    // <select> emits strings — map back to the original option value so the
    // fallback stays type-preserving (numbers, booleans) like the segment path.
    const resolve = s => {
      const m = options.find(o => String(typeof o === 'object' ? o.value : o) === s);
      return m === undefined ? s : typeof m === 'object' ? m.value : m;
    };
    return /*#__PURE__*/React.createElement(TweakSelect, {
      label: label,
      value: value,
      options: options,
      onChange: s => onChange(resolve(s))
    });
  }
  const opts = options.map(o => typeof o === 'object' ? o : {
    value: o,
    label: o
  });
  const idx = Math.max(0, opts.findIndex(o => o.value === value));
  const n = opts.length;
  const segAt = clientX => {
    const r = trackRef.current.getBoundingClientRect();
    const inner = r.width - 4;
    const i = Math.floor((clientX - r.left - 2) / inner * n);
    return opts[Math.max(0, Math.min(n - 1, i))].value;
  };
  const onPointerDown = e => {
    setDragging(true);
    const v0 = segAt(e.clientX);
    if (v0 !== valueRef.current) onChange(v0);
    const move = ev => {
      if (!trackRef.current) return;
      const v = segAt(ev.clientX);
      if (v !== valueRef.current) onChange(v);
    };
    const up = () => {
      setDragging(false);
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', up);
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
  };
  return /*#__PURE__*/React.createElement(TweakRow, {
    label: label
  }, /*#__PURE__*/React.createElement("div", {
    ref: trackRef,
    role: "radiogroup",
    onPointerDown: onPointerDown,
    className: dragging ? 'twk-seg dragging' : 'twk-seg'
  }, /*#__PURE__*/React.createElement("div", {
    className: "twk-seg-thumb",
    style: {
      left: `calc(2px + ${idx} * (100% - 4px) / ${n})`,
      width: `calc((100% - 4px) / ${n})`
    }
  }), opts.map(o => /*#__PURE__*/React.createElement("button", {
    key: o.value,
    type: "button",
    role: "radio",
    "aria-checked": o.value === value
  }, o.label))));
}
function TweakSelect({
  label,
  value,
  options,
  onChange
}) {
  return /*#__PURE__*/React.createElement(TweakRow, {
    label: label
  }, /*#__PURE__*/React.createElement("select", {
    className: "twk-field",
    value: value,
    onChange: e => onChange(e.target.value)
  }, options.map(o => {
    const v = typeof o === 'object' ? o.value : o;
    const l = typeof o === 'object' ? o.label : o;
    return /*#__PURE__*/React.createElement("option", {
      key: v,
      value: v
    }, l);
  })));
}
function TweakText({
  label,
  value,
  placeholder,
  onChange
}) {
  return /*#__PURE__*/React.createElement(TweakRow, {
    label: label
  }, /*#__PURE__*/React.createElement("input", {
    className: "twk-field",
    type: "text",
    value: value,
    placeholder: placeholder,
    onChange: e => onChange(e.target.value)
  }));
}
function TweakNumber({
  label,
  value,
  min,
  max,
  step = 1,
  unit = '',
  onChange
}) {
  const clamp = n => {
    if (min != null && n < min) return min;
    if (max != null && n > max) return max;
    return n;
  };
  const startRef = React.useRef({
    x: 0,
    val: 0
  });
  const onScrubStart = e => {
    e.preventDefault();
    startRef.current = {
      x: e.clientX,
      val: value
    };
    const decimals = (String(step).split('.')[1] || '').length;
    const move = ev => {
      const dx = ev.clientX - startRef.current.x;
      const raw = startRef.current.val + dx * step;
      const snapped = Math.round(raw / step) * step;
      onChange(clamp(Number(snapped.toFixed(decimals))));
    };
    const up = () => {
      window.removeEventListener('pointermove', move);
      window.removeEventListener('pointerup', up);
    };
    window.addEventListener('pointermove', move);
    window.addEventListener('pointerup', up);
  };
  return /*#__PURE__*/React.createElement("div", {
    className: "twk-num"
  }, /*#__PURE__*/React.createElement("span", {
    className: "twk-num-lbl",
    onPointerDown: onScrubStart
  }, label), /*#__PURE__*/React.createElement("input", {
    type: "number",
    value: value,
    min: min,
    max: max,
    step: step,
    onChange: e => onChange(clamp(Number(e.target.value)))
  }), unit && /*#__PURE__*/React.createElement("span", {
    className: "twk-num-unit"
  }, unit));
}

// Relative-luminance contrast pick — checkmarks drawn over a swatch need to
// read on both #111 and #fafafa without per-option configuration. Hex input
// only (#rgb / #rrggbb); named or rgb()/hsl() colors fall through to "light".
function __twkIsLight(hex) {
  const h = String(hex).replace('#', '');
  const x = h.length === 3 ? h.replace(/./g, c => c + c) : h.padEnd(6, '0');
  const n = parseInt(x.slice(0, 6), 16);
  if (Number.isNaN(n)) return true;
  const r = n >> 16 & 255,
    g = n >> 8 & 255,
    b = n & 255;
  return r * 299 + g * 587 + b * 114 > 148000;
}
const __TwkCheck = ({
  light
}) => /*#__PURE__*/React.createElement("svg", {
  viewBox: "0 0 14 14",
  "aria-hidden": "true"
}, /*#__PURE__*/React.createElement("path", {
  d: "M3 7.2 5.8 10 11 4.2",
  fill: "none",
  strokeWidth: "2.2",
  strokeLinecap: "round",
  strokeLinejoin: "round",
  stroke: light ? 'rgba(0,0,0,.78)' : '#fff'
}));

// TweakColor — curated color/palette picker. Each option is either a single
// hex string or an array of 1-5 hex strings; the card adapts — a lone color
// renders solid, a palette renders colors[0] as the hero (left ~2/3) with the
// rest stacked in a sharp column on the right. onChange emits the
// option in the shape it was passed (string stays string, array stays array).
// Without options it falls back to the native color input for back-compat.
function TweakColor({
  label,
  value,
  options,
  onChange
}) {
  if (!options || !options.length) {
    return /*#__PURE__*/React.createElement("div", {
      className: "twk-row twk-row-h"
    }, /*#__PURE__*/React.createElement("div", {
      className: "twk-lbl"
    }, /*#__PURE__*/React.createElement("span", null, label)), /*#__PURE__*/React.createElement("input", {
      type: "color",
      className: "twk-swatch",
      value: value,
      onChange: e => onChange(e.target.value)
    }));
  }
  // Native <input type=color> emits lowercase hex per the HTML spec, so
  // compare case-insensitively. String() guards JSON.stringify(undefined),
  // which returns the primitive undefined (no .toLowerCase).
  const key = o => String(JSON.stringify(o)).toLowerCase();
  const cur = key(value);
  return /*#__PURE__*/React.createElement(TweakRow, {
    label: label
  }, /*#__PURE__*/React.createElement("div", {
    className: "twk-chips",
    role: "radiogroup"
  }, options.map((o, i) => {
    const colors = Array.isArray(o) ? o : [o];
    const [hero, ...rest] = colors;
    const sup = rest.slice(0, 4);
    const on = key(o) === cur;
    return /*#__PURE__*/React.createElement("button", {
      key: i,
      type: "button",
      className: "twk-chip",
      role: "radio",
      "aria-checked": on,
      "data-on": on ? '1' : '0',
      "aria-label": colors.join(', '),
      title: colors.join(' · '),
      style: {
        background: hero
      },
      onClick: () => onChange(o)
    }, sup.length > 0 && /*#__PURE__*/React.createElement("span", null, sup.map((c, j) => /*#__PURE__*/React.createElement("i", {
      key: j,
      style: {
        background: c
      }
    }))), on && /*#__PURE__*/React.createElement(__TwkCheck, {
      light: __twkIsLight(hero)
    }));
  })));
}
function TweakButton({
  label,
  onClick,
  secondary = false
}) {
  return /*#__PURE__*/React.createElement("button", {
    type: "button",
    className: secondary ? 'twk-btn secondary' : 'twk-btn',
    onClick: onClick
  }, label);
}
Object.assign(window, {
  useTweaks,
  TweaksPanel,
  TweakSection,
  TweakRow,
  TweakSlider,
  TweakToggle,
  TweakRadio,
  TweakSelect,
  TweakText,
  TweakNumber,
  TweakColor,
  TweakButton
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/calm-studio/tweaks-panel.jsx", error: String((e && e.message) || e) }); }

// ui_kits/material/android-frame.jsx
try { (() => {
// @ds-adherence-ignore -- omelette starter scaffold (raw elements/hex/px by design)

/* BEGIN USAGE */
// Android.jsx — Simplified Android (Material 3) device frame
// Status bar + top app bar + content + gesture nav + keyboard.
// Based on Figma M3 spec. No dependencies, no image assets.
// Exports (to window): AndroidDevice, AndroidStatusBar, AndroidAppBar, AndroidListItem, AndroidNavBar, AndroidKeyboard
//
// Usage — wrap your screen content in <AndroidDevice> to get the bezel, status
// bar and gesture nav (props: title, large, keyboard, dark):
//
//   <AndroidDevice title="Inbox" large>
//     ...your screen content...
//   </AndroidDevice>
//   <AndroidDevice title="Compose" keyboard>…</AndroidDevice>
/* END USAGE */

const MD_C = {
  surface: '#f4fbf8',
  surfaceVariant: '#dae5e1',
  inverseOnSurface: '#ecf2ef',
  secondaryContainer: '#cde8e1',
  primaryFixedDim: '#83d5c6',
  onSurface: '#171d1b',
  onSurfaceVar: '#49454f',
  onPrimaryContainer: '#00201c',
  primary: '#006a60',
  frameBorder: 'rgba(116,119,117,0.5)'
};

// ─────────────────────────────────────────────────────────────
// Status bar (time left, wifi/cell/battery right)
// ─────────────────────────────────────────────────────────────
function AndroidStatusBar({
  dark = false
}) {
  const c = dark ? '#fff' : MD_C.onSurface;
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: 40,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'space-between',
      padding: '0 16px',
      position: 'relative',
      fontFamily: 'Roboto, system-ui, sans-serif'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 128,
      display: 'flex',
      alignItems: 'center',
      gap: 8
    }
  }, /*#__PURE__*/React.createElement("span", {
    style: {
      fontSize: 14,
      fontWeight: 400,
      letterSpacing: 0.25,
      lineHeight: '20px',
      color: c
    }
  }, "9:30")), /*#__PURE__*/React.createElement("div", {
    style: {
      position: 'absolute',
      left: '50%',
      top: 8,
      transform: 'translateX(-50%)',
      width: 24,
      height: 24,
      borderRadius: 100,
      background: '#2e2e2e'
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      paddingRight: 2
    }
  }, /*#__PURE__*/React.createElement("svg", {
    width: "16",
    height: "16",
    viewBox: "0 0 16 16",
    style: {
      marginRight: -2
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: "M8 13.3L.67 5.97a10.37 10.37 0 0114.66 0L8 13.3z",
    fill: c
  })), /*#__PURE__*/React.createElement("svg", {
    width: "16",
    height: "16",
    viewBox: "0 0 16 16",
    style: {
      marginRight: -2
    }
  }, /*#__PURE__*/React.createElement("path", {
    d: "M14.67 14.67V1.33L1.33 14.67h13.34z",
    fill: c
  }))), /*#__PURE__*/React.createElement("svg", {
    width: "16",
    height: "16",
    viewBox: "0 0 16 16"
  }, /*#__PURE__*/React.createElement("rect", {
    x: "3.75",
    y: "2",
    width: "8.5",
    height: "13",
    rx: "1.5",
    fill: c
  }), /*#__PURE__*/React.createElement("rect", {
    x: "5.5",
    y: "0.9",
    width: "5",
    height: "2",
    rx: "0.5",
    fill: c
  }))));
}

// ─────────────────────────────────────────────────────────────
// Top app bar (Material 3 small/medium)
// ─────────────────────────────────────────────────────────────
function AndroidAppBar({
  title = 'Title',
  large = false
}) {
  const iconDot = /*#__PURE__*/React.createElement("div", {
    style: {
      width: 48,
      height: 48,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 22,
      height: 22,
      borderRadius: '50%',
      background: MD_C.onSurfaceVar,
      opacity: 0.3
    }
  }));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      background: MD_C.surface,
      padding: '4px 4px 0'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      height: 56,
      display: 'flex',
      alignItems: 'center',
      gap: 4
    }
  }, iconDot, !large && /*#__PURE__*/React.createElement("span", {
    style: {
      flex: 1,
      fontSize: 22,
      fontWeight: 400,
      color: MD_C.onSurface,
      fontFamily: 'Roboto, system-ui, sans-serif'
    }
  }, title), large && /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1
    }
  }), iconDot), large && /*#__PURE__*/React.createElement("div", {
    style: {
      padding: '16px 16px 20px',
      fontSize: 28,
      fontWeight: 400,
      color: MD_C.onSurface,
      fontFamily: 'Roboto, system-ui, sans-serif'
    }
  }, title));
}

// ─────────────────────────────────────────────────────────────
// List item (Material 3)
// ─────────────────────────────────────────────────────────────
function AndroidListItem({
  headline,
  supporting,
  leading
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      alignItems: 'center',
      gap: 16,
      padding: '12px 16px',
      minHeight: 56,
      boxSizing: 'border-box',
      fontFamily: 'Roboto, system-ui, sans-serif'
    }
  }, leading && /*#__PURE__*/React.createElement("div", {
    style: {
      width: 40,
      height: 40,
      borderRadius: '50%',
      background: MD_C.primary,
      color: '#fff',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontSize: 18,
      fontWeight: 500,
      flexShrink: 0
    }
  }, leading), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      minWidth: 0
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 16,
      color: MD_C.onSurface,
      lineHeight: '24px'
    }
  }, headline), supporting && /*#__PURE__*/React.createElement("div", {
    style: {
      fontSize: 14,
      color: MD_C.onSurfaceVar,
      lineHeight: '20px'
    }
  }, supporting)));
}

// ─────────────────────────────────────────────────────────────
// Gesture nav bar (pill)
// ─────────────────────────────────────────────────────────────
function AndroidNavBar({
  dark = false
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      height: 24,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center'
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 108,
      height: 4,
      borderRadius: 2,
      background: dark ? '#fff' : MD_C.onSurface,
      opacity: 0.4
    }
  }));
}

// ─────────────────────────────────────────────────────────────
// Device frame — wraps everything
// ─────────────────────────────────────────────────────────────
function AndroidDevice({
  children,
  width = 412,
  height = 892,
  dark = false,
  title,
  large = false,
  keyboard = false
}) {
  return /*#__PURE__*/React.createElement("div", {
    style: {
      width,
      height,
      borderRadius: 18,
      overflow: 'hidden',
      background: dark ? '#1d1b20' : MD_C.surface,
      border: `8px solid ${MD_C.frameBorder}`,
      boxShadow: '0 30px 80px rgba(0,0,0,0.25)',
      display: 'flex',
      flexDirection: 'column',
      boxSizing: 'border-box'
    }
  }, /*#__PURE__*/React.createElement(AndroidStatusBar, {
    dark: dark
  }), title !== undefined && /*#__PURE__*/React.createElement(AndroidAppBar, {
    title: title,
    large: large
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      flex: 1,
      overflow: 'auto'
    }
  }, children), keyboard && /*#__PURE__*/React.createElement(AndroidKeyboard, null), /*#__PURE__*/React.createElement(AndroidNavBar, {
    dark: dark
  }));
}

// ─────────────────────────────────────────────────────────────
// Keyboard — Gboard (Material 3)
// ─────────────────────────────────────────────────────────────
function AndroidKeyboard() {
  let _k = 0;
  const key = (l, {
    flex = 1,
    bg = MD_C.surface,
    r = 6,
    minW,
    fs = 21
  } = {}) => /*#__PURE__*/React.createElement("div", {
    key: _k++,
    style: {
      height: 46,
      borderRadius: r,
      flex,
      minWidth: minW,
      background: bg,
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      fontFamily: 'Roboto, system-ui',
      fontSize: fs,
      color: MD_C.onPrimaryContainer
    }
  }, l);
  const row = (keys, style = {}) => /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 6,
      justifyContent: 'center',
      ...style
    }
  }, keys.map(l => key(l)));
  return /*#__PURE__*/React.createElement("div", {
    style: {
      background: MD_C.inverseOnSurface,
      padding: '0 8px 8px',
      display: 'flex',
      flexDirection: 'column',
      gap: 4
    }
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      height: 44
    }
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      flexDirection: 'column',
      gap: 12
    }
  }, row(['q', 'w', 'e', 'r', 't', 'y', 'u', 'i', 'o', 'p']), row(['a', 's', 'd', 'f', 'g', 'h', 'j', 'k', 'l'], {
    padding: '0 20px'
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 6
    }
  }, key('', {
    bg: MD_C.surfaceVariant
  }), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 6,
      flex: 7,
      minWidth: 274
    }
  }, ['z', 'x', 'c', 'v', 'b', 'n', 'm'].map(l => key(l))), key('', {
    bg: MD_C.surfaceVariant
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      display: 'flex',
      gap: 6
    }
  }, key('?123', {
    bg: MD_C.secondaryContainer,
    r: 100,
    minW: 58,
    fs: 14
  }), key(',', {
    bg: MD_C.surfaceVariant
  }), key('', {
    flex: 3,
    minW: 154
  }), key('.', {
    bg: MD_C.surfaceVariant
  }), key('', {
    bg: MD_C.primaryFixedDim,
    r: 100,
    minW: 58
  }))));
}
Object.assign(window, {
  AndroidDevice,
  AndroidStatusBar,
  AndroidAppBar,
  AndroidListItem,
  AndroidNavBar,
  AndroidKeyboard
});
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/material/android-frame.jsx", error: String((e && e.message) || e) }); }

// ui_kits/material/material-app.jsx
try { (() => {
function _extends() { return _extends = Object.assign ? Object.assign.bind() : function (n) { for (var e = 1; e < arguments.length; e++) { var t = arguments[e]; for (var r in t) ({}).hasOwnProperty.call(t, r) && (n[r] = t[r]); } return n; }, _extends.apply(null, arguments); }
/* WishlistApp Material 3 app — shared by the Android and Desktop UI kits.
 * Recreates the Compose Material 3 client screens. Exposes window.MaterialApp.
 * Depends on window.WL_DATA (data.js). */
(function () {
  const {
    useState
  } = React;
  const D = window.WL_DATA;
  const SORTS = ["Default", "Cost", "Priority", "Title"];
  const PRI = {
    High: 3,
    Custom: 2.5,
    Medium: 2,
    Small: 1
  };
  const PLABEL = {
    Small: "Low",
    Medium: "Medium",
    High: "High",
    Custom: "Custom"
  };
  function sortItems(items, mode) {
    const a = [...items];
    if (mode === "Cost") a.sort((x, y) => (x.price || 0) - (y.price || 0));else if (mode === "Title") a.sort((x, y) => x.title.localeCompare(y.title));else if (mode === "Priority") a.sort((x, y) => (PRI[y.priority] || 0) - (PRI[x.priority] || 0));
    return a;
  }
  const pLabel = it => PLABEL[it.priority] + (it.priority === "Custom" && it.weight != null ? ` (${it.weight})` : "");
  function Btn({
    kind = "filled",
    className = "",
    ...p
  }) {
    const c = ["m3-btn"];
    if (kind === "outlined") c.push("m3-btn--outlined");
    if (kind === "text") c.push("m3-btn--text");
    if (className) c.push(className);
    return /*#__PURE__*/React.createElement("button", _extends({
      className: c.join(" ")
    }, p));
  }
  const Badge = ({
    it
  }) => /*#__PURE__*/React.createElement("span", {
    className: "m3-badge"
  }, pLabel(it));

  /* ---- screens ---- */
  function Users({
    me,
    nav
  }) {
    return /*#__PURE__*/React.createElement("div", {
      className: "m3-list"
    }, D.users.map(u => /*#__PURE__*/React.createElement("div", {
      className: "m3-row",
      key: u.id,
      onClick: () => nav.push("wishlists", {
        userId: u.id
      })
    }, /*#__PURE__*/React.createElement("img", {
      className: "m3-row__thumb m3-row__thumb--circle",
      src: "../../assets/user-silhouette.svg",
      alt: ""
    }), /*#__PURE__*/React.createElement("div", {
      className: "m3-row__main"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-row__title"
    }, u.username, u.you && /*#__PURE__*/React.createElement("span", {
      className: "m3-muted"
    }, " (you)")), u.admin && /*#__PURE__*/React.createElement("div", {
      className: "m3-row__sub"
    }, "admin")))));
  }
  function UserWishlists({
    me,
    params,
    nav
  }) {
    const lists = D.wishlistsByOwner(params.userId);
    const isOwner = me && me.id === params.userId;
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
      className: "m3-toolbar"
    }, /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      className: "m3-btn--sm",
      onClick: nav.pop
    }, "Back"), /*#__PURE__*/React.createElement("span", {
      className: "m3-spacer"
    }), /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      className: "m3-btn--sm",
      onClick: () => nav.push("allItems", {
        userId: params.userId
      })
    }, "All items"), isOwner && /*#__PURE__*/React.createElement(Btn, {
      className: "m3-btn--sm",
      onClick: () => {}
    }, "New Wishlist")), lists.length === 0 ? /*#__PURE__*/React.createElement("p", {
      className: "m3-muted"
    }, "No wishlists yet") : /*#__PURE__*/React.createElement("div", {
      className: "m3-list"
    }, lists.map(w => /*#__PURE__*/React.createElement("div", {
      className: "m3-row",
      key: w.id,
      onClick: () => nav.push("wishlist", {
        wishlistId: w.id
      })
    }, /*#__PURE__*/React.createElement("img", {
      className: "m3-row__thumb",
      src: "../../assets/stacked-items.svg",
      alt: ""
    }), /*#__PURE__*/React.createElement("div", {
      className: "m3-row__main"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-row__title"
    }, w.title))))));
  }
  function AllItems({
    params,
    nav
  }) {
    const lists = D.wishlistsByOwner(params.userId);
    const items = lists.flatMap(w => D.itemsByWishlist(w.id).map(i => ({
      ...i,
      wishlistTitle: w.title
    })));
    const [sort, setSort] = useState("Default");
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
      className: "m3-toolbar"
    }, /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      className: "m3-btn--sm",
      onClick: nav.pop
    }, "Back")), /*#__PURE__*/React.createElement("div", {
      className: "m3-selectors"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield",
      style: {
        marginBottom: 0,
        minWidth: 140
      }
    }, /*#__PURE__*/React.createElement("label", null, "Sort"), /*#__PURE__*/React.createElement("select", {
      value: sort,
      onChange: e => setSort(e.target.value)
    }, SORTS.map(s => /*#__PURE__*/React.createElement("option", {
      key: s
    }, s))))), /*#__PURE__*/React.createElement(Grid, {
      items: sortItems(items, sort),
      nav: nav,
      withSub: true
    }));
  }
  const Grid = ({
    items,
    nav,
    withSub
  }) => /*#__PURE__*/React.createElement("div", {
    className: "m3-grid"
  }, items.map(it => /*#__PURE__*/React.createElement("div", {
    className: "m3-card",
    key: it.id,
    onClick: () => nav.push("item", {
      itemId: it.id
    })
  }, /*#__PURE__*/React.createElement("span", {
    className: "m3-card__badge"
  }, /*#__PURE__*/React.createElement(Badge, {
    it: it
  })), /*#__PURE__*/React.createElement("img", {
    className: "m3-card__media m3-card__media--placeholder",
    src: "../../assets/giftbox.svg",
    alt: ""
  }), /*#__PURE__*/React.createElement("div", {
    className: "m3-card__body"
  }, /*#__PURE__*/React.createElement("p", {
    className: "m3-card__title"
  }, it.title), withSub && it.wishlistTitle && /*#__PURE__*/React.createElement("p", {
    className: "m3-card__subtitle"
  }, it.wishlistTitle), it.description && /*#__PURE__*/React.createElement("p", {
    className: "m3-card__desc"
  }, it.description), D.priceText(it) && /*#__PURE__*/React.createElement("p", {
    className: "m3-card__price"
  }, D.priceText(it))))));
  function Wishlist({
    me,
    params,
    nav
  }) {
    const w = D.wishlistById(params.wishlistId);
    const items = D.itemsByWishlist(w.id);
    const isOwner = me && me.id === w.ownerId;
    const [sort, setSort] = useState("Default");
    const [view, setView] = useState("Grid");
    const [copied, setCopied] = useState(false);
    const sorted = sortItems(items, sort);
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
      className: "m3-toolbar"
    }, /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      className: "m3-btn--sm",
      onClick: nav.pop
    }, "Back"), /*#__PURE__*/React.createElement("span", {
      className: "m3-spacer"
    }), !isOwner && /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      className: "m3-btn--sm",
      disabled: copied,
      onClick: () => setCopied(true)
    }, "Copy to my profile"), isOwner && /*#__PURE__*/React.createElement(Btn, {
      className: "m3-btn--sm",
      onClick: () => {}
    }, "Edit")), copied && /*#__PURE__*/React.createElement("p", {
      className: "m3-status"
    }, "Copy queued. It will appear in your profile shortly."), /*#__PURE__*/React.createElement("div", {
      className: "m3-selectors"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield",
      style: {
        marginBottom: 0,
        minWidth: 140
      }
    }, /*#__PURE__*/React.createElement("label", null, "Sort"), /*#__PURE__*/React.createElement("select", {
      value: sort,
      onChange: e => setSort(e.target.value)
    }, SORTS.map(s => /*#__PURE__*/React.createElement("option", {
      key: s
    }, s)))), /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield",
      style: {
        marginBottom: 0,
        minWidth: 120
      }
    }, /*#__PURE__*/React.createElement("label", null, "View"), /*#__PURE__*/React.createElement("select", {
      value: view,
      onChange: e => setView(e.target.value)
    }, ["Grid", "List"].map(s => /*#__PURE__*/React.createElement("option", {
      key: s
    }, s))))), items.length === 0 ? /*#__PURE__*/React.createElement("p", {
      className: "m3-muted"
    }, "No items yet") : view === "Grid" ? /*#__PURE__*/React.createElement(Grid, {
      items: sorted,
      nav: nav
    }) : /*#__PURE__*/React.createElement("div", {
      className: "m3-list"
    }, sorted.map(it => /*#__PURE__*/React.createElement("div", {
      className: "m3-row",
      key: it.id,
      onClick: () => nav.push("item", {
        itemId: it.id
      })
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-row__main"
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        alignItems: "center",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement("span", {
      className: "m3-row__title"
    }, it.title), /*#__PURE__*/React.createElement(Badge, {
      it: it
    })), D.priceText(it) && /*#__PURE__*/React.createElement("span", {
      className: "m3-row__sub"
    }, D.priceText(it))), it.description && /*#__PURE__*/React.createElement("div", {
      className: "m3-row__sub"
    }, it.description))))), isOwner && /*#__PURE__*/React.createElement("div", {
      style: {
        marginTop: 12
      }
    }, /*#__PURE__*/React.createElement(Btn, {
      className: "m3-btn--full",
      onClick: () => nav.push("itemEdit", {
        wishlistId: w.id
      })
    }, "Add Item")));
  }
  function Item({
    me,
    params,
    nav
  }) {
    const it = D.itemById(params.itemId);
    const w = D.wishlistById(it.wishlistId);
    const isOwner = me && me.id === w.ownerId;
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
      className: "m3-toolbar"
    }, /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      className: "m3-btn--sm",
      onClick: nav.pop
    }, "Back"), /*#__PURE__*/React.createElement("span", {
      className: "m3-spacer"
    }), !isOwner && /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      className: "m3-btn--sm",
      onClick: () => {}
    }, "Copy to my wishlist"), isOwner && /*#__PURE__*/React.createElement(Btn, {
      className: "m3-btn--sm",
      onClick: () => nav.push("itemEdit", {
        itemId: it.id,
        wishlistId: w.id
      })
    }, "Edit")), it.description && /*#__PURE__*/React.createElement("div", {
      className: "m3-field"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-field__label"
    }, "Description"), /*#__PURE__*/React.createElement("div", {
      className: "m3-field__value"
    }, it.description)), /*#__PURE__*/React.createElement("div", {
      className: "m3-field"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-field__label"
    }, "Approximate price"), /*#__PURE__*/React.createElement("div", {
      className: "m3-field__value"
    }, D.priceText(it) || /*#__PURE__*/React.createElement("span", {
      className: "m3-muted"
    }, "No price"))), /*#__PURE__*/React.createElement("div", {
      className: "m3-field"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-field__label"
    }, "Priority"), /*#__PURE__*/React.createElement("div", null, /*#__PURE__*/React.createElement(Badge, {
      it: it
    }))), /*#__PURE__*/React.createElement("div", {
      className: "m3-field"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-field__label"
    }, "Links"), it.links.length === 0 ? /*#__PURE__*/React.createElement("div", {
      className: "m3-muted"
    }, "No links") : it.links.map((l, i) => /*#__PURE__*/React.createElement("div", {
      key: i
    }, /*#__PURE__*/React.createElement("a", {
      href: l.url,
      target: "_blank",
      rel: "noreferrer"
    }, l.title || l.url)))), /*#__PURE__*/React.createElement("div", {
      className: "m3-field"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-field__label"
    }, "Images"), /*#__PURE__*/React.createElement("img", {
      src: "../../assets/giftbox.svg",
      alt: "",
      style: {
        width: 160,
        height: 160,
        borderRadius: "var(--m3-corner-md)",
        objectFit: "cover"
      }
    })));
  }
  function ItemEdit({
    params,
    nav
  }) {
    const editing = params.itemId ? D.itemById(params.itemId) : null;
    const [title, setTitle] = useState(editing ? editing.title : "");
    const [desc, setDesc] = useState(editing ? editing.description : "");
    const [price, setPrice] = useState(editing ? editing.price : "");
    const [priority, setPriority] = useState(editing ? editing.priority : "Medium");
    const [confirm, setConfirm] = useState(false);
    return /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement("div", {
      className: "m3-toolbar"
    }, /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      className: "m3-btn--sm",
      onClick: nav.pop
    }, "Back")), /*#__PURE__*/React.createElement("div", {
      style: {
        maxWidth: 460
      }
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield"
    }, /*#__PURE__*/React.createElement("label", null, "Title"), /*#__PURE__*/React.createElement("input", {
      value: title,
      onChange: e => setTitle(e.target.value)
    })), /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield"
    }, /*#__PURE__*/React.createElement("label", null, "Description"), /*#__PURE__*/React.createElement("input", {
      value: desc,
      onChange: e => setDesc(e.target.value)
    })), /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield"
    }, /*#__PURE__*/React.createElement("label", null, "Approximate price"), /*#__PURE__*/React.createElement("input", {
      type: "number",
      value: price,
      onChange: e => setPrice(e.target.value)
    })), /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield"
    }, /*#__PURE__*/React.createElement("label", null, "Priority"), /*#__PURE__*/React.createElement("select", {
      value: priority,
      onChange: e => setPriority(e.target.value)
    }, /*#__PURE__*/React.createElement("option", {
      value: "Small"
    }, "Low"), /*#__PURE__*/React.createElement("option", {
      value: "Medium"
    }, "Medium"), /*#__PURE__*/React.createElement("option", {
      value: "High"
    }, "High"), /*#__PURE__*/React.createElement("option", {
      value: "Custom"
    }, "Custom"))), /*#__PURE__*/React.createElement("div", {
      style: {
        display: "flex",
        gap: 8
      }
    }, /*#__PURE__*/React.createElement(Btn, {
      disabled: !title.trim(),
      onClick: nav.pop
    }, "Save"), editing && /*#__PURE__*/React.createElement(Btn, {
      kind: "outlined",
      onClick: () => setConfirm(true),
      style: {
        color: "var(--m3-error)",
        borderColor: "var(--m3-error)"
      }
    }, "Delete"))), confirm && /*#__PURE__*/React.createElement("div", {
      className: "m3-scrim",
      onClick: () => setConfirm(false)
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-dialog",
      onClick: e => e.stopPropagation()
    }, /*#__PURE__*/React.createElement("h3", {
      className: "m3-dialog__title"
    }, "Delete item?"), /*#__PURE__*/React.createElement("div", null, "This item will be permanently removed. Continue?"), /*#__PURE__*/React.createElement("div", {
      className: "m3-dialog__actions"
    }, /*#__PURE__*/React.createElement(Btn, {
      kind: "text",
      onClick: () => setConfirm(false)
    }, "Cancel"), /*#__PURE__*/React.createElement(Btn, {
      kind: "text",
      onClick: () => {
        setConfirm(false);
        nav.pop();
      },
      style: {
        color: "var(--m3-error)"
      }
    }, "Delete")))));
  }
  function LoginDialog({
    onClose,
    onLogin,
    register
  }) {
    const [u, setU] = useState("you");
    const [p, setP] = useState("");
    return /*#__PURE__*/React.createElement("div", {
      className: "m3-scrim",
      onClick: onClose
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-dialog",
      onClick: e => e.stopPropagation()
    }, /*#__PURE__*/React.createElement("h3", {
      className: "m3-dialog__title"
    }, register ? "Register" : "Log in"), /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield"
    }, /*#__PURE__*/React.createElement("label", null, "Username"), /*#__PURE__*/React.createElement("input", {
      value: u,
      onChange: e => setU(e.target.value)
    })), /*#__PURE__*/React.createElement("div", {
      className: "m3-textfield"
    }, /*#__PURE__*/React.createElement("label", null, "Password"), /*#__PURE__*/React.createElement("input", {
      type: "password",
      value: p,
      onChange: e => setP(e.target.value)
    })), /*#__PURE__*/React.createElement("div", {
      className: "m3-dialog__actions"
    }, /*#__PURE__*/React.createElement(Btn, {
      kind: "text",
      onClick: onClose
    }, "Cancel"), /*#__PURE__*/React.createElement(Btn, {
      onClick: () => onLogin(u)
    }, register ? "Create account" : "Log in"))));
  }
  const TITLES = {
    users: () => "Users",
    wishlists: p => {
      const u = D.usersById(p.userId);
      return u ? `${u.username}'s Wishlists` : "Wishlists";
    },
    allItems: p => {
      const u = D.usersById(p.userId);
      return u ? `${u.username}'s wishes` : "All items";
    },
    wishlist: p => {
      const w = D.wishlistById(p.wishlistId);
      return w ? w.title : "Wishlist";
    },
    item: p => {
      const i = D.itemById(p.itemId);
      return i ? i.title : "Item";
    },
    itemEdit: p => p.itemId ? "Edit Item" : "New Item"
  };
  const SCREENS = {
    users: Users,
    wishlists: UserWishlists,
    allItems: AllItems,
    wishlist: Wishlist,
    item: Item,
    itemEdit: ItemEdit
  };
  function MaterialApp() {
    const [stack, setStack] = useState([{
      screen: "users",
      params: {}
    }]);
    const [me, setMe] = useState(null);
    const [login, setLogin] = useState(null);
    const nav = {
      push: (screen, params = {}) => setStack(s => [...s, {
        screen,
        params
      }]),
      pop: () => setStack(s => s.length > 1 ? s.slice(0, -1) : s)
    };
    const top = stack[stack.length - 1];
    const crumbs = stack.map(f => TITLES[f.screen](f.params)).join(" / ");
    const Screen = SCREENS[top.screen];
    return /*#__PURE__*/React.createElement("div", {
      className: "m3-root"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-screen"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-topbar"
    }, /*#__PURE__*/React.createElement("div", {
      className: "m3-topbar__title"
    }, crumbs), /*#__PURE__*/React.createElement("div", {
      className: "m3-topbar__actions"
    }, me ? /*#__PURE__*/React.createElement(Btn, {
      className: "m3-btn--sm",
      onClick: () => {
        setMe(null);
        setStack([{
          screen: "users",
          params: {}
        }]);
      }
    }, "Log out") : /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Btn, {
      className: "m3-btn--sm",
      onClick: () => setLogin({
        register: false
      })
    }, "Log in"), /*#__PURE__*/React.createElement(Btn, {
      className: "m3-btn--sm",
      onClick: () => setLogin({
        register: true
      })
    }, "Register")))), /*#__PURE__*/React.createElement("div", {
      className: "m3-scroll"
    }, /*#__PURE__*/React.createElement(Screen, {
      me: me,
      params: top.params,
      nav: nav
    })), login && /*#__PURE__*/React.createElement(LoginDialog, {
      register: login.register,
      onClose: () => setLogin(null),
      onLogin: () => {
        setMe(D.usersById("u_you"));
        setLogin(null);
      }
    })));
  }
  window.MaterialApp = MaterialApp;
})();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/material/material-app.jsx", error: String((e && e.message) || e) }); }

// ui_kits/web/app.jsx
try { (() => {
/* WishlistApp web UI kit — interactive recreation of the Compose-HTML web client.
 * Composes the design-system primitives (window.WishlistApp_ef9ce8) over the fake
 * data in data.js. Single-file app with a small navigation stack.
 *
 * IMPORTANT: this file is also compiled into _ds_bundle.js, which loads BEFORE the
 * namespace and WL_DATA are populated. So everything is resolved at RENDER time
 * (inside component bodies / TITLES getters), never at module scope, and the mount
 * happens from index.html — not here. */
const {
  useState
} = React;
const SORTS = ["Default", "Cost", "Priority", "Title"];
const PRIORITY_ORDER = {
  High: 3,
  Custom: 2.5,
  Medium: 2,
  Small: 1
};
function sortItems(items, mode) {
  const a = [...items];
  if (mode === "Cost") a.sort((x, y) => (x.price || 0) - (y.price || 0));else if (mode === "Title") a.sort((x, y) => x.title.localeCompare(y.title));else if (mode === "Priority") a.sort((x, y) => (PRIORITY_ORDER[y.priority] || 0) - (PRIORITY_ORDER[x.priority] || 0));
  return a;
}
function Container({
  children
}) {
  return /*#__PURE__*/React.createElement("div", {
    className: "container py-3"
  }, children);
}

/* ------------------------------------------------------------- users list */
function UsersListScreen({
  me,
  nav
}) {
  const {
    Button,
    ListRow,
    Avatar
  } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  return /*#__PURE__*/React.createElement(Container, null, /*#__PURE__*/React.createElement("div", {
    className: "d-flex justify-content-between align-items-center mb-3"
  }, /*#__PURE__*/React.createElement("h1", {
    className: "h3 mb-0"
  }, "Users"), me && /*#__PURE__*/React.createElement(Button, {
    variant: "outline-primary",
    onClick: () => nav.push("wishlists", {
      userId: me.id
    })
  }, "My profile")), /*#__PURE__*/React.createElement("ul", {
    className: "list-group"
  }, D.users.map(u => /*#__PURE__*/React.createElement(ListRow, {
    key: u.id,
    leading: /*#__PURE__*/React.createElement(Avatar, {
      size: 48
    }),
    onSelect: () => nav.push("wishlists", {
      userId: u.id
    })
  }, /*#__PURE__*/React.createElement("span", null, u.username), u.you && /*#__PURE__*/React.createElement("span", {
    className: "text-muted small ms-2"
  }, "(you)"), u.admin && /*#__PURE__*/React.createElement("span", {
    className: "badge bg-secondary-subtle text-secondary-emphasis ms-2"
  }, "admin")))));
}

/* --------------------------------------------------------- user wishlists */
function UserWishlistsScreen({
  me,
  params,
  nav
}) {
  const {
    Button,
    ListRow
  } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const lists = D.wishlistsByOwner(params.userId);
  const isOwner = me && me.id === params.userId;
  return /*#__PURE__*/React.createElement(Container, null, /*#__PURE__*/React.createElement("div", {
    className: "d-flex justify-content-between align-items-center mb-3"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "outline-secondary",
    onClick: nav.pop
  }, "Back"), /*#__PURE__*/React.createElement("div", {
    className: "d-flex gap-2"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "outline-secondary",
    onClick: () => nav.push("allItems", {
      userId: params.userId
    })
  }, "All items"), isOwner && /*#__PURE__*/React.createElement(Button, {
    variant: "success",
    onClick: () => alert("New wishlist (demo)")
  }, "New Wishlist"))), lists.length === 0 ? /*#__PURE__*/React.createElement("p", {
    className: "text-muted"
  }, "No wishlists yet") : /*#__PURE__*/React.createElement("ul", {
    className: "list-group"
  }, lists.map(w => /*#__PURE__*/React.createElement(ListRow, {
    key: w.id,
    leading: /*#__PURE__*/React.createElement("img", {
      src: "../../assets/stacked-items.svg",
      alt: "",
      width: "48",
      height: "48",
      className: "rounded flex-shrink-0"
    }),
    onSelect: () => nav.push("wishlist", {
      wishlistId: w.id
    })
  }, /*#__PURE__*/React.createElement("span", null, w.title)))));
}

/* ----------------------------------------------------------- item grid */
function ItemGrid({
  items,
  nav,
  withSub
}) {
  const {
    ItemCard
  } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  return /*#__PURE__*/React.createElement("div", {
    className: "row row-cols-1 row-cols-sm-2 row-cols-md-3 g-3 mb-3"
  }, items.map(it => /*#__PURE__*/React.createElement("div", {
    className: "col",
    key: it.id
  }, /*#__PURE__*/React.createElement(ItemCard, {
    title: it.title,
    wishlistTitle: withSub ? it.wishlistTitle : undefined,
    description: it.description,
    priceText: D.priceText(it),
    priority: it.priority,
    weight: it.weight,
    onSelect: () => nav.push("item", {
      itemId: it.id
    })
  }))));
}

/* ------------------------------------------------------------ all items */
function AllItemsScreen({
  params,
  nav
}) {
  const {
    Button,
    Select
  } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const lists = D.wishlistsByOwner(params.userId);
  const items = lists.flatMap(w => D.itemsByWishlist(w.id).map(i => ({
    ...i,
    wishlistTitle: w.title
  })));
  const [sort, setSort] = useState("Default");
  return /*#__PURE__*/React.createElement(Container, null, /*#__PURE__*/React.createElement("div", {
    className: "d-flex align-items-center mb-3 gap-2"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "outline-secondary",
    onClick: nav.pop
  }, "Back"), /*#__PURE__*/React.createElement("div", {
    className: "flex-grow-1"
  })), /*#__PURE__*/React.createElement("div", {
    className: "d-flex gap-3 align-items-end mb-3",
    style: {
      maxWidth: 220
    }
  }, /*#__PURE__*/React.createElement(Select, {
    label: "Sort",
    value: sort,
    onChange: e => setSort(e.target.value),
    options: SORTS,
    size: "sm"
  })), /*#__PURE__*/React.createElement(ItemGrid, {
    items: sortItems(items, sort),
    nav: nav,
    withSub: true
  }));
}

/* -------------------------------------------------------- wishlist detail */
function WishlistDetailScreen({
  me,
  params,
  nav
}) {
  const {
    Button,
    Select,
    ListRow,
    PriorityBadge,
    Alert
  } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const w = D.wishlistById(params.wishlistId);
  const items = D.itemsByWishlist(w.id);
  const isOwner = me && me.id === w.ownerId;
  const [sort, setSort] = useState("Default");
  const [view, setView] = useState("Grid");
  const [copied, setCopied] = useState(false);
  const sorted = sortItems(items, sort);
  return /*#__PURE__*/React.createElement(Container, null, /*#__PURE__*/React.createElement("div", {
    className: "d-flex align-items-center mb-3 gap-2"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "outline-secondary",
    onClick: nav.pop
  }, "Back"), /*#__PURE__*/React.createElement("div", {
    className: "flex-grow-1"
  }), !isOwner && /*#__PURE__*/React.createElement(Button, {
    variant: "outline-success",
    disabled: copied,
    onClick: () => setCopied(true)
  }, "Copy to my profile"), isOwner && /*#__PURE__*/React.createElement(Button, {
    variant: "outline-primary",
    onClick: () => alert("Edit wishlist (demo)")
  }, "Edit")), copied && /*#__PURE__*/React.createElement(Alert, {
    variant: "success"
  }, "Copy queued. It will appear in your profile shortly."), /*#__PURE__*/React.createElement("div", {
    className: "d-flex gap-3 align-items-end mb-3 flex-wrap"
  }, /*#__PURE__*/React.createElement("div", {
    style: {
      width: 160
    }
  }, /*#__PURE__*/React.createElement(Select, {
    label: "Sort",
    value: sort,
    onChange: e => setSort(e.target.value),
    options: SORTS,
    size: "sm"
  })), /*#__PURE__*/React.createElement("div", {
    style: {
      width: 140
    }
  }, /*#__PURE__*/React.createElement(Select, {
    label: "View",
    value: view,
    onChange: e => setView(e.target.value),
    options: ["Grid", "List"],
    size: "sm"
  }))), items.length === 0 ? /*#__PURE__*/React.createElement("p", {
    className: "text-muted"
  }, "No items yet") : view === "Grid" ? /*#__PURE__*/React.createElement(ItemGrid, {
    items: sorted,
    nav: nav
  }) : /*#__PURE__*/React.createElement("ul", {
    className: "list-group mb-3"
  }, sorted.map(it => /*#__PURE__*/React.createElement(ListRow, {
    key: it.id,
    onSelect: () => nav.push("item", {
      itemId: it.id
    })
  }, /*#__PURE__*/React.createElement("div", {
    className: "flex-grow-1"
  }, /*#__PURE__*/React.createElement("div", {
    className: "d-flex justify-content-between align-items-center"
  }, /*#__PURE__*/React.createElement("div", {
    className: "d-flex align-items-center gap-2"
  }, /*#__PURE__*/React.createElement("span", null, it.title), /*#__PURE__*/React.createElement(PriorityBadge, {
    priority: it.priority,
    weight: it.weight
  })), D.priceText(it) && /*#__PURE__*/React.createElement("span", {
    className: "text-muted small"
  }, D.priceText(it))), it.description && /*#__PURE__*/React.createElement("p", {
    className: "mb-0 text-muted small mt-1"
  }, it.description))))), isOwner && /*#__PURE__*/React.createElement(Button, {
    variant: "success",
    onClick: () => nav.push("itemEdit", {
      wishlistId: w.id
    })
  }, "Add Item"));
}

/* ------------------------------------------------------------ item detail */
function ItemDetailScreen({
  me,
  params,
  nav
}) {
  const {
    Button,
    PriorityBadge,
    ListRow
  } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const it = D.itemById(params.itemId);
  const w = D.wishlistById(it.wishlistId);
  const isOwner = me && me.id === w.ownerId;
  return /*#__PURE__*/React.createElement(Container, null, /*#__PURE__*/React.createElement("div", {
    className: "d-flex align-items-center mb-3 gap-2"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "outline-secondary",
    onClick: nav.pop
  }, "Back"), /*#__PURE__*/React.createElement("div", {
    className: "flex-grow-1"
  }), !isOwner && /*#__PURE__*/React.createElement(Button, {
    variant: "outline-success",
    onClick: () => alert("Copy to my wishlist (demo)")
  }, "Copy to my wishlist"), isOwner && /*#__PURE__*/React.createElement(Button, {
    variant: "outline-primary",
    onClick: () => nav.push("itemEdit", {
      itemId: it.id,
      wishlistId: w.id
    })
  }, "Edit")), it.description && /*#__PURE__*/React.createElement("div", {
    className: "mb-3"
  }, /*#__PURE__*/React.createElement("h6", {
    className: "text-muted"
  }, "Description"), /*#__PURE__*/React.createElement("p", null, it.description)), /*#__PURE__*/React.createElement("div", {
    className: "mb-3"
  }, /*#__PURE__*/React.createElement("h6", {
    className: "text-muted"
  }, "Approximate price"), D.priceText(it) ? /*#__PURE__*/React.createElement("p", null, D.priceText(it)) : /*#__PURE__*/React.createElement("p", {
    className: "text-muted"
  }, "No price")), /*#__PURE__*/React.createElement("div", {
    className: "mb-3"
  }, /*#__PURE__*/React.createElement("h6", {
    className: "text-muted"
  }, "Priority"), /*#__PURE__*/React.createElement("p", null, /*#__PURE__*/React.createElement(PriorityBadge, {
    priority: it.priority,
    weight: it.weight
  }))), /*#__PURE__*/React.createElement("div", {
    className: "mb-3"
  }, /*#__PURE__*/React.createElement("h6", {
    className: "text-muted"
  }, "Links"), it.links.length === 0 ? /*#__PURE__*/React.createElement("p", {
    className: "text-muted"
  }, "No links") : /*#__PURE__*/React.createElement("ul", {
    className: "list-group"
  }, it.links.map((l, i) => /*#__PURE__*/React.createElement(ListRow, {
    key: i
  }, /*#__PURE__*/React.createElement("a", {
    href: l.url,
    target: "_blank",
    rel: "noreferrer"
  }, l.title || l.url))))), /*#__PURE__*/React.createElement("div", {
    className: "mb-3"
  }, /*#__PURE__*/React.createElement("h6", {
    className: "text-muted"
  }, "Images"), /*#__PURE__*/React.createElement("img", {
    src: "../../assets/giftbox.svg",
    alt: "Gift placeholder",
    className: "rounded border",
    style: {
      width: 160,
      height: 160,
      objectFit: "cover"
    }
  })));
}

/* -------------------------------------------------------------- item edit */
function ItemEditScreen({
  params,
  nav
}) {
  const {
    Button,
    Input,
    Select,
    Modal
  } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const editing = params.itemId ? D.itemById(params.itemId) : null;
  const [title, setTitle] = useState(editing ? editing.title : "");
  const [desc, setDesc] = useState(editing ? editing.description : "");
  const [price, setPrice] = useState(editing ? editing.price : "");
  const [priority, setPriority] = useState(editing ? editing.priority : "Medium");
  const [confirm, setConfirm] = useState(false);
  return /*#__PURE__*/React.createElement(Container, null, /*#__PURE__*/React.createElement("div", {
    className: "d-flex align-items-center mb-3 gap-2"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "outline-secondary",
    onClick: nav.pop
  }, "Back")), /*#__PURE__*/React.createElement("div", {
    style: {
      maxWidth: 520
    }
  }, /*#__PURE__*/React.createElement(Input, {
    label: "Title",
    id: "it-title",
    value: title,
    placeholder: "Title",
    onChange: e => setTitle(e.target.value)
  }), /*#__PURE__*/React.createElement(Input, {
    label: "Description",
    id: "it-desc",
    value: desc,
    placeholder: "Description",
    onChange: e => setDesc(e.target.value)
  }), /*#__PURE__*/React.createElement(Input, {
    label: "Approximate price",
    id: "it-price",
    type: "number",
    value: price,
    placeholder: "0",
    onChange: e => setPrice(e.target.value)
  }), /*#__PURE__*/React.createElement(Select, {
    label: "Priority",
    value: priority,
    onChange: e => setPriority(e.target.value),
    options: [{
      value: "Small",
      label: "Low"
    }, {
      value: "Medium",
      label: "Medium"
    }, {
      value: "High",
      label: "High"
    }, {
      value: "Custom",
      label: "Custom"
    }]
  }), /*#__PURE__*/React.createElement("div", {
    className: "d-flex gap-2"
  }, /*#__PURE__*/React.createElement(Button, {
    variant: "primary",
    disabled: !title.trim(),
    onClick: nav.pop
  }, "Save"), editing && /*#__PURE__*/React.createElement(Button, {
    variant: "danger",
    onClick: () => setConfirm(true)
  }, "Delete"))), /*#__PURE__*/React.createElement(Modal, {
    open: confirm,
    title: "Delete item?",
    onCancel: () => setConfirm(false),
    onConfirm: () => {
      setConfirm(false);
      nav.pop();
    },
    confirmLabel: "Delete",
    confirmVariant: "danger"
  }, "This item will be permanently removed. Continue?"));
}

/* -------------------------------------------------------------- login modal */
function LoginModal({
  onClose,
  onLogin,
  registerMode
}) {
  const {
    Button,
    Modal
  } = window.WishlistApp_ef9ce8;
  const [username, setUsername] = useState("you");
  const [password, setPassword] = useState("");
  return /*#__PURE__*/React.createElement(Modal, {
    open: true,
    title: registerMode ? "Register" : "Log in",
    onCancel: onClose,
    footer: /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Button, {
      variant: "outline-secondary",
      onClick: onClose
    }, "Cancel"), /*#__PURE__*/React.createElement(Button, {
      variant: "primary",
      onClick: () => onLogin(username)
    }, registerMode ? "Create account" : "Log in"))
  }, /*#__PURE__*/React.createElement("div", {
    className: "d-flex flex-column gap-2"
  }, /*#__PURE__*/React.createElement("input", {
    className: "form-control",
    value: username,
    placeholder: "Username",
    onChange: e => setUsername(e.target.value)
  }), /*#__PURE__*/React.createElement("input", {
    className: "form-control",
    type: "password",
    value: password,
    placeholder: "Password",
    onChange: e => setPassword(e.target.value)
  })));
}

/* --------------------------------------------------------------------- App */
const TITLES = {
  users: () => "Users",
  wishlists: p => {
    const u = window.WL_DATA.usersById(p.userId);
    return u ? `${u.username}'s Wishlists` : "Wishlists";
  },
  allItems: p => {
    const u = window.WL_DATA.usersById(p.userId);
    return u ? `${u.username}'s wishes` : "All items";
  },
  wishlist: p => {
    const w = window.WL_DATA.wishlistById(p.wishlistId);
    return w ? w.title : "Wishlist";
  },
  item: p => {
    const i = window.WL_DATA.itemById(p.itemId);
    return i ? i.title : "Item";
  },
  itemEdit: p => p.itemId ? "Edit Item" : "New Item"
};
const SCREENS = {
  users: UsersListScreen,
  wishlists: UserWishlistsScreen,
  allItems: AllItemsScreen,
  wishlist: WishlistDetailScreen,
  item: ItemDetailScreen,
  itemEdit: ItemEditScreen
};
function App() {
  const {
    NavBar,
    Button
  } = window.WishlistApp_ef9ce8;
  const D = window.WL_DATA;
  const [stack, setStack] = useState([{
    screen: "users",
    params: {}
  }]);
  const [me, setMe] = useState(null);
  const [login, setLogin] = useState(null); // null | {register}

  const nav = {
    push: (screen, params = {}) => setStack(s => [...s, {
      screen,
      params
    }]),
    pop: () => setStack(s => s.length > 1 ? s.slice(0, -1) : s)
  };
  const top = stack[stack.length - 1];
  const crumbs = stack.map(f => TITLES[f.screen](f.params));
  const Screen = SCREENS[top.screen];
  return /*#__PURE__*/React.createElement("div", {
    style: {
      minHeight: "100vh",
      background: "var(--wl-surface-page)"
    }
  }, /*#__PURE__*/React.createElement(NavBar, {
    title: crumbs,
    actions: me ? /*#__PURE__*/React.createElement(Button, {
      variant: "outline-light",
      size: "sm",
      onClick: () => {
        setMe(null);
        setStack([{
          screen: "users",
          params: {}
        }]);
      }
    }, "Log out") : /*#__PURE__*/React.createElement(React.Fragment, null, /*#__PURE__*/React.createElement(Button, {
      variant: "outline-light",
      size: "sm",
      onClick: () => setLogin({
        register: false
      })
    }, "Log in"), /*#__PURE__*/React.createElement(Button, {
      variant: "outline-light",
      size: "sm",
      onClick: () => setLogin({
        register: true
      })
    }, "Register"))
  }), /*#__PURE__*/React.createElement(Screen, {
    me: me,
    params: top.params,
    nav: nav
  }), login && /*#__PURE__*/React.createElement(LoginModal, {
    registerMode: login.register,
    onClose: () => setLogin(null),
    onLogin: () => {
      setMe(D.usersById("u_you"));
      setLogin(null);
    }
  }));
}

// Exposed for ui_kits/web/index.html to mount. No top-level createRoot here:
// this file is also compiled into _ds_bundle.js, and a top-level mount would run
// at bundle-load (before the components exist) and double-mount.
window.WishlistWebApp = App;
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/web/app.jsx", error: String((e && e.message) || e) }); }

// ui_kits/web/data.js
try { (() => {
/* Fake data for the WishlistApp web UI kit. Mirrors the server's domain model:
 * users own wishlists, wishlists hold items (title, description, price, priority,
 * images). Exposed on window for the Babel-transpiled screens. */
window.WL_DATA = function () {
  // Priority: "Small" (shown as Low), "Medium", "High", or "Custom" (+weight).
  const users = [{
    id: "u_you",
    username: "you",
    you: true
  }, {
    id: "u_alice",
    username: "alice"
  }, {
    id: "u_bob",
    username: "bob"
  }, {
    id: "u_mira",
    username: "mira"
  }, {
    id: "u_root",
    username: "root",
    admin: true
  }];
  const wishlists = [{
    id: "w_bday",
    ownerId: "u_you",
    title: "Birthday",
    defaultUnits: "USD"
  }, {
    id: "w_home",
    ownerId: "u_you",
    title: "New apartment",
    defaultUnits: "USD"
  }, {
    id: "w_alice_wed",
    ownerId: "u_alice",
    title: "Wedding registry",
    defaultUnits: "EUR"
  }, {
    id: "w_alice_books",
    ownerId: "u_alice",
    title: "Books to read",
    defaultUnits: "EUR"
  }, {
    id: "w_bob_bike",
    ownerId: "u_bob",
    title: "Bike build",
    defaultUnits: "USD"
  }, {
    id: "w_mira_art",
    ownerId: "u_mira",
    title: "Art supplies",
    defaultUnits: "USD"
  }];
  const items = [{
    id: "i1",
    wishlistId: "w_bday",
    title: "Mechanical keyboard",
    description: "Brown switches, TKL layout, wireless.",
    price: 120,
    units: "USD",
    amount: 1,
    priority: "High",
    links: [{
      url: "https://example.com/kbd",
      title: "Product page"
    }],
    images: []
  }, {
    id: "i2",
    wishlistId: "w_bday",
    title: "Linen apron",
    description: "Natural color, with front pocket.",
    price: 35,
    units: "USD",
    amount: 1,
    priority: "Medium",
    links: [],
    images: []
  }, {
    id: "i3",
    wishlistId: "w_bday",
    title: "Espresso cups",
    description: "Set of 4, matte ceramic.",
    price: 28,
    units: "USD",
    amount: 2,
    priority: "Small",
    links: [],
    images: []
  }, {
    id: "i4",
    wishlistId: "w_bday",
    title: "Noise-cancelling headphones",
    description: "Over-ear, long battery life.",
    price: 240,
    units: "USD",
    amount: 1,
    priority: "Custom",
    weight: 90,
    links: [{
      url: "https://example.com/hp",
      title: "Review"
    }],
    images: []
  }, {
    id: "i5",
    wishlistId: "w_home",
    title: "Floor lamp",
    description: "Warm dimmable LED, oak base.",
    price: 85,
    units: "USD",
    amount: 1,
    priority: "Medium",
    links: [],
    images: []
  }, {
    id: "i6",
    wishlistId: "w_home",
    title: "Cast-iron skillet",
    description: "12-inch, pre-seasoned.",
    price: 45,
    units: "USD",
    amount: 1,
    priority: "High",
    links: [],
    images: []
  }, {
    id: "i7",
    wishlistId: "w_alice_wed",
    title: "Dinner plate set",
    description: "Stoneware, service for 8.",
    price: 160,
    units: "EUR",
    amount: 1,
    priority: "High",
    links: [],
    images: []
  }, {
    id: "i8",
    wishlistId: "w_alice_wed",
    title: "Wool throw blanket",
    description: "",
    price: 70,
    units: "EUR",
    amount: 1,
    priority: "Medium",
    links: [],
    images: []
  }, {
    id: "i9",
    wishlistId: "w_alice_books",
    title: "The Overstory",
    description: "Richard Powers, hardcover.",
    price: 18,
    units: "EUR",
    amount: 1,
    priority: "Small",
    links: [],
    images: []
  }, {
    id: "i10",
    wishlistId: "w_bob_bike",
    title: "Carbon handlebars",
    description: "31.8mm clamp, 420mm.",
    price: 130,
    units: "USD",
    amount: 1,
    priority: "High",
    links: [],
    images: []
  }, {
    id: "i11",
    wishlistId: "w_mira_art",
    title: "Gouache set",
    description: "24 tubes, artist grade.",
    price: 52,
    units: "USD",
    amount: 1,
    priority: "Medium",
    links: [],
    images: []
  }];
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
    usersById: id => users.find(u => u.id === id),
    wishlistsByOwner: id => wishlists.filter(w => w.ownerId === id),
    wishlistById: id => wishlists.find(w => w.id === id),
    itemsByWishlist: id => items.filter(i => i.wishlistId === id),
    itemById: id => items.find(i => i.id === id)
  };
}();
})(); } catch (e) { __ds_ns.__errors.push({ path: "ui_kits/web/data.js", error: String((e && e.message) || e) }); }

__ds_ns.Button = __ds_scope.Button;

__ds_ns.Avatar = __ds_scope.Avatar;

__ds_ns.Badge = __ds_scope.Badge;

__ds_ns.ItemCard = __ds_scope.ItemCard;

__ds_ns.ListRow = __ds_scope.ListRow;

__ds_ns.PriorityBadge = __ds_scope.PriorityBadge;

__ds_ns.Alert = __ds_scope.Alert;

__ds_ns.Modal = __ds_scope.Modal;

__ds_ns.Input = __ds_scope.Input;

__ds_ns.Select = __ds_scope.Select;

__ds_ns.NavBar = __ds_scope.NavBar;

})();
