package project_group.project_name.features.users.common.repo

import dev.inmo.micro_utils.repos.ReadCRUDRepo
import project_group.project_name.features.users.common.models.RegisteredUser
import project_group.project_name.features.users.common.models.UserId
import project_group.project_name.features.users.common.models.Username

interface ReadUsersRepo : ReadCRUDRepo<RegisteredUser, UserId> {
    suspend fun getUserByUsername(username: Username): RegisteredUser?
}
