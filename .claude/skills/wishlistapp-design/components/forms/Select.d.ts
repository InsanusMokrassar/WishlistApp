import * as React from "react";

export interface SelectOption {
  value: string;
  label: string;
}

/** Labeled Bootstrap select. Drives the sort / view-mode / currency selectors. */
export interface SelectProps
  extends Omit<React.SelectHTMLAttributes<HTMLSelectElement>, "onChange"> {
  /** Field label. Omit for an unlabeled select. */
  label?: string;
  id?: string;
  value?: string;
  /** Options as `{value,label}` objects or bare strings. */
  options?: Array<SelectOption | string>;
  onChange?: (e: React.ChangeEvent<HTMLSelectElement>) => void;
  disabled?: boolean;
  size?: "sm" | "lg";
  className?: string;
}

export function Select(props: SelectProps): JSX.Element;
