package ru.senin.kotlin.net.registry.storage

import org.jetbrains.exposed.sql.Table
import ru.senin.kotlin.net.UserInfo


interface UserStorage {
    fun getUserList() : List<UserInfo>
    fun containsUser(username: String): Boolean
    fun updateUser(user: UserInfo)
    fun removeUser(user: String)
    fun clearStorage()
    fun init()
}

object Users : Table() {
    val name = varchar("name", length = 50)
    val addressId = (integer("address_id") references Addresses.id).nullable()

    override val primaryKey = PrimaryKey(name, name = "PK_User_ID")
}

object Addresses : Table() {
    val id = integer("id").autoIncrement()
    val protocol = varchar("protocol", 10)
    val host = varchar("host", 50)
    val port = integer("port")

    override val primaryKey = PrimaryKey(id, name = "PK_Addresses_ID")
}
