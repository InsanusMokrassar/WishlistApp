import * as React from "react";

/**
 * Labeled Bootstrap text input inside the standard `mb-3` field block.
 *
 * @startingPoint section="Forms" subtitle="Labeled text input" viewport="700x150"
 */
export interface InputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, "onChange"> {
  /** Field label. Omit for an unlabeled input. */
  label?: string;
  /** Input id (ties the label to the field). */
  id?: string;
  /** Input type. */
  type?: string;
  value?: string | number;
  placeholder?: string;
  onChange?: (e: React.ChangeEvent<HTMLInputElement>) => void;
  disabled?: boolean;
  /** Helper text shown under the field. */
  help?: string;
  className?: string;
}

export function Input(props: InputProps): JSX.Element;
