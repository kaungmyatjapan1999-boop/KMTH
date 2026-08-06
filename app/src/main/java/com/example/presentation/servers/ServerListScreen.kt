package com.example.presentation.servers

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.ServerSyncState
import com.example.domain.model.ServerCategory
import com.example.domain.model.VpnServer
import com.example.presentation.components.FrostedMeshBackground
import com.example.presentation.components.GlassmorphicCard
import com.example.presentation.components.ServerItemCard
import com.example.presentation.viewmodel.VpnViewModel
import com.example.ui.theme.CyberDarkSurface
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.GreenConnected
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.VividOrange

@Composable
fun ServerListScreen(
    viewModel: VpnViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val servers by viewModel.allServers.collectAsStateWithLifecycle()
    val selectedServer by viewModel.selectedServer.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val syncState by viewModel.serverSyncState.collectAsStateWithLifecycle()
    val isPingingServers by viewModel.isPingingServers.collectAsStateWithLifecycle()

    val filteredServers = servers.filter { server ->
        val matchesCategory = when (selectedCategory) {
            ServerCategory.ALL -> true
            ServerCategory.FASTEST -> server.pingMs < 30
            ServerCategory.GAMING -> server.category == ServerCategory.GAMING || server.pingMs < 35
            ServerCategory.STREAMING -> server.category == ServerCategory.STREAMING
            ServerCategory.P2P -> server.category == ServerCategory.P2P
            ServerCategory.FAVORITES -> server.isFavorite
        }
        val matchesSearch = searchQuery.isEmpty() ||
                server.countryName.contains(searchQuery, ignoreCase = true) ||
                server.cityName.contains(searchQuery, ignoreCase = true) ||
                server.countryCode.contains(searchQuery, ignoreCase = true)

        matchesCategory && matchesSearch
    }

    FrostedMeshBackground(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "SELECT VPN SERVER",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "${filteredServers.size} Nodes Available • Global Mesh",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { viewModel.manualPingAllServers() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, NeonPurple.copy(alpha = 0.5f), CircleShape)
                        .testTag("ping_servers_button")
                ) {
                    if (isPingingServers) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = NeonPurple,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Ping Real-Time Latency",
                            tint = NeonPurple,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = { viewModel.refreshServers() },
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, ElectricBlue.copy(alpha = 0.5f), CircleShape)
                        .testTag("refresh_servers_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Sync VLESS Config",
                        tint = ElectricBlue,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Sync Status Indicator Banner
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                borderColor = when (syncState) {
                    is ServerSyncState.Success -> GreenConnected.copy(alpha = 0.5f)
                    is ServerSyncState.Offline -> VividOrange.copy(alpha = 0.5f)
                    is ServerSyncState.Syncing -> ElectricBlue.copy(alpha = 0.5f)
                    else -> Color(0x33FFFFFF)
                }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        when (syncState) {
                            is ServerSyncState.Syncing -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = ElectricBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = "Fetching VLESS config from cloud...",
                                    fontSize = 11.sp,
                                    color = ElectricBlue,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is ServerSyncState.Success -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Online Synced",
                                    tint = GreenConnected,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Online Mode • Synced ${(syncState as ServerSyncState.Success).count} VLESS nodes to Room DB",
                                    fontSize = 11.sp,
                                    color = GreenConnected,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is ServerSyncState.Offline -> {
                                Icon(
                                    imageVector = Icons.Default.WifiOff,
                                    contentDescription = "Offline Mode",
                                    tint = VividOrange,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Offline Mode • ${(syncState as ServerSyncState.Offline).localCount} local nodes loaded from Room DB",
                                    fontSize = 11.sp,
                                    color = VividOrange,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            is ServerSyncState.Error -> {
                                Icon(
                                    imageVector = Icons.Default.CloudSync,
                                    contentDescription = "Sync Notice",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Local Database Mode • ${(syncState as ServerSyncState.Error).message}",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    maxLines = 1
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Ready",
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Room DB VLESS Node Manager",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Search Input Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search country, city, or code...", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = ElectricBlue
                    )
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .testTag("server_search_input"),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0x1AFFFFFF),
                    unfocusedContainerColor = Color(0x0CFFFFFF),
                    focusedBorderColor = ElectricBlue,
                    unfocusedBorderColor = Color(0x22FFFFFF),
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ServerCategory.values()) { category ->
                    val isSelected = category == selectedCategory

                    Box(
                        modifier = Modifier
                            .testTag("category_chip_${category.name}")
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                if (isSelected) {
                                    Brush.horizontalGradient(listOf(ElectricBlue.copy(alpha = 0.35f), NeonPurple.copy(alpha = 0.35f)))
                                } else {
                                    Brush.linearGradient(listOf(Color(0x12FFFFFF), Color(0x0CFFFFFF)))
                                }
                            )
                            .border(
                                width = if (isSelected) 1.5.dp else 1.dp,
                                color = if (isSelected) ElectricBlue else Color(0x22FFFFFF),
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { viewModel.setSelectedCategory(category) }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = category.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) ElectricBlue else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Quick Smart Location Feature Banner
            GlassmorphicCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clickable {
                        servers.minByOrNull { it.pingMs }?.let { fastest ->
                            viewModel.selectServer(fastest)
                            onBackClick()
                        }
                    },
                borderColor = VividOrange.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(VividOrange.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Bolt,
                            contentDescription = "Smart Connect",
                            tint = VividOrange,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "SMART AUTO-CONNECT",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = VividOrange
                        )
                        Text(
                            text = "Connects instantly to the nearest server with lowest ping",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Server List
            if (filteredServers.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No VPN Nodes Found",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Try adjusting your search query or category filter.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filteredServers, key = { it.id }) { server ->
                        ServerItemCard(
                            server = server,
                            isSelected = server.id == selectedServer?.id,
                            onSelect = {
                                viewModel.selectServer(server)
                                onBackClick()
                            },
                            onToggleFavorite = {
                                viewModel.toggleFavorite(server)
                            }
                        )
                    }
                }
            }
        }
    }
}

