import React from "react";
import { Button } from "../actions/Button.jsx";

/**
 * Bootstrap modal dialog (centered, with backdrop) — the app's pattern for the
 * login/register form and for destructive confirmations (Discard changes?,
 * Delete item?). Render conditionally when `open` is true. `footer` overrides
 * the default Cancel/Confirm pair built from the `*Label` / `on*` props.
 */
export function Modal({
  open = true,
  title,
  children,
  onCancel,
  onConfirm,
  cancelLabel = "Cancel",
  confirmLabel = "OK",
  confirmVariant = "primary",
  footer,
  centered = true,
}) {
  if (!open) return null;
  const dialogClasses = ["modal-dialog"];
  if (centered) dialogClasses.push("modal-dialog-centered");
  return (
    <>
      <div className="modal-backdrop fade show" />
      <div className="modal fade show d-block" tabIndex={-1}>
        <div className={dialogClasses.join(" ")}>
          <div className="modal-content">
            <div className="modal-header">
              <span className="modal-title h5">{title}</span>
              {onCancel && (
                <button
                  type="button"
                  className="btn-close"
                  aria-label="Close"
                  onClick={onCancel}
                />
              )}
            </div>
            <div className="modal-body">{children}</div>
            <div className="modal-footer">
              {footer || (
                <>
                  {onCancel && (
                    <Button variant="secondary" onClick={onCancel}>
                      {cancelLabel}
                    </Button>
                  )}
                  {onConfirm && (
                    <Button variant={confirmVariant} onClick={onConfirm}>
                      {confirmLabel}
                    </Button>
                  )}
                </>
              )}
            </div>
          </div>
        </div>
      </div>
    </>
  );
}
