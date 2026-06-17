import * as React from "react";
import { ButtonVariant } from "../actions/Button";

/**
 * Centered Bootstrap modal with backdrop — login form & destructive confirms.
 *
 * @startingPoint section="Feedback" subtitle="Confirmation / form modal" viewport="700x320"
 */
export interface ModalProps {
  /** Controls visibility. When false, renders nothing. */
  open?: boolean;
  /** Header title. */
  title?: string;
  /** Modal body content. */
  children?: React.ReactNode;
  /** Cancel / close handler (also wires the header close button). */
  onCancel?: () => void;
  /** Confirm handler (renders the primary footer button). */
  onConfirm?: () => void;
  cancelLabel?: string;
  confirmLabel?: string;
  /** Variant of the confirm button (e.g. `danger` for deletes). */
  confirmVariant?: ButtonVariant;
  /** Custom footer; overrides the default Cancel/Confirm pair. */
  footer?: React.ReactNode;
  /** Vertically center the dialog. */
  centered?: boolean;
}

export function Modal(props: ModalProps): JSX.Element | null;
