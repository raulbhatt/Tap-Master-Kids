package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.GameDatabase
import com.example.data.database.ScoreRecord
import com.example.data.repository.GameRepository
import com.example.ui.audio.SoundSynthesizer
import com.example.ui.models.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.UUID
import kotlin.math.hypot
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: GameRepository

    init {
        val database = GameDatabase.getDatabase(application)
        repository = GameRepository(database.scoreDao)
    }

    // High Scores list observed directly from Room Database
    val scoreHistory: StateFlow<List<ScoreRecord>> = repository.allScores
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _currentScreen = MutableStateFlow(Screen.MENU)
    val currentScreen: StateFlow<Screen> = _currentScreen.asStateFlow()

    private val _selectedMode = MutableStateFlow("classic") // "classic" or "color_match"
    val selectedMode: StateFlow<String> = _selectedMode.asStateFlow()

    private val _selectedTheme = MutableStateFlow("meadow") // "meadow", "space", "underwater"
    val selectedTheme: StateFlow<String> = _selectedTheme.asStateFlow()

    // Game Core State
    private val _score = MutableStateFlow(0)
    val score: StateFlow<Int> = _score.asStateFlow()

    private val _gameLevel = MutableStateFlow(1)
    val gameLevel: StateFlow<Int> = _gameLevel.asStateFlow()

    private val _levelStartScore = MutableStateFlow(0)
    val levelStartScore: StateFlow<Int> = _levelStartScore.asStateFlow()

    private val _showLevelUpBoard = MutableStateFlow(false)
    val showLevelUpBoard: StateFlow<Boolean> = _showLevelUpBoard.asStateFlow()

    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    private val _isGamePaused = MutableStateFlow(false)
    val isGamePaused: StateFlow<Boolean> = _isGamePaused.asStateFlow()

    private val _timeLeft = MutableStateFlow(45f) // 45-second level timer
    val timeLeft: StateFlow<Float> = _timeLeft.asStateFlow()

    private val _isFeverActive = MutableStateFlow(false)
    val isFeverActive: StateFlow<Boolean> = _isFeverActive.asStateFlow()

    private val _feverCharge = MutableStateFlow(0f) // 0.0f to 1.0f (reaches fever at 5 streak hits)
    val feverCharge: StateFlow<Float> = _feverCharge.asStateFlow()

    // In educational mode, the active target rule
    private val _activeColorPrompt = MutableStateFlow<TargetColor?>(null)
    val activeColorPrompt: StateFlow<TargetColor?> = _activeColorPrompt.asStateFlow()

    private val _activeShapePrompt = MutableStateFlow<TargetShape?>(null)
    val activeShapePrompt: StateFlow<TargetShape?> = _activeShapePrompt.asStateFlow()

    private val _spawnedTargets = MutableStateFlow<List<ActiveTarget>>(emptyList())
    val spawnedTargets: StateFlow<List<ActiveTarget>> = _spawnedTargets.asStateFlow()

    private val _activeEffects = MutableStateFlow<List<TapEffect>>(emptyList())
    val activeEffects: StateFlow<List<TapEffect>> = _activeEffects.asStateFlow()

    private val _lastFinishedScore = MutableStateFlow(0)
    val lastFinishedScore: StateFlow<Int> = _lastFinishedScore.asStateFlow()

    private val _lastFinishedStars = MutableStateFlow(0)
    val lastFinishedStars: StateFlow<Int> = _lastFinishedStars.asStateFlow()

    private val _maxSessionStreak = MutableStateFlow(0)
    val maxSessionStreak: StateFlow<Int> = _maxSessionStreak.asStateFlow()

    private val _lastFinishedMaxStreak = MutableStateFlow(0)
    val lastFinishedMaxStreak: StateFlow<Int> = _lastFinishedMaxStreak.asStateFlow()

    // Interactive unlocked stickers
    private val _stickers = MutableStateFlow<List<Sticker>>(emptyList())
    val stickers: StateFlow<List<Sticker>> = _stickers.asStateFlow()

    private val _incorrectMatchEvent = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val incorrectMatchEvent = _incorrectMatchEvent.asSharedFlow()

    private var gameLoopJob: Job? = null
    private var lastSpawnTime = 0L
    private var lastPromptChangeTime = 0L
    private var feverEndTime = 0L
    private var promptsCompletedCount = 0

    init {
        updateStickers()
        // Listen to database changes to refresh sticker locks instantly!
        viewModelScope.launch {
            scoreHistory.collect {
                updateStickers()
            }
        }
    }

    fun setScreen(screen: Screen) {
        _currentScreen.value = screen
        if (screen == Screen.MENU) {
            stopGameLoop()
        }
    }

    fun selectTheme(themeId: String) {
        _selectedTheme.value = themeId
        SoundSynthesizer.playPop()
    }

    fun startGame(mode: String) {
        _selectedMode.value = mode
        _score.value = 0
        _gameLevel.value = 1
        _levelStartScore.value = 0
        _showLevelUpBoard.value = false
        _streak.value = 0
        _maxSessionStreak.value = 0
        _isGamePaused.value = false
        _timeLeft.value = 45f
        _isFeverActive.value = false
        _feverCharge.value = 0f
        _spawnedTargets.value = emptyList()
        _activeEffects.value = emptyList()
        promptsCompletedCount = 0

        if (mode == "color_match") {
            generateNewMatchingPrompt()
        } else {
            _activeColorPrompt.value = null
            _activeShapePrompt.value = null
        }

        _currentScreen.value = Screen.PLAYING
        startGameLoop()
        SoundSynthesizer.playChime()
    }

    private fun startGameLoop() {
        stopGameLoop()
        lastSpawnTime = System.currentTimeMillis()
        lastPromptChangeTime = System.currentTimeMillis()
        
        gameLoopJob = viewModelScope.launch {
            var lastTime = System.currentTimeMillis()
            while (true) {
                delay(16) // tick roughly at ~60fps
                val currentTime = System.currentTimeMillis()
                val elapsedSeconds = (currentTime - lastTime) / 1000f
                lastTime = currentTime

                if (_isGamePaused.value) {
                    val latencyMs = (elapsedSeconds * 1000f).toLong()
                    lastSpawnTime += latencyMs
                    lastPromptChangeTime += latencyMs
                    if (_isFeverActive.value) {
                        feverEndTime += latencyMs
                    }
                    _spawnedTargets.value = _spawnedTargets.value.map {
                        it.copy(
                            spawnTime = it.spawnTime + latencyMs,
                            popTime = if (it.isPopping) it.popTime + latencyMs else it.popTime
                        )
                    }
                    continue
                }

                // Game ticker logic
                updateGameTick(elapsedSeconds)
            }
        }
    }

    private fun stopGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = null
    }

    fun pauseGame() {
        if (_currentScreen.value == Screen.PLAYING) {
            _isGamePaused.value = true
            SoundSynthesizer.playPop()
        }
    }

    fun resumeGame() {
        _isGamePaused.value = false
        SoundSynthesizer.playPop()
    }

    fun proceedToNextLevel() {
        _gameLevel.value += 1
        _levelStartScore.value = _score.value
        _timeLeft.value = 45f
        _showLevelUpBoard.value = false
        _isGamePaused.value = false
        SoundSynthesizer.playChime()
    }

    fun restartCurrentLevel() {
        _score.value = _levelStartScore.value
        _timeLeft.value = 45f
        _showLevelUpBoard.value = false
        _isGamePaused.value = false
        _spawnedTargets.value = emptyList()
        SoundSynthesizer.playPop()
    }

    fun collectPrizeAndFinish() {
        _showLevelUpBoard.value = false
        onGameOver()
    }

    private fun triggerLevelCompletion() {
        _isGamePaused.value = true
        _showLevelUpBoard.value = true
        _spawnedTargets.value = emptyList()
        SoundSynthesizer.playChime()
    }

    private fun updateGameTick(elapsedSeconds: Float) {
        val currentT = System.currentTimeMillis()

        // 1. Update Timer
        val newTimeLeft = (_timeLeft.value - elapsedSeconds).coerceAtLeast(0f)
        _timeLeft.value = newTimeLeft

        if (newTimeLeft <= 0f) {
            triggerLevelCompletion()
            return
        }

        // 2. Handle Fever mode expiration
        if (_isFeverActive.value) {
            if (currentT >= feverEndTime) {
                _isFeverActive.value = false
                _feverCharge.value = 0f
            } else {
                // Decay fever meter visually
                val remainingRatio = (feverEndTime - currentT).toFloat() / 5000f
                _feverCharge.value = remainingRatio.coerceIn(0f, 1f)
            }
        }

        // 3. Dynamic education prompt rotating
        if (_selectedMode.value == "color_match") {
            if (currentT - lastPromptChangeTime > 12000) { // change target every 12s
                generateNewMatchingPrompt()
            }
        }

        // 4. Shrink and Fade expiring targets, remove dead ones
        val activeList = _spawnedTargets.value.mapNotNull { target ->
            val currentT = System.currentTimeMillis()
            if (target.isPopping) {
                val poppedElapsed = currentT - target.popTime
                if (poppedElapsed >= 250) {
                    null // fully burst, remove from screen
                } else {
                    val progress = poppedElapsed / 250f // 0f to 1f
                    // Playful spring/elastic burst curve: starts at 1.0, swells up rapidly, then shrinks to 0.0
                    val scaleFactor = if (progress < 0.4f) {
                        1f + (progress / 0.4f) * 0.4f // swells up to 1.4
                    } else {
                        1.4f - ((progress - 0.4f) / 0.6f) * 1.4f // collapses down to 0
                    }
                    val alphaFactor = (1f - progress).coerceIn(0f, 1f)
                    target.copy(
                        scale = scaleFactor,
                        alpha = alphaFactor
                    )
                }
            } else {
                val elapsed = currentT - target.spawnTime
                if (elapsed >= target.durationMs) {
                    null // let untouched targets fade out quietly
                } else {
                    // Entrance animation
                    val entranceMs = 300f
                    val scale = if (elapsed < entranceMs) {
                        elapsed / entranceMs
                    } else if (target.durationMs - elapsed < 300) {
                        (target.durationMs - elapsed) / 300f
                    } else {
                        1f
                    }
                    
                    // Animated gentle waving/wiggling
                    val wiggle = kotlin.math.sin((currentT % 2000) / 2000f * 2.0 * Math.PI).toFloat() * 1.5f

                    target.copy(
                        scale = scale,
                        alpha = if (scale < 1f) scale else 1f,
                        wiggleOffset = wiggle
                    )
                }
            }
        }
        _spawnedTargets.value = activeList

        // 5. Spawn new targets
        val currentLevel = _gameLevel.value
        val levelScoreBase = _levelStartScore.value
        val pointsIntoLevel = (_score.value - levelScoreBase).coerceAtLeast(0)
        val levelProgress = (pointsIntoLevel / 80f).coerceIn(0f, 2f)
        val continuousLevel = currentLevel + levelProgress

        // Max targets scales up from 3 up to 5 as level progresses
        val maxTargets = if (_selectedMode.value == "reflex") {
            if (currentLevel >= 5) 6 else 4
        } else if (_isFeverActive.value) {
            5
        } else if (currentLevel >= 6) {
            5
        } else if (currentLevel >= 3) {
            4
        } else {
            3
        }

        // Spawn interval gets continuously shorter as level/continuous progress scales up
        val spawnInterval = if (_selectedMode.value == "reflex") {
            (1100f / (0.9f + continuousLevel * 0.35f)).toLong().coerceIn(300L, 1000L)
        } else if (_isFeverActive.value) {
            450L
        } else {
            (2100f / (0.9f + continuousLevel * 0.25f)).toLong().coerceIn(500L, 2000L)
        }

        if (activeList.size < maxTargets && (currentT - lastSpawnTime >= spawnInterval)) {
            spawnNewTarget()
        }

        // 6. Update particle effects (gravity & translation)
        updateParticlesAndEffects()
    }

    private fun spawnNewTarget() {
        // Random layout coordinates inside safe central margins (0.08 to 0.92 x, 0.18 to 0.82 y)
        var bestX = 0.5f
        var bestY = 0.5f
        var foundSpot = false

        for (attempt in 1..10) {
            val candidateX = Random.nextFloat() * 0.64f + 0.18f
            val candidateY = Random.nextFloat() * 0.54f + 0.23f

            // Overlap detection
            var overlaps = false
            for (t in _spawnedTargets.value) {
                val distance = hypot(candidateX - t.xPercent, candidateY - t.yPercent)
                if (distance < 0.18f) {
                    overlaps = true
                    break
                }
            }
            if (!overlaps) {
                bestX = candidateX
                bestY = candidateY
                foundSpot = true
                break
            }
        }

        if (!foundSpot && _spawnedTargets.value.isNotEmpty()) {
            return // skip spawning this tick if congested
        }

        val shape = if (_selectedMode.value == "reflex") {
            listOf(TargetShape.CIRCLE, TargetShape.SQUARE, TargetShape.TRIANGLE).random()
        } else {
            TargetShape.values().random()
        }
        val color = TargetColor.values().random()

        // 1. Roll for target type with kid-friendly percentages
        val typeRoll = Random.nextFloat()
        val targetType = when {
            _isFeverActive.value -> com.example.ui.models.TargetType.NORMAL
            typeRoll < 0.08f -> com.example.ui.models.TargetType.CLOCK     // 8% Alarm Clock (+5s time bonus)
            typeRoll < 0.16f -> com.example.ui.models.TargetType.BOMB      // 8% Party Popper Bomb (clears screen)
            typeRoll < 0.24f -> com.example.ui.models.TargetType.GOLD_STAR  // 8% High-Score Star (+30 points)
            else -> com.example.ui.models.TargetType.NORMAL
        }

        // Base target sizes shrink slightly as level/difficulty progresses (from 102dp down to 72dp)
        val currentLevel = _gameLevel.value
        val levelScoreBase = _levelStartScore.value
        val pointsIntoLevel = (_score.value - levelScoreBase).coerceAtLeast(0)
        val levelProgress = (pointsIntoLevel / 80f).coerceIn(0f, 2f)
        val continuousLevel = currentLevel + levelProgress

        val sizeDecreaseFactor = (continuousLevel - 1f) / 10f
        val targetSizeDp = if (targetType != com.example.ui.models.TargetType.NORMAL) {
            105f
        } else {
            (102f - (30f * sizeDecreaseFactor).coerceIn(0f, 30f))
        }

        // Shapes keep staying on screen for less duration as level progresses
        val defaultDurationMs = if (_selectedMode.value == "reflex") {
            // Highly challenging reflex speed: from 1500ms down to 600ms dynamically based on level
            (1800f / (1.1f + continuousLevel * 0.40f)).toLong().coerceIn(550L, 1600L)
        } else if (_isFeverActive.value) {
            2000L
        } else {
            // Continuous decay curve: Level 1 starts around 3600ms, decreasing continuously to ~1000ms at Level 10
            (4400f / (0.9f + continuousLevel * 0.32f)).toLong().coerceIn(900L, 3800L)
        }

        val lifetimeMs = if (targetType != com.example.ui.models.TargetType.NORMAL) {
            // Special targets duration decays, but always remains 1000ms higher than normal ones so they are easier to target
            defaultDurationMs + 1000L
        } else {
            defaultDurationMs
        }

        val newTarget = ActiveTarget(
            id = UUID.randomUUID().toString(),
            xPercent = bestX,
            yPercent = bestY,
            shape = shape,
            color = color,
            baseSizeDp = targetSizeDp,
            spawnTime = System.currentTimeMillis(),
            durationMs = lifetimeMs,
            targetType = targetType
        )

        _spawnedTargets.value = _spawnedTargets.value + newTarget
        lastSpawnTime = System.currentTimeMillis()
    }

    private fun generateNewMatchingPrompt() {
        val matchCategory = Random.nextBoolean() // true = match color, false = match shape
        if (matchCategory) {
            _activeColorPrompt.value = TargetColor.values().random()
            _activeShapePrompt.value = null
        } else {
            _activeShapePrompt.value = TargetShape.values().random()
            _activeColorPrompt.value = null
        }
        lastPromptChangeTime = System.currentTimeMillis()
        promptsCompletedCount = 0
    }

    fun onTapTarget(target: ActiveTarget) {
        if (_isGamePaused.value) return
        if (target.isPopping) return

        // Check if the tapped item is correct
        val isCorrect = if (_timeLeft.value <= 0) {
            false
        } else if (target.targetType != com.example.ui.models.TargetType.NORMAL) {
            true // Special targets are always correct and fun!
        } else if (_isFeverActive.value) {
            true // During Fever Mode, everything is ALWAYS correct! Double scores!
        } else if (_selectedMode.value == "classic" || _selectedMode.value == "reflex") {
            true
        } else {
            // Educational: match shape or color
            val colorMatch = _activeColorPrompt.value?.let { it == target.color } ?: true
            val shapeMatch = _activeShapePrompt.value?.let { it == target.shape } ?: true
            colorMatch && shapeMatch
        }

        // Tag item as popping to trigger instant visual size expand/fade out
        _spawnedTargets.value = _spawnedTargets.value.map {
            if (it.id == target.id) it.copy(isPopping = true, popTime = System.currentTimeMillis()) else it
        }

        if (isCorrect) {
            // 🎉 Success path
            SoundSynthesizer.playPop()

            val multiplier = if (_isFeverActive.value) 2 else 1
            var pointIncrement = 10 * multiplier
            var customText: String? = null

            when (target.targetType) {
                com.example.ui.models.TargetType.CLOCK -> {
                    // Add 5 seconds time bonus to the countdown!
                    _timeLeft.value = (_timeLeft.value + 5f).coerceAtMost(55f)
                    customText = "+5s TIME! ⏰"
                    SoundSynthesizer.playChime()
                }
                com.example.ui.models.TargetType.GOLD_STAR -> {
                    pointIncrement = 30 * multiplier
                    customText = "+$pointIncrement MEGA! 💫"
                    SoundSynthesizer.playChime()
                }
                com.example.ui.models.TargetType.BOMB -> {
                    customText = "BOOM! 💥"
                    SoundSynthesizer.playFever()
                    
                    // Pop all other targets in a magical popcorn waterfall!
                    viewModelScope.launch {
                        val otherTargets = _spawnedTargets.value.filter { it.id != target.id && !it.isPopping }
                        otherTargets.forEach { other ->
                            delay(120) // popcorn delay!
                            if (_timeLeft.value > 0) {
                                _spawnedTargets.value = _spawnedTargets.value.map {
                                    if (it.id == other.id) it.copy(isPopping = true, popTime = System.currentTimeMillis()) else it
                                }
                                SoundSynthesizer.playPop()
                                val extraInc = (if (other.targetType == com.example.ui.models.TargetType.GOLD_STAR) 30 else 10) * multiplier
                                _score.value += extraInc
                                triggerConfettiAndText(
                                    other.xPercent,
                                    other.yPercent,
                                    "+$extraInc",
                                    other.color.hexColor
                                )
                            }
                        }
                    }
                }
                else -> {}
            }

            val nextScore = _score.value + pointIncrement
            _score.value = nextScore

            // In matching mode, track correct hits to shift prompts dynamically for variety
            if (_selectedMode.value == "color_match" && !_isFeverActive.value) {
                promptsCompletedCount++
                if (promptsCompletedCount >= 3) {
                    generateNewMatchingPrompt()
                }
            }

            // High-contrast floating +10 or +20 text and massive colorful confetti explode
            val countText = customText ?: (if (_isFeverActive.value) "+$pointIncrement! FEVER!" else "+$pointIncrement")
            triggerConfettiAndText(
                target.xPercent,
                target.yPercent,
                countText,
                target.color.hexColor
            )

            // Dynamic Fever Accumulation (reaches maximum on 5 consecutive correct hits)
            if (!_isFeverActive.value) {
                val nextStreak = _streak.value + 1
                _streak.value = nextStreak
                _maxSessionStreak.value = maxOf(_maxSessionStreak.value, nextStreak)
                val charge = (nextStreak / 5f).coerceIn(0f, 1f)
                _feverCharge.value = charge

                if (nextStreak >= 5) {
                    triggerFeverMode()
                }
            } else {
                // keep streak counting
                _streak.value += 1
                _maxSessionStreak.value = maxOf(_maxSessionStreak.value, _streak.value)
            }

        } else {
            // ❌ Educational Incorrect Tap
            SoundSynthesizer.playBoing()
            _streak.value = 0 // reset streak
            _feverCharge.value = 0f
            _incorrectMatchEvent.tryEmit(Unit)

            // Trigger cartoon negative text ("Oops!") and subtle grey stars falling
            triggerConfettiAndText(
                target.xPercent,
                target.yPercent,
                "Try Again! ✨",
                0xFF888888
            )
        }
    }

    private fun triggerFeverMode() {
        _isFeverActive.value = true
        _streak.value = 5
        feverEndTime = System.currentTimeMillis() + 5000L // 5-second intense hyper mode
        SoundSynthesizer.playFever()

        // Mass celebratory splash confetti
        triggerConfettiAndText(0.5f, 0.4f, "🚀 FEVER MODE! 🚀", 0xFFFFE082)
    }

    private fun triggerConfettiAndText(x: Float, y: Float, text: String, color: Long) {
        val count = 22
        val vibrantColors = listOf(
            0xFFFFD54F, 0xFFFF4081, 0xFF00E5FF, 0xFFFF6F00,
            0xFF64DD17, 0xFFD500F9, 0xFF2979FF, 0xFFFF1744
        )
        val confettiParticles = List(count) { index ->
            val angle = Random.nextFloat() * 2.0 * Math.PI
            // Alternate between fast/slow particle layers to create a multi-layered ring explosion
            val layerMultiplier = if (index % 2 == 0) 1.2f else 0.7f
            val speed = (Random.nextFloat() * 0.016f + 0.007f) * layerMultiplier
            
            // Draw colorful rainbow burst + shape-matched colors
            val pColor = if (color == 0xFF888888) {
                0xFFCCCCCC
            } else if (Random.nextFloat() < 0.65f) {
                vibrantColors.random()
            } else {
                color
            }
            
            Particle(
                xPercent = x,
                yPercent = y,
                vx = (kotlin.math.cos(angle) * speed).toFloat(),
                vy = (kotlin.math.sin(angle) * speed - 0.002f).toFloat(), // push upward initially
                color = pColor,
                size = Random.nextFloat() * 14f + 7f, // size in dps
                alpha = 1.0f,
                rotation = Random.nextFloat() * 360f,
                vRotation = Random.nextFloat() * 16f - 8f
            )
        }

        val designEffect = TapEffect(
            id = UUID.randomUUID().toString(),
            xPercent = x,
            yPercent = y,
            text = text,
            color = color,
            particles = confettiParticles
        )

        _activeEffects.value = _activeEffects.value + designEffect
    }

    private fun updateParticlesAndEffects() {
        val currentEffects = _activeEffects.value
        if (currentEffects.isEmpty()) return

        val updated = currentEffects.mapNotNull { effect ->
            val nextAlpha = effect.alpha - 0.04f
            if (nextAlpha <= 0f) {
                null
            } else {
                val updatedParticles = effect.particles.mapNotNull { p ->
                    val nextPAlpha = p.alpha - 0.05f
                    if (nextPAlpha <= 0f) {
                        null
                    } else {
                        p.copy(
                            xPercent = (p.xPercent + p.vx).coerceIn(0f, 1f),
                            yPercent = (p.yPercent + p.vy).coerceIn(0f, 1f),
                            vy = p.vy + 0.0004f, // simple downward gravity in Canvas terms
                            alpha = nextPAlpha,
                            rotation = p.rotation + p.vRotation
                        )
                    }
                }
                effect.copy(
                    alpha = nextAlpha,
                    // slightly launch floating texts upwards
                    yPercent = effect.yPercent - 0.003f,
                    particles = updatedParticles
                )
            }
        }
        _activeEffects.value = updated
    }

    private fun saveMaxStreakToPrefs(streak: Int) {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("tap_master_prefs", android.content.Context.MODE_PRIVATE)
        val currentRecord = sharedPrefs.getInt("highest_combo_streak", 0)
        if (streak > currentRecord) {
            sharedPrefs.edit().putInt("highest_combo_streak", streak).apply()
        }
    }

    private fun onGameOver() {
        stopGameLoop()
        val totalPoints = _score.value
        val mode = _selectedMode.value

        // Determine Stars
        val stars = when {
            totalPoints >= 120 -> 3
            totalPoints >= 50 -> 2
            else -> 1 // Every kid gets at least 1 star! Forgiving & encouraging!
        }

        _lastFinishedScore.value = totalPoints
        _lastFinishedStars.value = stars
        _lastFinishedMaxStreak.value = _maxSessionStreak.value

        // Save max streak and dynamic reload of stickers
        saveMaxStreakToPrefs(_maxSessionStreak.value)
        updateStickers()

        // Save to Database asynchronously
        viewModelScope.launch {
            repository.saveScore(
                score = totalPoints,
                gameMode = mode,
                starsCount = stars
            )
        }

        _currentScreen.value = Screen.GAME_OVER
        SoundSynthesizer.playChime()
    }

    fun clearGameHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    private fun updateStickers() {
        val history = scoreHistory.value
        val totalStars = history.sumOf { it.starsCount }
        val maxSingleScore = history.maxOfOrNull { it.score } ?: 0
        val gamesPlayed = history.size
        val hasPlayedBoth = history.any { it.gameMode == "classic" } && history.any { it.gameMode == "color_match" }

        // Retrieve highest combo streak from local storage
        val sharedPrefs = getApplication<Application>().getSharedPreferences("tap_master_prefs", android.content.Context.MODE_PRIVATE)
        val highestComboStreak = sharedPrefs.getInt("highest_combo_streak", 0)

        val initialList = listOf(
            Sticker(
                id = "puppy",
                name = "Puppy Pal 🐶",
                emoji = "🐶",
                description = "Always here to support you! Wiggle-wag!",
                unlockCondition = "Unlocked automatically at start!"
            ),
            Sticker(
                id = "cat",
                name = "Magic Cat 🐱",
                emoji = "🐱",
                description = "Purrs and spells! Meow-gical!",
                unlockCondition = "Score 50 points or more in any game."
            ),
            Sticker(
                id = "unicorn",
                name = "Unicorn Sparkle 🦄",
                emoji = "🦄",
                description = "Prances across sky-high rainbows!",
                unlockCondition = "Earn 5 total stars across games."
            ),
            Sticker(
                id = "dino",
                name = "Dino Rex 🦖",
                emoji = "🦖",
                description = "Roars politely and eats leaf salads!",
                unlockCondition = "Play both Classic and Matching modes."
            ),
            Sticker(
                id = "astronaut",
                name = "Astro Bunny 🧑‍🚀",
                emoji = "🐰",
                description = "Floating gently in the cosmic carrot fields!",
                unlockCondition = "Aim high! Score 100 points in one game."
            ),
            Sticker(
                id = "octopus",
                name = "Sea Joy Octopus 🐙",
                emoji = "🐙",
                description = "Wiggle-tickling eight bubbles at a time!",
                unlockCondition = "Play 3 games or more."
            ),
            Sticker(
                id = "combo_fire",
                name = "Fire Sparkler 🔥",
                emoji = "🔥",
                description = "A fiery sparkler that sparkles wildly!",
                unlockCondition = "Reach a high combo of 7+ in a single level!"
            ),
            Sticker(
                id = "combo_lightning",
                name = "Lightning Ribbon ⚡",
                emoji = "⚡",
                description = "Crackles with extreme popping energy!",
                unlockCondition = "Reach an incredible combo of 12+ in a single level!"
            ),
            Sticker(
                id = "combo_king",
                name = "Sovereign Crown 👑",
                emoji = "👑",
                description = "The ultimate crown for legendary tap speed!",
                unlockCondition = "Reach a legendary combo of 18+ in a single level!"
            )
        )

        // Map lock/unlock state based on parameters
        _stickers.value = initialList.map { sticker ->
            val unlocked = when (sticker.id) {
                "puppy" -> true
                "cat" -> maxSingleScore >= 50
                "unicorn" -> totalStars >= 5
                "dino" -> hasPlayedBoth
                "astronaut" -> maxSingleScore >= 100
                "octopus" -> gamesPlayed >= 3
                "combo_fire" -> highestComboStreak >= 7
                "combo_lightning" -> highestComboStreak >= 12
                "combo_king" -> highestComboStreak >= 18
                else -> false
            }
            if (unlocked) {
                sticker.copy(unlockCondition = "UNLOCKED! 🎉")
            } else {
                sticker
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopGameLoop()
    }
}
