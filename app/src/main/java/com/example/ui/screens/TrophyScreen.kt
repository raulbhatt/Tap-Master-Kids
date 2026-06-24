package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.audio.SoundSynthesizer
import com.example.ui.models.Screen
import com.example.ui.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Data classes for the space game loop
data class ShooterBullet(
    val id: Long,
    var x: Float,
    var y: Float
)

data class ShooterEnemy(
    val id: Long,
    var x: Float,
    var y: Float,
    val emoji: String,
    val speed: Float,
    val size: Float,
    var hp: Int = 1,
    var baseX: Float = x,
    val angleFrequency: Float = 0.015f + Random.nextFloat() * 0.02f,
    val angleAmplitude: Float = 40f + Random.nextFloat() * 70f,
    val hoverOffset: Float = Random.nextFloat() * 100f,
    val isBoss: Boolean = false,
    var maxHp: Int = hp
)

data class BossProjectile(
    val id: Long,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val emoji: String = "⚡",
    val size: Float = 35f
)

data class ShooterParticle(
    val id: Long,
    var x: Float,
    var y: Float,
    val vx: Float,
    val vy: Float,
    val emoji: String,
    val size: Float,
    var alpha: Float,
    var rotation: Float,
    val rotSpeed: Float
)

data class ShooterBlastLabel(
    val id: Long,
    var x: Float,
    var y: Float,
    val text: String,
    var age: Int,
    val maxAge: Int,
    val color: Color,
    val scaleMultiplier: Float
)

data class SpaceStar(
    var x: Float,
    var y: Float,
    val speed: Float,
    val alpha: Float
)

