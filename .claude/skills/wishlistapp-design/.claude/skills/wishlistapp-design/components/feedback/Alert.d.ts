import * as React from "react";

/** Bootstrap contextual alert banner (success / danger / etc.). */
export interface AlertProps extends React.HTMLAttributes<HTMLDivElement> {
  /** Bootstrap alert color. */
  variant?: "primary" | "secondary" | "success" | "danger" | "warning" | "info" | "light" | "dark";
  className?: string;
  children?: React.ReactNode;
}

export function Alert(props: AlertProps): JSX.Element;
