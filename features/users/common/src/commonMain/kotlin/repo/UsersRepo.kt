package dev.inmo.wishlist.features.users.common.repo

import dev.inmo.micro_utils.repos.CRUDRepo
import dev.inmo.wishlist.features.users.common.models.NewUser
import dev.inmo.wishlist.features.users.common.models.RegisteredUser
import dev.inmo.wishlist.features.users.common.models.UserId

interface UsersRepo : ReadUsersRepo, WriteUsersRepo, CRUDRepo<RegisteredUser, UserId, NewUser>
