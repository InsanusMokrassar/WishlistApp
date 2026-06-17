import React from "react";

/**
 * Labeled Bootstrap select (`form-select`) in the standard `mb-3` field block.
 * Drives the sort, view-mode, and currency selectors. `options` is a list of
 * `{ value, label }`; a bare string is used as both.
 */
export function Select({
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
  return (
    <div className="mb-3">
      {label && (
        <label htmlFor={id} className="form-label">
          {label}
        </label>
      )}
      <select
        id={id}
        className={classes.join(" ")}
        value={value}
        onChange={onChange}
        disabled={disabled}
        {...rest}
      >
        {options.map((o) => {
          const opt = typeof o === "string" ? { value: o, label: o } : o;
          return (
            <option key={opt.value} value={opt.value}>
              {opt.label}
            </option>
          );
        })}
      </select>
    </div>
  );
}
