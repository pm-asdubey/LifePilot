package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lifepilot.data.database.entity.ProfileEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profiles ORDER BY is_primary DESC, display_name ASC")
    fun observeProfiles(): Flow<List<ProfileEntity>>

    @Query("SELECT * FROM profiles WHERE profile_id = :profileId")
    suspend fun getProfileById(profileId: String): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)

    @Update
    suspend fun updateProfile(profile: ProfileEntity)

    @Query("DELETE FROM profiles WHERE profile_id = :profileId")
    suspend fun deleteProfile(profileId: String)

    @Query("UPDATE profiles SET is_primary = 0")
    suspend fun clearAllPrimary()

    @Query("UPDATE profiles SET is_primary = 1 WHERE profile_id = :profileId")
    suspend fun setPrimary(profileId: String)

    @Query("SELECT * FROM profiles WHERE is_primary = 1 LIMIT 1")
    fun observePrimaryProfile(): Flow<ProfileEntity?>
}
