package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.Context
import android.os.Vibrator
import android.os.VibratorManager
import android.os.VibrationEffect
import android.os.Build
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.SoundSynthesizer
import kotlinx.coroutines.delay
import androidx.compose.ui.graphics.drawscope.withTransform
import com.example.ui.models.ActiveTarget
import com.example.ui.models.Screen
import com.example.ui.viewmodel.GameViewModel
import kotlin.math.sin
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

val TriangleShape = object : Shape {
    override fun createOutline(
        size: androidx.compose.ui.geometry.Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            moveTo(size.width / 2f, 0f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val totalScore by viewModel.score.collectAsState()
    val activeLevel by viewModel.gameLevel.collectAsState()
    val streakCount by viewModel.streak.collectAsState()
    val timeLeftSeconds by viewModel.timeLeft.collectAsState()
    val isFever by viewModel.isFeverActive.collectAsState()
    val feverMeter by viewModel.feverCharge.collectAsState()
    val activeTargets by viewModel.spawnedTargets.collectAsState()
    val visualEffects by viewModel.activeEffects.collectAsState()
    val selectedTheme by viewModel.selectedTheme.collectAsState()
    val selectedMode by viewModel.selectedMode.collectAsState()
    val isPaused by viewModel.isGamePaused.collectAsState()
    val showLevelUpBoard by viewModel.showLevelUpBoard.collectAsState()

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    // State for tracking custom level up transitions
    var lastSeenLevel by remember { mutableStateOf(activeLevel) }
    var levelUpLevelToDisplay by remember { mutableStateOf(activeLevel) }
    var showLevelUpAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(activeLevel) {
        if (activeLevel > lastSeenLevel) {
            levelUpLevelToDisplay = activeLevel
            showLevelUpAnimation = true
            SoundSynthesizer.playChime()
            delay(1800)
            showLevelUpAnimation = false
        }
        lastSeenLevel = activeLevel
    }

    LaunchedEffect(Unit) {
        viewModel.incorrectMatchEvent.collect {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            try {
                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                    vibratorManager?.defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                }
                if (vibrator != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val pattern = longArrayOf(0, 110, 70, 110)
                        vibrator.vibrate(VibrationEffect.createWaveform(pattern, -1))
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(longArrayOf(0, 110, 70, 110), -1)
                    }
                }
            } catch (ignored: Exception) {}
        }
    }

    // Matching prompts (educational mode)
    val colorPrompt by viewModel.activeColorPrompt.collectAsState()
    val shapePrompt by viewModel.activeShapePrompt.collectAsState()

    // Bouncing pulse animation for target scale
    val pulseTransition = rememberInfiniteTransition(label = "balloon_pulse")
    val pulseScale by pulseTransition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        val widthDp = maxWidth
        val heightDp = maxHeight

        // 1. Confetti, Sprinkles, and Particle Explosion Canvas (Drawn over background and under targets,
        // but text layered over targets!)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            visualEffects.forEach { effect ->
                effect.particles.forEach { p ->
                    val px = p.xPercent * canvasW
                    val py = p.yPercent * canvasH
                    
                    val particleShapeIndex = (p.color xor p.size.toLong()).mod(3)
                    
                    if (particleShapeIndex == 0) {
                        // Smooth glowing particle circles
                        drawCircle(
                            color = Color(p.color).copy(alpha = p.alpha),
                            radius = p.size,
                            center = Offset(px, py)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = p.alpha * 0.7f),
                            radius = p.size * 0.4f,
                            center = Offset(px - p.size * 0.2f, py - p.size * 0.2f)
                        )
                    } else if (particleShapeIndex == 1) {
                        // Vibrant rotating squares
                        val sizePx = p.size * 2f
                        withTransform({
                            rotate(p.rotation, Offset(px, py))
                        }) {
                            drawRect(
                                color = Color(p.color).copy(alpha = p.alpha),
                                topLeft = Offset(px - p.size, py - p.size),
                                size = androidx.compose.ui.geometry.Size(sizePx, sizePx)
                            )
                            drawRect(
                                color = Color.White.copy(alpha = p.alpha * 0.7f),
                                topLeft = Offset(px - p.size * 0.6f, py - p.size * 0.6f),
                                size = androidx.compose.ui.geometry.Size(sizePx * 0.4f, sizePx * 0.4f)
                            )
                        }
                    } else {
                        // Vibrant rotating triangles
                        withTransform({
                            rotate(p.rotation + 45f, Offset(px, py))
                        }) {
                            val path = Path().apply {
                                moveTo(px, py - p.size)
                                lineTo(px + p.size, py + p.size)
                                lineTo(px - p.size, py + p.size)
                                close()
                            }
                            drawPath(
                                path = path,
                                color = Color(p.color).copy(alpha = p.alpha)
                            )
                            // Inner highlight path
                            val innerPath = Path().apply {
                                moveTo(px, py - p.size * 0.4f)
                                lineTo(px + p.size * 0.4f, py + p.size * 0.4f)
                                lineTo(px - p.size * 0.4f, py + p.size * 0.4f)
                                close()
                            }
                            drawPath(
                                path = innerPath,
                                color = Color.White.copy(alpha = p.alpha * 0.7f)
                            )
                        }
                    }
                }
            }
        }

        // 2. Main Gameplay Column (HUD and Rules)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // HUD Row: Home button, Progress Bar Tracker, Points Tracker
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Pause Game Button (Custom stylized bars)
                IconButton(
                    onClick = { viewModel.pauseGame() },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        .testTag("pause_game_button")
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.width(5.dp).height(16.dp).background(Color.White, RoundedCornerShape(1.dp)))
                        Box(modifier = Modifier.width(5.dp).height(16.dp).background(Color.White, RoundedCornerShape(1.dp)))
                    }
                }

                // Playful unpunitive crawler timeline
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 12.dp)
                        .height(30.dp)
                        .background(Color.White.copy(alpha = 0.25f), RoundedCornerShape(15.dp))
                        .padding(4.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    val progressRatio = (timeLeftSeconds / 45f).coerceIn(0f, 1f)
                    val bgBoxWidth = widthDp - 150.dp // approx width

                    // Glowing Caterpillar / Rocket Trail
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressRatio)
                            .fillMaxHeight()
                            .background(
                                color = if (selectedTheme == "space") Color(0xFFE040FB) else Color(0xFF81C784),
                                shape = RoundedCornerShape(11.dp)
                            )
                    )

                    // Moving Mascot Emoji at right-edge of trail
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progressRatio),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        val mascotEmoji = when (selectedTheme) {
                            "space" -> "🚀"
                            "underwater" -> "🐠"
                            "candy" -> "🍭"
                            "volcano" -> "🦖"
                            "carnival" -> "🎡"
                            else -> "🐛"
                        }
                        Text(
                            text = mascotEmoji,
                            fontSize = 20.sp,
                            modifier = Modifier.offset(x = 6.dp, y = (-2).dp)
                        )
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Level Indicator Bubble
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF00E5FF), RoundedCornerShape(20.dp))
                            .shadow(4.dp, RoundedCornerShape(20.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "LVL $activeLevel 🌟",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF004D40)
                        )
                    }

                    // High Contrast Score Bubble (Big bold text)
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFEB3B), RoundedCornerShape(20.dp))
                            .shadow(4.dp, RoundedCornerShape(20.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$totalScore 🏆",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF333333)
                        )
                    }
                }
            }

            // EDUCATIONAL BANNER: display target color or shape
            AnimatedVisibility(visible = selectedMode == "color_match") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(4.dp, RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.92f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "LOOK & MATCH! 👀",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF757575)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Tap the ",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF212121)
                            )
                            
                            colorPrompt?.let { color ->
                                Text(
                                    text = "${color.displayName}! ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(color.hexColor)
                                )
                            }
                            shapePrompt?.let { shape ->
                                Text(
                                    text = "${shape.displayName}s! ",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFF9800)
                                )
                                Text(
                                    text = shape.emoji,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // FEVER PROGRESS STATUS BAR (Renders near the bottom as multiplier tracker)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .height(24.dp)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(feverMeter)
                        .fillMaxHeight()
                        .background(
                            color = if (isFever) Color(0xFFFF1744) else Color(0xFFFFD54F),
                            shape = RoundedCornerShape(9.dp)
                        )
                )
                Text(
                    text = if (isFever) "⭐ FEVER MULTIPLIER ACTIVE (2X POINTS!) ⭐" else "STREAK METER 🔥",
                    color = Color.White,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().align(Alignment.Center)
                )
            }
        }

        // 3. Spawning Target Layer (Renders floating targets inside safe coordinates space)
        val currentDensity = LocalDensity.current.density
        activeTargets.forEach { target ->
            // Use local density to convert percentages into absolute IntOffsets
            val xOffsetPx = (target.xPercent * (widthDp.value * currentDensity)).toInt()
            val yOffsetPx = (target.yPercent * (heightDp.value * currentDensity)).toInt()

            val shapeColor = Color(target.color.hexColor)

            // Calculate responsive size (with pulse modifier)
            val computedSizeDp = target.baseSizeDp.dp * if (target.isPopping) 1.0f else pulseScale
            val targetAlpha = target.alpha

            // 20% larger collision tap box to prevent child frustration!
            val collisionPadDp = target.baseSizeDp.dp * 0.22f

            Box(
                modifier = Modifier
                    .offset {
                        // Apply additional micro wiggle from current VM cycle and subtract half size to center the bubble
                        val sizePx = (((computedSizeDp + collisionPadDp).value) * currentDensity).toInt()
                        IntOffset(
                            xOffsetPx - sizePx / 2,
                            (yOffsetPx - sizePx / 2 + (target.wiggleOffset * currentDensity)).toInt()
                        )
                    }
                    .size(computedSizeDp + collisionPadDp)
                    // High-contrast, accessibility-positive tap targets (minimum 48dp, usually 120dp for kids!)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null // bypass default grey squares, we render satisfying custom Canvas Confetti!
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        try {
                            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                                vibratorManager?.defaultVibrator
                            } else {
                                @Suppress("DEPRECATION")
                                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                            }
                            if (vibrator != null) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    vibrator.vibrate(VibrationEffect.createOneShot(45, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    @Suppress("DEPRECATION")
                                    vibrator.vibrate(45)
                                }
                            }
                        } catch (e: Exception) {
                            // ignore if unsupported
                        }
                        viewModel.onTapTarget(target)
                    }
                    .alpha(targetAlpha)
                    .scale(target.scale)
                    .testTag("target_bubble_${target.shape.name.lowercase()}_${target.color.name.lowercase()}"),
                contentAlignment = Alignment.Center
            ) {
                val isSpecial = target.targetType != com.example.ui.models.TargetType.NORMAL
                val displayBgColor = when (target.targetType) {
                    com.example.ui.models.TargetType.CLOCK -> Color(0xFF00E5FF) // neon cyan
                    com.example.ui.models.TargetType.BOMB -> Color(0xFFFF3D00) // fire orange-red
                    com.example.ui.models.TargetType.GOLD_STAR -> Color(0xFFFFD600) // sparkling gold
                    else -> shapeColor
                }

                val displayEmoji = when (target.targetType) {
                    com.example.ui.models.TargetType.CLOCK -> "⏰"
                    com.example.ui.models.TargetType.BOMB -> "💥"
                    com.example.ui.models.TargetType.GOLD_STAR -> "👑"
                    else -> target.shape.emoji
                }

                val isGeometricReflex = selectedMode == "reflex" && !isSpecial
                val geometricShape = if (isGeometricReflex) {
                    when (target.shape) {
                        com.example.ui.models.TargetShape.SQUARE -> RoundedCornerShape(14.dp)
                        com.example.ui.models.TargetShape.TRIANGLE -> TriangleShape
                        else -> CircleShape
                    }
                } else {
                    CircleShape
                }

                val borderModifier = if (isSpecial) {
                    Modifier.border(
                        width = 4.dp,
                        brush = androidx.compose.ui.graphics.Brush.sweepGradient(
                            colors = listOf(Color(0xFFFF1744), Color(0xFFFFEA00), Color(0xFF00E676), Color(0xFF00E5FF), Color(0xFFFF1744))
                        ),
                        shape = CircleShape
                    )
                } else if (isGeometricReflex) {
                    Modifier.border(
                        width = 3.dp,
                        color = Color.White.copy(alpha = 0.9f),
                        shape = geometricShape
                    )
                } else {
                    Modifier
                }

                // VISIBLE SPRITE: A glowing circular cartoon bubble or geometric shape
                Box(
                    modifier = Modifier
                        .size(computedSizeDp)
                        .shadow(if (isSpecial) 10.dp else 6.dp, geometricShape)
                        .then(borderModifier)
                        .background(displayBgColor, geometricShape)
                        .clip(geometricShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (isGeometricReflex) {
                        // Nested glowing inner layered shape
                        Box(
                            modifier = Modifier
                                .fillMaxSize(0.42f)
                                .alpha(0.35f)
                                .background(Color.White, geometricShape)
                        )
                        // Dynamic exciting action symbol
                        Text(
                            text = "⚡",
                            fontSize = (computedSizeDp.value * 0.35f).sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    } else {
                        // Bubble Inner Glass Glare
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                                .alpha(0.2f)
                                .background(
                                    color = Color.White,
                                    shape = CircleShape
                                )
                                .align(Alignment.TopStart)
                                .fillMaxWidth(0.5f)
                                .fillMaxHeight(0.5f)
                        )

                        // Playful Central Item Emoji (highly recognizable to kids)
                        Text(
                            text = displayEmoji,
                            fontSize = (computedSizeDp.value * (if (isSpecial) 0.52f else 0.48f)).sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }

        // 4. Overlapping Score Float numbers ("+10", "+20!") rendered inside view heights
        visualEffects.forEach { effect ->
            val fxOffsetPx = (effect.xPercent * (widthDp.value * LocalDensity.current.density)).toInt()
            val fyOffsetPx = (effect.yPercent * (heightDp.value * LocalDensity.current.density)).toInt()

            Box(
                modifier = Modifier
                    .offset { IntOffset(fxOffsetPx - 50, fyOffsetPx - 30) }
                    .alpha(effect.alpha)
            ) {
                Text(
                    text = effect.text,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.8f),
                            offset = Offset(2f, 2f),
                            blurRadius = 6f
                        )
                    ),
                    textAlign = TextAlign.Center
                )
            }
        }

        // 5. High-fidelity Pause Modal Overlay
        AnimatedVisibility(
            visible = isPaused,
            enter = fadeIn(animationSpec = tween(250)),
            exit = fadeOut(animationSpec = tween(200)),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(
                        enabled = true,
                        onClickLabel = "none",
                        onClick = {} // Capture clicks so players can't tap things behind the modal
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Outer bouncing colorful dynamic borders or glowing bubble card
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFF5F5F5) // light cozy warm-grey
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 340.dp)
                        .border(
                            width = 6.dp,
                            color = Color(0xFFFFEB3B), // neon yellow borders
                            shape = RoundedCornerShape(32.dp)
                        )
                        .testTag("pause_options_modal")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Big Playful Header
                        Text(
                            text = "GAME PAUSED ⏸️",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF37474F),
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "Take a quick deep breath, then jump right back in! 🌸",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF78909C),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Large Kid-friendly Buttons (touch targets >= 48.dp guaranteed by min = 52.dp)
                        
                        // 1. RESUME BUTTON (Neon Green)
                        Button(
                            onClick = { viewModel.resumeGame() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .testTag("pause_resume_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🚀", fontSize = 20.sp)
                                Text(
                                    text = "RESUME GAME",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // 2. RESTART LEVEL (Orange)
                        Button(
                            onClick = { viewModel.startGame(selectedMode) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .testTag("pause_restart_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🔄", fontSize = 20.sp)
                                Text(
                                    text = "RESTART LEVEL",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }

                        // 3. MAIN MENU (Red-pinkish)
                        Button(
                            onClick = { viewModel.setScreen(Screen.MENU) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE57373),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .testTag("pause_to_menu_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🏠", fontSize = 20.sp)
                                Text(
                                    text = "MAIN MENU",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 16.sp,
                                    letterSpacing = 1.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // 6. Playful Level Up Transition Overlay (Interactive Next Level Board)
        AnimatedVisibility(
            visible = showLevelUpBoard,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.6f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioHighBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(250)) + scaleOut(
                targetScale = 1.2f,
                animationSpec = tween(250, easing = FastOutSlowInEasing)
            ),
            modifier = Modifier.fillMaxSize()
        ) {
            val levelUpTransition = rememberInfiniteTransition(label = "levelup_fx")
            val rotateStar by levelUpTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(4000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotate"
            )
            val scalePulse by levelUpTransition.animateFloat(
                initialValue = 0.94f,
                targetValue = 1.06f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.75f))
                    .clickable(enabled = true, onClick = {}), // Trap Taps
                contentAlignment = Alignment.Center
            ) {
                // Background rotating rays
                Box(
                    modifier = Modifier
                        .size(320.dp)
                        .rotate(rotateStar)
                        .alpha(0.25f)
                ) {
                    Text(
                        text = "⭐   ⭐\n  🎈  \n⭐   ⭐",
                        fontSize = 80.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }

                // Central Card
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFDE7) // cozy lemon cream
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 16.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 350.dp)
                        .scale(scalePulse)
                        .border(
                            width = 6.dp,
                            color = Color(0xFFFFC107), // Golden amber border
                            shape = RoundedCornerShape(32.dp)
                        )
                        .testTag("level_complete_board")
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🎉 LEVEL COMPLETE! 🎉",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFFF6F00),
                            textAlign = TextAlign.Center,
                            style = TextStyle(
                                shadow = Shadow(
                                    color = Color(0xFFFFE082),
                                    offset = Offset(2f, 2f),
                                    blurRadius = 4f
                                )
                            )
                        )

                        // Circular badge
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .background(Color(0xFFFFEB3B), CircleShape)
                                .border(4.dp, Color(0xFFFF3D00), CircleShape)
                                .shadow(4.dp, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(
                                    text = "PASSED",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF3D00)
                                )
                                Text(
                                    text = "$activeLevel",
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF3E2723)
                                )
                            }
                        }

                        Text(
                            text = "Amazing tapping! Total Score: $totalScore pts! 🌟",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4E342E),
                            textAlign = TextAlign.Center
                        )

                        // Nice message based on theme
                        Text(
                            text = when (selectedTheme) {
                                "space" -> "Up next: Space balloons are zooming much faster! 🚀🌌"
                                "underwater" -> "Up next: Deep sea bubbles are rising even speedier! 🐠⚓"
                                "candy" -> "Up next: Even sweeter pop rush is waiting! 🍭🍰"
                                "volcano" -> "Up next: Lava pops are cooling quicker! 🦖🌋"
                                "carnival" -> "Up next: Step up for faster carnival clicks! 🎡🎈"
                                else -> "New challenges are unlocked! Ready? 🎈"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF7D6608),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Large Kid-friendly Buttons (touch targets >= 48.dp)

                        // 1. PLAY NEXT LEVEL (Neon Green)
                        Button(
                            onClick = { viewModel.proceedToNextLevel() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .testTag("level_up_continue_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🚀", fontSize = 20.sp)
                                Text(
                                    text = "CONTINUE TO LEVEL ${activeLevel + 1}",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // 2. RESTART LEVEL (Orange)
                        Button(
                            onClick = { viewModel.restartCurrentLevel() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFF9800),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .testTag("level_up_replay_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "🔄", fontSize = 20.sp)
                                Text(
                                    text = "REPLAY LEVEL $activeLevel",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // 3. COLLECT PRIZES (Red-pinkish)
                        Button(
                            onClick = { viewModel.collectPrizeAndFinish() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFE57373),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .shadow(4.dp, RoundedCornerShape(16.dp))
                                .testTag("level_up_exit_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(text = "👑", fontSize = 20.sp)
                                Text(
                                    text = "COLLECT PRIZE & QUIT",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
