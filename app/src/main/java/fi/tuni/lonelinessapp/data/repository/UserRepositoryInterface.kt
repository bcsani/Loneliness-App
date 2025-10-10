package fi.tuni.lonelinessapp.data.repository

interface UserRepositoryInterface {
    fun fetchName(userID: Int): String

    fun fetchAge(userID: Int): Int
}