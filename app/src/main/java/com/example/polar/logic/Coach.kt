package com.example.polar.logic

import kotlin.random.Random

// How the coach talks to the user. The user will pick one in Settings (later).
enum class CoachMode {
    SUPPORTIVE,  // kind and encouraging
    BULLY,       // playful teasing (never insulting)
    MIXED,       // sometimes supportive, sometimes bully
    OFF          // no messages at all
}

// The event that makes the coach send a message
enum class CoachTrigger {
    INACTIVE,        // heart rate stays near the resting baseline for too long during a workout
    GOAL_REACHED,    // the user reached their daily goal
    STREAK_AT_RISK   // the user has a streak, but no workout today yet
}

// ---------- Message pools ----------
// 4 messages for each trigger. MIXED uses both pools. OFF has no messages.

val supportiveMessages = mapOf(
    CoachTrigger.INACTIVE to listOf(
        "Take a deep breath and pick up the pace a little. You can do it!",
        "A little more movement and your plant will feel it 🌱",
        "Every step counts. Let's get that heart rate up together!",
        "Short break? No problem. Ready when you are!"
    ),
    CoachTrigger.GOAL_REACHED to listOf(
        "Yeah! You got it, your plant can thrive and become larger than others!",
        "Goal reached! Your plant is so proud of you 🌸",
        "Amazing work today. Time to rest and grow!",
        "You did it! Another step closer to a blooming plant."
    ),
    CoachTrigger.STREAK_AT_RISK to listOf(
        "Your plant is close to blooming!",
        "A short workout today keeps your streak alive 🔥",
        "Don't lose your streak. Even 10 minutes helps!",
        "Your plant is waiting for today's water. One workout is enough 💧"
    )
)

// Playful teasing only: no insults, no comments about the user's body or skills
val bullyMessages = mapOf(
    CoachTrigger.INACTIVE to listOf(
        "Hello? Your plant is missing you. Start moving!",
        "Is this a workout or a nap? Your plant is asking 🥱",
        "Your plant just yawned. Show it some action!",
        "Even a snail is overtaking you right now 🐌 Let's go!"
    ),
    CoachTrigger.GOAL_REACHED to listOf(
        "Okay, okay, not bad. Your plant is impressed. A little.",
        "Wow, you actually did it! Your plant owes you one.",
        "Goal done. Fine, you can have a snack now 🍪",
        "Look who's showing off! Your plant is doing a happy dance 🌿"
    ),
    CoachTrigger.STREAK_AT_RISK to listOf(
        "Your streak is crying in the corner 😢 Go save it!",
        "Tick tock! Your streak ends at midnight ⏰",
        "Your plant is getting thirsty and dramatic. Water it with a workout!",
        "Don't make your plant write a sad poem about you 📝"
    )
)

// ---------- Picking a message ----------

// Picks one message for this mode and trigger.
// - OFF: returns null, so no message is sent
// - MIXED: picks from the supportive and the bully messages together
// - never returns lastMessage, so the user doesn't see the same text twice in a row
// random is a parameter, so tests can use Random(1) and always get the same result.
fun pickMessage(mode: CoachMode, trigger: CoachTrigger, random: Random, lastMessage: String?): String? {
    val choices = when (mode) {
        CoachMode.SUPPORTIVE -> supportiveMessages[trigger] ?: emptyList()
        CoachMode.BULLY -> bullyMessages[trigger] ?: emptyList()
        CoachMode.MIXED -> (supportiveMessages[trigger] ?: emptyList()) + (bullyMessages[trigger] ?: emptyList())
        CoachMode.OFF -> return null
    }

    // Remove the message we showed last time
    val allowed = choices.filter { it != lastMessage }
    if (allowed.isEmpty()) {
        return null
    }

    // Pick one at random: nextInt(size) gives 0 .. size - 1
    return allowed[random.nextInt(allowed.size)]
}
