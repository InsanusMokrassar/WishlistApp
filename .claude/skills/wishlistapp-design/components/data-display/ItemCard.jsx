import React from "react";
import { PriorityBadge } from "./PriorityBadge.jsx";

const GIFTBOX =
  "data:image/svg+xml;charset=UTF-8,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 100 100'%3E%3Crect width='100' height='100' fill='%23eef1f4'/%3E%3Crect x='24' y='44' width='52' height='38' rx='3' fill='%23c0c6cf'/%3E%3Crect x='20' y='36' width='60' height='14' rx='3' fill='%23a8b0bb'/%3E%3Crect x='46' y='36' width='8' height='46' fill='%238b94a1'/%3E%3Cpath d='M50 36 C40 24 26 30 50 36 C74 30 60 24 50 36 Z' fill='%238b94a1'/%3E%3C/svg%3E";

/**
 * Bootstrap card presenting a single wishlist item — the building block of the
 * grid view. Media = first image (or the gift-box placeholder), title = item
 * title, subtitle = owning wishlist, body = description, footer = price text.
 * The priority pill is overlaid in the top-right corner. The whole card is
 * clickable.
 */
export function ItemCard({
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
  return (
    <div
      className={classes.join(" ")}
      style={{ cursor: onSelect ? "pointer" : undefined }}
      onClick={onSelect}
      {...rest}
    >
      <div className="position-absolute top-0 end-0 m-2">
        <PriorityBadge priority={priority} weight={weight} />
      </div>
      <img
        src={imageUrl || GIFTBOX}
        alt=""
        className="card-img-top"
        style={{
          width: "100%",
          height: "var(--wl-card-media-h, 180px)",
          objectFit: imageUrl ? "cover" : "contain",
          backgroundColor: "var(--wl-surface-placeholder, #eef1f4)",
        }}
      />
      <div className="card-body">
        <h5 className="card-title">{title}</h5>
        {wishlistTitle && (
          <h6 className="card-subtitle mb-2 text-muted">{wishlistTitle}</h6>
        )}
        {description && <p className="card-text">{description}</p>}
      </div>
      {priceText && <div className="card-footer text-muted small">{priceText}</div>}
    </div>
  );
}
