package com.lifepilot.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.lifepilot.data.database.entity.DomainLifeStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DomainLifeStateDao {

    @Query("SELECT * FROM domain_life_states WHERE profile_id = :profileId ORDER BY domain ASC")
    fun observeAllByProfile(profileId: String): Flow<List<DomainLifeStateEntity>>

    @Query("SELECT * FROM domain_life_states WHERE profile_id = :profileId AND domain = :domain LIMIT 1")
    fun observeByDomain(profileId: String, domain: String): Flow<DomainLifeStateEntity?>

    @Query("SELECT * FROM domain_life_states WHERE profile_id = :profileId AND domain = :domain LIMIT 1")
    suspend fun getByDomain(profileId: String, domain: String): DomainLifeStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: DomainLifeStateEntity)
}
