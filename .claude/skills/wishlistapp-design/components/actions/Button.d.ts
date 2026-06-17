import * as React from "react";

export type ButtonVariant =
  | "primary"
  | "secondary"
  | "success"
  | "danger"
  | "warning"
  | "info"
  | "light"
  | "dark"
  | "outline-primary"
  | "outline-secondary"
  | "outline-success"
  | "outline-danger"
  | "outline-light";

/**
 * Bootstrap button — the single action primitive in WishlistApp.
 *
 * @startingPoint section="Actions" subtitle="Bootstrap button, all variants" viewport="700x140"
 */
export interface ButtonProps
  extends Omit<React.ButtonHTMLAttributes<HTMLButtonElement>, "type"> {
  /** Visual style. Solid for primary flows, outline for toolbar actions. */
  variant?: ButtonVariant;
  /** Bootstrap size modifier. */
  size?: "sm" | "lg";
  /** Native button type. */
  type?: "button" | "submit" | "reset";
  /** Disabled state. */
  disabled?: boolean;
  className?: string;
  children?: React.ReactNode;
}

export function Button(props: ButtonProps): JSX.Element;