@Composable
fun TrophyScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current

    // Local SharedPreferences for High Scores
    val sharedPrefs = remember { context.getSharedPreferences("space_shooter_prefs", Context.MODE_PRIVATE) }
    var spaceHighScore by remember { mutableStateOf(sharedPrefs.getInt("high_score", 0)) }

    // Game stats
    var gameScore by remember { mutableStateOf(0) }
    var hearts by remember { mutableStateOf(3) }
    var level by remember { mutableStateOf(1) }
    var isGameOver by remember { mutableStateOf(false) }
    var isPaused by remember { mutableStateOf(false) }

    // Dimensions from BoxWithConstraints
    var areaWidth by remember { mutableStateOf(0f) }
    var areaHeight by remember { mutableStateOf(0f) }

    // Spaceship position & target coordinate (smooth glide lerp)
    var shipX by remember { mutableStateOf(0f) }
    var targetShipX by remember { mutableStateOf(0f) }
    var shipY by remember { mutableStateOf(0f) }
    var targetShipY by remember { mutableStateOf(0f) }

    // Game entities
    val bullets = remember { mutableStateListOf<ShooterBullet>() }
    val enemies = remember { mutableStateListOf<ShooterEnemy>() }
    val bossProjectiles = remember { mutableStateListOf<BossProjectile>() }
    val particles = remember { mutableStateListOf<ShooterParticle>() }
    val blastLabels = remember { mutableStateListOf<ShooterBlastLabel>() }
    val stars = remember { mutableStateListOf<SpaceStar>() }
    var lastSpawnedBossLevel by remember { mutableStateOf(0) }

    // ID counter to generate unique Long values
    var nextId by remember { mutableStateOf(1L) }

    // Flash screen on hit or damage effect
    var damageFlashCount by remember { mutableStateOf(0) }

    // Init background stars once we get dimensions
    LaunchedEffect(areaWidth, areaHeight) {
        if (areaWidth > 0f && areaHeight > 0f) {
            if (shipY == 0f || shipX == 0f) {
                shipX = areaWidth / 2f
                targetShipX = areaWidth / 2f
                shipY = areaHeight - 120f
                targetShipY = areaHeight - 120f
            }
            if (stars.isEmpty()) {
                for (i in 0 until 40) {
                    stars.add(
                        SpaceStar(
                            x = Random.nextFloat() * areaWidth,
                            y = Random.nextFloat() * areaHeight,
                            speed = 1.5f + Random.nextFloat() * 4f,
                            alpha = 0.2f + Random.nextFloat() * 0.7f
                        )
                    )
                }
            }
        }
    }

    // High Score Persistence
    val saveHighScore = { newScore: Int ->
        if (newScore > spaceHighScore) {
            spaceHighScore = newScore
            sharedPrefs.edit().putInt("high_score", newScore).apply()
        }
    }

    // Helper to trigger haptic tap
    val triggerHaptic = {
        try {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }
            if (vibrator != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator.vibrate(VibrationEffect.createOneShot(35, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator.vibrate(35)
                }
            }
        } catch (ignored: Exception) {}
    }

    // 1. Core Physics & Spawning Game Loop
    LaunchedEffect(isGameOver, isPaused) {
        if (isGameOver || isPaused) return@LaunchedEffect

        var lastSpawnTime = 0L
        var lastAutoFireTime = 0L
        var lastBossAttackTime = 0L

        while (true) {
            // Target FPS update rate (~16ms per frame = ~60 FPS)
            withFrameMillis { frameTime ->
                if (areaWidth <= 0f || areaHeight <= 0f) return@withFrameMillis

                // Smoothship lerp coordinate calculation
                shipX += (targetShipX - shipX) * 0.22f
                shipY += (targetShipY - shipY) * 0.22f

                // Limit range
                shipX = shipX.coerceIn(40f, areaWidth - 40f)
                shipY = shipY.coerceIn(40f, areaHeight - 40f)

                // Background star field drift
                stars.forEach { star ->
                    star.y += star.speed
                    if (star.y > areaHeight) {
                        star.y = 0f
                        star.x = Random.nextFloat() * areaWidth
                    }
                }

                // Move bullets up
                bullets.forEach { bullet ->
                    bullet.y -= 34f
                }
                // Filter offscreen bullets
                bullets.removeAll { it.y < -50f }

                // Move boss projectiles down & sideways
                bossProjectiles.forEach { bp ->
                    bp.x += bp.vx
                    bp.y += bp.vy
                }
                // Filter offscreen boss projectiles
                bossProjectiles.removeAll { it.y > areaHeight + 50f || it.y < -50f || it.x < -50f || it.x > areaWidth + 50f }

                // Check collision of boss projectiles with user ship
                val collidedBossProjectiles = mutableListOf<BossProjectile>()
                bossProjectiles.forEach { bp ->
                    val dx = bp.x - shipX
                    val dy = bp.y - shipY
                    val distSq = dx * dx + dy * dy
                    val hitRadius = 38f
                    if (distSq < hitRadius * hitRadius) {
                        collidedBossProjectiles.add(bp)
                    }
                }
                if (collidedBossProjectiles.isNotEmpty()) {
                    bossProjectiles.removeAll(collidedBossProjectiles)
                    damageFlashCount = 2
                    hearts = (hearts - collidedBossProjectiles.size).coerceAtLeast(0)
                    SoundSynthesizer.playBoing()
                    triggerHaptic()

                    collidedBossProjectiles.forEach { bp ->
                        triggerSparksBurst(
                            x = shipX,
                            y = shipY,
                            color = Color.Red,
                            count = 7,
                            emojis = listOf("💥", "🔥", "⚡", "🔮"),
                            nextIdTracker = { nextId++ },
                            onAddParticle = { particles.add(it) }
                        )
                    }

                    if (hearts <= 0) {
                        isGameOver = true
                        saveHighScore(gameScore)
                    }
                }

                // Move enemies down or apply boss patterns
                val bottomThreshold = areaHeight - 60f
                val reachedBottom = mutableListOf<ShooterEnemy>()
                val collidedWithShip = mutableListOf<ShooterEnemy>()
                enemies.forEach { enemy ->
                    if (enemy.isBoss) {
                        // Boss Movement: Wave oscillation near the top of screen
                        val hoverTime = System.currentTimeMillis() * 0.0012
                        // Smoothly ease standard baseX left/right
                        enemy.baseX = (areaWidth / 2f) + sin(hoverTime).toFloat() * (areaWidth / 2f - enemy.size - 20f)
                        enemy.y = 120f + cos(hoverTime * 1.6).toFloat() * 30f
                        enemy.x = enemy.baseX.coerceIn(enemy.size, areaWidth - enemy.size)
                        
                        // Check collision with ship itself
                        val dx = enemy.x - shipX
                        val dy = enemy.y - shipY
                        val distSq = dx * dx + dy * dy
                        val shipCollisionRadius = enemy.size * 0.5f + 25f
                        if (distSq < shipCollisionRadius * shipCollisionRadius) {
                            collidedWithShip.add(enemy)
                        }
                    } else {
                        enemy.y += enemy.speed
                        
                        // Swing left and right (satisfying zig-zag waves)
                        val waveX = sin(enemy.y * enemy.angleFrequency + enemy.hoverOffset) * enemy.angleAmplitude
                        
                        // Soft horizontal drift tracker towards the user's spacecraft
                        val hoverPull = 0.065f * level.coerceAtMost(6)
                        if (shipX > enemy.baseX) {
                            enemy.baseX += hoverPull * enemy.speed
                        } else if (shipX < enemy.baseX) {
                            enemy.baseX -= hoverPull * enemy.speed
                        }
                        
                        enemy.x = (enemy.baseX + waveX).coerceIn(enemy.size / 2f, areaWidth - enemy.size / 2f)
                        
                        // Check collision with ship itself
                        val dx = enemy.x - shipX
                        val dy = enemy.y - shipY
                        val distSq = dx * dx + dy * dy
                        val shipCollisionRadius = 45f // spaceship size matches perfectly
                        if (distSq < shipCollisionRadius * shipCollisionRadius) {
                            collidedWithShip.add(enemy)
                        } else if (enemy.y >= bottomThreshold) {
                            reachedBottom.add(enemy)
                        }
                    }
                }
                // Boss is persistent, cannot leave bottom of map
                enemies.removeAll(reachedBottom)
                enemies.removeAll(collidedWithShip)

                // Handle damage penalty for leakage or ship crash
                if (reachedBottom.isNotEmpty() || collidedWithShip.isNotEmpty()) {
                    damageFlashCount = 2
                    val damageAmount = reachedBottom.size + collidedWithShip.size
                    hearts = (hearts - damageAmount).coerceAtLeast(0)
                    SoundSynthesizer.playBoing()
                    triggerHaptic()

                    // Visual sparks at ship crash coordinates
                    collidedWithShip.forEach { collidedEnemy ->
                        triggerSparksBurst(
                            x = shipX,
                            y = shipY,
                            color = Color.Red,
                            count = 8,
                            emojis = listOf("💥", "🔥", "💔", "⚡"),
                            nextIdTracker = { nextId++ },
                            onAddParticle = { particles.add(it) }
                        )
                    }

                    if (hearts <= 0) {
                        isGameOver = true
                        saveHighScore(gameScore)
                    }
                }

                // Move particles
                particles.forEach { p ->
                    p.x += p.vx
                    p.y += p.vy
                    p.alpha -= 0.04f
                    p.rotation += p.rotSpeed
                }
                particles.removeAll { it.alpha <= 0f }

                // Update text blast labels
                blastLabels.forEach { label ->
                    label.age += 1
                    label.y -= 1.5f
                }
                blastLabels.removeAll { it.age >= it.maxAge }

                // Bullet <-> Enemy Collision check
                val hitEnemies = mutableListOf<ShooterEnemy>()
                val hitBullets = mutableListOf<ShooterBullet>()

                bullets.forEach { bullet ->
                    enemies.forEach { enemy ->
                        val dx = bullet.x - enemy.x
                        val dy = bullet.y - enemy.y
                        val distSq = dx * dx + dy * dy
                        val collisionRadius = enemy.size + 15f
                        if (distSq < collisionRadius * collisionRadius) {
                            hitBullets.add(bullet)
                            
                            // Highly colorful bullet hit impact explosion right at the exact coordinate of impact!
                            triggerSparksBurst(
                                x = bullet.x,
                                y = bullet.y,
                                color = Color(0xFF03A9F4),
                                count = 8,
                                emojis = listOf("✨", "⭐", "💥", "⚡", "🔥", "🔮", "☄️", "💫"),
                                nextIdTracker = { nextId++ },
                                onAddParticle = { particles.add(it) }
                            )
                            
                            enemy.hp--
                            if (enemy.hp <= 0) {
                                hitEnemies.add(enemy)
                            } else {
                                triggerHaptic()
                                SoundSynthesizer.playPop()
                            }
                        }
                    }
                }

                bullets.removeAll(hitBullets)

                // Process absolute enemy death & triggers (animations!)
                hitEnemies.forEach { enemy ->
                    if (enemy.isBoss) {
                        gameScore += 100 // Huge score reward!
                        level = (gameScore / 100) + 1

                        // Enormous explosion!
                        triggerSparksBurst(
                            x = enemy.x,
                            y = enemy.y,
                            color = Color.Yellow,
                            count = 25, // massive particles!
                            emojis = listOf("✨", "⭐", "🎉", "🔥", "🎖️", "🍭", "🍬", "🎈", "🎁"),
                            nextIdTracker = { nextId++ },
                            onAddParticle = { particles.add(it) }
                        )

                        blastLabels.add(
                            ShooterBlastLabel(
                                id = nextId++,
                                x = enemy.x,
                                y = enemy.y,
                                text = "🏆 BOSS DEFEATED! 🏆",
                                age = 0,
                                maxAge = 80,
                                color = Color(0xFF00E676),
                                scaleMultiplier = 1.4f
                            )
                        )
                        SoundSynthesizer.playChime()
                        triggerHaptic()
                    } else {
                        gameScore += 10
                        level = (gameScore / 100) + 1

                        // A satisfying huge explosion burst of sparkles and emoji candies!
                        triggerSparksBurst(
                            x = enemy.x,
                            y = enemy.y,
                            color = Color(0xFFE91E63),
                            count = 18,
                            emojis = listOf("✨", "⭐", "💥", "🍬", "🍭", "🎁", "🔥", "🔮", "🌈", "☄️", "💫", "🌟"),
                            nextIdTracker = { nextId++ },
                            onAddParticle = { particles.add(it) }
                        )

                        // Creative colorful battle popout text label at collision site!
                        val popLabel = listOf("WOW!", "POP!", "BOOM!", "ZAP!", "BANG!", "OOH!", "YES!").random()
                        val labelColor = listOf(
                            Color(0xFFFFEB3B), Color(0xFF00E676), Color(0xFF00B0FF),
                            Color(0xFFFF1744), Color(0xFFFF8F00), Color(0xFFD500F9)
                        ).random()

                        blastLabels.add(
                            ShooterBlastLabel(
                                id = nextId++,
                                x = enemy.x,
                                y = enemy.y - 20f,
                                text = popLabel,
                                age = 0,
                                maxAge = 25,
                                color = labelColor,
                                scaleMultiplier = 0.8f + Random.nextFloat() * 0.7f
                            )
                        )

                        // Play pop
                        SoundSynthesizer.playPop()
                        triggerHaptic()
                    }
                }

                enemies.removeAll(hitEnemies)

                // 2. Continuous Automatic shooting ticker
                val currentMilli = System.currentTimeMillis()
                val fireCooldown = 320L - (level * 10L).coerceIn(0L, 100L) // shoots faster as level goes up!
                if (currentMilli - lastAutoFireTime >= fireCooldown) {
                    bullets.add(
                        ShooterBullet(
                            id = nextId++,
                            x = shipX,
                            y = shipY - 30f
                        )
                    )
                    lastAutoFireTime = currentMilli
                }

                // 3. Spawners: Boss vs Normal
                val containsBoss = enemies.any { it.isBoss }
                val isBossLevel = (level % 5 == 0)

                if (isBossLevel) {
                    // Boss Encounter Trigger!
                    if (!containsBoss && lastSpawnedBossLevel != level) {
                        lastSpawnedBossLevel = level
                        // Clear active enemies and existing dangerous projectiles
                        enemies.clear()
                        bossProjectiles.clear()

                        val bossEmojis = listOf("👑👽", "👹", "👾", "🛸", "🐙", "💀", "🐉", "🤖")
                        val bossEmoji = bossEmojis.random()
                        val bossMaxHp = 10 + level * 2

                        enemies.add(
                            ShooterEnemy(
                                id = nextId++,
                                x = areaWidth / 2f,
                                y = -80f,
                                emoji = bossEmoji,
                                speed = 1.0f,
                                size = 110f,
                                hp = bossMaxHp,
                                isBoss = true,
                                maxHp = bossMaxHp
                            )
                        )

                        blastLabels.add(
                            ShooterBlastLabel(
                                id = nextId++,
                                x = areaWidth / 2f,
                                y = areaHeight / 2f - 100f,
                                text = "⚠️ ALERT: BOSS DETECTED! ⚠️",
                                age = 0,
                                maxAge = 80,
                                color = Color(0xFFFF1744),
                                scaleMultiplier = 1.4f
                            )
                        )
                        SoundSynthesizer.playBoing()
                    }

                    // Boss periodic shooting loops
                    val activeBoss = enemies.firstOrNull { it.isBoss }
                    if (activeBoss != null) {
                        val attackCooldown = 1300L - (level * 40L).coerceIn(0L, 500L)
                        if (currentMilli - lastBossAttackTime >= attackCooldown) {
                            lastBossAttackTime = currentMilli

                            val pattern = Random.nextInt(3)
                            val spawnedProjectiles = mutableListOf<BossProjectile>()

                            when (pattern) {
                                0 -> { // Fast Homing fireball
                                    val angle = kotlin.math.atan2(shipY - activeBoss.y, shipX - activeBoss.x)
                                    val speed = 8f + level * 0.4f
                                    spawnedProjectiles.add(
                                        BossProjectile(
                                            id = nextId++,
                                            x = activeBoss.x,
                                            y = activeBoss.y + 35f,
                                            vx = cos(angle) * speed,
                                            vy = sin(angle) * speed,
                                            emoji = "⚡",
                                            size = 35f
                                        )
                                    )
                                }
                                1 -> { // Triple spread shots
                                    val vy = 7f + level * 0.3f
                                    val emojis = listOf("🔥", "☄️", "🔮")
                                    val selectedEmoji = emojis.random()
                                    spawnedProjectiles.add(
                                        BossProjectile(id = nextId++, x = activeBoss.x, y = activeBoss.y + 35f, vx = -3.5f, vy = vy, emoji = selectedEmoji, size = 32f)
                                    )
                                    spawnedProjectiles.add(
                                        BossProjectile(id = nextId++, x = activeBoss.x, y = activeBoss.y + 35f, vx = 0f, vy = vy + 1.2f, emoji = selectedEmoji, size = 36f)
                                    )
                                    spawnedProjectiles.add(
                                        BossProjectile(id = nextId++, x = activeBoss.x, y = activeBoss.y + 35f, vx = 3.5f, vy = vy, emoji = selectedEmoji, size = 32f)
                                    )
                                }
                                2 -> { // Dual bouncing mini-orbs
                                    val vy = 6f + level * 0.3f
                                    val selectedEmoji = "🫧"
                                    spawnedProjectiles.add(
                                        BossProjectile(id = nextId++, x = activeBoss.x - 30f, y = activeBoss.y + 35f, vx = -1.8f, vy = vy, emoji = selectedEmoji, size = 30f)
                                    )
                                    spawnedProjectiles.add(
                                        BossProjectile(id = nextId++, x = activeBoss.x + 30f, y = activeBoss.y + 35f, vx = 1.8f, vy = vy, emoji = selectedEmoji, size = 30f)
                                    )
                                }
                            }
                            bossProjectiles.addAll(spawnedProjectiles)
                            SoundSynthesizer.playPop()
                        }
                    }
                } else {
                    // Standard level spawning (no active boss)
                    val spawnCooldown = (1200f / (1.0f + level * 0.20f)).toLong().coerceIn(300L, 1600L)
                    if (currentMilli - lastSpawnTime >= spawnCooldown) {
                        val alienEmojis = listOf("👾", "🛸", "👾", "🐙", "🪐", "🎈", "😈", "🤖", "⭐", "🔮")
                        
                        // Dynamically scale the spawned squad size based on level
                        val minCount = if (level >= 3) 2 else 1
                        val maxCount = if (level >= 5) 4 else if (level >= 3) 3 else 2
                        val countToSpawn = Random.nextInt(minCount, maxCount + 1)
                        
                        for (i in 0 until countToSpawn) {
                            val speed = 3.6f + (level * 0.35f) + Random.nextFloat() * 1.8f
                            val size = 50f + Random.nextFloat() * 30f
                            val alienHp = if (size > 68f && level >= 2) 2 else 1
                            
                            // Stagger horizontally and vertically so they spread out gracefully
                            val randOffsetX = -45f + Random.nextFloat() * 90f
                            val randOffsetY = -65f * i
                            
                            enemies.add(
                                ShooterEnemy(
                                    id = nextId++,
                                    x = (50f + Random.nextFloat() * (areaWidth - 100f) + randOffsetX).coerceIn(40f, areaWidth - 40f),
                                    y = -50f + randOffsetY,
                                    emoji = alienEmojis.random(),
                                    speed = speed.coerceAtMost(13.5f),
                                    size = size,
                                    hp = alienHp
                                )
                            )
                        }
                        lastSpawnTime = currentMilli
                    }
                }
            }
            delay(16) // tick lock
        }
    }

    // Double-flash decay routine for taking damage
    LaunchedEffect(damageFlashCount) {
        if (damageFlashCount > 0) {
            delay(100)
            damageFlashCount--
        }
    }

    // Reset Game parameters
    val handleRestart = {
        bullets.clear()
        enemies.clear()
        bossProjectiles.clear()
        particles.clear()
        blastLabels.clear()
        gameScore = 0
        hearts = 3
        level = 1
        lastSpawnedBossLevel = 0
        isGameOver = false
        isPaused = false
        shipX = areaWidth / 2f
        targetShipX = areaWidth / 2f
        shipY = areaHeight - 120f
        targetShipY = areaHeight - 120f
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F0C20), // Dark deep starry night blue
                        Color(0xFF201335), // Playful midnight violet
                        Color(0xFF140D24)
                    )
                )
            )
    ) {
        areaWidth = constraints.maxWidth.toFloat()
        areaHeight = constraints.maxHeight.toFloat()

        val shipDiameter = 62.dp
        val pDens = LocalDensity.current

        // Canvas for Rendering all visual elements (Background Stars, Bullets, Ships, Particles)
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    // Touch inputs coordinate tracker to slide/teleport spaceship in all directions (horizontal, vertical, diagonal)
                    detectDragGestures(
                        onDragStart = { offset ->
                            targetShipX = offset.x
                            targetShipY = offset.y
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            targetShipX = change.position.x
                            targetShipY = change.position.y
                        }
                    )
                }
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = { offset ->
                            targetShipX = offset.x
                            targetShipY = offset.y
                        }
                    )
                }
        ) {
            // Draw warp background stars
            stars.forEach { star ->
                drawCircle(
                    color = Color.White.copy(alpha = star.alpha),
                    radius = 3f + (star.speed * 0.4f),
                    center = Offset(star.x, star.y)
                )
            }

            // Draw glowing colorful laser bullets (retro design!)
            bullets.forEach { b ->
                drawCircle(
                    color = Color(0xFF00FFFF), // Cyan core
                    radius = 8f,
                    center = Offset(b.x, b.y)
                )
                drawCircle(
                    color = Color(0xFF00FFCC).copy(alpha = 0.5f), // Outer neon vibe
                    radius = 16f,
                    center = Offset(b.x, b.y)
                )
            }

            // Render interactive emojis for Enemies, Flying Particles, and Blast Text
            drawIntoCanvas { canvas ->
                val paintObj = android.graphics.Paint().apply {
                    textAlign = android.graphics.Paint.Align.CENTER
                }

                // Render enemies
                enemies.forEach { enemy ->
                    paintObj.textSize = enemy.size
                    
                    if (enemy.isBoss) {
                        // Drawing epic fiery red and golden orange HP bar for the Boss!
                        val barWidth = enemy.size * 1.3f
                        val barHeight = 11f
                        val bx = enemy.x - barWidth / 2f
                        val by = enemy.y - (enemy.size * 0.75f)
                        canvas.nativeCanvas.drawRect(
                            bx, by, bx + barWidth, by + barHeight,
                            android.graphics.Paint().apply { color = android.graphics.Color.BLACK }
                        )
                        canvas.nativeCanvas.drawRect(
                            bx, by, bx + (barWidth * (enemy.hp.toFloat() / enemy.maxHp.toFloat())), by + barHeight,
                            android.graphics.Paint().apply { color = android.graphics.Color.HSVToColor(floatArrayOf(15f, 1f, 1f)) } // Orange/Golden glow
                        )
                    } else if (enemy.hp > 1) {
                        // Regular tough enemies
                        val barWidth = enemy.size * 0.8f
                        val barHeight = 8f
                        val bx = enemy.x - barWidth / 2f
                        val by = enemy.y - (enemy.size * 0.7f)
                        canvas.nativeCanvas.drawRect(
                            bx, by, bx + barWidth, by + barHeight,
                            android.graphics.Paint().apply { color = android.graphics.Color.DKGRAY }
                        )
                        canvas.nativeCanvas.drawRect(
                            bx, by, bx + (barWidth * (enemy.hp.toFloat() / 2f)), by + barHeight,
                            android.graphics.Paint().apply { color = android.graphics.Color.GREEN }
                        )
                    }

                    canvas.nativeCanvas.drawText(
                        enemy.emoji,
                        enemy.x,
                        enemy.y + (enemy.size / 3f),
                        paintObj
                    )
                }

                // Render active boss projectiles
                bossProjectiles.forEach { bp ->
                    paintObj.textSize = bp.size
                    canvas.nativeCanvas.drawText(
                        bp.emoji,
                        bp.x,
                        bp.y + (bp.size / 3f),
                        paintObj
                    )
                }

                // Render particle debris from hits
                particles.forEach { p ->
                    paintObj.textSize = p.size
                    paintObj.alpha = (p.alpha * 255f).coerceIn(0f, 255f).toInt()
                    
                    canvas.save()
                    canvas.nativeCanvas.rotate(p.rotation, p.x, p.y)
                    canvas.nativeCanvas.drawText(
                        p.emoji,
                        p.x,
                        p.y + (p.size / 3f),
                        paintObj
                    )
                    canvas.restore()
                }

                // Render cartoon floating comic feedback texts ("WOW!", "POP!", etc.)
                blastLabels.forEach { label ->
                    val progress = label.age.toFloat() / label.maxAge.toFloat()
                    val scaleFactor = label.scaleMultiplier * (1f + progress * 0.3f)
                    
                    val nativePaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(
                            ((1f - progress) * 255f).toInt().coerceIn(0, 255),
                            (label.color.red * 255f).toInt(),
                            (label.color.green * 255f).toInt(),
                            (label.color.blue * 255f).toInt()
                        )
                        textSize = 34f * scaleFactor
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        textAlign = android.graphics.Paint.Align.CENTER
                    }

                    canvas.nativeCanvas.drawText(
                        label.text,
                        label.x,
                        label.y,
                        nativePaint
                    )
                }
            }
        }

        // Spaceship Icon overlays seamlessly (floating 🚀 with dynamic size & drag instructions)
        Box(
            modifier = Modifier
                .offset(
                    x = with(pDens) { (shipX).toDp() } - (shipDiameter / 2),
                    y = with(pDens) { (shipY).toDp() } - (shipDiameter / 2)
                )
                .size(shipDiameter)
                .shadow(elevation = 12.dp, shape = CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE040FB), Color(0xFF673AB7)),
                        radius = 120f
                    ),
                    shape = CircleShape
                )
                .border(2.dp, Color.White.copy(alpha = 0.8f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "🚀",
                fontSize = 32.sp,
                modifier = Modifier.scale(if (bullets.isNotEmpty()) 1.1f else 1.0f)
            )
        }

        // Damage warning Flash Overlay
        if (damageFlashCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Red.copy(alpha = 0.25f))
            )
        }

        // 2. HUD: Header overlay, Scores, Streak counters, Help
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Return to Main Menu Back button
                IconButton(
                    onClick = {
                        isPaused = true
                        viewModel.setScreen(Screen.MENU)
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color.White.copy(alpha = 0.25f), CircleShape)
                        .testTag("spacer_shooter_exit_back")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Leave space shooter",
                        tint = Color.White
                    )
                }

                // Heart/Shield indicators and Level indicator bubble
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFE040FB).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFFE040FB), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "LEVEL $level",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            for (h in 1..3) {
                                Text(
                                    text = if (hearts >= h) "❤️" else "🖤",
                                    fontSize = 14.sp,
                                    modifier = Modifier.scale(if (hearts == h - 1 && damageFlashCount > 0) 1.25f else 1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Score and Highscore indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Score Ticker details
                Text(
                    text = "SCORE: $gameScore",
                    color = Color(0xFFFFEB3B),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    style = TextStyle(
                        shadow = Shadow(Color.Black, Offset(2f, 2f), 4f)
                    )
                )

                // High score badge
                Text(
                    text = "🏆 HIGH: $spaceHighScore",
                    color = Color(0xFF81C784),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    style = TextStyle(
                        shadow = Shadow(Color.Black, Offset(1.5f, 1.5f), 3f)
                    )
                )
            }

            // Interactive "Drag to Move" Help banner (fades out as score progresses)
            if (gameScore < 40) {
                Text(
                    text = "👇 Drag/Hold anywhere on the screen below to guide your Spaceship!",
                    textAlign = TextAlign.Center,
                    color = Color.White.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                )
            }
        }

        // 3. OVERLAY: Game Over Dialog Overlay
        AnimatedVisibility(
            visible = isGameOver,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f))
                    .clickable(enabled = true, onClick = {}), // Trap clicks
                contentAlignment = Alignment.Center
            ) {
                Card(
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1035)),
                    border = androidx.compose.foundation.BorderStroke(4.dp, Color(0xFFE040FB)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 24.dp),
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 350.dp)
                        .testTag("space_game_over_card")
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "🌌 MISSION COMPLETED! 🌌",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.Cyan,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "🚀👶🕹️",
                            fontSize = 48.sp,
                            textAlign = TextAlign.Center
                        )

                        Text(
                            text = "You space-blasted the bubbles and saved the day!",
                            fontSize = 14.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center
                        )

                        Divider(color = Color.White.copy(alpha = 0.15f))

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FINAL SCORE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                            Text(
                                text = "$gameScore PTS",
                                fontSize = 38.sp,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFFFFEB3B)
                            )
                        }

                        // High Score verification update
                        if (gameScore >= spaceHighScore && gameScore > 0) {
                            Box(
                                modifier = Modifier
                                    .background(Color(0xFF4CAF50), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "👑 NEW PERSONAL RECORD! 👑",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color.White
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Actions row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Button(
                                onClick = { viewModel.setScreen(Screen.MENU) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White.copy(alpha = 0.15f),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = "MENU",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }

                            Button(
                                onClick = { handleRestart() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE040FB),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = "REPLAY 🔄",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// Particle splash generator on hits
fun triggerSparksBurst(
    x: Float,
    y: Float,
    color: Color,
    count: Int,
    emojis: List<String>,
    nextIdTracker: () -> Long,
    onAddParticle: (ShooterParticle) -> Unit
) {
    for (i in 0 until count) {
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        val speed = 2f + Random.nextFloat() * 10f
        val vx = cos(angle) * speed
        val vy = sin(angle) * speed - 2f // biased upward slightly like standard debris
        val sparkEmoji = emojis.random()
        val size = 20f + Random.nextFloat() * 22f

        onAddParticle(
            ShooterParticle(
                id = nextIdTracker(),
                x = x,
                y = y,
                vx = vx,
                vy = vy,
                emoji = sparkEmoji,
                size = size,
                alpha = 1.0f,
                rotation = Random.nextFloat() * 360f,
                rotSpeed = -8f + Random.nextFloat() * 16f
            )
        )
    }
}
