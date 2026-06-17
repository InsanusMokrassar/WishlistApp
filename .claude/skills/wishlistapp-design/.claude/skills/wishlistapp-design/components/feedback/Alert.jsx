import React from "react";

/**
 * Bootstrap contextual alert. WishlistApp uses success alerts to confirm queued
 * copies and danger alerts for failed actions. Compact (`py-2`) by default to
 * match the app's inline banners.
 */
export function Alert({ variant = "success", className = "", children, ...rest }) {
  const classes = ["alert", `alert-${variant}`, "py-2"];
  if (className) classes.push(className);
  return (
    <div className={classes.join(" ")} role="alert" {...rest}>
      {children}
    </div>
  );
}
