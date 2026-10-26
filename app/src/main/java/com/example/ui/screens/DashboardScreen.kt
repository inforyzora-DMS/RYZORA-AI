package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RyzoraRepository
import com.example.model.OrderStatus
import com.example.ui.components.*
import com.example.ui.theme.*

@Composable
fun DashboardScreen(
    repository: RyzoraRepository,
    onNavigateToConversations: () -> Unit,
    onNavigateToOrders: () -> Unit,
    onNavigateToSimulator: () -> Unit
) {
    val orders by repository.orders.collectAsState()
    val conversations by repository.conversations.collectAsState()
    val config by repository.businessConfig.collectAsState()
    val logs by repository.webhookLogs.collectAsState()

    val pendingTakeovers = conversations.count { it.requiresHumanTakeover }
    val todayRevenueBdt = orders.filter { it.status != OrderStatus.Cancelled }.sumOf { it.totalBdt }
    val totalOrdersCount = orders.size

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        // 1. Brand Banner & Meta Status
        item {
            RyzoraGlassCard(
                borderColor = if (config.isGlobalAiAutoReplyEnabled) RyzoraCyan.copy(alpha = 0.5f) else RyzoraRose.copy(alpha = 0.5f)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "RYZORA",
                                color = TextPrimary,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = 2.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            StatusBadge(
                                text = "v21.0 Connected",
                                color = RyzoraEmerald
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Page ID: ${config.facebookPageId} (@ryzorabd) • BD",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Smart Choice. Better Life.",
                            color = RyzoraCyan,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Global AI Auto-reply Toggle
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (config.isGlobalAiAutoReplyEnabled) "AI AUTO-REPLY ON" else "AI PAUSED",
                            color = if (config.isGlobalAiAutoReplyEnabled) RyzoraEmerald else RyzoraRose,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Switch(
                            checked = config.isGlobalAiAutoReplyEnabled,
                            onCheckedChange = { repository.toggleGlobalAi(it) },
                            modifier = Modifier.testTag("global_ai_toggle"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = RyzoraEmerald,
                                checkedTrackColor = RyzoraEmerald.copy(alpha = 0.2f),
                                uncheckedThumbColor = RyzoraRose,
                                uncheckedTrackColor = SurfaceDark
                            )
                        )
                    }
                }
            }
        }

        // 2. Urgent Attention Banner for Human Escalations
        if (pendingTakeovers > 0) {
            item {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, RyzoraAmber, RoundedCornerShape(14.dp))
                        .clickable { onNavigateToConversations() },
                    color = RyzoraAmber.copy(alpha = 0.12f)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(RyzoraAmber.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = null,
                                tint = RyzoraAmber,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Human Attention Required",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$pendingTakeovers customer conversation(s) escalated for human support.",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "View conversations",
                            tint = RyzoraAmber,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // 3. KPI Grid (2x2)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "TOTAL SALES (BDT)",
                        value = "৳${todayRevenueBdt.toInt()}",
                        subtitle = "+18% from Facebook",
                        icon = Icons.Default.Payments,
                        accentColor = RyzoraEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "ORDERS RECORDED",
                        value = "$totalOrdersCount",
                        subtitle = "COD & Express",
                        icon = Icons.Default.LocalMall,
                        accentColor = RyzoraCyan,
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricStatCard(
                        title = "AI AUTOMATION",
                        value = "98.2%",
                        subtitle = "Bangla & English",
                        icon = Icons.Default.AutoAwesome,
                        accentColor = RyzoraPurple,
                        modifier = Modifier.weight(1f)
                    )
                    MetricStatCard(
                        title = "ACTIVE CHATS",
                        value = "${conversations.size}",
                        subtitle = "$pendingTakeovers escalated",
                        icon = Icons.Default.Forum,
                        accentColor = if (pendingTakeovers > 0) RyzoraAmber else RyzoraBlue,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 4. Quick Action Shortcuts
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onNavigateToSimulator,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("simulator_shortcut_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = RyzoraCyan),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Test Webhook", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onNavigateToOrders,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("orders_shortcut_button"),
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ListAlt,
                        contentDescription = null,
                        tint = TextPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Manage Orders", color = TextPrimary)
                }
            }
        }

        // 5. Recent Orders Preview
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Recent Confirmed Orders",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See All",
                    color = RyzoraCyan,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.clickable { onNavigateToOrders() }
                )
            }
        }

        items(orders.take(3).size) { index ->
            val order = orders[index]
            RyzoraGlassCard(
                modifier = Modifier.clickable { onNavigateToOrders() }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = order.orderNumber,
                            color = RyzoraCyan,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${order.customerName} • ${order.customerPhone}",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                    OrderStatusBadge(status = order.status)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = order.items.joinToString(", ") { "${it.quantity}x ${it.productName}" },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total: ৳${order.totalBdt.toInt()} (COD)",
                        color = RyzoraEmerald,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.deliveryZone,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // 6. Real-time Webhook Activity Logs
        item {
            Text(
                text = "Live Webhook & Agent Activity",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        items(logs.take(4).size) { index ->
            val log = logs[index]
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, SurfaceBorder.copy(alpha = 0.5f), RoundedCornerShape(10.dp)),
                color = SurfaceCardLight
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (log.status == "SUCCESS") RyzoraEmerald else RyzoraAmber)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = log.type,
                                color = TextPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.timestamp,
                                color = TextMuted,
                                fontSize = 10.sp
                            )
                        }
                        Text(
                            text = log.details,
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }
    }
}
