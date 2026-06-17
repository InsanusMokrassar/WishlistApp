import * as React from "react";

/**
 * WishlistApp top navbar (blue, dark) with breadcrumb brand + auth actions slot.
 *
 * @startingPoint section="Navigation" subtitle="App top navbar" viewport="900x80"
 */
export interface NavBarProps extends React.HTMLAttributes<HTMLElement> {
  /** Brand text, or breadcrumb segments joined with " / ". */
  title?: string | string[];
  /** Right-aligned actions (Log in / Log out / etc.). */
  actions?: React.ReactNode;
  className?: string;
}

export function NavBar(props: NavBarProps): JSX.Element;
