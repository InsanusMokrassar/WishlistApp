import React from "react";

/**
 * Generic Bootstrap badge. WishlistApp uses badges sparingly for small counts
 * and labels. For item priority, prefer the dedicated PriorityBadge. Set `pill`
 * for the rounded-pill shape; `bg` accepts a Bootstrap color or a *-subtle name.
 */
export function Badge({ bg = "secondary", pill = false, className = "", children, ...rest }) {
  const subtle = bg.endsWith("-subtle");
  const classes = ["badge", `bg-${bg}`];
  if (subtle) classes.push(`text-${bg.replace("-subtle", "")}-emphasis`);
  if (pill) classes.push("rounded-pill");
  if (className) classes.push(className);
  return (
    <span className={classes.join(" ")} {...rest}>
      {children}
    </span>
  );
}
