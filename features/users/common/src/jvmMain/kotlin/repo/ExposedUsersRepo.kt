package project_group.project_name.features.users.common.repo

import dev.inmo.micro_utils.repos.exposed.AbstractExposedCRUDRepo
import dev.inmo.micro_utils.repos.exposed.initTable
import org.jetbrains.exposed.v1.core.Op
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.statements.InsertStatement
import org.jetbrains.exposed.v1.core.statements.UpdateBuilder
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import project_group.project_name.features.users.common.models.NewUser
import project_group.project_name.features.users.common.models.RegisteredUser
import project_group.project_name.features.users.common.models.UserId
import project_group.project_name.features.users.common.models.Username

class ExposedUsersRepo(
    override val database: Database
) : UsersRepo, AbstractExposedCRUDRepo<RegisteredUser, UserId, NewUser>(tableName = "users") {
    private val idColumn = long("id").autoIncrement()
    private val usernameColumn = text("username").uniqueIndex()

    override val primaryKey = PrimaryKey(idColumn)

    override val ResultRow.asObject: RegisteredUser
        get() = RegisteredUser(
            id = UserId(get(idColumn)),
            username = Username(get(usernameColumn))
        )

    override val ResultRow.asId: UserId
        get() = UserId(get(idColumn))

    override val selectById: (UserId) -> Op<Boolean> = { idColumn.eq(it.long) }

    override fun update(id: UserId?, value: NewUser, it: UpdateBuilder<Int>) {
        it[usernameColumn] = value.username.string
    }

    override fun InsertStatement<Number>.asObject(value: NewUser): RegisteredUser =
        RegisteredUser(
            id = UserId(this[idColumn]),
            username = value.username
        )

    override suspend fun getUserByUsername(username: Username): RegisteredUser? =
        transaction(db = database) {
            selectAll().where { usernameColumn eq username.string }.limit(1).firstOrNull()?.asObject
        }

    init {
        initTable()
    }
}
