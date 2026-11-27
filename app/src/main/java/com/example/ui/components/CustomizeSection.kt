package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.game.model.EyeStyle
import com.example.game.model.PlayerProgress
import com.example.game.model.SnakeSkin
import com.example.game.model.SnakeSkins
import com.example.game.model.TrailStyle
import com.example.game.model.UiAccentPreset
import com.example.ui.theme.HudBorderStroke
import com.example.ui.theme.HudGlassSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonSnakeTheme
import com.example.ui.theme.SpaceObsidian
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun CustomizeSection(
    progress: PlayerProgress,
    accentColor: Color,
    wavePhase: Float,
    onSelectSkin: (SnakeSkin) -> Unit,
    onSelectTrail: (TrailStyle) -> Unit,
    onSelectEye: (EyeStyle) -> Unit,
    onSelectAccentPreset: (UiAccentPreset) -> Unit,
    modifier: Modifier = Modifier,
) {
    var boostPreviewActive by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.customize_title),
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
            )
            FilterChip(
                selected = boostPreviewActive,
                onClick = { boostPreviewActive = !boostPreviewActive },
                label = { Text(text = stringResource(R.string.customize_preview_boost)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = accentColor.copy(alpha = 0.22f),
                    selectedLabelColor = accentColor,
                    selectedLeadingIconColor = accentColor,
                ),
                modifier = Modifier.testTag("customize_boost_preview_toggle"),
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(155.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(HudGlassSurface)
                .border(1.5.dp, accentColor.copy(alpha = 0.50f), RoundedCornerShape(24.dp)),
        ) {
            CoordinatedSnakePreviewCanvas(
                skin = progress.equippedSkin,
                trailStyle = progress.equippedTrail,
                eyeStyle = progress.equippedEye,
                wavePhase = wavePhase,
                isBoostPreview = boostPreviewActive,
                modifier = Modifier.fillMaxSize(),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. Owned Skins
        SectionHeader(title = stringResource(R.string.customize_section_skin))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            SnakeSkins.all.filter { it.id in progress.unlockedSkinIds }.forEach { skin ->
                val isSelected = skin.id == progress.equippedSkinId
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(listOf(skin.primaryColor, skin.secondaryColor)),
                        )
                        .border(
                            width = if (isSelected) 3.dp else 1.5.dp,
                            color = if (isSelected) Color.White else HudBorderStroke,
                            shape = CircleShape,
                        )
                        .clickable { onSelectSkin(skin) }
                        .testTag("customize_skin_${skin.id}"),
                ) {
                    if (isSelected) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = skin.displayName,
                            tint = SpaceObsidian,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Owned Trails
        SectionHeader(title = stringResource(R.string.customize_section_trail))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            TrailStyle.entries.filter { it.id in progress.unlockedTrailIds }.forEach { trail ->
                val isSelected = trail.id == progress.equippedTrailId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectTrail(trail) },
                    label = { Text(text = stringResource(trail.nameResId)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.22f),
                        selectedLabelColor = accentColor,
                    ),
                    modifier = Modifier.testTag("customize_trail_${trail.id}"),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Owned Eyes
        SectionHeader(title = stringResource(R.string.customize_section_eyes))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
        ) {
            EyeStyle.entries.filter { it.id in progress.unlockedEyeIds }.forEach { eye ->
                val isSelected = eye.id == progress.equippedEyeId
                FilterChip(
                    selected = isSelected,
                    onClick = { onSelectEye(eye) },
                    label = { Text(text = stringResource(eye.nameResId)) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = accentColor.copy(alpha = 0.22f),
                        selectedLabelColor = accentColor,
                    ),
                    modifier = Modifier.testTag("customize_eye_${eye.id}"),
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 4. Color-Coordinated UI & Controls Sync
        SectionHeader(title = stringResource(R.string.customize_section_ui_sync))
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            UiAccentPreset.entries.forEach { preset ->
                val isSelected = preset.id == progress.accentPresetId
                val swatchColor = preset.resolveColor(progress.equippedSkin)
                if (preset == UiAccentPreset.SYNC_WITH_SKIN) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) swatchColor.copy(alpha = 0.20f) else HudGlassSurface)
                            .border(
                                width = if (isSelected) 2.dp else 1.dp,
                                color = if (isSelected) swatchColor else HudBorderStroke,
                                shape = RoundedCornerShape(16.dp),
                            )
                            .clickable { onSelectAccentPreset(preset) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                            .testTag("customize_accent_auto"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = null,
                            tint = swatchColor,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.customize_sync_auto),
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) swatchColor else TextPrimary,
                        )
                    }
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(swatchColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.5.dp,
                                color = if (isSelected) Color.White else HudBorderStroke,
                                shape = CircleShape,
                            )
                            .clickable { onSelectAccentPreset(preset) }
                            .testTag("customize_accent_${preset.id}"),
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Rounded.Palette,
                                contentDescription = preset.name,
                                tint = SpaceObsidian,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        color = TextSecondary,
    )
}

@Preview(showBackground = true, widthDp = 390, heightDp = 700, backgroundColor = 0xFF060812)
@Composable
private fun CustomizeSectionPreview() {
    NeonSnakeTheme {
        Box(modifier = Modifier.background(SpaceObsidian)) {
            CustomizeSection(
                progress = PlayerProgress(
                    unlockedSkinIds = setOf(SnakeSkins.CYBER_CYAN.id, SnakeSkins.PLASMA_MAGENTA.id),
                    unlockedTrailIds = setOf(TrailStyle.CLASSIC_GLOW.id, TrailStyle.PLASMA_SPARKS.id),
                    unlockedEyeIds = setOf(EyeStyle.CYBER_ROUND.id, EyeStyle.NEON_VISOR.id),
                ),
                accentColor = NeonCyan,
                wavePhase = 0.8f,
                onSelectSkin = {},
                onSelectTrail = {},
                onSelectEye = {},
                onSelectAccentPreset = {},
            )
        }
    }
}
