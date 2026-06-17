import React from "react";

const LABELS = { Small: "Low", Medium: "Medium", High: "High", Custom: "Custom" };

/**
 * Pill badge showing a wishlist item's priority. Renders the localized preset
 * label; for a Custom priority it appends the weight in parentheses, e.g.
 * "Custom (42)". Used on item cards (overlaid top-right), list rows, and the
 * item detail screen. Visual is intentionally neutral (subtle gray pill) so it
 * reads as metadata, not a status alarm.
 */
export function PriorityBadge({ priority = "Medium", weight, className = "", ...rest }) {
  const label = LABELS[priority] || priority;
  const suffix = priority === "Custom" && weight != null ? ` (${weight})` : "";
  const classes = [
    "badge",
    "rounded-pill",
    "bg-secondary-subtle",
    "text-secondary-emphasis",
  ];
  if (className) classes.push(className);
  return (
    <span className={classes.join(" ")} {...rest}>
      {label}
      {suffix}
    </span>
  );
}
