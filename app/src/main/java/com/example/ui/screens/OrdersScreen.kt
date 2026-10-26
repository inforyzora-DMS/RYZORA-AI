package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RyzoraRepository
import com.example.model.Order
import com.example.model.OrderStatus
import com.example.ui.components.OrderStatusBadge
import com.example.ui.components.RyzoraGlassCard
import com.example.ui.theme.*

@Composable
fun OrdersScreen(
    repository: RyzoraRepository
) {
    val orders by repository.orders.collectAsState()
    var selectedFilter by remember { mutableStateOf<OrderStatus?>(null) }
    val context = LocalContext.current

    val filteredOrders = if (selectedFilter == null) {
        orders
    } else {
        orders.filter { it.status == selectedFilter }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Order Management",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${orders.size} Total Orders • Bangladesh (BDT ৳)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Status filter tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedFilter == null,
                    onClick = { selectedFilter = null },
                    label = { Text("All (${orders.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RyzoraCyan.copy(alpha = 0.2f),
                        selectedLabelColor = RyzoraCyan
                    )
                )
            }
            items(OrderStatus.entries.toTypedArray()) { status ->
                val count = orders.count { it.status == status }
                FilterChip(
                    selected = selectedFilter == status,
                    onClick = { selectedFilter = status },
                    label = { Text("$status ($count)") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = RyzoraCyan.copy(alpha = 0.2f),
                        selectedLabelColor = RyzoraCyan
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            if (filteredOrders.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Inbox,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No orders found in this status.",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            items(filteredOrders) { order ->
                OrderCardItem(
                    order = order,
                    onUpdateStatus = { newStatus ->
                        repository.updateOrderStatus(order.id, newStatus)
                    },
                    onCallCustomer = { phone ->
                        val intent = Intent(Intent.ACTION_DIAL).apply {
                            data = Uri.parse("tel:$phone")
                        }
                        context.startActivity(intent)
                    }
                )
            }
        }
    }
}

@Composable
private fun OrderCardItem(
    order: Order,
    onUpdateStatus: (OrderStatus) -> Unit,
    onCallCustomer: (String) -> Unit
) {
    var expandedStatusMenu by remember { mutableStateOf(false) }

    RyzoraGlassCard {
        // Order Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = order.orderNumber,
                    color = RyzoraCyan,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = order.createdAt,
                    color = TextMuted,
                    fontSize = 11.sp
                )
            }
            OrderStatusBadge(status = order.status)
        }

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 10.dp),
            color = SurfaceBorder
        )

        // Customer details
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = order.customerName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Phone: ${order.customerPhone}",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Text(
                    text = "Delivery Address: ${order.deliveryAddress}",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }

            IconButton(
                onClick = { onCallCustomer(order.customerPhone) },
                modifier = Modifier.testTag("call_customer_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = "Call Customer",
                    tint = RyzoraEmerald
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Items List
        Surface(
            color = SurfaceDark,
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${item.quantity}x ${item.productName} (${item.variant})",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "৳${item.totalPriceBdt.toInt()}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Price breakdown & total
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Delivery (${order.deliveryZone}): ৳${order.deliveryFeeBdt.toInt()}",
                color = TextSecondary,
                fontSize = 12.sp
            )
            Text(
                text = "Total: ৳${order.totalBdt.toInt()}",
                color = RyzoraEmerald,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Status update actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                OutlinedButton(
                    onClick = { expandedStatusMenu = true },
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    modifier = Modifier.testTag("change_status_button")
                ) {
                    Text("Change Status", fontSize = 12.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }

                DropdownMenu(
                    expanded = expandedStatusMenu,
                    onDismissRequest = { expandedStatusMenu = false },
                    modifier = Modifier.background(SurfaceCard)
                ) {
                    OrderStatus.entries.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(status.name, color = TextPrimary) },
                            onClick = {
                                onUpdateStatus(status)
                                expandedStatusMenu = false
                            }
                        )
                    }
                }
            }
        }
    }
}
