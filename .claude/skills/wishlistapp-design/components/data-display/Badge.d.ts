import * as React from "react";

/** Generic Bootstrap badge for small counts and labels. */
export interface BadgeProps extends React.HTMLAttributes<HTMLSpanElement> {
  /** Bootstrap background color, or a `*-subtle` variant (auto-pairs emphasis text). */
  bg?:
    | "primary" | "secondary" | "success" | "danger" | "warning" | "info" | "light" | "dark"
    | "primary-subtle" | "secondary-subtle" | "success-subtle" | "danger-subtle";
  /** Use the rounded-pill shape. */
  pill?: boolean;
  className?: string;
  children?: React.ReactNode;
}

export function Badge(props: BadgeProps): JSX.Element;
