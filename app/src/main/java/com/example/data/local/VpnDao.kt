package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VpnDao {

    @Query("SELECT * FROM vpn_servers ORDER BY pingMs ASC")
    fun getAllServers(): Flow<List<VpnServerEntity>>

    @Query("SELECT * FROM vpn_servers WHERE isFavorite = 1")
    fun getFavoriteServers(): Flow<List<VpnServerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServers(servers: List<VpnServerEntity>)

    @Query("UPDATE vpn_servers SET isFavorite = :isFavorite WHERE id = :serverId")
    suspend fun updateFavoriteStatus(serverId: String, isFavorite: Boolean)

    @Query("SELECT * FROM connection_logs ORDER BY connectedAtTimestamp DESC LIMIT 30")
    fun getConnectionLogs(): Flow<List<ConnectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConnectionLog(log: ConnectionLogEntity)

    @Query("DELETE FROM connection_logs")
    suspend fun clearLogs()
}
