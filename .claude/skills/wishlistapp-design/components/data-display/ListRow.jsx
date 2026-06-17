import React from "react";

/**
 * A single Bootstrap list-group row — the workhorse layout for the users list,
 * wishlists list, and link lists. Optional `leading` (avatar/thumbnail) and
 * `trailing` (action buttons) slots flank the primary content. Must be rendered
 * inside a `<ul className="list-group">`. The row is clickable when `onSelect`
 * is set.
 */
export function ListRow({ onSelect, leading, trailing, children, className = "", ...rest }) {
  const classes = [
    "list-group-item",
    "list-group-item-action",
    "d-flex",
    "justify-content-between",
    "align-items-center",
  ];
  if (className) classes.push(className);
  const bare = !leading && !trailing;
  return (
    <li
      className={classes.join(" ")}
      style={bare && onSelect ? { cursor: "pointer" } : undefined}
      onClick={bare ? onSelect : undefined}
      {...rest}
    >
      {bare ? (
        children
      ) : (
        <>
          <div
            className="d-flex align-items-center gap-3 flex-grow-1"
            style={onSelect ? { cursor: "pointer" } : undefined}
            onClick={onSelect}
          >
            {leading}
            <div className="flex-grow-1">{children}</div>
          </div>
          {trailing}
        </>
      )}
    </li>
  );
}
