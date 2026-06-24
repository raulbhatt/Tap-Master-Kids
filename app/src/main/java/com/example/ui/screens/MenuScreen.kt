package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.Screen
import com.example.ui.viewmodel.GameViewModel

@Composable
fun MenuScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val selectedTheme by viewModel.selectedTheme.collectAsState()

    // Bouncing animation for title
    val infiniteTransition = rememberInfiniteTransition(label = "title_bounce")
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )
    val titleRotation by infiniteTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rotation"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
    ) {
        // Main Contents
        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // Playful Title Banner
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .scale(titleScale)
                    .rotate(titleRotation)
            ) {
                Text(
                    text = "🎈 TAP MASTER 🎈",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFFFEB3B),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.headlineLarge,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
                Text(
                    text = "KIDS ACADEMY",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
            }

            // Central Game Mode Grid (2x2)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mode 1: Classic Pop Card (Soft pastel orange yellow)
                    GameModeCard(
                        title = "Classic Pop! 🎈",
                        description = "Fun endless bubble popping!",
                        cardColor = Color(0xFFFFB74D),
                        icon = "🎈",
                        onClick = { viewModel.startGame("classic") },
                        testTag = "mode_classic",
                        modifier = Modifier.weight(1f)
                    )

                    // Mode 2: Match & Learn (Playful blue purple)
                    GameModeCard(
                        title = "Match & Learn! 🎓",
                        description = "Smart shapes & color puzzle!",
                        cardColor = Color(0xFF64B5F6),
                        icon = "⭐",
                        onClick = { viewModel.startGame("color_match") },
                        testTag = "mode_color_match",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Mode 4: Reflex Challenge (Exciting Coral Red)
                    GameModeCard(
                        title = "Reflex Challenge! ⚡",
                        description = "Pop fast geometric shapes!",
                        cardColor = Color(0xFFFF7043),
                        icon = "⚡",
                        onClick = { viewModel.startGame("reflex") },
                        testTag = "mode_reflex",
                        modifier = Modifier.weight(1f)
                    )

                    // Mode 3: Space Shooter! (Fabulous Purple)
                    GameModeCard(
                        title = "Space Shooter! 🚀",
                        description = "Rescue friendly cute aliens!",
                        cardColor = Color(0xFF9575CD),
                        icon = "🚀",
                        onClick = { viewModel.setScreen(Screen.TROPHY) },
                        testTag = "mode_trophy",
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Bottom Theme Selection Block (Playgrounds!)
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = "CHOOSE YOUR WORLD 🗺️",
                    color = Color.White.copy(alpha = 0.9f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    PlayfulWorldCard(
                        themeName = "Sunny Meadow",
                        emoji = "☀️",
                        isSelected = selectedTheme == "meadow",
                        activeColor = Color(0xFF4CAF50),
                        cardBgColor = Color(0xFFE8F5E9),
                        onClick = { viewModel.selectTheme("meadow") }
                    )
                    PlayfulWorldCard(
                        themeName = "Space Cosmos",
                        emoji = "🚀",
                        isSelected = selectedTheme == "space",
                        activeColor = Color(0xFF673AB7),
                        cardBgColor = Color(0xFFEDE7F6),
                        onClick = { viewModel.selectTheme("space") }
                    )
                    PlayfulWorldCard(
                        themeName = "Deep Ocean",
                        emoji = "🐳",
                        isSelected = selectedTheme == "underwater",
                        activeColor = Color(0xFF00ACC1),
                        cardBgColor = Color(0xFFE0F7FA),
                        onClick = { viewModel.selectTheme("underwater") }
                    )
                    PlayfulWorldCard(
                        themeName = "Candy Land",
                        emoji = "🍭",
                        isSelected = selectedTheme == "candy",
                        activeColor = Color(0xFFE91E63),
                        cardBgColor = Color(0xFFFCE4EC),
                        onClick = { viewModel.selectTheme("candy") }
                    )
                    PlayfulWorldCard(
                        themeName = "Dino Volcano",
                        emoji = "🦖",
                        isSelected = selectedTheme == "volcano",
                        activeColor = Color(0xFFD84315),
                        cardBgColor = Color(0xFFFBE9E7),
                        onClick = { viewModel.selectTheme("volcano") }
                    )
                    PlayfulWorldCard(
                        themeName = "Toy Carnival",
                        emoji = "🎡",
                        isSelected = selectedTheme == "carnival",
                        activeColor = Color(0xFFFFB300),
                        cardBgColor = Color(0xFFFFF8E1),
                        onClick = { viewModel.selectTheme("carnival") }
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "Scroll right to see more worlds",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "➡️",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
fun GameModeCard(
    title: String,
    description: String,
    cardColor: Color,
    icon: String,
    onClick: () -> Unit,
    testTag: String,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
        modifier = modifier
            .testTag(testTag)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(46.dp)
                    .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = icon,
                    fontSize = 26.sp
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    maxLines = 1
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f),
                    textAlign = TextAlign.Center,
                    lineHeight = 14.sp,
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
fun PlayfulWorldCard(
    themeName: String,
    emoji: String,
    isSelected: Boolean,
    activeColor: Color,
    cardBgColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "card_scale"
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) activeColor else cardBgColor.copy(alpha = 0.85f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isSelected) 3.dp else 1.5.dp,
            color = if (isSelected) Color.White else activeColor.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        modifier = modifier
            .width(92.dp)
            .height(92.dp)
            .scale(scale)
            .clickable { onClick() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(36.dp)
                    .background(Color.White.copy(alpha = 0.35f), CircleShape)
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = themeName,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                color = if (isSelected) Color.White else Color(0xFF333333),
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 11.sp
            )
        }
    }
}
