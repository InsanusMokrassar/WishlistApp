package dev.inmo.wishlist.features.wishlist.common.repo

import dev.inmo.micro_utils.repos.CRUDRepo
import dev.inmo.wishlist.features.wishlist.common.models.BookingId
import dev.inmo.wishlist.features.wishlist.common.models.NewBooking
import dev.inmo.wishlist.features.wishlist.common.models.RegisteredBooking

/**
 * Full CRUD repository for [RegisteredBooking] entities.
 *
 * Combines [ReadBookingRepo] and [WriteBookingRepo] into a single interface used by the
 * service and cache layers.
 */
interface BookingRepo : ReadBookingRepo, WriteBookingRepo, CRUDRepo<RegisteredBooking, BookingId, NewBooking>
