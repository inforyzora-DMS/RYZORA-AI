package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import com.example.model.ChatMessage
import com.example.model.Conversation
import com.example.model.SenderType
import com.example.ui.components.RyzoraGlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun ConversationsScreen(
    repository: RyzoraRepository
) {
    val conversations by repository.conversations.collectAsState()
    val allMessages by repository.chatMessages.collectAsState()

    var selectedConversationId by remember { mutableStateOf<String?>(null) }

    val activeConversation = conversations.find { it.id == selectedConversationId }

    if (activeConversation != null) {
        BackHandler {
            selectedConversationId = null
        }

        ConversationDetailView(
            conversation = activeConversation,
            messages = allMessages[activeConversation.id] ?: emptyList(),
            onBack = { selectedConversationId = null },
            onToggleAi = { repository.toggleConversationAi(activeConversation.id, it) },
            onSendReply = { repository.sendAdminReply(activeConversation.id, it) }
        )
    } else {
        ConversationListView(
            conversations = conversations,
            onSelectConversation = { selectedConversationId = it }
        )
    }
}

@Composable
private fun ConversationListView(
    conversations: List<Conversation>,
    onSelectConversation: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var filterOnlyEscalated by remember { mutableStateOf(false) }

    val filtered = conversations.filter {
        val matchesQuery = it.customerName.contains(searchQuery, ignoreCase = true) ||
                it.lastMessage.contains(searchQuery, ignoreCase = true)
        val matchesFilter = if (filterOnlyEscalated) it.requiresHumanTakeover else true
        matchesQuery && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Messenger Inbox",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Live customer inquiries from @ryzorabd",
            color = TextSecondary,
            fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by customer or message...", color = TextMuted) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted)
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("inbox_search_field"),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = RyzoraCyan,
                unfocusedBorderColor = SurfaceBorder,
                focusedContainerColor = SurfaceDark,
                unfocusedContainerColor = SurfaceDark,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            ),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Filter chips
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !filterOnlyEscalated,
                onClick = { filterOnlyEscalated = false },
                label = { Text("All Chats (${conversations.size})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RyzoraCyan.copy(alpha = 0.2f),
                    selectedLabelColor = RyzoraCyan
                )
            )
            FilterChip(
                selected = filterOnlyEscalated,
                onClick = { filterOnlyEscalated = true },
                label = { Text("Escalated to Human") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = RyzoraAmber.copy(alpha = 0.2f),
                    selectedLabelColor = RyzoraAmber
                )
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 96.dp)
        ) {
            items(filtered) { conv ->
                RyzoraGlassCard(
                    modifier = Modifier.clickable { onSelectConversation(conv.id) },
                    borderColor = if (conv.requiresHumanTakeover) RyzoraAmber else SurfaceBorder
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(if (conv.requiresHumanTakeover) RyzoraAmber.copy(alpha = 0.2f) else RyzoraCyan.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conv.customerName.take(1).uppercase(),
                                color = if (conv.requiresHumanTakeover) RyzoraAmber else RyzoraCyan,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = conv.customerName,
                                    color = TextPrimary,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = conv.lastMessageTime,
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = conv.lastMessage,
                                color = TextSecondary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                if (conv.requiresHumanTakeover) {
                                    StatusBadge(text = "Human Help Needed", color = RyzoraAmber)
                                } else {
                                    StatusBadge(text = "AI Replying", color = RyzoraEmerald)
                                }

                                StatusBadge(text = conv.currentIntent, color = RyzoraCyan)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationDetailView(
    conversation: Conversation,
    messages: List<ChatMessage>,
    onBack: () -> Unit,
    onToggleAi: (Boolean) -> Unit,
    onSendReply: (String) -> Unit
) {
    var replyInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
    ) {
        // Top App Bar for Chat
        Surface(
            color = SurfaceDark,
            modifier = Modifier.fillMaxWidth(),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = conversation.customerName,
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PSID: ${conversation.customerPsid} • @ryzorabd",
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                }

                // Takeover switch
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (conversation.isAiActive) "AI ACTIVE" else "HUMAN TAKEOVER",
                        color = if (conversation.isAiActive) RyzoraEmerald else RyzoraAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Switch(
                        checked = conversation.isAiActive,
                        onCheckedChange = onToggleAi,
                        modifier = Modifier.testTag("conversation_ai_switch"),
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = RyzoraEmerald,
                            checkedTrackColor = RyzoraEmerald.copy(alpha = 0.2f),
                            uncheckedThumbColor = RyzoraAmber,
                            uncheckedTrackColor = SurfaceCard
                        )
                    )
                }
            }
        }

        // Messages list
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(messages) { msg ->
                MessageBubble(message = msg)
            }
        }

        // Reply input bar
        Surface(
            color = SurfaceDark,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 60.dp),
            border = BorderStroke(1.dp, SurfaceBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = replyInput,
                    onValueChange = { replyInput = it },
                    placeholder = {
                        Text(
                            text = if (conversation.isAiActive) "Send message as human admin..." else "Type reply (AI paused)...",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("admin_reply_field"),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RyzoraCyan,
                        unfocusedBorderColor = SurfaceBorder,
                        focusedContainerColor = SurfaceCard,
                        unfocusedContainerColor = SurfaceCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = {
                        if (replyInput.isNotBlank()) {
                            onSendReply(replyInput.trim())
                            replyInput = ""
                        }
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(RyzoraCyan)
                        .testTag("send_reply_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Send",
                        tint = Color.Black,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isCustomer = message.senderType == SenderType.CUSTOMER
    val isAi = message.senderType == SenderType.AI_AGENT

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isCustomer) Alignment.Start else Alignment.End
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 2.dp)
        ) {
            if (isAi) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = RyzoraCyan,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("RYZORA AI", color = RyzoraCyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else if (!isCustomer) {
                Text("Human Admin", color = RyzoraEmerald, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                Text("Customer", color = TextMuted, fontSize = 11.sp)
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(message.timestamp, color = TextMuted, fontSize = 10.sp)
        }

        Surface(
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isCustomer) 2.dp else 14.dp,
                bottomEnd = if (isCustomer) 14.dp else 2.dp
            ),
            color = when {
                isCustomer -> SurfaceCard
                isAi -> Color(0xFF0C2A38)
                else -> Color(0xFF0F392B)
            },
            border = BorderStroke(
                1.dp,
                when {
                    isCustomer -> SurfaceBorder
                    isAi -> RyzoraCyan.copy(alpha = 0.4f)
                    else -> RyzoraEmerald.copy(alpha = 0.4f)
                }
            ),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = message.text,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                if (message.detectedIntent != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Intent: ${message.detectedIntent}",
                        color = RyzoraCyan.copy(alpha = 0.7f),
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
