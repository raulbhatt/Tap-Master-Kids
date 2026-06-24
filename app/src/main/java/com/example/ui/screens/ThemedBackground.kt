package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import kotlin.math.sin

@Composable
fun ThemedBackground(
    themeId: String,
    isFeverActive: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    // Shared infinite state tracker for drifting effects
    val infiniteTransition = rememberInfiniteTransition(label = "background_drift")
    
    // Low-cost translation animations for elements
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "progress"
    )

    val waveProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave"
    )

    // Fever mode background sparkle colors
    val feverColorIndex by infiniteTransition.animateValue(
        initialValue = 0,
        targetValue = 3,
        typeConverter = Int.VectorConverter,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fever_color"
    )

    // Dynamic background brush
    val backgroundBrush = remember(themeId, isFeverActive, feverColorIndex) {
        if (isFeverActive) {
            val colors = when (feverColorIndex) {
                0 -> listOf(Color(0xFFFF5252), Color(0xFFFF9800), Color(0xFFFFEB3B), Color(0xFF4CAF50))
                1 -> listOf(Color(0xFFE040FB), Color(0xFF3F51B5), Color(0xFF00BCD4), Color(0xFF4CAF50))
                else -> listOf(Color(0xFF00E676), Color(0xFF2979FF), Color(0xFFD500F9), Color(0xFFFF1744))
            }
            Brush.verticalGradient(colors)
        } else {
            when (themeId) {
                "space" -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF0D0230), Color(0xFF1E1154), Color(0xFF3B1F75))
                )
                "underwater" -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF00ACC1), Color(0xFF00838F), Color(0xFF006064))
                )
                "candy" -> Brush.verticalGradient(
                    colors = listOf(Color(0xFFFFF1F2), Color(0xFFFCE4EC), Color(0xFFF8BBD0))
                )
                "volcano" -> Brush.verticalGradient(
                    colors = listOf(Color(0xFFE65100), Color(0xFFFF5722), Color(0xFFFF8A65))
                )
                "carnival" -> Brush.verticalGradient(
                    colors = listOf(Color(0xFF311B92), Color(0xFF1D2F6F), Color(0xFF00101E))
                )
                else -> Brush.verticalGradient( // meadow
                    colors = listOf(Color(0xFF81D4FA), Color(0xFFB3E5FC), Color(0xFFE1F5FE))
                )
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush = backgroundBrush)
    ) {
        // Draw decorative items on Canvas based on selected universe
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            if (isFeverActive) {
                drawFeverSparkles(width, height, animationProgress)
            } else {
                when (themeId) {
                    "space" -> drawSpaceTheme(width, height, animationProgress)
                    "underwater" -> drawUnderwaterTheme(width, height, animationProgress, waveProgress)
                    "candy" -> drawCandyTheme(width, height, animationProgress)
                    "volcano" -> drawVolcanoTheme(width, height, animationProgress)
                    "carnival" -> drawCarnivalTheme(width, height, animationProgress)
                    else -> drawMeadowTheme(width, height, animationProgress)
                }
            }
        }

        // Inner controls
        content()
    }
}

