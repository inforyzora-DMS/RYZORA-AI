package com.example.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.RyzoraRepository
import com.example.model.Product
import com.example.ui.components.RyzoraGlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun ProductsScreen(
    repository: RyzoraRepository
) {
    val products by repository.products.collectAsState()
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var editingProduct by remember { mutableStateOf<Product?>(null) }

    val categories = remember(products) {
        listOf("All") + products.map { it.category }.distinct()
    }

    val filteredProducts = if (selectedCategory == null || selectedCategory == "All") {
        products
    } else {
        products.filter { it.category == selectedCategory }
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
                    text = "Product Catalog",
                    color = TextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Source of truth for AI pricing & stock in BDT (৳)",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Categories
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 4.dp)
        ) {
            items(categories) { cat ->
                val isSelected = (selectedCategory == cat) || (selectedCategory == null && cat == "All")
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedCategory = if (cat == "All") null else cat },
                    label = { Text(cat) },
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
            items(filteredProducts) { product ->
                ProductCardItem(
                    product = product,
                    onToggleStock = { repository.toggleProductStock(product.id) },
                    onEditPrice = { editingProduct = product }
                )
            }
        }
    }

    // Price editing dialog
    if (editingProduct != null) {
        val prod = editingProduct!!
        var regPriceText by remember { mutableStateOf(prod.priceBdt.toInt().toString()) }
        var disPriceText by remember { mutableStateOf(prod.discountPriceBdt?.toInt()?.toString() ?: "") }

        AlertDialog(
            onDismissRequest = { editingProduct = null },
            title = { Text("Update Price (BDT ৳)", color = TextPrimary) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(prod.name, color = TextSecondary, fontSize = 13.sp)

                    OutlinedTextField(
                        value = regPriceText,
                        onValueChange = { regPriceText = it },
                        label = { Text("Regular Price (BDT ৳)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RyzoraCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = disPriceText,
                        onValueChange = { disPriceText = it },
                        label = { Text("Offer / Discount Price (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RyzoraCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val reg = regPriceText.toDoubleOrNull() ?: prod.priceBdt
                        val dis = disPriceText.toDoubleOrNull()
                        repository.updateProductPrice(prod.id, reg, dis)
                        editingProduct = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RyzoraCyan)
                ) {
                    Text("Save Price", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProduct = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = SurfaceCard
        )
    }
}

@Composable
private fun ProductCardItem(
    product: Product,
    onToggleStock: () -> Unit,
    onEditPrice: () -> Unit
) {
    val inStock = product.stockQuantity > 0

    RyzoraGlassCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.sku,
                    color = RyzoraCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.name,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = product.banglaName,
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }

            StatusBadge(
                text = if (inStock) "In Stock (${product.stockQuantity})" else "Stock Out",
                color = if (inStock) RyzoraEmerald else RyzoraRose
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = product.description,
            color = TextMuted,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Variants
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Column {
                Text("Variants / Colors", color = TextMuted, fontSize = 10.sp)
                Text(
                    product.availableColors.joinToString(", "),
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            if (product.availableSizes.isNotEmpty()) {
                Column {
                    Text("Sizes", color = TextMuted, fontSize = 10.sp)
                    Text(
                        product.availableSizes.joinToString(", "),
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pricing & actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (product.discountPriceBdt != null) {
                    Text(
                        text = "৳${product.discountPriceBdt.toInt()}",
                        color = RyzoraEmerald,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "৳${product.priceBdt.toInt()}",
                        color = TextMuted,
                        fontSize = 13.sp,
                        textDecoration = androidx.compose.ui.text.style.TextDecoration.LineThrough
                    )
                } else {
                    Text(
                        text = "৳${product.priceBdt.toInt()}",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onEditPrice,
                    border = BorderStroke(1.dp, SurfaceBorder),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("edit_price_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Price",
                        tint = TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Price", color = TextSecondary, fontSize = 11.sp)
                }

                Button(
                    onClick = onToggleStock,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (inStock) RyzoraRose.copy(alpha = 0.2f) else RyzoraEmerald.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.testTag("toggle_stock_button")
                ) {
                    Text(
                        text = if (inStock) "Set Out of Stock" else "Restock",
                        color = if (inStock) RyzoraRose else RyzoraEmerald,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
