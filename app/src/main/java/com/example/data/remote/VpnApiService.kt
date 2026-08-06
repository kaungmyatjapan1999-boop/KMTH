package com.example.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class ServerDto(
    val id: String,
    val countryName: String,
    val countryCode: String,
    val cityName: String,
    val ipAddress: String,
    val flagEmoji: String,
    val pingMs: Int,
    val serverLoadPercentage: Int,
    val isPremium: Boolean,
    val categoryName: String
)

data class IpCheckResponse(
    val ip: String,
    val country: String,
    val city: String,
    val org: String,
    val secure: Boolean
)

data class SpeedTestResultDto(
    val pingMs: Int,
    val downloadSpeedMbps: Float,
    val uploadSpeedMbps: Float,
    val jitterMs: Int,
    val serverName: String
)

interface VpnApiService {

    @GET("servers")
    suspend fun getServers(): List<ServerDto>

    @GET("ip-check")
    suspend fun checkCurrentIp(): IpCheckResponse

    @GET("ping")
    suspend fun pingServer(@Query("server_ip") ip: String): SpeedTestResultDto
}