private fun DrawScope.drawMeadowTheme(width: Float, height: Float, progress: Float) {
    // 1. Draw a big smiling sun in the top-right
    val sunCenter = Offset(width * 0.85f, height * 0.12f)
    drawCircle(
        color = Color(0xFFFFD54F),
        radius = 50f,
        center = sunCenter
    )
    for (i in 0 until 8) {
        val angle = (i * Math.PI / 4.0) + (progress * 2.0 * Math.PI)
        val startOffset = Offset(
            (sunCenter.x + cos(angle) * 65f).toFloat(),
            (sunCenter.y + sin(angle) * 65f).toFloat()
        )
        val endOffset = Offset(
            (sunCenter.x + cos(angle) * 85f).toFloat(),
            (sunCenter.y + sin(angle) * 85f).toFloat()
        )
        drawLine(
            color = Color(0xFFFFB74D),
            start = startOffset,
            end = endOffset,
            strokeWidth = 8f
        )
    }

    // 2. Draw fluffy drifting clouds
    val positions = listOf(
        Offset(0.1f, 0.2f),
        Offset(0.6f, 0.35f),
        Offset(-0.2f, 0.15f)
    )

    positions.forEachIndexed { index, pos ->
        val xShift = (progress + pos.x) % 1.2f - 0.2f
        val cloudX = xShift * width
        val cloudY = pos.y * height

        // Draw cloud triplets (3 overlapping white circles)
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 35f, center = Offset(cloudX, cloudY))
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 45f, center = Offset(cloudX + 30f, cloudY - 10f))
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = 35f, center = Offset(cloudX + 60f, cloudY))
    }

    // 3. Draw deep green rolling soft hills at the bottom
    val hill1Center = Offset(width * 0.25f, height * 1.05f)
    drawCircle(color = Color(0xFF81C784), radius = width * 0.45f, center = hill1Center)

    val hill2Center = Offset(width * 0.75f, height * 1.08f)
    drawCircle(color = Color(0xFF66BB6A), radius = width * 0.55f, center = hill2Center)
}

private fun DrawScope.drawSpaceTheme(width: Float, height: Float, progress: Float) {
    // 1. Draw glowing stars (fixed but twinkling)
    val starPositions = listOf(
        Offset(0.15f, 0.12f), Offset(0.42f, 0.08f), Offset(0.85f, 0.18f),
        Offset(0.28f, 0.35f), Offset(0.72f, 0.42f), Offset(0.12f, 0.58f),
        Offset(0.88f, 0.65f), Offset(0.38f, 0.78f), Offset(0.68f, 0.88f)
    )

    starPositions.forEachIndexed { index, pos ->
        // Synthesise twinkling with a simple local offset
        val twinkleSpeed = 3.0f + index * 0.4f
        val starAlpha = (0.35f + 0.65f * sin((progress * twinkleSpeed * 2.0 * Math.PI).toFloat())).coerceIn(0.1f, 1f)
        val size = if (index % 3 == 0) 12f else 7f

        // Stars can be small yellow/light-blue circles
        val starColor = if (index % 2 == 0) Color(0xFFFFEE58) else Color(0xFF80DEEA)
        drawCircle(
            color = starColor.copy(alpha = starAlpha),
            radius = size,
            center = Offset(pos.x * width, pos.y * height)
        )
    }

    // 2. Draw a cute cartoon pink planet
    val planetCenter = Offset(width * 0.2f, height * 0.25f)
    drawCircle(
        color = Color(0xFFEC407A),
        radius = 45f,
        center = planetCenter
    )
    // Planet ring
    drawLine(
        color = Color(0xFFFFB74D).copy(alpha = 0.75f),
        start = Offset(planetCenter.x - 70f, planetCenter.y + 15f),
        end = Offset(planetCenter.x + 70f, planetCenter.y - 15f),
        strokeWidth = 10f
    )
}

private fun DrawScope.drawUnderwaterTheme(width: Float, height: Float, progress: Float, wave: Float) {
    // 1. Translucent bubbles rising
    val bubbleOffsets = listOf(
        Offset(0.15f, 0.95f), Offset(0.45f, 0.85f), Offset(0.75f, 0.90f),
        Offset(0.30f, 0.70f), Offset(0.85f, 0.65f), Offset(0.10f, 0.45f)
    )

    bubbleOffsets.forEachIndexed { index, pos ->
        // Rising drift path
        val bubbleYProgress = (pos.y - progress * 1.1f) % 1.1f
        val bubbleY = (if (bubbleYProgress < 0) bubbleYProgress + 1.1f else bubbleYProgress) * height
        
        // Sway sideways
        val wobbleValue = sin((wave * 2 * Math.PI + index).toFloat()) * 20f
        val bubbleX = pos.x * width + wobbleValue

        val size = 15f + (index % 3) * 10f

        // Outer ring
        drawCircle(
            color = Color.White.copy(alpha = 0.45f),
            radius = size,
            center = Offset(bubbleX, bubbleY),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )
        // Highlight shine point
        drawCircle(
            color = Color.White.copy(alpha = 0.65f),
            radius = size * 0.25f,
            center = Offset(bubbleX - size * 0.4f, bubbleY - size * 0.4f)
        )
    }

    // 2. Rising sea anemone / seaweed at the bottom
    val plantCount = 5
    for (i in 0 until plantCount) {
        val xBase = (i * width / (plantCount - 1))
        val xShift = if (i == 0) 15f else if (i == plantCount - 1) -15f else 0f
        val plantX = xBase + xShift
        val heightMultiplier = 0.12f + 0.04f * (i % 2)
        val controlSway = plantX + sin((wave * 2.0 * Math.PI + i).toFloat()) * 25f

        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(plantX, height)
            quadraticTo(
                controlSway.toFloat(), height - (height * heightMultiplier * 0.5f),
                (plantX + sin((wave * 2.0 * Math.PI + i).toFloat()) * 15f).toFloat(), height - (height * heightMultiplier)
            )
        }

        drawPath(
            path = path,
            color = Color(0xFF00E676).copy(alpha = 0.4f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 35f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
        )
    }
}

