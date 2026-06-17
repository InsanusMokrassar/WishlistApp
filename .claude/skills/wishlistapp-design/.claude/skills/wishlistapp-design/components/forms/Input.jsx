import React from "react";

/**
 * Labeled Bootstrap text input (`form-control`). Wraps the input in the app's
 * standard `mb-3` field block with an optional label and helper text. Used on
 * the wishlist/item edit screens and the login form.
 */
export function Input({
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
  return (
    <div className="mb-3">
      {label && (
        <label htmlFor={id} className="form-label">
          {label}
        </label>
      )}
      <input
        id={id}
        type={type}
        className={classes.join(" ")}
        value={value}
        placeholder={placeholder}
        onChange={onChange}
        disabled={disabled}
        {...rest}
      />
      {help && <div className="form-text">{help}</div>}
    </div>
  );
}
