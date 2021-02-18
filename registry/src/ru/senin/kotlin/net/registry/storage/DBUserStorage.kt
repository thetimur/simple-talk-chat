package ru.senin.kotlin.net.registry.storage

import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import ru.senin.kotlin.net.Protocol
import ru.senin.kotlin.net.UserAddress
import ru.senin.kotlin.net.UserInfo
import ru.senin.kotlin.net.registry.UserWithoutAddressException
import java.lang.Thread.sleep

class DBUserStorage(
        private val url: String,
        private val driver: String
    ) : UserStorage {

    override fun init() {
        Database.connect(url, driver = driver)
        transaction {
            addLogger(StdOutSqlLogger)
            SchemaUtils.create(Users, Addresses)

            commit()
        }
        sleep(1000) // Initializing database
    }


    private fun getAddressIdByUsername(username: String): Int? {
        return transaction {
            addLogger(StdOutSqlLogger)
            Users
                    .select { Users.name eq username }
                    .withDistinct()
                    .map { it[Users.addressId] }
                    .first()
        }
    }

    private fun getAddressById(id: Int?): UserAddress {
        if (id == null) {
            throw UserWithoutAddressException()
        }
        return transaction {
            addLogger(StdOutSqlLogger)
            Addresses
                    .select { Addresses.id eq id }
                    .withDistinct()
                    .map { UserAddress(Protocol.valueOf(it[Addresses.protocol]), it[Addresses.host], it[Addresses.port]) }
                    .first()
        }
    }

    override fun getUserList(): List<UserInfo> {
        return transaction {
            addLogger(StdOutSqlLogger)
            Users.selectAll().map {
                    UserInfo(
                            it[Users.name],
                            getAddressById(it[Users.addressId])
                    )
                }
            }
    }

    override fun containsUser(username: String): Boolean {
        return transaction {
            addLogger(StdOutSqlLogger)
            !Users.select { Users.name eq username }.empty()
        }
    }

    private fun insertUser(user: UserInfo) {
        transaction {
            addLogger(StdOutSqlLogger)
            Users.insert { newUser ->
                newUser[name] = user.name
                val newAddress = Addresses.insert {
                    it[protocol] = user.address.protocol.stringValue
                    it[host] = user.address.host
                    it[port] = user.address.port
                }
                newUser[addressId] = newAddress[Addresses.id]
            }
            commit()
        }
    }

    override fun updateUser(user: UserInfo) {
        if (!containsUser(user.name)) {
            insertUser(user);
            return
        }
        transaction {
            addLogger(StdOutSqlLogger)
            Users.update { newUser ->
                newUser[name] = user.name
                val newAddress = Addresses.update {
                    it[protocol] = user.address.protocol.stringValue
                    it[host] = user.address.host
                    it[port] = user.address.port
                }
                newUser[addressId] = newAddress
            }
            commit()
        }
    }

    override fun removeUser(name: String) {
        transaction {
            addLogger(StdOutSqlLogger)
            Users.deleteWhere {
                Users.name eq name
            }
            commit()
        }
    }

    override fun clearStorage() {
        transaction {
            addLogger(StdOutSqlLogger)
            Users.deleteAll()
            commit()
        }
    }
}