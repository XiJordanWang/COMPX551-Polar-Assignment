package com.example.polar.ui.page

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.polar.data.online.LeaderboardRow
import com.example.polar.data.online.LeaderboardTable
import com.example.polar.data.prefs.PrivacyMode
import com.example.polar.data.prefs.SettingsStore
import com.example.polar.logic.plantStages
import com.example.polar.logic.progressToNextStage
import com.example.polar.logic.stageIndexFor

// "Plant Friends" tab: everyone's plant side by side, ranked by total points
// (from the online leaderboard view, see supabase/schema.sql).
@Composable
fun SocialContent(username: String) {
    val context = LocalContext.current
    val modeFlow = remember { SettingsStore.privacyMode(context, username) }
    val privacyMode by modeFlow.collectAsState(initial = PrivacyMode.FULL)

    // null = not loaded yet or failed
    var rows by remember { mutableStateOf<List<LeaderboardRow>?>(null) }
    var loading by remember { mutableStateOf(true) }

    // Runs every time the tab is opened or mode changes
    LaunchedEffect(privacyMode) {
        if (privacyMode == PrivacyMode.SHARE || privacyMode == PrivacyMode.FULL) {
            rows = LeaderboardTable.getAll()
        } else {
            rows = emptyList()
        }
        loading = false
    }

    Spacer(modifier = Modifier.height(8.dp))

    if (privacyMode == PrivacyMode.READ_ONLY) {
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "🌱 Turn on sharing to see Plant Friends",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "You are currently in Read-only mode. Switch to Share mode on the Profile tab to see the garden and share your plant.",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        return
    }

    val list = rows
    // Where am I in the list? -1 if not found
    val myIndex = list?.indexOfFirst { it.username == username } ?: -1

    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = when {
                loading -> "Loading the garden…"
                list == null -> "Could not load the garden, check your internet"
                list.isEmpty() -> "No one in the garden yet"
                myIndex >= 0 -> "🌳 You are #${myIndex + 1} of ${list.size} gardeners"
                else -> "🌱 Finish a workout to join the garden"
            },
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold
        )
    }

    if (!list.isNullOrEmpty()) {
        Spacer(modifier = Modifier.height(20.dp))
        Podium(top = list.take(3), username = username)

        Spacer(modifier = Modifier.height(20.dp))
        for ((index, row) in list.withIndex()) {
            RankRow(rank = index + 1, row = row, isMe = row.username == username)
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

// Top 3 on a podium: 2nd on the left, 1st in the middle (biggest), 3rd on the right
@Composable
fun Podium(top: List<LeaderboardRow>, username: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        if (top.size >= 2) PodiumSpot(top[1], rank = 2, plantSize = 96.dp, blockHeight = 50.dp, isMe = top[1].username == username)
        PodiumSpot(top[0], rank = 1, plantSize = 120.dp, blockHeight = 72.dp, isMe = top[0].username == username)
        if (top.size >= 3) PodiumSpot(top[2], rank = 3, plantSize = 84.dp, blockHeight = 36.dp, isMe = top[2].username == username)
    }
}

@Composable
fun PodiumSpot(row: LeaderboardRow, rank: Int, plantSize: Dp, blockHeight: Dp, isMe: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PlantImage(
            stageIndex = stageIndexFor(row.totalPoints),
            progress = progressToNextStage(row.totalPoints),
            modifier = Modifier.size(plantSize)
        )
        Text(
            text = if (isMe) "${row.firstName} (you)" else row.firstName,
            color = Color.White,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
        Text(text = "${row.totalPoints} pts · 🔥 ${row.streak}", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))
        // The podium block, taller for a better rank
        Box(
            modifier = Modifier
                .width(96.dp)
                .height(blockHeight)
                .background(Color.White.copy(alpha = 0.3f), RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text = medal(rank), fontSize = 24.sp)
        }
    }
}

// One line in the full list. My own line has a white outline.
@Composable
fun RankRow(rank: Int, row: LeaderboardRow, isMe: Boolean) {
    val stage = plantStages[stageIndexFor(row.totalPoints)]
    val outline = if (isMe) Modifier.border(2.dp, Color.White, RoundedCornerShape(24.dp)) else Modifier

    GlassCard(modifier = outline.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = medal(rank),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(40.dp)
            )
            PlantImage(
                stageIndex = stageIndexFor(row.totalPoints),
                progress = progressToNextStage(row.totalPoints),
                modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isMe) "${row.firstName} (you)" else row.firstName,
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${stage.name} · ${row.workouts} " + if (row.workouts == 1) "workout" else "workouts" + " · 🔥 ${row.streak}",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 13.sp
                )
            }
            Text(text = "${row.totalPoints}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            Text(text = " pts", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
        }
    }
}

// 1 -> 🥇, 2 -> 🥈, 3 -> 🥉, others -> "#4"
fun medal(rank: Int): String {
    return when (rank) {
        1 -> "🥇"
        2 -> "🥈"
        3 -> "🥉"
        else -> "#$rank"
    }
}
