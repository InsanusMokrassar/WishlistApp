package project_group.project_name.features.auth.server.repo

import dev.inmo.micro_utils.repos.KeyValueRepo
import project_group.project_name.features.auth.common.models.Password
import project_group.project_name.features.users.common.models.UserId
import project_group.project_name.features.users.common.models.Username

interface PasswordsRepo : KeyValueRepo<UserId, Password>
