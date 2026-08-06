package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ServerCategory
import com.example.domain.model.VpnServer

@Entity(tableName = "vpn_servers")
data class VpnServerEntity(
    @PrimaryKey val id: String,
    val countryName: String,
    val countryCode: String,
    val cityName: String,
    val ipAddress: String,
    val flagEmoji: String,
    val pingMs: Int,
    val serverLoadPercentage: Int,
    val isPremium: Boolean,
    val isFavorite: Boolean,
    val categoryName: String
) {
    fun toDomainModel(): VpnServer {
        val cat = try {
            ServerCategory.valueOf(categoryName)
        } catch (e: Exception) {
            ServerCategory.FASTEST
        }
        return VpnServer(
            id = id,
            countryName = countryName,
            countryCode = countryCode,
            cityName = cityName,
            ipAddress = ipAddress,
            flagEmoji = flagEmoji,
            pingMs = pingMs,
            serverLoadPercentage = serverLoadPercentage,
            isPremium = isPremium,
            isFavorite = isFavorite,
            category = cat
        )
    }

    companion object {
        fun fromDomainModel(server: VpnServer): VpnServerEntity {
            return VpnServerEntity(
                id = server.id,
                countryName = server.countryName,
                countryCode = server.countryCode,
                cityName = server.cityName,
                ipAddress = server.ipAddress,
                flagEmoji = server.flagEmoji,
                pingMs = server.pingMs,
                serverLoadPercentage = server.serverLoadPercentage,
                isPremium = server.isPremium,
                isFavorite = server.isFavorite,
                categoryName = server.category.name
            )
        }
    }
}
