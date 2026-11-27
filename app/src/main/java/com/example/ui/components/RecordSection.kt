package com.example.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.model.PlayerProgress
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonSnakeTheme
import com.example.ui.theme.SolarGold
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.ToxicLime

@Composable
fun RecordSection(
    progress: PlayerProgress,
    accentColor: Color,
    modifier: Modifier = Modifier,
) {
    val collectionRatio = (progress.totalUnlockedItemsCount.toFloat() /
        progress.totalCatalogItemsCount.coerceAtLeast(1)).coerceIn(0f, 1f)

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Text(
            text = stringResource(R.string.record_title),
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
        )
        Spacer(modifier = Modifier.height(14.dp))

        // Collection Progress Card
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(20.dp))
                .background(HudGlassSurface)
                .border(1.dp, accentColor.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                .padding(16.dp),
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = stringResource(R.string.record_collection_progress),
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                )
                Text(
                    text = "${progress.totalUnlockedItemsCount} / ${progress.totalCatalogItemsCount}",
                    style = MaterialTheme.typography.titleMedium,
                    color = accentColor,
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { collectionRatio },
                color = accentColor,
                trackColor = SpaceObsidian,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2x3 Stats Grid
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RecordMetricCard(
                label = stringResource(R.string.record_best_score),
                value = progress.bestScore.toString(),
                valueColor = SolarGold,
                modifier = Modifier.weight(1f),
            )
            RecordMetricCard(
                label = stringResource(R.string.record_total_kills),
                value = progress.totalEliminations.toString(),
                valueColor = ToxicLime,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RecordMetricCard(
                label = stringResource(R.string.record_total_matches),
                value = progress.totalMatches.toString(),
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f),
            )
            RecordMetricCard(
                label = stringResource(R.string.record_best_rank),
                value = if (progress.bestRank > 0) "#${progress.bestRank}" else "-",
                valueColor = accentColor,
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            RecordMetricCard(
                label = stringResource(R.string.record_total_sparks),
                value = progress.totalSparksEarned.toString(),
                valueColor = SolarGold,
                modifier = Modifier.weight(1f),
            )
            RecordMetricCard(
                label = stringResource(R.string.record_food_eaten),
                value = progress.totalFoodEaten.toString(),
                valueColor = TextPrimary,
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(18.dp))

        Text(
            text = stringResource(R.string.record_achievements_title),
            style = MaterialTheme.typography.titleMedium,
            color = TextSecondary,
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 18.dp),
        ) {
            AchievementRow(
                title = stringResource(R.string.ach_first_blood_title),
                description = stringResource(R.string.ach_first_blood_desc),
                unlocked = progress.totalEliminations >= 5,
                accentColor = accentColor,
            )
            AchievementRow(
                title = stringResource(R.string.ach_high_roller_title),
                description = stringResource(R.string.ach_high_roller_desc),
                unlocked = progress.bestScore >= 1500,
                accentColor = accentColor,
            )
            AchievementRow(
                title = stringResource(R.string.ach_collector_title),
                description = stringResource(R.string.ach_collector_desc),
                unlocked = progress.nonStarterUnlockedCount >= 4,
                accentColor = accentColor,
            )
            AchievementRow(
                title = stringResource(R.string.ach_veteran_title),
                description = stringResource(R.string.ach_veteran_desc),
                unlocked = progress.totalMatches >= 5,
                accentColor = accentColor,
            )
        }
    }
}

@Composable
private fun RecordMetricCard(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(HudGlassSurface)
            .border(1.dp, HudBorderStroke, RoundedCornerShape(18.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = valueColor,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AchievementRow(
    title: String,
    description: String,
    unlocked: Boolean,
    accentColor: Color,
) {
    val activeColor = if (unlocked) accentColor else TextSecondary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HudGlassSurface)
            .border(
                width = 1.dp,
                color = if (unlocked) accentColor.copy(alpha = 0.50f) else HudBorderStroke,
                shape = RoundedCornerShape(16.dp),
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(activeColor.copy(alpha = 0.14f)),
        ) {
            Icon(
                imageVector = if (unlocked) Icons.Rounded.EmojiEvents else Icons.Rounded.Lock,
                contentDescription = null,
                tint = activeColor,
                modifier = Modifier.size(20.dp),
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp),
                color = if (unlocked) TextPrimary else TextSecondary,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp),
                color = TextSecondary,
            )
        }
        if (unlocked) {
            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription = null,
                tint = ToxicLime,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 390, heightDp = 700, backgroundColor = 0xFF060812)
@Composable
private fun RecordSectionPreview() {
    NeonSnakeTheme {
        Box(modifier = Modifier.background(SpaceObsidian)) {
            RecordSection(
                progress = PlayerProgress(
                    sparksBalance = 420,
                    totalSparksEarned = 680,
                    bestScore = 2140,
                    totalMatches = 7,
                    totalEliminations = 9,
                    totalFoodEaten = 190,
                    bestRank = 1,
                ),
                accentColor = NeonCyan,
            )
        }
    }
}
