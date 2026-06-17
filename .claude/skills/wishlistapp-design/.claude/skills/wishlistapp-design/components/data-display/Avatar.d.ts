import * as React from "react";

/**
 * User avatar with a built-in gray silhouette placeholder fallback.
 *
 * @startingPoint section="Data display" subtitle="User avatar + placeholder" viewport="700x120"
 */
export interface AvatarProps extends Omit<React.ImgHTMLAttributes<HTMLImageElement>, "width" | "height"> {
  /** Photo URL. When omitted, the default silhouette placeholder is shown. */
  src?: string;
  /** Accessible alt text. */
  alt?: string;
  /** Square size in CSS pixels. */
  size?: number;
  /** Circle clip (list rows, navbar) vs rounded square (large profile preview). */
  circle?: boolean;
  className?: string;
}

export function Avatar(props: AvatarProps): JSX.Element;
