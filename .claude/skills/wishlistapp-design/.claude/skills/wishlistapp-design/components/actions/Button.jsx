import React from "react";

/**
 * Bootstrap button used across every WishlistApp screen. Solid variants drive
 * primary flows (Save = primary, Add Item / Copy = success, Delete = danger);
 * outline variants are the lighter-weight toolbar actions (Back, Edit). The
 * `outline-light` variant is reserved for the blue navbar (Log in / Log out).
 */
export function Button({
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
  return (
    <button type={type} className={classes.join(" ")} disabled={disabled} {...rest}>
      {children}
    </button>
  );
}
