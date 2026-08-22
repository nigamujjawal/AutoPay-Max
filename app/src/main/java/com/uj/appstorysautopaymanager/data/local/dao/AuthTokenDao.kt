package com.uj.appstorysautopaymanager.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.uj.appstorysautopaymanager.data.local.entity.AuthTokenEntity

@Dao
interface AuthTokenDao {
    @Query("SELECT * FROM auth_token LIMIT 1")
    suspend fun getToken(): AuthTokenEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveToken(token: AuthTokenEntity)

    @Query("UPDATE auth_token SET name = :name WHERE uid = :uid")
    suspend fun updateName(uid: String, name: String)

    @Query("DELETE FROM auth_token")
    suspend fun clear()
}