private fun DrawScope.drawFeverSparkles(width: Float, height: Float, progress: Float) {
    // Energetic stars bursting upwards during Fever Mode!
    val sparklePositions = listOf(
        Offset(0.2f, 0.8f), Offset(0.5f, 0.7f), Offset(0.8f, 0.9f),
        Offset(0.1f, 0.4f), Offset(0.4f, 0.3f), Offset(0.9f, 0.5f),
        Offset(0.3f, 0.1f), Offset(0.7f, 0.2f), Offset(0.6f, 0.6f)
    )

    sparklePositions.forEachIndexed { index, pos ->
        val yProg = (pos.y - progress * 1.5f) % 1.1f
        val sparkleY = (if (yProg < 0) yProg + 1.1f else yProg) * height
        val sparkleX = pos.x * width + sin(((progress * 4f * Math.PI) + index).toFloat()) * 25f

        val radius = 10f + (index % 4) * 8f
        val color = when (index % 4) {
            0 -> Color(0xFFFFEB3B)
            1 -> Color(0xFFE040FB)
            2 -> Color(0xFF00E676)
            else -> Color(0xFF00E5FF)
        }

        drawCircle(
            color = color,
            radius = radius,
            center = Offset(sparkleX, sparkleY)
        )
    }
}

private fun DrawScope.drawCandyTheme(width: Float, height: Float, progress: Float) {
    // 1. A smiling giant spinning lollipop in top right
    val lollyCenter = Offset(width * 0.82f, height * 0.16f)
    
    // Stick
    drawLine(
        color = Color(0xFFECEFF1),
        start = lollyCenter,
        end = Offset(lollyCenter.x - 30f, lollyCenter.y + 75f),
        strokeWidth = 10f,
        cap = androidx.compose.ui.graphics.StrokeCap.Round
    )
    
    // Colorful rings spinning
    drawCircle(color = Color(0xFFF06292), radius = 50f, center = lollyCenter) // base sweet pink
    drawCircle(color = Color(0xFFFFF9C4), radius = 38f, center = lollyCenter) // vanilla yellow ring
    drawCircle(color = Color(0xFF80DEEA), radius = 26f, center = lollyCenter) // minty blue ring
    drawCircle(color = Color(0xFFBA68C8), radius = 14f, center = lollyCenter) // lavender heart
    
    // 2. Sweet falling candy drops / sprinkles in the atmosphere
    val sprinkles = listOf(
        Offset(0.12f, 0.15f), Offset(0.38f, 0.1f), Offset(0.68f, 0.28f),
        Offset(0.25f, 0.45f), Offset(0.85f, 0.42f), Offset(0.18f, 0.68f),
        Offset(0.9f, 0.75f), Offset(0.48f, 0.85f)
    )

    sprinkles.forEachIndexed { index, pos ->
        val yProg = (pos.y + progress * 0.4f) % 1.1f
        val sprinkleY = yProg * height
        val sway = sin((progress * 4f * Math.PI + index).toFloat()) * 18f
        val sprinkleX = pos.x * width + sway

        val sprinkleColor = when (index % 4) {
            0 -> Color(0xFFEC407A) // bright pink
            1 -> Color(0xFFFFB74D) // pastel orange
            2 -> Color(0xFF4DD0E1) // sky cyan
            else -> Color(0xFFC2185B) // ruby candy
        }

        // Draw pills/capsule sprinkle lines
        val angle = (index * 45f + progress * 120f) * (Math.PI / 180f).toFloat()
        val len = 14f
        val dx = cos(angle.toDouble()).toFloat() * len
        val dy = sin(angle.toDouble()).toFloat() * len

        drawLine(
            color = sprinkleColor.copy(alpha = 0.75f),
            start = Offset(sprinkleX - dx, sprinkleY - dy),
            end = Offset(sprinkleX + dx, sprinkleY + dy),
            strokeWidth = 6f,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }

    // 3. Fluffy marshmallow hills at the bottom
    drawCircle(color = Color(0xFFF8BBD0).copy(alpha = 0.6f), radius = width * 0.5f, center = Offset(width * 0.25f, height * 1.05f))
    drawCircle(color = Color(0xFFE1BEE7).copy(alpha = 0.5f), radius = width * 0.45f, center = Offset(width * 0.75f, height * 1.05f))
}

private fun DrawScope.drawVolcanoTheme(width: Float, height: Float, progress: Float) {
    // 1. Majestic dormant volcano outlines at the bottom
    drawCircle(
        color = Color(0xFF3E2723), // Prehistoric deep brown volcanic rock
        radius = width * 0.42f,
        center = Offset(width * 0.35f, height * 1.05f)
    )
    drawCircle(
        color = Color(0xFF2D1500), // Dark obsidian rock
        radius = width * 0.48f,
        center = Offset(width * 0.72f, height * 1.08f)
    )

    // Glowing magma crest on the hills
    drawCircle(
        color = Color(0xFFFF3D00).copy(alpha = 0.35f), // hot active magma glow
        radius = width * 0.38f,
        center = Offset(width * 0.35f, height * 1.03f)
    )

    // 2. Active floating lava/ash embers lifting up
    val emberOffsets = listOf(
        Offset(0.15f, 0.9f), Offset(0.45f, 0.78f), Offset(0.75f, 0.88f),
        Offset(0.28f, 0.6f), Offset(0.88f, 0.55f), Offset(0.12f, 0.4f),
        Offset(0.58f, 0.3f), Offset(0.32f, 0.18f), Offset(0.82f, 0.22f)
    )

    emberOffsets.forEachIndexed { index, pos ->
        // Floating upwards
        val emberYProgress = (pos.y - progress * 0.7f) % 1.1f
        val emberY = (if (emberYProgress < 0) emberYProgress + 1.1f else emberYProgress) * height
        val sway = sin((progress * 5f * Math.PI + index * 1.5f).toFloat()) * 16f
        val emberX = pos.x * width + sway

        val size = 6f + (index % 3) * 6f
        val sparkColor = when (index % 3) {
            0 -> Color(0xFFFFD54F) // intense golden spark
            1 -> Color(0xFFFF9100) // scorching orange
            else -> Color(0xFFFF3D00) // radiant lava red
        }

        // Animated heat-pulse alpha
        val pulseSpeed = 4.0f + index * 0.5f
        val alpha = (0.45f + 0.55f * sin((progress * pulseSpeed * 2.0 * Math.PI).toFloat())).coerceIn(0.2f, 1f)

        drawCircle(
            color = sparkColor.copy(alpha = alpha),
            radius = size,
            center = Offset(emberX, emberY)
        )
    }
}

private fun DrawScope.drawCarnivalTheme(width: Float, height: Float, progress: Float) {
    // 1. Hanging string of magical fairy lights at the very top
    val ropeMidY = 90f
    val ropeEndY = 50f
    
    // Draw string paths as nice bezier curves
    val path1 = androidx.compose.ui.graphics.Path().apply {
        moveTo(0f, ropeEndY)
        quadraticTo(width * 0.25f, ropeMidY, width * 0.5f, ropeEndY)
        quadraticTo(width * 0.75f, ropeMidY, width, ropeEndY)
    }
    
    drawPath(
        path = path1,
        color = Color(0xFFA1887F).copy(alpha = 0.5f), // light brown twisted rope
        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f)
    )

    // Lit lightbulbs dangling from the rope!
    val bulbCount = 8
    for (i in 0..bulbCount) {
        val t = i.toFloat() / bulbCount
        val lx = t * width
        
        // Simple parabolic formula to evaluate height on strings
        val actualY = if (t <= 0.5f) {
            val localT = t / 0.5f
            ropeEndY + (ropeMidY - ropeEndY) * (4f * localT * (1f - localT)) * 0.25f
        } else {
            val localT = (t - 0.5f) / 0.5f
            ropeEndY + (ropeMidY - ropeEndY) * (4f * localT * (1f - localT)) * 0.25f
        }

        // Alternating colors
        val bulbColor = when (i % 4) {
            0 -> Color(0xFFFFEB3B)  // Gold yellow
            1 -> Color(0xFF00E5FF)  // Electrifying blue
            2 -> Color(0xFFE040FB)  // Pastel violet
            else -> Color(0xFFFF5252) // Poppy pink-red
        }

        // Flashing animation state
        val flashState = sin((progress * 15f + i).toFloat()) > 0f
        val bulbAlpha = if (flashState) 1.0f else 0.4f
        val glowRadius = if (flashState) 16f else 8f

        // Draw light beam glow behind itself
        drawCircle(
            color = bulbColor.copy(alpha = if (flashState) 0.35f else 0.1f),
            radius = glowRadius + 14f,
            center = Offset(lx, actualY + 12f)
        )
        // Draw real glass bulb
        drawCircle(
            color = bulbColor.copy(alpha = bulbAlpha),
            radius = 11f,
            center = Offset(lx, actualY + 12f)
        )
        // Little top copper socket
        drawLine(
            color = Color(0xFF90A4AE),
            start = Offset(lx, actualY),
            end = Offset(lx, actualY + 7f),
            strokeWidth = 6f
        )
    }

    // 2. Festive helium party balloons drifting upwards!
    val balloonOffsets = listOf(
        Offset(0.2f, 0.95f), Offset(0.7f, 0.85f), Offset(0.4f, 0.65f), Offset(0.85f, 0.5f), Offset(0.12f, 0.35f)
    )

    balloonOffsets.forEachIndexed { index, pos ->
        // Floating up slower
        val balloonYProgress = (pos.y - progress * 0.45f) % 1.1f
        val balloonY = (if (balloonYProgress < 0) balloonYProgress + 1.1f else balloonYProgress) * height
        val sway = sin((progress * 3f * Math.PI + index * 2.0f).toFloat()) * 25f
        val balloonX = pos.x * width + sway

        val balloonColor = when (index % 3) {
            0 -> Color(0xFFFFFF00) // Vivid yellow
            1 -> Color(0xFF00FFCC) // Neon teal
            else -> Color(0xFFFF0DFF) // Luminous hot pink
        }

        // Draw balloon string
        val stringPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(balloonX, balloonY + 24f)
            quadraticTo(
                balloonX + sin((progress * 4f * Math.PI).toFloat()) * 12f, balloonY + 45f,
                balloonX, balloonY + 75f
            )
        }
        drawPath(
            path = stringPath,
            color = Color.White.copy(alpha = 0.3f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
        )

        // Draw balloon ellipsoid
        drawCircle(
            color = balloonColor.copy(alpha = 0.65f),
            radius = 24f,
            center = Offset(balloonX, balloonY)
        )
        // Balloon little knot triangle
        drawCircle(
            color = balloonColor.copy(alpha = 0.8f),
            radius = 6f,
            center = Offset(balloonX, balloonY + 23f)
        )
    }
}

// Math approximations for DrawScope Kotlin-only usage
private fun cos(v: Double) = kotlin.math.cos(v).toFloat()
private fun sin(v: Double) = kotlin.math.sin(v).toFloat()
