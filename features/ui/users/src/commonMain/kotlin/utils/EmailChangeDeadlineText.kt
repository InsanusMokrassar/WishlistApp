package dev.inmo.wishlist.features.ui.users.utils

import korlibs.time.DateFormat
import korlibs.time.DateTime
import korlibs.time.minutes

/** Formats a server-supplied epoch-millisecond email-change deadline as a stable UTC timestamp. */
fun emailChangeDeadlineText(epochMillis: Long): String =
    "${DateFormat("yyyy-MM-dd HH:mm:ss").format(DateTime.fromUnixMillis(epochMillis).toOffsetUnadjusted(0.minutes))} UTC"
