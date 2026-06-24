package com.example.ui.models

enum class Screen {
    MENU,
    PLAYING,
    GAME_OVER,
    TROPHY
}

enum class TargetShape(val emoji: String, val displayName: String) {
    CIRCLE("🎈", "Balloon"),
    STAR("⭐", "Star"),
    HEART("❤️", "Heart"),
    SQUARE("🎁", "Box"),
    TRIANGLE("📐", "Triangle")
}

enum class TargetColor(val displayName: String, val hexColor: Long, val emoji: String) {
    RED("RED 🔴", 0xFFFF5252, "🔴"),
    BLUE("BLUE 🔵", 0xFF2979FF, "🔵"),
    YELLOW("YELLOW 🟡", 0xFFFFD600, "🟡"),
    GREEN("GREEN 🟢", 0xFF00E676, "🟢"),
    PURPLE("PURPLE 🟣", 0xFFD500F9, "🟣"),
    ORANGE("ORANGE 🟠", 0xFFFF6D00, "🟠")
}

enum class TargetType {
    NORMAL,
    CLOCK,      // Adds extra time (+5s)
    BOMB,       // Clears screen
    GOLD_STAR   // Giant score bonus (+30 points)
}

data class ActiveTarget(
    val id: String,
    val xPercent: Float, // 0.05 to 0.95 (safe area)
    val yPercent: Float, // 0.15 to 0.85 (safe area)
    val shape: TargetShape,
    val color: TargetColor,
    val baseSizeDp: Float,
    val spawnTime: Long,
    val durationMs: Long,
    val scale: Float = 0f,
    val alpha: Float = 0f,
    val isPopping: Boolean = false,
    val popTime: Long = 0L,
    val wiggleOffset: Float = 0f,
    val targetType: TargetType = TargetType.NORMAL
)

data class Particle(
    val xPercent: Float,
    val yPercent: Float,
    val vx: Float, // change in xPercent per frame
    val vy: Float, // change in yPercent per frame
    val color: Long,
    val size: Float, // size in dps
    val alpha: Float,
    val rotation: Float = 0f,
    val vRotation: Float = 0f
)

data class TapEffect(
    val id: String,
    val xPercent: Float,
    val yPercent: Float,
    val text: String,
    val color: Long,
    val alpha: Float = 1.0f,
    val particles: List<Particle>
)

data class Sticker(
    val id: String,
    val name: String,
    val emoji: String,
    val description: String,
    val unlockCondition: String
)
