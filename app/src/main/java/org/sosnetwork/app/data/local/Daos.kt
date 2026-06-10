package org.sosnetwork.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface IdentityDao {
    @Query("SELECT * FROM identity LIMIT 1")
    fun observeIdentity(): Flow<IdentityEntity?>

    @Query("SELECT * FROM identity LIMIT 1")
    suspend fun getIdentity(): IdentityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(identity: IdentityEntity)
}

@Dao
interface VerificationDao {
    @Query("SELECT * FROM verification_requests WHERE idVerified = 0 OR physicallyVerified = 0 ORDER BY submittedAtEpochMs ASC")
    fun observePending(): Flow<List<VerificationRequestEntity>>

    @Query("SELECT * FROM verification_requests WHERE peerId = :peerId")
    suspend fun get(peerId: String): VerificationRequestEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(request: VerificationRequestEntity)

    @Query("UPDATE verification_requests SET idVerified = :idVerified, physicallyVerified = :physicallyVerified, adminNotes = :notes WHERE peerId = :peerId")
    suspend fun updateVerification(peerId: String, idVerified: Boolean, physicallyVerified: Boolean, notes: String?)
}

@Dao
interface AlertDao {
    @Query("SELECT * FROM received_alerts ORDER BY receivedAtEpochMs DESC")
    fun observeAlerts(): Flow<List<ReceivedAlertEntity>>

    @Query("SELECT * FROM received_alerts WHERE alertId = :alertId LIMIT 1")
    suspend fun getByAlertId(alertId: String): ReceivedAlertEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(alert: ReceivedAlertEntity)

    @Query("UPDATE received_alerts SET acknowledged = 1 WHERE alertId = :alertId")
    suspend fun acknowledge(alertId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM seen_messages WHERE messageId = :id)")
    suspend fun hasSeenMessage(id: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markSeen(id: String, seenAt: Long)
}
