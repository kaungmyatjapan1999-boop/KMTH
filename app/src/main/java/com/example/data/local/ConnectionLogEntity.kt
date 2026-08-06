package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ConnectionSessionLog

@Entity(tableName = "connection_logs")
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverName: String,
    val countryCode: String,
    val connectedAtTimestamp: Long,
    val durationSeconds: Long,
    val bytesDownloaded: Long,
    val bytesUploaded: Long,
    val protocolUsed: String
) {
    fun toDomainModel(): ConnectionSessionLog {
        return ConnectionSessionLog(
            id = id,
            serverName = serverName,
            countryCode = countryCode,
            connectedAtTimestamp = connectedAtTimestamp,
            durationSeconds = durationSeconds,
            bytesDownloaded = bytesDownloaded,
            bytesUploaded = bytesUploaded,
            protocolUsed = protocolUsed
        )
    }

    companion object {
        fun fromDomainModel(log: ConnectionSessionLog): ConnectionLogEntity {
            return ConnectionLogEntity(
                id = log.id,
                serverName = log.serverName,
                countryCode = log.countryCode,
                connectedAtTimestamp = log.connectedAtTimestamp,
                durationSeconds = log.durationSeconds,
                bytesDownloaded = log.bytesDownloaded,
                bytesUploaded = log.bytesUploaded,
                protocolUsed = log.protocolUsed
            )
        }
    }
}
