package com.example.model

enum class SenderType {
    CUSTOMER,
    AI_AGENT,
    HUMAN_ADMIN,
    SYSTEM
}

enum class OrderStatus {
    Pending,
    Confirmed,
    Processing,
    Shipped,
    Delivered,
    Cancelled
}

data class Product(
    val id: String,
    val sku: String,
    val name: String,
    val banglaName: String,
    val description: String,
    val category: String,
    val priceBdt: Double,
    val discountPriceBdt: Double? = null,
    val availableSizes: List<String> = emptyList(),
    val availableColors: List<String> = emptyList(),
    val stockQuantity: Int = 0,
    val isActive: Boolean = true
)

data class OrderItem(
    val productName: String,
    val variant: String,
    val quantity: Int,
    val unitPriceBdt: Double,
    val totalPriceBdt: Double
)

data class Order(
    val id: String,
    val orderNumber: String,
    val customerName: String,
    val customerPhone: String,
    val deliveryAddress: String,
    val deliveryZone: String = "Inside Dhaka",
    val deliveryFeeBdt: Double = 60.0,
    val subtotalBdt: Double,
    val totalBdt: Double,
    val status: OrderStatus = OrderStatus.Pending,
    val paymentMethod: String = "Cash on Delivery (COD)",
    val items: List<OrderItem> = emptyList(),
    val createdAt: String
)

data class ChatMessage(
    val id: String,
    val conversationId: String,
    val senderType: SenderType,
    val text: String,
    val timestamp: String,
    val detectedIntent: String? = null
)

data class Conversation(
    val id: String,
    val customerPsid: String,
    val customerName: String,
    val isAiActive: Boolean = true,
    val requiresHumanTakeover: Boolean = false,
    val currentIntent: String = "GREETING",
    val lastMessage: String = "",
    val lastMessageTime: String = "Just now",
    val unreadCount: Int = 0
)

data class WebhookLog(
    val id: String,
    val timestamp: String,
    val type: String,
    val status: String,
    val details: String
)

data class BusinessConfig(
    val brandName: String = "RYZORA",
    val tagline: String = "Smart Choice. Better Life.",
    val website: String = "https://ryzorabd.com",
    val facebookPage: String = "https://www.facebook.com/ryzorabd",
    val facebookPageId: String = "1268010728837960",
    val metaAppId: String = "1268010728837960",
    val hotline: String = "+880 1700-000000",
    val insideDhakaFee: Double = 60.0,
    val outsideDhakaFee: Double = 120.0,
    val freeDeliveryThreshold: Double = 3000.0,
    val returnWindowDays: Int = 7,
    val isGlobalAiAutoReplyEnabled: Boolean = true
)
