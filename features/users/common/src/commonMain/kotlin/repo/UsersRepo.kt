package project_group.project_name.features.users.common.repo

import dev.inmo.micro_utils.repos.CRUDRepo
import project_group.project_name.features.users.common.models.NewUser
import project_group.project_name.features.users.common.models.RegisteredUser
import project_group.project_name.features.users.common.models.UserId

interface UsersRepo : ReadUsersRepo, WriteUsersRepo, CRUDRepo<RegisteredUser, UserId, NewUser>
