import React from "react";

const SILHOUETTE =
  "data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Crect width='100' height='100' fill='%23d0d3d8'/%3E%3Ccircle cx='50' cy='38' r='18' fill='%23f1f3f5'/%3E%3Cpath d='M20 86 a30 30 0 0 1 60 0 Z' fill='%23f1f3f5'/%3E%3C/svg%3E";

/**
 * User avatar. Shows the uploaded photo when `src` is given, otherwise the
 * default gray silhouette placeholder. Circle shape for list rows and the
 * navbar; rounded-square for large profile previews. Sized by `size` (px).
 */
export function Avatar({ src, alt = "", size = 48, circle = true, className = "", ...rest }) {
  const classes = [circle ? "rounded-circle" : "rounded", "flex-shrink-0"];
  if (className) classes.push(className);
  return (
    <img
      src={src || SILHOUETTE}
      alt={alt}
      width={size}
      height={size}
      className={classes.join(" ")}
      style={{ width: size, height: size, objectFit: "cover" }}
      {...rest}
    />
  );
}
