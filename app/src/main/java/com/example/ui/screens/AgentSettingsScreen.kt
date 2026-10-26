package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import com.example.ui.components.RyzoraGlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun AgentSettingsScreen(
    repository: RyzoraRepository
) {
    val config by repository.businessConfig.collectAsState()

    var insideDhakaFee by remember { mutableStateOf(config.insideDhakaFee.toInt().toString()) }
    var outsideDhakaFee by remember { mutableStateOf(config.outsideDhakaFee.toInt().toString()) }
    var freeThreshold by remember { mutableStateOf(config.freeDeliveryThreshold.toInt().toString()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBlack)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 96.dp)
    ) {
        item {
            Text(
                text = "AI Agent & Store Settings",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Persona, delivery policies, and safety rules for @ryzorabd",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // 1. Emergency Kill Switch Card
        item {
            RyzoraGlassCard(
                borderColor = if (config.isGlobalAiAutoReplyEnabled) RyzoraEmerald else RyzoraRose
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Global AI Auto-Reply Switch",
                            color = TextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (config.isGlobalAiAutoReplyEnabled)
                                "AI is actively answering customers 24/7 on Facebook Messenger."
                            else
                                "AI is PAUSED. Messages will queue for manual human reply.",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    Switch(
                        checked = config.isGlobalAiAutoReplyEnabled,
                        onCheckedChange = { repository.toggleGlobalAi(it) },
                        modifier = Modifier.testTag("settings_ai_killswitch"),
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

        // 2. Business Profile Card
        item {
            RyzoraGlassCard {
                Text(
                    text = "Business Identity (Verified)",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                InfoRow("Brand Name", config.brandName)
                InfoRow("Tagline", config.tagline)
                InfoRow("Website", config.website)
                InfoRow("Facebook Page", config.facebookPage)
                InfoRow("Meta Page ID", config.facebookPageId)
                InfoRow("Meta App ID", config.metaAppId)
                InfoRow("Location", "Bangladesh (Dhaka, Chittagong, Sylhet, etc.)")
                InfoRow("Currency", "BDT (৳)")
            }
        }

        // 3. Bangladesh Delivery Policies Card
        item {
            RyzoraGlassCard {
                Text(
                    text = "Shipping Rates & Delivery Thresholds",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = insideDhakaFee,
                        onValueChange = { insideDhakaFee = it },
                        label = { Text("Inside Dhaka (৳)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RyzoraCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = outsideDhakaFee,
                        onValueChange = { outsideDhakaFee = it },
                        label = { Text("Outside Dhaka (৳)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = RyzoraCyan,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = freeThreshold,
                    onValueChange = { freeThreshold = it },
                    label = { Text("Free Delivery for Orders Over (৳)") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RyzoraCyan,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }
        }

        // 4. AI Persona & Safety Rules
        item {
            RyzoraGlassCard {
                Text(
                    text = "AI Safety & Escalation Rules",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "• Prompt Injection Defense: Enforced (Rejects jailbreaks and fake free-product claims).",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Price Hallucination Guard: Enabled (Strict catalog lookups only).",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Escalation Keywords: refund, complaint, manager, agent, scam, broken, অভিযোগ, রিফান্ড, কথা বলতে চাই.",
                    color = RyzoraAmber,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "• Order Confirmation: Mandates customer confirmation before order creation.",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(value, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}
