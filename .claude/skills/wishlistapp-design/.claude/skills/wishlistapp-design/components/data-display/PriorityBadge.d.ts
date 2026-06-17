import * as React from "react";

export type Priority = "Small" | "Medium" | "High" | "Custom";

/**
 * Pill badge for a wishlist item's priority.
 *
 * @startingPoint section="Data display" subtitle="Item priority pill" viewport="700x120"
 */
export interface PriorityBadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** Priority preset. `Small` renders as the label "Low". */
  priority?: Priority;
  /** Weight shown in parentheses when `priority` is `Custom`. */
  weight?: number;
  className?: string;
}

export function PriorityBadge(props: PriorityBadgeProps): JSX.Element;
