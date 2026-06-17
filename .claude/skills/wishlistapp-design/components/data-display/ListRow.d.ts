import * as React from "react";

/**
 * One Bootstrap `list-group-item` row with optional leading/trailing slots.
 * Render inside `<ul className="list-group">`.
 *
 * @startingPoint section="Data display" subtitle="List row with avatar + actions" viewport="700x150"
 */
export interface ListRowProps {
  /** Click handler for the row's primary area. Omit to make the row static. */
  onSelect?: () => void;
  /** Leading content (avatar / thumbnail) at the row start. */
  leading?: React.ReactNode;
  /** Trailing content (action buttons) at the row end. */
  trailing?: React.ReactNode;
  /** Primary content of the row. */
  children?: React.ReactNode;
  className?: string;
}

export function ListRow(props: ListRowProps): JSX.Element;
