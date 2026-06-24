package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.models.Screen
import com.example.ui.viewmodel.GameViewModel

@Composable
fun GameOverScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val finalScore by viewModel.lastFinishedScore.collectAsState()
    val finalStars by viewModel.lastFinishedStars.collectAsState()
    val maxStreak by viewModel.lastFinishedMaxStreak.collectAsState()
    val gameMode by viewModel.selectedMode.collectAsState()
    val scoreHistory by viewModel.scoreHistory.collectAsState()

    // Find if this is a historical personal best!
    val isNewHighScore = remember(finalScore, scoreHistory) {
        val pastScores = scoreHistory.drop(1) // exclude current newly inserted score
        if (pastScores.isEmpty()) {
            finalScore > 0 // first score is always high score
        } else {
            val maxPast = pastScores.maxOf { it.score }
            finalScore > maxPast
        }
    }

    // Stars pop-up scaling animation
    val infiniteTransition = rememberInfiniteTransition(label = "star_wiggle")
    val starWiggleScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // 1. Praise Header Box
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "GAME OVER!",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.9f)
            )

            val praiseText = when (finalStars) {
                3 -> "🏆 TAP MASTER SUPREME! 🏆"
                2 -> "🌟 INCREDIBLE POPPING! 🌟"
                else -> "🎈 LOVELY PLAYING! 🎈"
            }
            Text(
                text = praiseText,
                fontSize = 26.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFEB3B),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // 2. High Score Celebration card
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f)
        ) {
            if (isNewHighScore) {
                Box(
                    modifier = Modifier
                        .scale(starWiggleScale)
                        .background(Color(0xFFFFD54F), RoundedCornerShape(16.dp))
                        .padding(horizontal = 24.dp, vertical = 6.dp)
                        .shadow(2.dp, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "🥇 NEW HIGH SCORE! 🥇",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF333333)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Big star ratings (1, 2, or 3 stars drawn with cute emojis)
            Row(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .scale(starWiggleScale),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (s in 1..3) {
                    val isActive = s <= finalStars
                    Text(
                        text = if (isActive) "⭐" else "⚫",
                        fontSize = if (isActive) 56.sp else 32.sp,
                        modifier = Modifier.alpha(if (isActive) 1f else 0.35f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Score details bubble
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.92f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "TOTAL POINTS",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF757575)
                    )
                    Text(
                        text = "$finalScore",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF4CAF50)
                    )

                    val starsDescription = when (finalStars) {
                        3 -> "Splendid! Real 3-Star Mastery! ⭐⭐⭐"
                        2 -> "Terrific! Almost perfect tapping! ⭐⭐"
                        else -> "Great try! Keep popping to reach 3 stars! ⭐"
                    }
                    Text(
                        text = starsDescription,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF424242),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Combo Streak Sticker/Badge Award Card
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        maxStreak >= 18 -> Color(0xFFFFF9C4) // buttery gold
                        maxStreak >= 12 -> Color(0xFFE0F7FA) // lightning ice-blue
                        maxStreak >= 7 -> Color(0xFFFFE0B2) // cozy fire-orange
                        else -> Color.White.copy(alpha = 0.85f)
                    }
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
                modifier = Modifier
                    .widthIn(max = 300.dp)
                    .fillMaxWidth()
                    .testTag("combo_badge_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Large Animated Badge circle container
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(
                                color = when {
                                    maxStreak >= 18 -> Color(0xFFFFD600)
                                    maxStreak >= 12 -> Color(0xFF00E5FF)
                                    maxStreak >= 7 -> Color(0xFFFF6D00)
                                    else -> Color(0xFFCFD8DC)
                                },
                                shape = RoundedCornerShape(16.dp)
                            )
                            .shadow(2.dp, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = when {
                                maxStreak >= 18 -> "👑"
                                maxStreak >= 12 -> "⚡"
                                maxStreak >= 7 -> "🔥"
                                else -> "🎯"
                            },
                            fontSize = 32.sp
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = when {
                                maxStreak >= 18 -> "SOVEREIGN CROWN! 🎉"
                                maxStreak >= 12 -> "LIGHTNING RIBBON! ⚡"
                                maxStreak >= 7 -> "FIRE SPARKLER! 🔥"
                                else -> "Level Combo: $maxStreak"
                            },
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color(0xFF333333)
                        )
                        Text(
                            text = when {
                                maxStreak >= 18 -> "You hit a legendary 18+ combo! Master popper!"
                                maxStreak >= 12 -> "Unbelievable 12+ lightning speed combo!"
                                maxStreak >= 7 -> "Awesome 7+ streak achieved!"
                                else -> "Hit 7 targets without missing to unlock a Secret Sticker!"
                            },
                            fontSize = 11.sp,
                            color = Color(0xFF616161),
                            lineHeight = 14.sp
                        )
                    }
                }
            }
        }

        // 3. Play Again / Menu Navigation Buttons Choice
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // PLAY AGAIN Button (large action button)
            Button(
                onClick = { viewModel.startGame(gameMode) },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF9800)),
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(4.dp, RoundedCornerShape(24.dp))
                    .testTag("play_again_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play again",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "PLAY AGAIN! 🎮",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }

            // BACK TO MENU (secondary action button)
            Button(
                onClick = { viewModel.setScreen(Screen.MENU) },
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.25f)),
                contentPadding = PaddingValues(vertical = 14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("back_to_menu_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Home,
                        contentDescription = "Back to menu",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "BACK TO MENU 🏠",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}
