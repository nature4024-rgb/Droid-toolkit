package com.example.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay

enum class AdNetwork(val displayName: String, val brandColor: Color, val logoText: String) {
    ADMOB("Google AdMob", Color(0xFF4285F4), "G"),
    FACEBOOK("Facebook Audience Network", Color(0xFF1877F2), "f"),
    APPLOVIN("AppLovin MAX", Color(0xFF0A1128), "A"),
    UNITY("Unity Ads", Color(0xFF2196F3), "U")
}

data class MockAd(
    val title: String,
    val body: String,
    val callToAction: String,
    val rating: Float,
    val iconBgColor: Color,
    val network: AdNetwork
)

object MockAdAdapter {
    private val ADS_POOL = listOf(
        MockAd(
            title = "Fast & Secure VPN - DroidVPN",
            body = "Protect your mobile presence with military-grade encryption. One tap to total privacy.",
            callToAction = "Install Now",
            rating = 4.8f,
            iconBgColor = Color(0xFFE3F2FD),
            network = AdNetwork.ADMOB
        ),
        MockAd(
            title = "Play Candy Blast Premium!",
            body = "The ultimate matching game with high definition graphics and local high scores. Offline-ready!",
            callToAction = "Play Free",
            rating = 4.7f,
            iconBgColor = Color(0xFFFFF3E0),
            network = AdNetwork.FACEBOOK
        ),
        MockAd(
            title = "Cloud Cleaner - Optimize Storage",
            body = "Remove duplicate files, clean residual cache blocks, and speed up RAM memory in seconds.",
            callToAction = "Free Clean",
            rating = 4.6f,
            iconBgColor = Color(0xFFE8F5E9),
            network = AdNetwork.APPLOVIN
        ),
        MockAd(
            title = "Legend of Galaxy - Action RPG",
            body = "Join millions of players in real-time universe battle. Stunning 3D effects powered by Vulkan.",
            callToAction = "Download APK",
            rating = 4.9f,
            iconBgColor = Color(0xFFEDE7F6),
            network = AdNetwork.UNITY
        )
    )

    fun getAdForNetwork(network: AdNetwork): MockAd {
        return ADS_POOL.firstOrNull { it.network == network } ?: ADS_POOL[0]
    }
}

/**
 * Top Banner Ad Component (Placed at the top of each page)
 * Displays a sleek, authentic 52dp mobile ad banner (Google AdMob / Facebook / AppLovin)
 */
@Composable
fun MockBannerAd(
    modifier: Modifier = Modifier,
    initialNetwork: AdNetwork = AdNetwork.ADMOB
) {
    var currentNetwork by remember { mutableStateOf(initialNetwork) }
    val activeAd = remember(currentNetwork) { MockAdAdapter.getAdForNetwork(currentNetwork) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .shadow(1.dp, shape = RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Small "Ad / اشتہار" tag
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFFFF0D4))
                    .border(0.5.dp, Color(0xFFFFB300), RoundedCornerShape(4.dp))
                    .padding(horizontal = 5.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "AD / اشتہار",
                    color = Color(0xFFBF5000),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Black
                )
            }

            // Small Icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(activeAd.iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when(activeAd.network) {
                        AdNetwork.ADMOB -> Icons.Default.Security
                        AdNetwork.FACEBOOK -> Icons.Default.PlayArrow
                        AdNetwork.APPLOVIN -> Icons.Default.CleaningServices
                        AdNetwork.UNITY -> Icons.Default.Casino
                    },
                    contentDescription = "Ad Banner Icon",
                    tint = activeAd.network.brandColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Title & Short Tagline
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = activeAd.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color(0xFFFFB300),
                        modifier = Modifier.size(10.dp)
                    )
                    Text(
                        text = "${activeAd.rating} • ${activeAd.network.displayName}",
                        fontSize = 9.sp,
                        color = TextSecondary,
                        maxLines = 1
                    )
                }
            }

            // Small Action Button
            Button(
                onClick = { /* simulated ad interaction */ },
                shape = RoundedCornerShape(8.dp),
                colors = ButtonDefaults.buttonColors(containerColor = activeAd.network.brandColor),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
            ) {
                Text(
                    text = activeAd.callToAction,
                    color = PureWhite,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

/**
 * Bottom Native Ad Component (Placed at the bottom of each page)
 * Full rich Native Ad card featuring mediation network selection
 */
@Composable
fun MockNativeAdBanner(
    modifier: Modifier = Modifier,
    initialNetwork: AdNetwork = AdNetwork.FACEBOOK
) {
    var currentNetwork by remember { mutableStateOf(initialNetwork) }
    var isLoading by remember { mutableStateOf(false) }
    var activeAd by remember { mutableStateOf(MockAdAdapter.getAdForNetwork(initialNetwork)) }

    // Simulate network latency when switching networks (Facebook, AdMob etc)
    LaunchedEffect(currentNetwork) {
        isLoading = true
        delay(600) // simulated loading speed
        activeAd = MockAdAdapter.getAdForNetwork(currentNetwork)
        isLoading = false
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = BorderStroke(1.dp, CardBorderColor)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            // Header Row: Sponsored & Network Selector Adapter
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(0xFFFFF0D4))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "SPONSORED / اشتہار",
                            color = Color(0xFFBF5000),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Mediation Adapter Active",
                        fontSize = 10.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Adapter Network Chip Trigger
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AdNetwork.entries.forEach { network ->
                        val isSelected = currentNetwork == network
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) network.brandColor else OffWhite)
                                .border(1.dp, if (isSelected) Color.Transparent else CardBorderColor, CircleShape)
                                .clickable { currentNetwork = network },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = network.logoText,
                                color = if (isSelected) PureWhite else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            if (isLoading) {
                // Shimmer/Loading state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = currentNetwork.brandColor
                        )
                        Text(
                            text = "Loading ${currentNetwork.displayName} Ad...",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                // Ad Content layout (Full Native Banner style)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Ad Icon
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(activeAd.iconBgColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = when(activeAd.network) {
                                AdNetwork.ADMOB -> Icons.Default.Security
                                AdNetwork.FACEBOOK -> Icons.Default.PlayArrow
                                AdNetwork.APPLOVIN -> Icons.Default.CleaningServices
                                AdNetwork.UNITY -> Icons.Default.Casino
                            },
                            contentDescription = "Ad Icon",
                            tint = activeAd.network.brandColor,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    // Text parameters
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = activeAd.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = activeAd.body,
                            fontSize = 10.sp,
                            color = TextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 13.sp
                        )
                        Spacer(Modifier.height(4.dp))
                        // Stars Rating & Ad Source Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(10.dp)
                            )
                            Text(
                                text = "${activeAd.rating} • via ${activeAd.network.displayName}",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Elegant CTA Button
                    Button(
                        onClick = { /* simulated action click */ },
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = activeAd.network.brandColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text(
                            text = activeAd.callToAction,
                            color = PureWhite,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
