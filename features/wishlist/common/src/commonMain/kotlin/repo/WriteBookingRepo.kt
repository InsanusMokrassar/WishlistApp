package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.WriteCRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.BookingId
import dev.inmo.wishlist.features.wishlist.common.models.NewBooking
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredBooking

/**
 * Write-only repository for [RegisteredBooking] entities.
 *
 * Accepts [NewBooking] as the input type for create operations.
 */
interface WriteBookingRepo : WriteCRUDRepo<RegisteredBooking, BookingId, NewBooking>
