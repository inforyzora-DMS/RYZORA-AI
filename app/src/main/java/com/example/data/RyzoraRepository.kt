package com.example.data

import com.example.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RyzoraRepository private constructor() {

    private val _products = MutableStateFlow<List<Product>>(getInitialProducts())
    val products: StateFlow<List<Product>> = _products.asStateFlow()

    private val _orders = MutableStateFlow<List<Order>>(getInitialOrders())
    val orders: StateFlow<List<Order>> = _orders.asStateFlow()

    private val _conversations = MutableStateFlow<List<Conversation>>(getInitialConversations())
    val conversations: StateFlow<List<Conversation>> = _conversations.asStateFlow()

    private val _chatMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(getInitialMessages())
    val chatMessages: StateFlow<Map<String, List<ChatMessage>>> = _chatMessages.asStateFlow()

    private val _webhookLogs = MutableStateFlow<List<WebhookLog>>(getInitialLogs())
    val webhookLogs: StateFlow<List<WebhookLog>> = _webhookLogs.asStateFlow()

    private val _businessConfig = MutableStateFlow(BusinessConfig())
    val businessConfig: StateFlow<BusinessConfig> = _businessConfig.asStateFlow()

    // Active customer draft order context
    private val activeDrafts = mutableMapOf<String, MutableMap<String, Any>>()

    fun toggleGlobalAi(enabled: Boolean) {
        _businessConfig.update { it.copy(isGlobalAiAutoReplyEnabled = enabled) }
        addWebhookLog("AI_CONTROL", "TOGGLED", "Global AI Auto-Reply set to $enabled")
    }

    fun toggleConversationAi(conversationId: String, enableAi: Boolean) {
        _conversations.update { list ->
            list.map { conv ->
                if (conv.id == conversationId) {
                    conv.copy(
                        isAiActive = enableAi,
                        requiresHumanTakeover = !enableAi
                    )
                } else conv
            }
        }
        val statusText = if (enableAi) "AI Resumed" else "Human Takeover Active"
        addWebhookLog("TAKEOVER", if (enableAi) "RESUMED" else "TAKEOVER", "$statusText for conversation $conversationId")
    }

    fun updateOrderStatus(orderId: String, newStatus: OrderStatus) {
        _orders.update { list ->
            list.map { order ->
                if (order.id == orderId || order.orderNumber == orderId) {
                    order.copy(status = newStatus)
                } else order
            }
        }
        addWebhookLog("ORDER_UPDATE", "STATUS_CHANGED", "Order $orderId marked as $newStatus")
    }

    fun toggleProductStock(productId: String) {
        _products.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    val newStock = if (prod.stockQuantity > 0) 0 else 50
                    prod.copy(stockQuantity = newStock)
                } else prod
            }
        }
    }

    fun updateProductPrice(productId: String, regularPrice: Double, discountPrice: Double?) {
        _products.update { list ->
            list.map { prod ->
                if (prod.id == productId) {
                    prod.copy(priceBdt = regularPrice, discountPriceBdt = discountPrice)
                } else prod
            }
        }
    }

    fun sendAdminReply(conversationId: String, replyText: String) {
        val now = currentTimeString()
        val msg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = conversationId,
            senderType = SenderType.HUMAN_ADMIN,
            text = replyText,
            timestamp = now
        )

        _chatMessages.update { map ->
            val existing = map[conversationId]?.toMutableList() ?: mutableListOf()
            existing.add(msg)
            map + (conversationId to existing)
        }

        _conversations.update { list ->
            list.map {
                if (it.id == conversationId) {
                    it.copy(
                        lastMessage = "You: $replyText",
                        lastMessageTime = "Just now",
                        requiresHumanTakeover = false
                    )
                } else it
            }
        }

        addWebhookLog("OUTBOUND_MESSAGE", "SENT", "Human Admin reply delivered via Meta Graph API")
    }

    /**
     * Simulates an incoming Meta Messenger webhook message event and runs the AI reasoning loop.
     */
    fun processIncomingCustomerMessage(
        conversationId: String,
        customerText: String,
        overrideAi: Boolean = false
    ): Pair<String, String> {
        val now = currentTimeString()
        val conv = _conversations.value.find { it.id == conversationId } ?: _conversations.value.first()

        // 1. Record incoming customer message
        val incomingMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis()}",
            conversationId = conv.id,
            senderType = SenderType.CUSTOMER,
            text = customerText,
            timestamp = now
        )

        _chatMessages.update { map ->
            val list = map[conv.id]?.toMutableList() ?: mutableListOf()
            list.add(incomingMsg)
            map + (conv.id to list)
        }

        // Check if AI is disabled or human takeover active
        val isAiActive = if (overrideAi) true else (_businessConfig.value.isGlobalAiAutoReplyEnabled && conv.isAiActive)

        if (!isAiActive) {
            _conversations.update { list ->
                list.map {
                    if (it.id == conv.id) {
                        it.copy(
                            lastMessage = customerText,
                            lastMessageTime = "Just now",
                            requiresHumanTakeover = true
                        )
                    } else it
                }
            }
            addWebhookLog("WEBHOOK_EVENT", "QUEUED", "Human Takeover active. AI skipped for ${conv.customerName}")
            return Pair("HUMAN_TAKEOVER", "AI replies currently paused for human agent takeover.")
        }

        // 2. Run Natural Language and Intent Processing
        val (intent, aiReplyText, orderCreated) = generateAiResponse(conv.id, customerText)

        // 3. Record AI reply
        val aiMsg = ChatMessage(
            id = "msg_${System.currentTimeMillis() + 1}",
            conversationId = conv.id,
            senderType = SenderType.AI_AGENT,
            text = aiReplyText,
            timestamp = now,
            detectedIntent = intent
        )

        _chatMessages.update { map ->
            val list = map[conv.id]?.toMutableList() ?: mutableListOf()
            list.add(aiMsg)
            map + (conv.id to list)
        }

        _conversations.update { list ->
            list.map {
                if (it.id == conv.id) {
                    it.copy(
                        lastMessage = "RYZORA AI: $aiReplyText",
                        lastMessageTime = "Just now",
                        currentIntent = intent,
                        requiresHumanTakeover = (intent == "HUMAN_HANDOFF")
                    )
                } else it
            }
        }

        addWebhookLog(
            type = if (orderCreated) "ORDER_CREATED" else "AI_REPLY",
            status = "SUCCESS",
            details = "Intent: $intent | Customer: ${conv.customerName}"
        )

        return Pair(intent, aiReplyText)
    }

    private fun generateAiResponse(conversationId: String, text: String): Triple<String, String, Boolean> {
        val lower = text.trim().lowercase(Locale.ROOT)
        val draft = activeDrafts.getOrPut(conversationId) { mutableMapOf() }

        // Escalation check
        if (lower.contains("human") || lower.contains("agent") || lower.contains("manager") ||
            lower.contains("অভিযোগ") || lower.contains("রিফান্ড") || lower.contains("refund") ||
            lower.contains("কথা বলতে চাই") || lower.contains("manush")
        ) {
            _conversations.update { list ->
                list.map { if (it.id == conversationId) it.copy(requiresHumanTakeover = true, isAiActive = false) else it }
            }
            return Triple(
                "HUMAN_HANDOFF",
                "সম্মানিত গ্রাহক, আপনার বার্তাটি আমাদের কাস্টমার সাপোর্ট টিমের কাছে স্থানান্তর করা হয়েছে। খুব শীঘ্রই একজন সিনিয়র প্রতিনিধি আপনার সাথে যুক্ত হবেন। ধন্যবাদ! (A human agent has been alerted).",
                false
            )
        }

        // Prompt injection defense
        if (lower.contains("ignore previous") || lower.contains("system prompt") || lower.contains("free item")) {
            return Triple(
                "GREETING",
                "আসসালামু আলাইকুম! আমি RYZORA-র অফিসিয়াল সহায়ক। আমাদের যেকোনো পণ্য, সাইজ বা অর্ডার সম্পর্কে জিজ্ঞাসা করতে পারেন।",
                false
            )
        }

        // Delivery policy inquiry
        if (lower.contains("delivery") || lower.contains("shipping") || lower.contains("ডেলিভারি") || lower.contains("চার্জ")) {
            return Triple(
                "DELIVERY_INQUIRY",
                "🚚 RYZORA ডেলিভারি তথ্য:\n• ঢাকার ভেতরে: ৳৬০ (১-২ দিন)\n• ঢাকার বাইরে: ৳১২০ (২-৪ দিন)\n• ক্যাশ অন ডেলিভারি প্রযোজ্য।\n• ৳৩০০০+ অর্ডারে সম্পূর্ণ ফ্রি ডেলিভারি!",
                false
            )
        }

        // Return policy inquiry
        if (lower.contains("return") || lower.contains("exchange") || lower.contains("রিটার্ন") || lower.contains("পরিবর্তন")) {
            return Triple(
                "RETURN_POLICY",
                "🔄 RYZORA রিটার্ন পলিসি: পণ্য প্রাপ্তির ৭ দিনের মধ্যে অরিজিনাল ইনভয়েস ও ট্যাগ সহ সহজে রিটার্ন বা এক্সচেঞ্জ করতে পারবেন। ডিফেক্টিভ পণ্যের ক্ষেত্রে কোনো চার্জ নেওয়া হয় না।",
                false
            )
        }

        // Confirming order stage
        if (draft["stage"] == "AWAITING_CONFIRMATION") {
            if (lower.contains("yes") || lower.contains("confirm") || lower.contains("কনফার্ম") || lower.contains("হ্যাঁ") || lower.contains("ok")) {
                val prodId = draft["productId"] as? String ?: "p-1"
                val prod = _products.value.find { it.id == prodId } ?: _products.value.first()
                val unitPrice = prod.discountPriceBdt ?: prod.priceBdt
                val isInside = (draft["address"] as? String ?: "").lowercase().contains("dhaka")
                val fee = if (isInside) 60.0 else 120.0
                val total = unitPrice + fee

                val dateStr = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
                val randomId = (1000..9999).random()
                val orderNumber = "RYZ-$dateStr-$randomId"

                val newOrder = Order(
                    id = "ord_${System.currentTimeMillis()}",
                    orderNumber = orderNumber,
                    customerName = draft["name"] as? String ?: "Valued Customer",
                    customerPhone = draft["phone"] as? String ?: "01700000000",
                    deliveryAddress = draft["address"] as? String ?: "Dhaka, Bangladesh",
                    deliveryZone = if (isInside) "Inside Dhaka" else "Outside Dhaka",
                    deliveryFeeBdt = fee,
                    subtotalBdt = unitPrice,
                    totalBdt = total,
                    status = OrderStatus.Pending,
                    items = listOf(
                        OrderItem(
                            productName = prod.name,
                            variant = draft["variant"] as? String ?: "Standard",
                            quantity = 1,
                            unitPriceBdt = unitPrice,
                            totalPriceBdt = unitPrice
                        )
                    ),
                    createdAt = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date())
                )

                _orders.update { listOf(newOrder) + it }
                activeDrafts.remove(conversationId)

                val reply = "🎉 আপনার RYZORA অর্ডারটি সফলভাবে কনফার্ম হয়েছে!\n\n" +
                        "📦 অর্ডার নম্বর: $orderNumber\n" +
                        "🛍️ পণ্য: ${prod.name}\n" +
                        "💰 সর্বমোট প্রদেয়: ৳$total (ক্যাশ অন ডেলিভারি)\n" +
                        "🚚 ডেলিভারি: ${if (isInside) "১-২ কার্যদিবস" else "২-৪ কার্যদিবস"}\n\n" +
                        "ডেলিভারির পূর্বে আমাদের ডেলিভারি টিম আপনাকে কল করবে। RYZORA-র সাথে থাকার জন্য ধন্যবাদ! ✨"

                return Triple("ORDER_CONFIRM", reply, true)
            }
        }

        // Phone and Address extraction for ordering
        val bdPhoneRegex = Regex("(?:\\+?88)?01[3-9]\\d{8}")
        val phoneMatch = bdPhoneRegex.find(text)
        if (phoneMatch != null && draft["productId"] != null) {
            draft["phone"] = phoneMatch.value
            draft["stage"] = "AWAITING_ADDRESS"
            return Triple(
                "ORDER_DETAILS",
                "ধন্যবাদ! মোবাইল নম্বর পেয়েছি (${phoneMatch.value})। এবার অনুগ্রহ করে আপনার ডেলিভারি ঠিকানা (জেলা ও থানা সহ) লিখে দিন।",
                false
            )
        }

        // If in ordering stage and user sends address
        if (draft["stage"] == "AWAITING_ADDRESS" && text.length > 5) {
            draft["address"] = text
            draft["stage"] = "AWAITING_CONFIRMATION"

            val prodId = draft["productId"] as? String ?: "p-1"
            val prod = _products.value.find { it.id == prodId } ?: _products.value.first()
            val unitPrice = prod.discountPriceBdt ?: prod.priceBdt
            val isInside = text.lowercase().contains("dhaka") || text.contains("ঢাকা")
            val fee = if (isInside) 60.0 else 120.0
            val total = unitPrice + fee

            val summary = "📋 RYZORA অর্ডার সারাংশ:\n" +
                    "━━━━━━━━━━━━━━━━\n" +
                    "🛍️ পণ্য: ${prod.name}\n" +
                    "🎨 ভ্যারিয়েন্ট: ${draft["variant"] ?: "Standard"}\n" +
                    "💵 দাম: ৳$unitPrice\n" +
                    "🚚 ডেলিভারি ফি (${if (isInside) "Inside Dhaka" else "Outside Dhaka"}): ৳$fee\n" +
                    "💰 মোট প্রদেয়: ৳$total (Cash on Delivery)\n" +
                    "📱 মোবাইল: ${draft["phone"]}\n" +
                    "📍 ঠিকানা: $text\n" +
                    "━━━━━━━━━━━━━━━━\n" +
                    "অর্ডারটি চূড়ান্ত করতে 'CONFIRM' বা 'হ্যাঁ' লিখুন।"

            return Triple("ORDER_SUMMARY", summary, false)
        }

        // Find product mentioned
        val matchedProduct = _products.value.find { prod ->
            prod.name.lowercase().contains(lower) ||
                    prod.banglaName.contains(text) ||
                    lower.contains(prod.sku.lowercase()) ||
                    lower.contains(prod.category.lowercase()) ||
                    (lower.contains("earbud") && prod.name.contains("Earbuds", ignoreCase = true)) ||
                    (lower.contains("watch") && prod.name.contains("Watch", ignoreCase = true)) ||
                    (lower.contains("t-shirt") && prod.name.contains("Tee", ignoreCase = true)) ||
                    (lower.contains("tshirt") && prod.name.contains("Tee", ignoreCase = true)) ||
                    (lower.contains("polo") && prod.name.contains("Polo", ignoreCase = true)) ||
                    (lower.contains("bag") && prod.name.contains("Backpack", ignoreCase = true)) ||
                    (lower.contains("power") && prod.name.contains("Power Bank", ignoreCase = true))
        }

        if (matchedProduct != null) {
            val isOrdering = lower.contains("order") || lower.contains("কিনব") || lower.contains("নিতে") || lower.contains("চাই")

            if (isOrdering) {
                draft["productId"] = matchedProduct.id
                draft["productName"] = matchedProduct.name
                draft["variant"] = matchedProduct.availableColors.firstOrNull() ?: "Standard"
                draft["stage"] = "AWAITING_PHONE"

                return Triple(
                    "ORDER_START",
                    "🛍️ ${matchedProduct.name} অর্ডার করতে অনুগ্রহ করে আপনার ১১ ডিজিটের মোবাইল নম্বর (যেমন: 017XXXXXXXX) ও আপনার নাম জানান।",
                    false
                )
            }

            val priceFormatted = matchedProduct.discountPriceBdt?.let {
                "৳$it (অফার মূল্য, রেগুলার ৳${matchedProduct.priceBdt})"
            } ?: "৳${matchedProduct.priceBdt}"

            val colors = matchedProduct.availableColors.joinToString(", ")
            val sizes = matchedProduct.availableSizes.joinToString(", ")
            val stock = if (matchedProduct.stockQuantity > 0) "স্টকে আছে (${matchedProduct.stockQuantity} পিস)" else "স্টক আউট"

            return Triple(
                "PRICE_INQUIRY",
                "✨ ${matchedProduct.name}\n" +
                        "💵 মূল্য: $priceFormatted\n" +
                        "🎨 কালার: $colors\n" +
                        "📏 সাইজ: $sizes\n" +
                        "📊 প্রাপ্যতা: $stock\n\n" +
                        "অর্ডার করতে চাইলে 'Order' লিখুন অথবা আপনার ফোন নম্বর জানান।",
                false
            )
        }

        // Greeting fallback
        if (lower.startsWith("hi") || lower.startsWith("hello") || lower.startsWith("salam") || lower.contains("সালাম")) {
            return Triple(
                "GREETING",
                "আসসালামু আলাইকুম! RYZORA-তে আপনাকে স্বাগতম। \"Smart Choice. Better Life.\"\nআমাদের প্রিমিয়াম গ্যাজেট, ঘড়ি ও টি-শার্টের বিস্তারিত জানতে পণ্যের নাম লিখে মেসেজ করুন।",
                false
            )
        }

        return Triple(
            "FAQ",
            "ধন্যবাদ আপনার মেসেজের জন্য! RYZORA-র প্রিমিয়াম টি-শার্ট, পোলো, ওয়্যারলেস ইয়ারবাডস, অ্যামোলেড স্মার্ট ওয়াচ বা পাওয়ার ব্যাংকের দাম ও স্টক জানতে পণ্যের নাম জানান।",
            false
        )
    }

    private fun addWebhookLog(type: String, status: String, details: String) {
        val log = WebhookLog(
            id = "log_${System.currentTimeMillis()}",
            timestamp = currentTimeString(),
            type = type,
            status = status,
            details = details
        )
        _webhookLogs.update { listOf(log) + it.take(49) }
    }

    private fun currentTimeString(): String {
        return SimpleDateFormat("hh:mm a", Locale.US).format(Date())
    }

    companion object {
        val instance: RyzoraRepository by lazy { RyzoraRepository() }

        private fun getInitialProducts() = listOf(
            Product(
                id = "p-1",
                sku = "RYZ-PRO-01",
                name = "RYZORA Premium Pro Wireless Earbuds",
                banglaName = "রাইজোরা প্রিমিয়াম প্রো ওয়্যারলেস এয়ারবাডস",
                description = "Active Noise Cancellation, 36h Playtime, Deep Bass, IPX5 Waterproof.",
                category = "Electronics",
                priceBdt = 2450.0,
                discountPriceBdt = 1990.0,
                availableSizes = listOf("Standard"),
                availableColors = listOf("Matte Black", "Pearl White"),
                stockQuantity = 45,
                isActive = true
            ),
            Product(
                id = "p-2",
                sku = "RYZ-SW-02",
                name = "RYZORA Apex AMOLED Smart Watch",
                banglaName = "রাইজোরা অ্যাপেক্স অ্যামোলেড স্মার্ট ওয়াচ",
                description = "1.43 inch AMOLED Display, Bluetooth Calling, 100+ Sports Modes, Health Tracking.",
                category = "Electronics",
                priceBdt = 3800.0,
                discountPriceBdt = 3250.0,
                availableSizes = listOf("Free Size"),
                availableColors = listOf("Midnight Black", "Metallic Silver"),
                stockQuantity = 30,
                isActive = true
            ),
            Product(
                id = "p-3",
                sku = "RYZ-TEE-03",
                name = "RYZORA Heavyweight Cotton Graphic Tee",
                banglaName = "রাইজোরা হেভিওয়েট কটন টি-শার্ট",
                description = "220 GSM 100% Combed Compact Cotton, Pre-shrunk, Signature High-Density Print.",
                category = "Fashion",
                priceBdt = 950.0,
                discountPriceBdt = 790.0,
                availableSizes = listOf("M", "L", "XL", "XXL"),
                availableColors = listOf("Jet Black", "Off White", "Sage Green"),
                stockQuantity = 120,
                isActive = true
            ),
            Product(
                id = "p-4",
                sku = "RYZ-POLO-04",
                name = "RYZORA Classic Pique Knit Polo",
                banglaName = "রাইজোরা ক্লাসিক পোলো শার্ট",
                description = "Premium Mercerized Cotton Pique, Structured Collar, Regular Fit.",
                category = "Fashion",
                priceBdt = 1250.0,
                discountPriceBdt = 1050.0,
                availableSizes = listOf("M", "L", "XL"),
                availableColors = listOf("Navy Blue", "Maroon", "Olive Green"),
                stockQuantity = 85,
                isActive = true
            ),
            Product(
                id = "p-5",
                sku = "RYZ-BAG-05",
                name = "RYZORA Urban Commuter Waterproof Backpack",
                banglaName = "রাইজোরা আরবান ওয়াটারপ্রুফ ব্যাকপ্যাক",
                description = "Fits up to 15.6 inch laptop, Hidden Anti-theft Pocket, Ergonomic Shoulder Straps.",
                category = "Accessories",
                priceBdt = 2150.0,
                discountPriceBdt = 1750.0,
                availableSizes = listOf("20L"),
                availableColors = listOf("Obsidian Black", "Steel Grey"),
                stockQuantity = 25,
                isActive = true
            ),
            Product(
                id = "p-6",
                sku = "RYZ-PB-06",
                name = "RYZORA PowerCore 20000mAh 22.5W Fast Power Bank",
                banglaName = "রাইজোরা পাওয়ারকোর ২০০০০ এমএএইচ পাওয়ার ব্যাংক",
                description = "PD 3.0 & QC 4.0 Fast Charging, Digital Battery Indicator, Dual Type-C Output.",
                category = "Electronics",
                priceBdt = 1950.0,
                discountPriceBdt = 1590.0,
                availableSizes = listOf("Standard"),
                availableColors = listOf("Space Grey"),
                stockQuantity = 60,
                isActive = true
            )
        )

        private fun getInitialOrders() = listOf(
            Order(
                id = "ord_01",
                orderNumber = "RYZ-20261025-4819",
                customerName = "Tanvir Ahmed",
                customerPhone = "01712345678",
                deliveryAddress = "Flat 4B, House 12, Road 7, Dhanmondi, Dhaka",
                deliveryZone = "Inside Dhaka",
                deliveryFeeBdt = 60.0,
                subtotalBdt = 1990.0,
                totalBdt = 2050.0,
                status = OrderStatus.Confirmed,
                items = listOf(
                    OrderItem("RYZORA Premium Pro Wireless Earbuds", "Matte Black", 1, 1990.0, 1990.0)
                ),
                createdAt = "Today, 11:30 AM"
            ),
            Order(
                id = "ord_02",
                orderNumber = "RYZ-20261025-9204",
                customerName = "Sadia Rahman",
                customerPhone = "01899887766",
                deliveryAddress = "GEC Circle, Nasirabad, Chittagong",
                deliveryZone = "Outside Dhaka",
                deliveryFeeBdt = 120.0,
                subtotalBdt = 3250.0,
                totalBdt = 3370.0,
                status = OrderStatus.Processing,
                items = listOf(
                    OrderItem("RYZORA Apex AMOLED Smart Watch", "Metallic Silver", 1, 3250.0, 3250.0)
                ),
                createdAt = "Today, 10:15 AM"
            ),
            Order(
                id = "ord_03",
                orderNumber = "RYZ-20261024-3112",
                customerName = "Imtiaz Hossain",
                customerPhone = "01911223344",
                deliveryAddress = "Sector 3, Uttara, Dhaka",
                deliveryZone = "Inside Dhaka",
                deliveryFeeBdt = 60.0,
                subtotalBdt = 1580.0,
                totalBdt = 1640.0,
                status = OrderStatus.Shipped,
                items = listOf(
                    OrderItem("RYZORA Heavyweight Cotton Graphic Tee", "Jet Black (L)", 2, 790.0, 1580.0)
                ),
                createdAt = "Yesterday, 04:45 PM"
            )
        )

        private fun getInitialConversations() = listOf(
            Conversation(
                id = "conv_01",
                customerPsid = "fb_psid_847291039",
                customerName = "Tanvir Ahmed",
                isAiActive = true,
                requiresHumanTakeover = false,
                currentIntent = "ORDER_CONFIRM",
                lastMessage = "RYZORA AI: অর্ডারটি সফলভাবে সম্পন্ন হয়েছে।",
                lastMessageTime = "11:31 AM",
                unreadCount = 0
            ),
            Conversation(
                id = "conv_02",
                customerPsid = "fb_psid_928374615",
                customerName = "Nafis Mahmud",
                isAiActive = false,
                requiresHumanTakeover = true,
                currentIntent = "HUMAN_HANDOFF",
                lastMessage = "Customer: ভাই প্রোডাক্টে ডিফেক্ট পেয়েছি, ম্যানেজার এর সাথে কথা বলতে চাই",
                lastMessageTime = "12:15 PM",
                unreadCount = 2
            ),
            Conversation(
                id = "conv_03",
                customerPsid = "fb_psid_102938475",
                customerName = "Anika Tabassum",
                isAiActive = true,
                requiresHumanTakeover = false,
                currentIntent = "PRICE_INQUIRY",
                lastMessage = "RYZORA AI: স্মার্ট ওয়াচের অফার মূল্য ৳৩২৫০।",
                lastMessageTime = "01:04 PM",
                unreadCount = 0
            )
        )

        private fun getInitialMessages(): Map<String, List<ChatMessage>> = mapOf(
            "conv_01" to listOf(
                ChatMessage("m1", "conv_01", SenderType.CUSTOMER, "Earbuds er dam koto?", "11:28 AM", "PRICE_INQUIRY"),
                ChatMessage("m2", "conv_01", SenderType.AI_AGENT, "RYZORA Premium Pro Wireless Earbuds-এর অফার মূল্য ৳১৯৯০ (রেগুলার ৳২৪৫০)। স্টকে আছে। অর্ডার করতে চান?", "11:28 AM", "PRICE_INQUIRY"),
                ChatMessage("m3", "conv_01", SenderType.CUSTOMER, "Ha order korbo, 01712345678, Tanvir Ahmed, Dhanmondi 7", "11:29 AM", "ORDER_DETAILS"),
                ChatMessage("m4", "conv_01", SenderType.AI_AGENT, "📋 অর্ডার সারাংশ: পণ্য ৳১৯৯০ + ডেলিভারি ফি ৳৬০ = মোট ৳২০৫০। কনফার্ম করতে 'CONFIRM' লিখুন।", "11:29 AM", "ORDER_SUMMARY"),
                ChatMessage("m5", "conv_01", SenderType.CUSTOMER, "CONFIRM", "11:30 AM", "ORDER_CONFIRM"),
                ChatMessage("m6", "conv_01", SenderType.AI_AGENT, "🎉 অর্ডার কনফার্মড! অর্ডার নম্বর RYZ-20261025-4819। ডেলিভারির সময় আমাদের টিম কল করবে।", "11:30 AM", "ORDER_CONFIRM")
            ),
            "conv_02" to listOf(
                ChatMessage("m7", "conv_02", SenderType.CUSTOMER, "ভাই প্রোডাক্টে ডিফেক্ট পেয়েছি, ম্যানেজার এর সাথে কথা বলতে চাই", "12:15 PM", "HUMAN_HANDOFF"),
                ChatMessage("m8", "conv_02", SenderType.AI_AGENT, "সম্মানিত গ্রাহক, আপনার অনুরোধটি আমাদের সিনিয়র কাস্টমার সাপোর্ট টিমের কাছে পাঠানো হয়েছে। খুব শীঘ্রই একজন প্রতিনিধি আপনার সাথে যুক্ত হবেন।", "12:15 PM", "HUMAN_HANDOFF")
            ),
            "conv_03" to listOf(
                ChatMessage("m9", "conv_03", SenderType.CUSTOMER, "Smart watch er display ki AMOLED?", "01:02 PM", "PRODUCT_INFO"),
                ChatMessage("m10", "conv_03", SenderType.AI_AGENT, "জি! RYZORA Apex Smart Watch-এ ১.৪৩ ইঞ্চি ক্রিস্টাল ক্লিয়ার AMOLED ডিসপ্লে, ব্লুটুথ কলিং এবং ১০০+ স্পোর্টস মোড রয়েছে। অফার মূল্য ৳৩২৫০।", "01:02 PM", "PRODUCT_INFO")
            )
        )

        private fun getInitialLogs() = listOf(
            WebhookLog("l1", "01:05 PM", "AI_REPLY_SENT", "SUCCESS", "Intent: PRICE_INQUIRY | Recipient: fb_psid_102938475"),
            WebhookLog("l2", "12:15 PM", "ESCALATION_ALERT", "WARNING", "Human Handoff triggered for conv_02"),
            WebhookLog("l3", "11:30 AM", "ORDER_CONFIRMED", "SUCCESS", "Order RYZ-20261025-4819 created in Supabase"),
            WebhookLog("l4", "10:00 AM", "VERIFICATION", "SUCCESS", "Meta Webhook challenge verified with hub.verify_token")
        )
    }
}
