package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AdBadgeColor
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurfaceElevated
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted

@Composable
fun AdBannerOverlay(
    isAdPlaying: Boolean,
    adCountdownSeconds: Int,
    adTitle: String,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = isAdPlaying,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.88f))
                .padding(24.dp)
                .testTag("ad_banner_overlay"),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .border(2.dp, Brush.linearGradient(listOf(AdBadgeColor, GoldPrimary)), RoundedCornerShape(24.dp)),
                color = DarkSurfaceElevated,
                tonalElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header Tag
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(AdBadgeColor.copy(alpha = 0.2f))
                            .border(1.dp, AdBadgeColor, RoundedCornerShape(50))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = AdBadgeColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "AUDIO ADVERTISEMENT",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                ),
                                color = AdBadgeColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Circular Countdown Timer
                    Box(
                        modifier = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progress = (adCountdownSeconds.toFloat() / 30f).coerceIn(0f, 1f)
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(110.dp),
                            color = AdBadgeColor,
                            trackColor = Color.DarkGray,
                            strokeWidth = 6.dp
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${adCountdownSeconds}s",
                                style = MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            )
                            Text(
                                text = "REMAINING",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 9.sp,
                                    letterSpacing = 1.sp,
                                    color = TextMuted
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Ad Title
                    Text(
                        text = adTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        ),
                        color = Color.White
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Auto-fetched from Drive Ads Repository:\n1-x16HnYzOpzHIvefQj8txB21fC-YJkPf",
                        style = MaterialTheme.typography.labelSmall.copy(
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = GoldLight
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "30-second audio break scheduled after every 4 songs. Your next music track will resume automatically in $adCountdownSeconds seconds.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        ),
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Animated Visualizer Bars
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(9) { index ->
                            AnimatedEqualizerBar(index = index)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnimatedEqualizerBar(index: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "eq_bar_$index")
    val duration = 400 + (index * 80) % 500
    val heightScale by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = duration, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bar_height"
    )

    Box(
        modifier = Modifier
            .width(4.dp)
            .height((24.dp * heightScale).coerceAtLeast(4.dp))
            .clip(RoundedCornerShape(2.dp))
            .background(GoldPrimary)
    )
}
