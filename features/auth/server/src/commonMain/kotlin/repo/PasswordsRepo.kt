package dev.inmo.wishlist.features.auth.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import dev.inmo.wishlist.features.auth.common.models.Password
import dev.inmo.wishlist.features.users.common.models.UserId
import dev.inmo.wishlist.features.users.common.models.Username

interface PasswordsRepo : KeyValueRepo<UserId, Password>
