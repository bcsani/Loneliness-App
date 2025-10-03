package fi.tuni.lonelinessapp.data.repository

class UserRepository (
    private val userID: Int,
    private val userDataSource: UserDataSource
) : UserRepositoryInterface {
    override fun fetchAge(userID: Int): Int {
        return userDataSource.fetchAge(userID)
    }

    override fun fetchName(userID: Int): String {
        return userDataSource.fetchName(userID)
    }
}