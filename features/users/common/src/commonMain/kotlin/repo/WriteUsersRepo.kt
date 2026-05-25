package project_group.project_name.features.users.common.repo

import dev.inmo.micro_utils.repos.WriteCRUDRepo
import project_group.project_name.features.users.common.models.NewUser
import project_group.project_name.features.users.common.models.RegisteredUser
import project_group.project_name.features.users.common.models.UserId

interface WriteUsersRepo : WriteCRUDRepo<RegisteredUser, UserId, NewUser>
