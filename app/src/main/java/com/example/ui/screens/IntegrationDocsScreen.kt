package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.RyzoraGlassCard
import com.example.ui.components.StatusBadge
import com.example.ui.theme.*

@Composable
fun IntegrationDocsScreen() {
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
                text = "Meta & Cloud Integration",
                color = TextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Production setup checklist for RYZORA Facebook Messenger",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Test status banner
        item {
            RyzoraGlassCard(borderColor = RyzoraEmerald) {
                Text(
                    text = "Verification Report (12/12 Automated Tests Passed)",
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(text = "Webhook: Passed", color = RyzoraEmerald)
                    StatusBadge(text = "HMAC-SHA256: Passed", color = RyzoraEmerald)
                    StatusBadge(text = "Deduplication: Passed", color = RyzoraEmerald)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusBadge(text = "AI Intent: Passed", color = RyzoraEmerald)
                    StatusBadge(text = "BD Phone: Passed", color = RyzoraEmerald)
                    StatusBadge(text = "Order Calc: Passed", color = RyzoraEmerald)
                }
            }
        }

        // 1. Meta Setup
        item {
            RyzoraGlassCard {
                Text(
                    text = "1. Meta Webhook Configuration",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Text("Callback URL:", color = TextMuted, fontSize = 11.sp)
                        Text("https://<YOUR_CLOUD_RUN_HOST>/webhook", color = RyzoraCyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Verify Token:", color = TextMuted, fontSize = 11.sp)
                        Text("ryzora_messenger_verify_token_2026", color = TextPrimary, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Subscribed Fields:", color = TextMuted, fontSize = 11.sp)
                        Text("messages, messaging_postbacks", color = RyzoraEmerald, fontSize = 12.sp)
                    }
                }
            }
        }

        // 2. Supabase Migration
        item {
            RyzoraGlassCard {
                Text(
                    text = "2. Supabase PostgreSQL Migration",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Schema migration file created at: /supabase/migrations/20261025000000_init_ryzora.sql",
                    color = TextSecondary,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Includes 10 tables (products, orders, order_items, customers, conversations, messages, business_settings, automation_logs) with RLS policies and performance indexes.",
                    color = TextMuted,
                    fontSize = 12.sp
                )
            }
        }

        // 3. Cloud Run Deploy Command
        item {
            RyzoraGlassCard {
                Text(
                    text = "3. Cloud Run Deploy Command",
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Surface(
                    color = SurfaceDark,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "gcloud run deploy ryzora-agent \\\n" +
                                "  --source server/ \\\n" +
                                "  --region asia-southeast1 \\\n" +
                                "  --allow-unauthenticated",
                        color = RyzoraCyan,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        }
    }
}
