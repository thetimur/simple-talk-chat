package ru.senin.kotlin.net.registry.storage

import ru.senin.kotlin.net.UserInfo
import java.util.concurrent.ConcurrentHashMap

class MemoryUserStorage: UserStorage {

    private val storage = ConcurrentHashMap<String, UserInfo>()

    override fun getUserList(): List<UserInfo> {
        return storage.values.toList()
    }

    override fun containsUser(username: String): Boolean {
        return storage.containsKey(username)
    }

    override fun updateUser(user: UserInfo) {
        storage[user.name] = user
    }

    override fun removeUser(user: String) {
        storage.remove(user)
    }

    override fun clearStorage() {
        storage.clear()
    }

    override fun init() {
        storage.clear()
    }
}