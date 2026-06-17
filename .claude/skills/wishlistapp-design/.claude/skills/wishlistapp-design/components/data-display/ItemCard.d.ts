import * as React from "react";
import { Priority } from "./PriorityBadge";

/**
 * Wishlist item card — the grid-view building block.
 *
 * @startingPoint section="Data display" subtitle="Wishlist item card" viewport="380x360"
 */
export interface ItemCardProps {
  /** Item title (card title). */
  title: string;
  /** Owning wishlist name, shown as subtitle. Omit to hide. */
  wishlistTitle?: string;
  /** Item description (card body). Omit to hide. */
  description?: string;
  /** Pre-formatted price string (card footer). Omit to hide the footer. */
  priceText?: string;
  /** Item priority — rendered as the overlaid pill. */
  priority?: Priority;
  /** Weight for a Custom priority. */
  weight?: number;
  /** First image URL. When omitted the gift-box placeholder is shown. */
  imageUrl?: string;
  /** Click handler for the whole card. */
  onSelect?: () => void;
  className?: string;
}

export function ItemCard(props: ItemCardProps): JSX.Element;
