import React from "react";

/**
 * The WishlistApp top navigation bar: a blue (`bg-primary`) dark navbar whose
 * brand shows the current screen's breadcrumb title (segments joined with " / ")
 * and whose right side holds auth actions. `title` accepts a string or an array
 * of breadcrumb segments. `actions` is the right-aligned slot (Log in / Log out).
 */
export function NavBar({ title = "Wishlists", actions, className = "", ...rest }) {
  const text = Array.isArray(title) ? title.filter(Boolean).join(" / ") : title;
  const classes = ["navbar", "navbar-expand", "navbar-dark", "bg-primary"];
  if (className) classes.push(className);
  return (
    <nav className={classes.join(" ")} {...rest}>
      <div className="container-fluid">
        <a href="#" className="navbar-brand">
          {text}
        </a>
        <div className="d-flex gap-2">{actions}</div>
      </div>
    </nav>
  );
}
